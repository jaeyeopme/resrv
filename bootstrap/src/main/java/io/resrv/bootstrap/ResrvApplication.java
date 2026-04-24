package io.resrv.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.resrv")
@EnableJpaRepositories(basePackages = "io.resrv")
@EntityScan(basePackages = "io.resrv")
public class ResrvApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ResrvApplication.class, args);
    }
}
