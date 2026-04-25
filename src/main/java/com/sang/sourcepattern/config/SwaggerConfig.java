package com.sang.sourcepattern.config;

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
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PetEYE – Pet Care & Booking System API")
                        .version("1.0.0")
                        .description(
                                "Tài liệu API cho hệ thống petEYE. " +
                                        "Hệ thống hỗ trợ quản lý đặt lịch (booking) chăm sóc sức khỏe, " +
                                        "làm đẹp (grooming), lưu trú cho thú cưng và giám sát qua camera."
                        )
                        .contact(new Contact()
                                .name("petEYE Development Team")
                                .email("support@peteye.vn")
                                .url("https://peteye.vn"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )
                .addSecurityItem(
                        new SecurityRequirement().addList("Bearer Authentication")
                )
                .components(new Components()
                        .addSecuritySchemes(
                                "Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT Bearer token để xác thực quyền truy cập")
                        )
                );
    }
}