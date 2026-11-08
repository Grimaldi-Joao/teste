package com.acme.motor;

import com.acme.motor.config.EngineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EngineProperties.class)
public class MotorIaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MotorIaApplication.class, args);
    }
}
