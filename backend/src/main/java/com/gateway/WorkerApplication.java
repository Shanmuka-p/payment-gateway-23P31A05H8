package com.gateway;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkerApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(WorkerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
