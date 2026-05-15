package com.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(BackendApplication.class);
		ServletWebServerApplicationContext context = (ServletWebServerApplicationContext) app.run(args);

		int port = context.getWebServer().getPort();
		String host = "localhost";

		System.out.println(String.format("""

            🇰🇭 Cambodia Platform Started Successfully! 🇰🇭


            🌐 Access Points:
            • Application: http://%s:%d
            • Swagger UI: http://%s:%d/swagger-ui.html
            • Health Check: http://%s:%d/actuator/health

            """, host, port, host, port, host, port));
	}
}