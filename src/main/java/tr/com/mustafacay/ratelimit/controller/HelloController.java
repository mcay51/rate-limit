package tr.com.mustafacay.ratelimit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        String hello="Hello World!";
        return new ResponseEntity<>(hello, HttpStatus.OK);
    }
    @GetMapping("/goodbye")
    public String goodbye() {

        return "Güle Güle Dünya!";
    }
}
