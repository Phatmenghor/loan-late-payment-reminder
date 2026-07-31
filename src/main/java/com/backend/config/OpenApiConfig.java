package com.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Internal Service API")
                        .description("Internal Active Directory Auth & Notification Service")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Backend Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://example.com/license")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth").addList("apiKey"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("HTTP Basic Authentication"))
                        .addSecuritySchemes("apiKey",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("AD System API Key header (X-API-Key) for calling application identification")));
    }
}