package tr.com.mustafacay.ratelimit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
public class RatelimitApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatelimitApplication.class, args);
    }

}
