package com.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
		System.out.println("""
            
            🇰🇭 Cambodia E-Menu Platform Started Successfully! 🇰🇭
           
            
            🌐 Access Points:
            • Application: http://localhost:8080
            • Swagger UI: http://localhost:8080/swagger-ui.html
            • Health Check: http://localhost:8080/actuator/health
            
            """);
	}
}