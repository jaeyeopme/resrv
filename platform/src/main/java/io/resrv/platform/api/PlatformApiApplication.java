package io.resrv.platform.api;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(
        basePackages = {"io.resrv.platform", "io.resrv.timeslot"},
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "io\\.resrv\\.timeslot\\.api\\..*"))
@EnableJpaRepositories(basePackages = {"io.resrv.platform", "io.resrv.timeslot"})
@EntityScan(basePackages = {"io.resrv.platform", "io.resrv.timeslot"})
public class PlatformApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
