package com.travelplatform.busbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync // NotificationClient calls run off the request thread
public class BusBookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusBookingServiceApplication.class, args);
    }
}
