package com.corsairops.corsairopsuserservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableRedisRepositories
public class CorsairopsUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorsairopsUserServiceApplication.class, args);
    }

}