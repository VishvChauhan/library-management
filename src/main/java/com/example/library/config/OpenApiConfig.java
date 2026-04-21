package com.example.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management API")
                        .description("REST API for managing books, borrowing, returns, and overdue fines")
                        .version("1.0.0")
                        .contact(new Contact().name("Library Management Team"))
                        .license(new License().name("Apache 2.0")));
    }
}

