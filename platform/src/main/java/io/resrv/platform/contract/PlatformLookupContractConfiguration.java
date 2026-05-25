package io.resrv.platform.contract;

import io.resrv.platform.application.business.LookupActiveBusinessService;
import io.resrv.platform.application.membership.CheckBusinessAccessService;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ComponentScan("io.resrv.platform.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "io.resrv.platform.adapter.out.persistence")
@EntityScan(basePackages = "io.resrv.platform.adapter.out.persistence")
@Import({LookupActiveBusinessService.class, CheckBusinessAccessService.class})
public class PlatformLookupContractConfiguration {}
