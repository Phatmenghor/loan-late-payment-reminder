package com.backend.shared.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Utility class for resolving the real client IP address across direct TCP connections,
 * reverse proxies (Nginx, Traefik, HAProxy), Cloudflare, load balancers, and Docker bridges.
 */
public class ClientIpUtils {

    private static final List<String> IP_HEADER_CANDIDATES = List.of(
        "X-Forwarded-For",
        "X-Real-IP",
        "CF-Connecting-IP",
        "True-Client-IP",
        "X-Client-IP",
        "X-Original-Forwarded-For",
        "X-Cluster-Client-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED"
    );

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        // Pass 1: Scan headers for the first real non-loopback / non-container-bridge IP
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList.trim())) {
                String[] ips = ipList.split(",");
                for (String rawIp : ips) {
                    String cleanIp = normalizeIp(rawIp);
                    if (isRealClientIp(cleanIp)) {
                        return cleanIp;
                    }
                }
            }
        }

        // Pass 2: Fallback to the first non-empty IP from proxy headers
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList.trim())) {
                String cleanIp = normalizeIp(ipList.split(",")[0]);
                if (isValidIp(cleanIp)) {
                    return cleanIp;
                }
            }
        }

        // Pass 3: Fallback to direct servlet remote address
        return normalizeIp(request.getRemoteAddr());
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : "UNKNOWN";
    }

    private static boolean isRealClientIp(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }
        // Filter out local loopback and standard docker bridge IPs if actual client IP exists in header chain
        return !"127.0.0.1".equals(ip)
                && !"0:0:0:0:0:0:0:1".equals(ip)
                && !"::1".equals(ip)
                && !"localhost".equalsIgnoreCase(ip)
                && !"172.17.0.1".equals(ip);
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"UNKNOWN".equalsIgnoreCase(ip.trim());
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "UNKNOWN";
        }
        String cleanIp = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(cleanIp) || "::1".equals(cleanIp)) {
            return "127.0.0.1";
        }
        return cleanIp;
    }
}
