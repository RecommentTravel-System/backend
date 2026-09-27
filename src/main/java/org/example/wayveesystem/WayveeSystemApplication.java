package org.example.wayveesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WayveeSystemApplication {

    public static void main(String[] args) {
        System.out.println(">>> BEFORE SPRING START");

        SpringApplication.run(WayveeSystemApplication.class, args);

        System.out.println(">>> AFTER SPRING START");
    }

}
