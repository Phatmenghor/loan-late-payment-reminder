package com.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.name:E-Menu SaaS Platform}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:Simple E-Menu Platform for Restaurant Management}")
    private String appDescription;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        String securityDescription = "JWT authentication token";

        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .description(appDescription)
                        .version(appVersion)
                        .contact(new Contact()
                                .name("E-Menu Platform Support")
                                .email("support@emenu-platform.com")
                                .url("https://emenu-platform.com/support"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://emenu-platform.com/license")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(securityDescription)));
    }

    @Bean
    @SuppressWarnings("unchecked")
    public OpenApiCustomizer localTimeSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
                    if (schema.getProperties() != null) {
                        schema.getProperties().forEach((propertyName, propertySchemaObj) -> {
                            // Replace LocalTime object schema with string schema
                            if (propertySchemaObj instanceof Schema) {
                                Schema<?> propertySchema = (Schema<?>) propertySchemaObj;
                                if (isLocalTimeSchema(propertySchema)) {
                                    Schema<String> stringSchema = new Schema<>();
                                    stringSchema.setType("string");
                                    stringSchema.setPattern("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
                                    stringSchema.setExample("09:00");
                                    stringSchema.setDescription("Time in HH:mm format");
                                    schema.getProperties().put(propertyName, stringSchema);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

    private boolean isLocalTimeSchema(Schema<?> schema) {
        if (schema != null && schema.getProperties() != null) {
            // Check if it has the typical LocalTime structure
            return schema.getProperties().containsKey("hour") &&
                    schema.getProperties().containsKey("minute") &&
                    schema.getProperties().containsKey("second") &&
                    schema.getProperties().containsKey("nano");
        }
        return false;
    }
}