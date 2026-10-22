package com.khanyi.java.gautenglogistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GautengLogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GautengLogisticsApplication.class, args);
    }

}
