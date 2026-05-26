package com.telechat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class TelechatBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelechatBackendApplication.class, args);
        log.info("telechat backend start success");
    }

}
