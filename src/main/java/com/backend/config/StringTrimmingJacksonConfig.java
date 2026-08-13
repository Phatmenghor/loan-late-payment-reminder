package com.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Global Jackson configuration to automatically trim leading/trailing whitespace
 * and sanitize UTF-8 0x00 NUL bytes on ALL incoming JSON String fields across the application.
 */
@Configuration
public class StringTrimmingJacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer stringTrimmingCustomizer() {
        return builder -> builder.deserializerByType(String.class, new StdScalarDeserializer<String>(String.class) {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (value == null) {
                    return null;
                }
                String trimmed = value.trim();
                return trimmed.replace("\u0000", "").replace("\0", "");
            }
        });
    }
}
