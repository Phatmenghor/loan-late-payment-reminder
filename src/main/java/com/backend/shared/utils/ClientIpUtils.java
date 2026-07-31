package com.backend.shared.utils;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtils {

    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For",
        "X-Real-IP",
        "CF-Connecting-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR"
    };

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList.trim())) {
                String ip = ipList.split(",")[0].trim();
                return normalizeIp(ip);
            }
        }

        return normalizeIp(request.getRemoteAddr());
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : "UNKNOWN";
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
