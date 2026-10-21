package com.khanyi.java.gautenglogistics;

import org.springframework.boot.SpringApplication;

public class TestGautengLogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.from(GautengLogisticsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
