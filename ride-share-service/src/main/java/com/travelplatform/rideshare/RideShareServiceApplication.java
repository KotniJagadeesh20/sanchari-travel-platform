package com.travelplatform.rideshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync // NotificationClient calls run off the request thread
public class RideShareServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RideShareServiceApplication.class, args);
    }
}
