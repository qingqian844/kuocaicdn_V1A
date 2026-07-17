#!/usr/bin/env python3
import hashlib
import json
import os
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
    if not value or any(ch not in "abcdefghijklmnopqrstuvwxyz0123456789.-" for ch in value):
        raise ValueError("invalid domain name")
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:20]


def nginx_quote(value):
    value = str(value or "")
    if any(ch in value for ch in "\r\n{};$`\\\""):
        raise ValueError("unsafe nginx value")
    return value


def origin_url(domain):
    protocol = domain.get("originProtocol", "http")
    if protocol == "follow":
        protocol = "http"
    port = domain.get("httpsPort", 443) if protocol == "https" else domain.get("httpPort", 80)
    address = str(domain.get("originAddress", "")).split(";")[0].strip()
    if ":" in address and not address.startswith("["):
        address = "[" + address + "]"
    return "%s://%s:%d" % (nginx_quote(protocol), nginx_quote(address), int(port))


def ttl_seconds(rule):
    value = max(0, min(int(rule.get("ttl") or 0), 31536000))
    unit = str(rule.get("ttl_unit") or "s").lower()
    return min(value * {"s": 1, "m": 60, "h": 3600, "d": 86400}.get(unit, 1), 31536000)


def safe_path(value):
    value = nginx_quote(value.strip())
    if not value.startswith("/") or any(ch not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~/%" for ch in value):
        raise ValueError("unsafe cache path")
    return value


def proxy_location(selector, ttl, origin, origin_host, error_rules):
    lines = ["    location %s {" % selector,
             "        proxy_set_header Host %s;" % origin_host,
             "        proxy_set_header X-Real-IP $remote_addr;",
             "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;",
             "        proxy_set_header X-Forwarded-Proto $scheme;",
             "        proxy_http_version 1.1;",
             "        proxy_cache kuocai_edge_cache;"]
    if ttl <= 0:
        lines.extend(["        proxy_cache_bypass 1;", "        proxy_no_cache 1;"])
    else:
        lines.append("        proxy_cache_valid 200 206 %ds;" % ttl)
    for rule in error_rules:
        code = int(rule.get("code") or 0)
        error_ttl = max(0, min(int(rule.get("ttl") or 0), 31536000))
        if code in (400, 403, 404, 405, 414, 500, 501, 502, 503, 504):
            lines.append("        proxy_cache_valid %d %ds;" % (code, error_ttl))
    lines.extend(["        add_header X-Kuocai-Cache $upstream_cache_status always;",
                  "        proxy_pass %s;" % origin,
                  "    }"])
    return lines


def cache_locations(cache, origin, origin_host):
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
                locations.extend(proxy_location("~* \\.(%s)$" % "|".join(extensions), ttl, origin, origin_host, error_rules))
        elif match_type == "catalog":
            for value in values:
                if value.strip():
                    locations.extend(proxy_location("^~ " + safe_path(value), ttl, origin, origin_host, error_rules))
        elif match_type in ("full_path", "home_page"):
            paths = ["/"] if match_type == "home_page" else values
            for value in paths:
                if value.strip() and "*" not in value:
                    locations.extend(proxy_location("= " + safe_path(value), ttl, origin, origin_host, error_rules))
    locations.extend(proxy_location("/", default_ttl, origin, origin_host, error_rules))
    return locations


def write_domain_config(release_dir, domain):
    name = safe_name(domain["domainName"])
    domain_name = nginx_quote(domain["domainName"])
    origin = origin_url(domain)
    origin_host = nginx_quote(domain.get("originHost") or domain_name)
    cache_json = domain.get("cacheConfigJson") or "{}"
    try:
        cache = json.loads(cache_json)
    except Exception:
        cache = {}
    common = [
        "    server_name %s;" % domain_name,
        "    access_log /var/log/nginx/kuocai-edge-access.log kuocai_edge;",
    ]
    common.extend(cache_locations(cache, origin, origin_host))
    blocks = ["server {", "    listen 80;", *common, "}"]
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
            blocks = [
                "server {",
                "    listen 80;",
                "    server_name %s;" % domain_name,
                "    return 301 https://$host$request_uri;",
                "}",
            ]
        blocks.extend([
            "server {",
            "    listen 443 ssl http2;",
            "    ssl_certificate %s;" % cert_path,
            "    ssl_certificate_key %s;" % key_path,
            "    ssl_protocols TLSv1.2 TLSv1.3;",
            *common,
            "}",
        ])
    with open(os.path.join(release_dir, name + ".conf"), "w", encoding="utf-8") as stream:
        stream.write("\n".join(blocks) + "\n")


def install_base_config():
    os.makedirs(CACHE_DIR, exist_ok=True)
    path = "/etc/nginx/conf.d/kuocai-edge-base.conf"
    content = """proxy_cache_path /var/cache/kuocai-cdn levels=1:2 keys_zone=kuocai_edge_cache:100m max_size=50g inactive=7d use_temp_path=off;
log_format kuocai_edge '$msec $host $status $body_bytes_sent $upstream_cache_status $request_time';
include /etc/nginx/kuocai-edge/*.conf;
"""
    if not os.path.exists(path) or open(path, "r", encoding="utf-8").read() != content:
        with open(path, "w", encoding="utf-8") as stream:
            stream.write(content)


def apply_config(config, desired):
    version = int(desired.get("version") or 0)
    release_dir = os.path.join(RELEASE_ROOT, str(version))
    if os.path.exists(release_dir):
        shutil.rmtree(release_dir)
    os.makedirs(release_dir, mode=0o700)
    for domain in desired.get("domains") or []:
        write_domain_config(release_dir, domain)
    install_base_config()
    old_target = os.path.realpath(ACTIVE_LINK) if os.path.islink(ACTIVE_LINK) else None
    temp_link = ACTIVE_LINK + ".new"
    if os.path.lexists(temp_link):
        os.unlink(temp_link)
    os.symlink(release_dir, temp_link)
    os.replace(temp_link, ACTIVE_LINK)
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
        "agentVersion": "1.0.1",
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
