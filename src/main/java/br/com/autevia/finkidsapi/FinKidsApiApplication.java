package br.com.autevia.finkidsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinKidsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinKidsApiApplication.class, args);
    }

}
