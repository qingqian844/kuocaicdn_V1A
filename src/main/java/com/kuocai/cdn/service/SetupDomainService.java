package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.IDN;
import java.net.URI;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class SetupDomainService {

    private final CaddyProvisioningService caddyProvisioningService;
    private final String publicIp;

    public SetupDomainService(CaddyProvisioningService caddyProvisioningService,
                              @Value("${installation.public-ip:}") String publicIp) {
        this.caddyProvisioningService = caddyProvisioningService;
        this.publicIp = publicIp == null ? "" : publicIp.trim();
    }

    public JSONObject verify(String rawDomain) throws BusinessException {
        String domain = normalizeDomain(rawDomain);
        Set<String> addresses = resolve(domain);
        if (!publicIp.isEmpty() && !addresses.contains(publicIp)) {
            throw new BusinessException("域名当前解析到 " + String.join(", ", addresses)
                    + "，未解析到本机公网 IP " + publicIp);
        }
        checkPort(domain, 80);
        JSONObject result = new JSONObject(true);
        result.put("domain", domain);
        result.put("addresses", addresses);
        result.put("httpPort", true);
        return result;
    }

    public void apply(String rawDomain) throws BusinessException {
        String domain = normalizeDomain(rawDomain);
        verify(domain);
        caddyProvisioningService.provision(domain);
        if (!caddyProvisioningService.waitForHttps(domain, 90)) {
            throw new BusinessException("Caddy 已接收域名配置，但 HTTPS 证书尚未签发成功，请检查 80/443 端口和 DNS 后重试");
        }
    }

    public String normalizeDomain(String rawDomain) throws BusinessException {
        String domain = normalizeHost(rawDomain);
        if (Assert.isEmpty(domain) || domain.matches("^[0-9a-fA-F:.]+$") || !isDnsName(domain)) {
            throw new BusinessException("请输入有效的网站域名，不能使用 IP 地址");
        }
        return domain;
    }

    public String normalizeHost(String value) {
        if (value == null) {
            return "";
        }
        String host = value.trim();
        int comma = host.indexOf(',');
        if (comma >= 0) {
            host = host.substring(0, comma).trim();
        }
        try {
            if (host.contains("://")) {
                URI uri = URI.create(host);
                host = uri.getHost() == null ? host : uri.getHost();
            }
        } catch (Exception ignored) {
        }
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            if (end > 0) {
                host = host.substring(1, end);
            }
        } else {
            int colon = host.lastIndexOf(':');
            if (colon > -1 && host.indexOf(':') == colon && isPort(host.substring(colon + 1))) {
                host = host.substring(0, colon);
            }
        }
        host = host.trim().toLowerCase();
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        try {
            return IDN.toASCII(host).toLowerCase();
        } catch (Exception e) {
            return host;
        }
    }

    private boolean isPort(String value) {
        if (Assert.isEmpty(value)) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean isDnsName(String domain) {
        if (domain.length() > 253) {
            return false;
        }
        String[] labels = domain.split("\\.", -1);
        if (labels.length < 2) {
            return false;
        }
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63
                    || !Character.isLetterOrDigit(label.charAt(0))
                    || !Character.isLetterOrDigit(label.charAt(label.length() - 1))) {
                return false;
            }
            for (int index = 0; index < label.length(); index++) {
                char value = label.charAt(index);
                if (!Character.isLetterOrDigit(value) && value != '-') {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<String> resolve(String domain) throws BusinessException {
        try {
            Set<String> result = new LinkedHashSet<>();
            for (InetAddress address : InetAddress.getAllByName(domain)) {
                result.add(address.getHostAddress());
            }
            if (result.isEmpty()) {
                throw new BusinessException("域名尚未配置 DNS 解析");
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("域名 DNS 解析失败：" + e.getMessage());
        }
    }

    private void checkPort(String host, int port) throws BusinessException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 4000);
        } catch (Exception e) {
            throw new BusinessException("无法通过域名访问服务器 " + port + " 端口，请检查安全组和防火墙");
        }
    }
}
