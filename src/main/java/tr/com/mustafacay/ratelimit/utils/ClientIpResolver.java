package tr.com.mustafacay.ratelimit.utils;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpResolver {
    public static String getClientIP(HttpServletRequest request) {
        // X-Forwarded-For başlığı varsa, gerçek IP adresini al
        String clientIP = request.getHeader("X-Forwarded-For");

        // X-Forwarded-For başlığı yoksa veya boşsa, standart RemoteAddr kullan
        if (clientIP == null || clientIP.isEmpty() || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getRemoteAddr();
        }

        return clientIP;
    }
}
