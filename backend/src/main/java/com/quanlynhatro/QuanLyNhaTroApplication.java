package com.quanlynhatro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuanLyNhaTroApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuanLyNhaTroApplication.class, args);
    }
}
