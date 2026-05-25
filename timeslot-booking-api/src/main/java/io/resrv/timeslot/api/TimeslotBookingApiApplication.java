package io.resrv.timeslot.api;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.resrv.timeslot")
@EnableJpaRepositories(basePackages = "io.resrv.timeslot")
@EntityScan(basePackages = "io.resrv.timeslot")
public class TimeslotBookingApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(TimeslotBookingApiApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
