package io.resrv.bootstrap.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI resrvOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Resrv Reservation API")
                                .version("0.0.1")
                                .description(
                                        """
                                        Multi-tenant B2B reservation API covering tenant onboarding,
                                        admin/customer JWT authentication, tenant-scoped resource and
                                        availability management, customer reservation hold/confirm/cancel,
                                        and PostgreSQL-backed no-overbooking guarantees.
                                        """))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
