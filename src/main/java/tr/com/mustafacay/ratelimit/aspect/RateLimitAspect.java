package tr.com.mustafacay.ratelimit.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tr.com.mustafacay.ratelimit.exception.RateLimitExceededException;
import tr.com.mustafacay.ratelimit.utils.ClientIpResolver;
import tr.com.mustafacay.ratelimit.utils.SystemAdminNotifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Bu  sınıf, @GetMapping annotation'ı ile işaretlenmiş herhangi bir methoda yapılan istekleri
 * izler ve bir limit aşılırsa istekleri reddeder.
 */
@Aspect
@Component
@AllArgsConstructor
public class RateLimitAspect {
    // İstemci IP adreslerini ve blok sürelerini saklamak için bir harita
    private static final Map<String, Long> clientBlockTimeMap = new ConcurrentHashMap<>();

    // IP adreslerinin bloklama sayısını tutmak için bir harita
    private static final Map<String, Long> blockCountMap = new ConcurrentHashMap<>();


    // İstemci IP adreslerinin ve her endpoint için istek sayılarını tutmak için bir harita
    private static final Map<String, Map<String, Integer>> requestCountMap = new ConcurrentHashMap<>();

    // Bir dakikada izin verilen maksimum istek sayısı
    private static final int REQUEST_LIMIT = 20;

    // İstekler arasındaki süre sınırı (60 saniye)
    private static final long TIME_LIMIT = TimeUnit.SECONDS.toMillis(60);

    // İstemcinin bloklandığı başlangıç süresi (ilk olarak 20 saniye bloklanacak)
    private static final long INITIAL_BLOCK_TIME = TimeUnit.SECONDS.toMillis(20);

    // Maksimum engelleme süresi (600 saniye)
    private static final long MAX_BLOCK_TIME = TimeUnit.SECONDS.toMillis(600);

    private SystemAdminNotifier systemAdminNotifier;

    // GetMapping annotasyonuna sahip metodları hedefleyen bir nokta kesimi
    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void getMappingPointcut() {}

    // Önceki nokta kesimi çağrıldığında çalışacak olan rateLimitAdvice metodu
    @Before("getMappingPointcut()")
    public void rateLimitAdvice(JoinPoint joinPoint) {
        // HTTP isteği alınır
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // İstemcinin IP adresi alınır
        String ipAddress = ClientIpResolver.getClientIP(request);
        // Metodun adı alınır
        String endpoint = joinPoint.getSignature().toShortString();

        // Şu anki zaman alınır
        long currentTime = System.currentTimeMillis();

        // İstemcinin bloklandığı durumu kontrol eder
        if (clientBlockTimeMap.containsKey(ipAddress)) {
            long blockEndTime = clientBlockTimeMap.get(ipAddress);
            // Blok süresi hala devam ediyorsa isteği reddeder
            if (currentTime < blockEndTime) {
                throw new RateLimitExceededException("Too Many Requests. Please try again later.");
            } else {
                // Blok süresi dolduğunda istemci blok listesinden kaldırılır
                clientBlockTimeMap.remove(ipAddress);
               //Kullanıcıyı 5 kere üstel olarak blokladıktan sonra en başa alıyor
                //gerçek senaryoda kullanıcı tamamen bloklanabilir.
                long blockCount=blockCountMap.get(ipAddress);
                if(blockCount>4){
                    blockCountMap.remove(ipAddress);
                }
            }
        }

        // İstemcinin istek sayısı haritasının olup olmadığını kontrol eder
        if (!requestCountMap.containsKey(ipAddress)) {
            requestCountMap.put(ipAddress, new ConcurrentHashMap<>());
        }

        // İsteklerin sayısının endpoint haritasında olup olmadığını kontrol eder
        Map<String, Integer> endpointMap = requestCountMap.get(ipAddress);
        if (!endpointMap.containsKey(endpoint)) {
            endpointMap.put(endpoint, 0);
        }
        if (!blockCountMap.containsKey(ipAddress)) {
            blockCountMap.put(ipAddress, 0L);
        }
        // İstek sayısını kontrol eder ve geçerlilik süresi dolmuş istekleri kaldırır
        int requestCount = endpointMap.get(endpoint);
        endpointMap.entrySet().removeIf(entry -> currentTime - entry.getValue() > TIME_LIMIT);

        // İstek sayısı sınırını aşıp aşmadığını kontrol eder
        if (requestCount >= REQUEST_LIMIT) {
            //Sistem yöneticisine bilgilendirme mesajı atılır
            systemAdminNotifier.notifyRateLimitSystemAdmin(endpoint,ipAddress);
            long blockCount=blockCountMap.get(ipAddress);
            System.out.println("Block count: "+blockCount);

            // İstemciyi bloke eder ve üstel olarak blok süresini artırır
            long blockTime = Math.min(INITIAL_BLOCK_TIME * (long) Math.pow(2, blockCount), MAX_BLOCK_TIME);
            System.out.println("Block Time: "+blockTime);
            clientBlockTimeMap.put(ipAddress, currentTime + blockTime);
            blockCountMap.put(ipAddress,(blockCount+1L));
            System.out.println("Math işlem Sonucu: blockCount = "+blockCount+" -- > "+ (long) Math.pow(2, blockCount));

            throw new RateLimitExceededException("Too Many Requests. Please try again later.");

        }

        // İstek sayısını artırır
        endpointMap.put(endpoint, requestCount + 1);

    }
}
