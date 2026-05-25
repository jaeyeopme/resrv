package io.resrv.platform.api;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.resrv.platform")
@EnableJpaRepositories(basePackages = "io.resrv.platform")
@EntityScan(basePackages = "io.resrv.platform")
public class PlatformApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
