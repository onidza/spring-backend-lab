package com.onidza.backend;

import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@Generated
@SpringBootApplication
public class SpringBackendLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBackendLabApplication.class, args);
    }
}
