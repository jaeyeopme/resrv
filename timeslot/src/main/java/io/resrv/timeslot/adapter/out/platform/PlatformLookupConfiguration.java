package io.resrv.timeslot.adapter.out.platform;

import io.resrv.platform.contract.PlatformLookupContractConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(PlatformLookupContractConfiguration.class)
class PlatformLookupConfiguration {}
