package com.hospital.scheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HospitalSchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalSchedulingApplication.class, args);
    }
}
