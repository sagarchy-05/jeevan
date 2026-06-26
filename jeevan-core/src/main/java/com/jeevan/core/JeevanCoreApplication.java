package com.jeevan.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JeevanCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(JeevanCoreApplication.class, args);
    }
}
