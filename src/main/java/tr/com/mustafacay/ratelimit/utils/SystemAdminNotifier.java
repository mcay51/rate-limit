package tr.com.mustafacay.ratelimit.utils;

import org.springframework.stereotype.Component;

@Component
public class SystemAdminNotifier {

    // Sistem yöneticisine bildirim gönderme metodu
    public void notifyRateLimitSystemAdmin(String endpoint, String ipAddress) {
        System.out.println("Uyarı: Sisteme saldırı olabilir. Lütfen kontrol yapın. \nIP: "+ipAddress+" \nEndpoint : "+endpoint );
    }
}
