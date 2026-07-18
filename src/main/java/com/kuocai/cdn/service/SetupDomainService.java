package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.license.HostLicenseValidator;
import com.kuocai.cdn.license.LicenseService;
import com.kuocai.cdn.util.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class SetupDomainService {

    private final LicenseService licenseService;
    private final HostLicenseValidator hostLicenseValidator;
    private final CaddyProvisioningService caddyProvisioningService;
    private final String publicIp;

    public SetupDomainService(LicenseService licenseService,
                              HostLicenseValidator hostLicenseValidator,
                              CaddyProvisioningService caddyProvisioningService,
                              @Value("${installation.public-ip:}") String publicIp) {
        this.licenseService = licenseService;
        this.hostLicenseValidator = hostLicenseValidator;
        this.caddyProvisioningService = caddyProvisioningService;
        this.publicIp = publicIp == null ? "" : publicIp.trim();
    }

    public JSONObject verify(String rawDomain) throws BusinessException {
        String domain = normalizeAndAuthorize(rawDomain);
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
        String domain = normalizeAndAuthorize(rawDomain);
        verify(domain);
        caddyProvisioningService.provision(domain);
        if (!caddyProvisioningService.waitForHttps(domain, 90)) {
            throw new BusinessException("Caddy 已接收域名配置，但 HTTPS 证书尚未签发成功，请检查 80/443 端口和 DNS 后重试");
        }
    }

    public String normalizeAndAuthorize(String rawDomain) throws BusinessException {
        String domain = hostLicenseValidator.normalizeHost(rawDomain);
        if (Assert.isEmpty(domain) || domain.matches("^[0-9a-fA-F:.]+$") || !isDnsName(domain)) {
            throw new BusinessException("请输入有效的网站域名，不能使用 IP 地址");
        }
        if (!licenseService.isHostAuthorized(domain)) {
            throw new BusinessException("域名不在当前授权范围内：" + domain);
        }
        return domain;
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
