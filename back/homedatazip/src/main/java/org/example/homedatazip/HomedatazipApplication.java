package org.example.homedatazip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomedatazipApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomedatazipApplication.class, args);
    }

}
