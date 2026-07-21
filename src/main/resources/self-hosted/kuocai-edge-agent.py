#!/usr/bin/env python3
import hashlib
import ipaddress
import json
import os
import re
import shutil
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import urllib.parse

CONFIG_FILE = "/etc/kuocai-edge/agent.json"
RELEASE_ROOT = "/etc/kuocai-edge/releases"
ACTIVE_LINK = "/etc/nginx/kuocai-edge"
STREAM_ACTIVE_LINK = "/etc/nginx/kuocai-edge-stream"
CACHE_DIR = "/var/cache/kuocai-cdn"


def load_agent_config():
    with open(CONFIG_FILE, "r", encoding="utf-8") as stream:
        return json.load(stream)


def api_request(config, method, path, payload=None):
    url = config["controlPlane"].rstrip("/") + path
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Authorization", "Bearer " + config["token"])
    request.add_header("X-Kuocai-Node-Id", str(config["nodeId"]))
    request.add_header("Accept", "application/json")
    if data is not None:
        request.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(request, timeout=30) as response:
        body = json.loads(response.read().decode("utf-8"))
    if body.get("code") != "SUCCESS":
        raise RuntimeError(body.get("message") or "control plane request failed")
    return body.get("data")


def safe_name(domain):
    value = domain.lower().strip()
    host = value[2:] if value.startswith("*.") else value[1:] if value.startswith(".") else value
    labels = host.split(".")
    valid_labels = all(
        label and len(label) <= 63 and label[0].isalnum() and label[-1].isalnum()
        and all(ch.isalnum() or ch == "-" for ch in label)
        for label in labels
    )
    if (not host or len(host) > 253 or len(labels) < 2 or not valid_labels
            or any(ord(ch) > 127 for ch in host)):
        raise ValueError("invalid domain name")
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:20]


def nginx_quote(value):
    value = str(value or "")
    if any(ch in value for ch in "\r\n{};$`\\\""):
        raise ValueError("unsafe nginx value")
    return value


def nginx_string(value):
    value = str(value or "")
    if any(ch in value for ch in "\r\n{}\x00"):
        raise ValueError("unsafe nginx string")
    return '"%s"' % value.replace("\\", "\\\\").replace('"', '\\"')


def json_config(value):
    if isinstance(value, dict):
        return value
    if not value:
        return {}
    try:
        parsed = json.loads(value)
        return parsed if isinstance(parsed, dict) else {}
    except Exception:
        return {}


def safe_header_name(value):
    value = str(value or "").strip()
    if not re.match(r"^[A-Za-z][A-Za-z0-9-]{0,99}$", value):
        raise ValueError("invalid HTTP header name")
    return value


def safe_url(value):
    value = str(value or "").strip()
    parsed = urllib.parse.urlsplit(value)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise ValueError("invalid HTTP URL")
    return nginx_quote(value)


def normalize_switch(value, default="off"):
    return "on" if str(value or default).lower() == "on" else "off"


def origin_protocol(domain, incoming_protocol=None):
    protocol = domain.get("originProtocol", "http")
    if protocol == "follow":
        protocol = incoming_protocol or "http"
    return "https" if protocol == "https" else "http"


def origin_endpoint(address, port):
    address = str(address or "").strip()
    if not address or any(ch in address for ch in "\r\n{};$`\\/\""):
        raise ValueError("invalid origin address")
    if ":" in address and not address.startswith("["):
        address = "[" + address + "]"
    return "%s:%d" % (address, int(port))


def origin_url(domain):
    protocol = origin_protocol(domain)
    port = domain.get("httpsPort", 443) if protocol == "https" else domain.get("httpPort", 80)
    address = str(domain.get("originAddress", "")).split(";")[0].strip()
    return "%s://%s" % (protocol, origin_endpoint(address, port))


def origin_upstream(domain, name, origin_config, protocol_override=None):
    protocol = origin_protocol(domain, protocol_override)
    port = int(domain.get("httpsPort", 443) if protocol == "https" else domain.get("httpPort", 80))
    addresses = [item.strip() for item in str(domain.get("originAddress") or "").replace(",", ";").split(";") if item.strip()]
    if not addresses:
        raise ValueError("origin address is empty")
    upstream_name = "kuocai_origin_" + name
    lines = ["upstream %s {" % upstream_name]
    for address in addresses:
        lines.append("    server %s;" % origin_endpoint(address, port))
    standby = origin_config.get("standby") or {}
    standby_address = standby.get("ipOrDomain") or standby.get("ip_or_domain")
    if standby_address:
        standby_port = standby.get("httpsPort", 443) if protocol == "https" else standby.get("httpPort", 80)
        for address in str(standby_address).replace(",", ";").split(";"):
            if address.strip():
                lines.append("    server %s backup;" % origin_endpoint(address.strip(), standby_port))
    lines.extend(["    keepalive 32;", "}"])
    return lines, "%s://%s" % (protocol, upstream_name)


def ttl_seconds(rule):
    value = max(0, min(int(rule.get("ttl") or 0), 31536000))
    unit = str(rule.get("ttl_unit") or "s").lower()
    return min(value * {"s": 1, "m": 60, "h": 3600, "d": 86400}.get(unit, 1), 31536000)


def safe_path(value):
    value = nginx_quote(value.strip())
    if not value.startswith("/") or any(ch not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~/%" for ch in value):
        raise ValueError("unsafe cache path")
    return value


def cache_key_line(config):
    config = config or {}
    if config.get("_querySuffixVariable"):
        return "        proxy_cache_key $scheme$proxy_host$uri%s;" % config.get("_querySuffixVariable")
    mode = str(config.get("url_parameter_type") or config.get("urlParameterType") or "")
    values = str(config.get("url_parameter_value") or config.get("urlParameterValue") or "")
    if not mode:
        ignore = config.get("ignoreQueryString") or {}
        if normalize_switch(ignore.get("enable")) == "on":
            mode = "reserve_params" if ignore.get("type") == "allow" else "del_params"
            values = str(ignore.get("hashKeyArgs") or "")
    if mode == "ignore_url_params":
        return "        proxy_cache_key $scheme$proxy_host$uri;"
    if mode == "reserve_params":
        parameters = []
        for value in re.split(r"[,;，；]", values):
            value = value.strip()
            if value and re.match(r"^[A-Za-z0-9_-]{1,64}$", value):
                parameters.append("%s=$arg_%s" % (value, value))
        suffix = "&".join(parameters)
        return "        proxy_cache_key %s;" % nginx_string("$scheme$proxy_host$uri?" + suffix)
    # Nginx cannot safely remove arbitrary unknown parameters without Lua/njs.
    # Keep the full query string for del_params so different resources never share a cache entry.
    return "        proxy_cache_key $scheme$proxy_host$uri$is_args$args;"


def query_filter_maps(cache, name):
    maps = []
    candidates = [("global", cache)]
    for index, rule in enumerate(cache.get("cacheRules") or cache.get("cache_rules") or []):
        candidates.append(("r%d" % index, rule))
    for label, config in candidates:
        mode = str(config.get("url_parameter_type") or config.get("urlParameterType") or "")
        values = str(config.get("url_parameter_value") or config.get("urlParameterValue") or "")
        if label == "global" and not mode:
            ignore = config.get("ignoreQueryString") or {}
            if normalize_switch(ignore.get("enable")) == "on" and ignore.get("type") == "block":
                mode = "del_params"
                values = str(ignore.get("hashKeyArgs") or "")
        parameters = [value for value in split_values(values)
                      if re.match(r"^[A-Za-z0-9_-]{1,64}$", value)]
        if mode != "del_params" or not parameters:
            continue
        source = "$args"
        prefix = "kuocai_q_%s_%s" % (name[:10], label)
        for index, parameter in enumerate(parameters):
            target = "$%s_%d" % (prefix, index)
            capture = "q%s%s%d" % (name[:6], label.replace("_", ""), index)
            escaped = re.escape(parameter)
            maps.extend([
                "map %s %s {" % (source, target),
                "    default %s;" % source,
                "    ~^%s=[^&]*&?(?<%sa>.*)$ $%sa;" % (escaped, capture, capture),
                "    ~^(?<%sb>.+)&%s=[^&]*$ $%sb;" % (capture, escaped, capture),
                "    ~^(?<%sb>.+)&%s=[^&]*&(?<%sa>.*)$ $%sb&$%sa;" %
                (capture, escaped, capture, capture, capture),
                "}",
            ])
            source = target
        suffix = "$%s_suffix" % prefix
        maps.extend(["map %s %s {" % (source, suffix), "    default ?%s;" % source,
                     '    "" "";', "}"])
        config["_querySuffixVariable"] = suffix
    return maps


def rewrite_lines(origin_config):
    result = []
    rules = origin_config.get("originRequestUrlRewrite") or []
    for rule in sorted(rules, key=lambda item: int(item.get("priority") or 0), reverse=True):
        match_type = str(rule.get("match_type") or rule.get("matchType") or "")
        source = str(rule.get("source_url") or rule.get("sourceUrl") or "")
        target = str(rule.get("target_url") or rule.get("targetUrl") or "")
        if not target.startswith("/") or not re.match(r"^/[A-Za-z0-9._~/%$-]*$", target):
            raise ValueError("invalid origin rewrite target")
        if match_type == "all":
            pattern = "^/.*$"
        elif match_type == "wildcard" and source.startswith("/"):
            chunks = source.split("*")
            pattern = "^" + "(.*)".join(re.escape(chunk) for chunk in chunks) + "$"
        elif source.startswith("/") and re.match(r"^/[A-Za-z0-9._~/%-]*$", source):
            pattern = "^" + re.escape(source) + ("$" if match_type == "full_path" else "")
        else:
            raise ValueError("invalid origin rewrite source")
        result.append("        rewrite %s %s break;" % (pattern, target))
    return result


def origin_header_lines(origin_config):
    result = []
    for item in origin_config.get("originRequestHeader") or []:
        name = safe_header_name(item.get("name"))
        action = str(item.get("action") or "set").lower()
        value = "" if action == "delete" else item.get("value")
        result.append("        proxy_set_header %s %s;" % (name, nginx_string(value)))
    return result


def response_header_lines(advanced):
    result = []
    for item in advanced.get("httpResponseHeaders") or []:
        name = safe_header_name(item.get("name"))
        if str(item.get("action") or "set").lower() == "delete":
            result.append("        proxy_hide_header %s;" % name)
        else:
            result.append("        add_header %s %s always;" % (name, nginx_string(item.get("value"))))
    return result


def proxy_location(selector, ttl, origin, origin_host, error_rules, options, cache_rule=None):
    origin_config = options.get("origin") or {}
    timeout = max(1, min(int(origin_config.get("originReceiveTimeout") or 30), 300))
    cache_settings = dict(options.get("cache") or {})
    if cache_rule:
        cache_settings.update(cache_rule)
    lines = ["    location %s {" % selector,
             "        proxy_set_header Host %s;" % origin_host,
             "        proxy_set_header X-Real-IP $remote_addr;",
             "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;",
             "        proxy_set_header X-Forwarded-Proto $scheme;",
             "        proxy_http_version 1.1;",
             "        proxy_connect_timeout %ds;" % timeout,
             "        proxy_read_timeout %ds;" % timeout,
             "        proxy_send_timeout %ds;" % timeout,
             "        proxy_cache kuocai_edge_cache;",
             cache_key_line(cache_settings)]
    lines.extend(origin_header_lines(origin_config))
    lines.extend(rewrite_lines(origin_config))
    if normalize_switch(origin_config.get("rangeStatus"), "on") == "on":
        lines.append("        proxy_force_ranges on;")
    if normalize_switch(origin_config.get("etagStatus")) == "on":
        lines.append("        proxy_cache_revalidate on;")
    if origin.startswith("https://"):
        lines.extend(["        proxy_ssl_server_name on;", "        proxy_ssl_name %s;" % origin_host])
    if normalize_switch(origin_config.get("followRedirectStatus")) == "on":
        lines.extend(["        proxy_intercept_errors on;",
                      "        error_page 301 302 307 308 = @kuocai_follow_redirect_1;"])
    if ttl <= 0:
        lines.extend(["        proxy_cache_bypass 1;", "        proxy_no_cache 1;"])
    else:
        lines.append("        proxy_cache_valid 200 206 %ds;" % ttl)
    for rule in error_rules:
        code = int(rule.get("code") or 0)
        error_ttl = max(0, min(int(rule.get("ttl") or 0), 31536000))
        if code in (400, 403, 404, 405, 414, 500, 501, 502, 503, 504):
            lines.append("        proxy_cache_valid %d %ds;" % (code, error_ttl))
    lines.extend(response_header_lines(options.get("advanced") or {}))
    lines.extend(["        add_header X-Kuocai-Cache $upstream_cache_status always;",
                  "        proxy_pass %s;" % origin,
                  "    }"])
    return lines


def cache_locations(cache, origin, origin_host, options):
    rules = cache.get("cacheRules") or cache.get("cache_rules") or []
    error_rules = cache.get("errorCodeCache") or cache.get("error_code_cache") or []
    default_ttl = max(0, min(int(cache.get("defaultTtl", 3600)), 31536000))
    locations = []
    for rule in sorted(rules, key=lambda item: int(item.get("priority") or 0), reverse=True):
        match_type = rule.get("match_type") or rule.get("matchType")
        values = str(rule.get("match_value") or rule.get("matchValue") or "").replace(",", ";").split(";")
        ttl = ttl_seconds(rule)
        if match_type == "all":
            default_ttl = ttl
        elif match_type == "file_extension":
            extensions = [value.strip().lstrip(".") for value in values if value.strip().lstrip(".").isalnum()]
            if extensions:
                locations.extend(proxy_location("~* \\.(%s)$" % "|".join(extensions), ttl, origin, origin_host, error_rules, options, rule))
        elif match_type == "catalog":
            for value in values:
                if value.strip():
                    locations.extend(proxy_location("^~ " + safe_path(value), ttl, origin, origin_host, error_rules, options, rule))
        elif match_type in ("full_path", "home_page"):
            paths = ["/"] if match_type == "home_page" else values
            for value in paths:
                if value.strip() and "*" not in value:
                    locations.extend(proxy_location("= " + safe_path(value), ttl, origin, origin_host, error_rules, options, rule))
    locations.extend(proxy_location("/", default_ttl, origin, origin_host, error_rules, options))
    return locations


def split_values(value):
    if isinstance(value, list):
        values = value
    else:
        values = re.split(r"[,;\r\n，；]", str(value or ""))
    return [str(item).strip() for item in values if str(item).strip()]


def wildcard_regex(values):
    patterns = []
    for value in values:
        if len(value) > 200:
            raise ValueError("access rule is too long")
        patterns.append(re.escape(value).replace(r"\*", ".*"))
    return "(?:%s)" % "|".join(patterns)


def access_server_lines(access):
    lines = []
    referer = access.get("referer") or {}
    referer_type = int(referer.get("referer_type") or referer.get("refererType") or 0)
    referers = split_values(referer.get("referers") or referer.get("referer_list"))
    include_empty = bool(referer.get("include_empty", True))
    if referer_type == 2:
        valid_referers = []
        if include_empty:
            valid_referers.append("none")
        for value in referers:
            value = value.replace("http://", "").replace("https://", "").split("/", 1)[0]
            if not re.match(r"^(?:\*\.)?[A-Za-z0-9.-]+(?::[0-9]{1,5})?$", value):
                raise ValueError("invalid referer whitelist value")
            valid_referers.append(value)
        if valid_referers:
            lines.extend(["    valid_referers %s;" % " ".join(valid_referers),
                          "    if ($invalid_referer) { return 403; }"])
        else:
            lines.append("    return 403;")
    elif referer_type == 1:
        if referers:
            lines.append("    if ($http_referer ~* %s) { return 403; }" % nginx_string(wildcard_regex(referers)))
        if include_empty:
            lines.append('    if ($http_referer = "") { return 403; }')

    ip_type = int(access.get("ipType") or 0)
    ips = split_values(access.get("ips"))
    normalized_ips = []
    for value in ips:
        try:
            normalized_ips.append(str(ipaddress.ip_network(value, strict=False)))
        except ValueError:
            raise ValueError("invalid IP access rule")
    if ip_type == 1:
        lines.extend("    deny %s;" % value for value in normalized_ips)
    elif ip_type == 2:
        lines.extend("    allow %s;" % value for value in normalized_ips)
        lines.append("    deny all;")

    user_agent = access.get("userAgent") or {}
    ua_type = int(user_agent.get("type") or 0)
    ua_values = split_values(user_agent.get("ua_list") or user_agent.get("uaList"))
    if ua_type == 1 and ua_values:
        lines.append("    if ($http_user_agent ~* %s) { return 403; }" % nginx_string(wildcard_regex(ua_values)))
    elif ua_type == 2:
        if ua_values:
            lines.append("    if ($http_user_agent !~* %s) { return 403; }" % nginx_string(wildcard_regex(ua_values)))
        else:
            lines.append("    return 403;")

    url_auth = access.get("urlAuth") or {}
    if normalize_switch(url_auth.get("status")) == "on":
        primary_key = str(url_auth.get("primary_key") or url_auth.get("primaryKey") or "")
        if not primary_key or len(primary_key) > 128:
            raise ValueError("URL auth primary key is invalid")
        auth_type = str(url_auth.get("type") or "typeA")
        signature_arg = "$arg_sign" if auth_type == "typeA" else "$arg_auth_key"
        expires_arg = "$arg_t" if auth_type == "typeA" else "$arg_timestamp"
        lines.extend(["    secure_link %s,%s;" % (signature_arg, expires_arg),
                      "    secure_link_md5 %s;" % nginx_string(primary_key + "$secure_link_expires$uri"),
                      '    if ($secure_link = "") { return 403; }',
                      '    if ($secure_link = "0") { return 410; }'])
    return lines


def advanced_server_lines(advanced):
    lines = []
    compress = advanced.get("compress") or {}
    if normalize_switch(compress.get("status"), "on") == "on":
        lines.extend(["    gzip on;", "    gzip_vary on;", "    gzip_min_length 1024;",
                      "    gzip_types text/plain text/css application/json application/javascript "
                      "application/xml application/xml+rss image/svg+xml;"])
    else:
        lines.append("    gzip off;")
    for item in advanced.get("errorCodeRedirectRules") or []:
        code = int(item.get("error_code") or item.get("errorCode") or 0)
        target_code = int(item.get("target_code") or item.get("targetCode") or 302)
        if code in (400, 403, 404, 405, 406, 414, 416, 451, 500, 501, 502, 503, 504) and target_code in (301, 302):
            lines.append("    error_page %d =%d %s;" % (code, target_code,
                                                        safe_url(item.get("target_link") or item.get("targetLink"))))
    for item in advanced.get("errorPages") or []:
        code = int(item.get("errorHttpCode") or 0)
        if code in (400, 403, 404, 405, 406, 414, 416, 500, 501, 502, 503, 504):
            lines.append("    error_page %d =302 %s;" % (code, safe_url(item.get("customPageUrl"))))
    return lines


def flexible_origin_locations(domain, origin_config, cache, origin_host, options, protocol_override=None):
    locations = []
    error_rules = cache.get("errorCodeCache") or cache.get("error_code_cache") or []
    protocol = origin_protocol(domain, protocol_override)
    port = domain.get("httpsPort", 443) if protocol == "https" else domain.get("httpPort", 80)
    for rule in sorted(origin_config.get("flexibleOrigins") or [],
                       key=lambda item: int(item.get("priority") or 0), reverse=True):
        sources = rule.get("back_sources") or rule.get("backSources") or []
        if not sources:
            continue
        address = sources[0].get("ip_or_domain") or sources[0].get("ipOrDomain")
        target_origin = "%s://%s" % (protocol, origin_endpoint(address, port))
        match_type = rule.get("match_type") or rule.get("matchType")
        values = split_values(rule.get("match_pattern") or rule.get("matchPattern"))
        selectors = []
        if match_type == "file_extension":
            extensions = [value.lstrip(".") for value in values if re.match(r"^[A-Za-z0-9_-]+$", value.lstrip("."))]
            if extensions:
                selectors.append("~* \\.(%s)$" % "|".join(extensions))
        elif match_type in ("file_path", "catalog"):
            selectors.extend("^~ " + safe_path(value) for value in values)
        elif match_type == "full_path":
            selectors.extend("= " + safe_path(value) for value in values if "*" not in value)
        for selector in selectors:
            locations.extend(proxy_location(selector, int(cache.get("defaultTtl", 3600)), target_origin,
                                            origin_host, error_rules, options))
    return locations


def default_flexible_origin(domain, origin_config, fallback, protocol_override=None):
    protocol = origin_protocol(domain, protocol_override)
    port = domain.get("httpsPort", 443) if protocol == "https" else domain.get("httpPort", 80)
    for rule in sorted(origin_config.get("flexibleOrigins") or [],
                       key=lambda item: int(item.get("priority") or 0), reverse=True):
        match_type = rule.get("match_type") or rule.get("matchType")
        sources = rule.get("back_sources") or rule.get("backSources") or []
        if match_type == "all" and sources:
            address = sources[0].get("ip_or_domain") or sources[0].get("ipOrDomain")
            return "%s://%s" % (protocol, origin_endpoint(address, port))
    return fallback


def follow_redirect_location(origin_host, origin_config):
    if normalize_switch(origin_config.get("followRedirectStatus")) != "on":
        return []
    timeout = max(1, min(int(origin_config.get("originReceiveTimeout") or 30), 300))
    max_times = max(1, min(int(origin_config.get("followRedirectMaxTimes") or 1), 5))
    lines = []
    for index in range(1, max_times + 1):
        lines.extend(["    location @kuocai_follow_redirect_%d {" % index,
                      "        resolver 223.5.5.5 119.29.29.29 valid=300s;",
                      "        proxy_set_header Host $proxy_host;",
                      "        proxy_set_header X-Real-IP $remote_addr;",
                      "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;",
                      "        proxy_connect_timeout %ds;" % timeout,
                      "        proxy_read_timeout %ds;" % timeout,
                      "        proxy_ssl_server_name on;"])
        if index < max_times:
            lines.extend(["        proxy_intercept_errors on;",
                          "        error_page 301 302 307 308 = @kuocai_follow_redirect_%d;" % (index + 1)])
        lines.extend(["        proxy_pass $upstream_http_location;", "    }"])
    return lines


def tls_protocols(value):
    supported = {"TLSv1.0": "TLSv1", "TLSv1.1": "TLSv1.1",
                 "TLSv1.2": "TLSv1.2", "TLSv1.3": "TLSv1.3"}
    protocols = []
    for item in split_values(value or "TLSv1.2,TLSv1.3"):
        if item in supported and supported[item] not in protocols:
            protocols.append(supported[item])
    return protocols or ["TLSv1.2", "TLSv1.3"]


def server_common(domain_name, origin_host, domain, origin_config, cache, access,
                  advanced, origin, protocol_override=None):
    options = {"origin": origin_config, "cache": cache, "advanced": advanced}
    actual_origin = default_flexible_origin(domain, origin_config, origin, protocol_override)
    locations = flexible_origin_locations(domain, origin_config, cache, origin_host,
                                          options, protocol_override)
    locations.extend(cache_locations(cache, actual_origin, origin_host, options))
    locations.extend(follow_redirect_location(origin_host, origin_config))
    common = ["    server_name %s;" % domain_name,
              "    access_log /var/log/nginx/kuocai-edge-access.log kuocai_edge;"]
    common.extend(access_server_lines(access))
    common.extend(advanced_server_lines(advanced))
    common.extend(locations)
    return common


def write_domain_config(release_dir, domain):
    name = safe_name(domain["domainName"])
    domain_name = nginx_quote(domain["domainName"])
    origin_host = nginx_quote(domain.get("originHost") or domain_name)
    origin_config = json_config(domain.get("originConfigJson"))
    cache = json_config(domain.get("cacheConfigJson"))
    access = json_config(domain.get("accessConfigJson"))
    advanced = json_config(domain.get("advancedConfigJson"))
    https_config = json_config(domain.get("httpsConfigJson"))
    query_maps = query_filter_maps(cache, name)
    follows_request = str(domain.get("originProtocol") or "http").lower() == "follow"
    upstream, http_origin = origin_upstream(domain, name + "_http" if follows_request else name,
                                            origin_config, "http" if follows_request else None)
    https_origin = http_origin
    if follows_request and int(domain.get("httpsEnabled") or 0) == 1:
        https_upstream, https_origin = origin_upstream(domain, name + "_https", origin_config, "https")
        upstream.extend([""] + https_upstream)
    http_common = server_common(domain_name, origin_host, domain, origin_config, cache,
                                access, advanced, http_origin, "http")
    http_listeners = ["    listen 80;"]
    if int(domain.get("ipv6Enabled") or 0) == 1:
        http_listeners.append("    listen [::]:80;")
    blocks = [*query_maps, "", *upstream, "", "server {", *http_listeners, *http_common, "}"]
    if int(domain.get("httpsEnabled") or 0) == 1:
        certificate = domain.get("certificate") or ""
        private_key = domain.get("privateKey") or ""
        if not certificate or not private_key:
            raise ValueError("HTTPS certificate is incomplete for " + domain_name)
        cert_path = os.path.join(release_dir, name + ".crt")
        key_path = os.path.join(release_dir, name + ".key")
        with open(cert_path, "w", encoding="utf-8") as stream:
            stream.write(certificate)
        with open(key_path, "w", encoding="utf-8") as stream:
            stream.write(private_key)
        os.chmod(key_path, 0o600)
        if domain.get("forceRedirect") == "on":
            redirect_code = int(https_config.get("redirectCode") or 301)
            if redirect_code not in (301, 302):
                redirect_code = 301
            blocks = [
                *query_maps,
                "",
                *upstream,
                "",
                "server {",
                *http_listeners,
                "    server_name %s;" % domain_name,
                "    return %d https://$host$request_uri;" % redirect_code,
                "}",
            ]
        https_listen = "    listen 443 ssl"
        if normalize_switch(https_config.get("http2Status"), "on") == "on":
            https_listen += " http2"
        https_listeners = [https_listen + ";"]
        if int(domain.get("ipv6Enabled") or 0) == 1:
            https_listeners.append(https_listen.replace("443", "[::]:443", 1) + ";")
        ssl_lines = [
            "    ssl_certificate %s;" % cert_path,
            "    ssl_certificate_key %s;" % key_path,
            "    ssl_protocols %s;" % " ".join(tls_protocols(https_config.get("tlsVersion"))),
        ]
        if normalize_switch(https_config.get("ocspStaplingStatus")) == "on":
            ssl_lines.extend(["    ssl_stapling on;", "    ssl_stapling_verify on;"])
        https_common = server_common(domain_name, origin_host, domain, origin_config, cache,
                                     access, advanced, https_origin, "https")
        blocks.extend([
            "server {",
            *https_listeners,
            *ssl_lines,
            *https_common,
            "}",
        ])
    with open(os.path.join(release_dir, name + ".conf"), "w", encoding="utf-8") as stream:
        stream.write("\n".join(blocks) + "\n")


def write_port_forward_config(release_dir, rule):
    rule_id = int(rule.get("id") or rule.get("ruleId") or 0)
    if rule_id <= 0:
        raise ValueError("invalid port forward rule id")
    protocol = str(rule.get("protocol") or "").lower()
    if protocol not in ("tcp", "udp"):
        raise ValueError("unsupported port forward protocol")
    listen_port = int(rule.get("listenPort") or 0)
    origin_port = int(rule.get("originPort") or 0)
    if not 1 <= listen_port <= 65535 or not 1 <= origin_port <= 65535:
        raise ValueError("invalid port forward port")
    origin_host = origin_endpoint(rule.get("originHost"), origin_port)
    upstream_name = "kuocai_port_forward_%d" % rule_id
    lines = ["upstream %s {" % upstream_name,
             "    server %s;" % origin_host,
             "}", "", "server {"]
    listen = "    listen %d" % listen_port
    if protocol == "udp":
        listen += " udp reuseport"
    lines.extend([listen + ";",
                  "    proxy_connect_timeout 10s;",
                  "    proxy_timeout 1h;",
                  "    proxy_pass %s;" % upstream_name,
                  "    access_log /var/log/nginx/kuocai-edge-stream-access.log kuocai_edge_stream;",
                  "}"])
    with open(os.path.join(release_dir, "port-forward-%d.conf" % rule_id), "w", encoding="utf-8") as stream:
        stream.write("\n".join(lines) + "\n")


def install_base_config():
    os.makedirs(CACHE_DIR, exist_ok=True)
    # 节点没有匹配到已下发域名时，禁止落到系统默认欢迎页，避免暴露操作系统信息。
    for default_path in ("/etc/nginx/conf.d/default.conf", "/etc/nginx/sites-enabled/default"):
        try:
            if os.path.lexists(default_path):
                os.unlink(default_path)
        except OSError:
            pass
    remove_distribution_default_server()
    path = "/etc/nginx/conf.d/kuocai-edge-base.conf"
    content = """proxy_cache_path /var/cache/kuocai-cdn levels=1:2 keys_zone=kuocai_edge_cache:100m max_size=50g inactive=7d use_temp_path=off;
log_format kuocai_edge '$msec $host $status $body_bytes_sent $upstream_cache_status $request_time';
include /etc/nginx/kuocai-edge/*.conf;
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    return 444;
}
"""
    if not os.path.exists(path) or open(path, "r", encoding="utf-8").read() != content:
        with open(path, "w", encoding="utf-8") as stream:
            stream.write(content)


def remove_distribution_default_server():
    path = "/etc/nginx/nginx.conf"
    if not os.path.exists(path):
        return
    try:
        with open(path, "r", encoding="utf-8") as stream:
            lines = stream.readlines()
        kept = []
        index = 0
        changed = False
        while index < len(lines):
            if lines[index].strip() != "server {":
                kept.append(lines[index])
                index += 1
                continue
            end = index
            depth = 0
            while end < len(lines):
                depth += lines[end].count("{") - lines[end].count("}")
                end += 1
                if depth == 0:
                    break
            block = "".join(lines[index:end])
            if "server_name  _;" in block and "root         /usr/share/nginx/html;" in block:
                changed = True
                index = end
            else:
                kept.extend(lines[index:end])
                index = end
        if changed:
            with open(path, "w", encoding="utf-8") as stream:
                stream.writelines(kept)
    except (OSError, UnicodeError):
        return


def ensure_stream_module():
    probe = subprocess.run(["nginx", "-V"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                           universal_newlines=True)
    output = probe.stdout or ""
    if "--with-stream" not in output:
        raise RuntimeError("当前 Nginx 未启用 stream 模块，无法配置 TCP/UDP 端口转发，请重新安装节点 Agent")
    if "--with-stream=dynamic" not in output:
        return
    config_probe = subprocess.run(["nginx", "-T"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                  universal_newlines=True)
    if "ngx_stream_module.so" in (config_probe.stdout or ""):
        return
    candidates = [
        "/usr/lib/nginx/modules/ngx_stream_module.so",
        "/usr/lib64/nginx/modules/ngx_stream_module.so",
        "/usr/share/nginx/modules/ngx_stream_module.so",
    ]
    module_path = next((path for path in candidates if os.path.exists(path)), None)
    if not module_path:
        raise RuntimeError("当前 Nginx 的 stream 模块未加载，请安装 nginx-mod-stream 或 libnginx-mod-stream 后重试")
    nginx_conf = "/etc/nginx/nginx.conf"
    content = open(nginx_conf, "r", encoding="utf-8").read()
    load_line = "load_module %s;" % module_path
    if load_line not in content:
        with open(nginx_conf, "w", encoding="utf-8") as stream:
            stream.write(load_line + "\n" + content)


def install_stream_base_config():
    ensure_stream_module()
    nginx_conf = "/etc/nginx/nginx.conf"
    old_nginx_conf = open(nginx_conf, "r", encoding="utf-8").read()
    include_line = "include /etc/nginx/kuocai-edge-stream-base.conf;"
    if include_line not in old_nginx_conf:
        match = re.search(r"(?m)^\s*events\s*\{", old_nginx_conf)
        if not match:
            raise RuntimeError("无法定位 Nginx events 配置块")
        updated = old_nginx_conf[:match.start()] + include_line + "\n" + old_nginx_conf[match.start():]
        with open(nginx_conf, "w", encoding="utf-8") as stream:
            stream.write(updated)
    base_path = "/etc/nginx/kuocai-edge-stream-base.conf"
    base_content = """stream {
    log_format kuocai_edge_stream '$msec $server_port $protocol $bytes_sent $bytes_received $status';
    include /etc/nginx/kuocai-edge-stream/*.conf;
}
"""
    if not os.path.exists(base_path) or open(base_path, "r", encoding="utf-8").read() != base_content:
        with open(base_path, "w", encoding="utf-8") as stream:
            stream.write(base_content)
    return nginx_conf, old_nginx_conf


def restore_stream_base_config(snapshot):
    if not snapshot:
        return
    path, content = snapshot
    with open(path, "w", encoding="utf-8") as stream:
        stream.write(content)


def apply_config(config, desired):
    version = int(desired.get("version") or 0)
    release_dir = os.path.join(RELEASE_ROOT, str(version))
    if os.path.exists(release_dir):
        shutil.rmtree(release_dir)
    http_release_dir = os.path.join(release_dir, "http")
    stream_release_dir = os.path.join(release_dir, "stream")
    os.makedirs(http_release_dir, mode=0o700)
    os.makedirs(stream_release_dir, mode=0o700)
    for domain in desired.get("domains") or []:
        write_domain_config(http_release_dir, domain)
    port_forwards = desired.get("portForwards") or []
    for rule in port_forwards:
        write_port_forward_config(stream_release_dir, rule)
    install_base_config()
    needs_stream = bool(port_forwards) or os.path.lexists(STREAM_ACTIVE_LINK)
    stream_snapshot = None
    if needs_stream:
        stream_snapshot = install_stream_base_config()
    old_target = os.path.realpath(ACTIVE_LINK) if os.path.islink(ACTIVE_LINK) else None
    old_stream_target = os.path.realpath(STREAM_ACTIVE_LINK) if os.path.islink(STREAM_ACTIVE_LINK) else None
    temp_link = ACTIVE_LINK + ".new"
    temp_stream_link = STREAM_ACTIVE_LINK + ".new"
    if os.path.lexists(temp_link):
        os.unlink(temp_link)
    if os.path.lexists(temp_stream_link):
        os.unlink(temp_stream_link)
    os.symlink(http_release_dir, temp_link)
    os.replace(temp_link, ACTIVE_LINK)
    if needs_stream:
        os.symlink(stream_release_dir, temp_stream_link)
        os.replace(temp_stream_link, STREAM_ACTIVE_LINK)
    try:
        test = subprocess.run(["nginx", "-t"], stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, universal_newlines=True)
        if test.returncode != 0:
            raise RuntimeError((test.stderr or test.stdout).strip())
        subprocess.check_call(["nginx", "-s", "reload"])
    except Exception:
        if old_target:
            rollback_link = ACTIVE_LINK + ".rollback"
            if os.path.lexists(rollback_link):
                os.unlink(rollback_link)
            os.symlink(old_target, rollback_link)
            os.replace(rollback_link, ACTIVE_LINK)
        if needs_stream:
            if old_stream_target:
                rollback_stream_link = STREAM_ACTIVE_LINK + ".rollback"
                if os.path.lexists(rollback_stream_link):
                    os.unlink(rollback_stream_link)
                os.symlink(old_stream_target, rollback_stream_link)
                os.replace(rollback_stream_link, STREAM_ACTIVE_LINK)
            elif os.path.lexists(STREAM_ACTIVE_LINK):
                os.unlink(STREAM_ACTIVE_LINK)
        restore_stream_base_config(stream_snapshot)
        if stream_snapshot:
            subprocess.call(["nginx", "-s", "reload"])
        raise
    api_request(config, "POST", "/api/self-hosted/agent/apply-result", {
        "version": version, "success": True, "error": ""
    })
    return version


def read_percent(path, index):
    try:
        output = shutil.disk_usage(path)
        return round(output.used * 100.0 / output.total, 2)
    except Exception:
        return 0


def memory_percent():
    values = {}
    try:
        with open("/proc/meminfo", "r", encoding="utf-8") as stream:
            for line in stream:
                key, value = line.split(":", 1)
                values[key] = int(value.strip().split()[0])
        total = values.get("MemTotal", 0)
        available = values.get("MemAvailable", 0)
        return round((total - available) * 100.0 / total, 2) if total else 0
    except Exception:
        return 0


def network_bytes():
    rx = tx = 0
    try:
        with open("/proc/net/dev", "r", encoding="utf-8") as stream:
            for line in stream.readlines()[2:]:
                _, data = line.split(":", 1)
                fields = data.split()
                rx += int(fields[0])
                tx += int(fields[8])
    except Exception:
        pass
    return rx, tx


def heartbeat(config, applied, last_error):
    rx, tx = network_bytes()
    return api_request(config, "POST", "/api/self-hosted/agent/heartbeat", {
        "agentVersion": "1.2.0",
        "appliedConfigVersion": applied,
        "cpuUsage": 0,
        "memoryUsage": memory_percent(),
        "diskUsage": read_percent("/", 0),
        "rxBytes": rx,
        "txBytes": tx,
        "cacheBytes": sum(os.path.getsize(os.path.join(root, name)) for root, _, files in os.walk(CACHE_DIR) for name in files),
        "lastError": last_error,
    })


def process_cache_jobs(config, jobs):
    for job in jobs or []:
        task_id = job.get("taskId")
        try:
            if job.get("operation") == "refresh":
                subprocess.check_call(["find", CACHE_DIR, "-mindepth", "1", "-delete"])
            else:
                for target in job.get("targets") or []:
                    parsed = urllib.parse.urlsplit(target)
                    if not parsed.hostname:
                        raise ValueError("invalid preheat URL")
                    port = parsed.port or (443 if parsed.scheme == "https" else 80)
                    command = ["curl", "-fsS", "--max-time", "30", "-o", "/dev/null",
                               "--resolve", "%s:%d:127.0.0.1" % (parsed.hostname, port)]
                    if parsed.scheme == "https":
                        command.append("-k")
                    command.append(target)
                    subprocess.check_call(command)
            api_request(config, "POST", "/api/self-hosted/agent/cache-result", {
                "taskId": task_id, "success": True, "error": ""
            })
        except Exception as error:
            api_request(config, "POST", "/api/self-hosted/agent/cache-result", {
                "taskId": task_id, "success": False, "error": str(error)[:900]
            })


def main():
    config = load_agent_config()
    os.makedirs(RELEASE_ROOT, exist_ok=True)
    applied = 0
    last_error = ""
    while True:
        try:
            response = heartbeat(config, applied, last_error)
            desired_version = int(response.get("desiredConfigVersion") or 0)
            if desired_version != applied:
                desired = api_request(config, "GET", "/api/self-hosted/agent/config")
                applied = apply_config(config, desired)
            process_cache_jobs(config, response.get("cacheJobs"))
            last_error = ""
        except Exception as error:
            last_error = str(error)[:900]
            try:
                api_request(config, "POST", "/api/self-hosted/agent/apply-result", {
                    "version": applied, "success": False, "error": last_error
                })
            except Exception:
                pass
        time.sleep(max(10, int(config.get("pollIntervalSeconds", 30))))


if __name__ == "__main__":
    main()
