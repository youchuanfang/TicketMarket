package com.ticketmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TicketMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketMarketApplication.class, args);
    }
}
