package com.sang.sourcepattern.config;

// MailConfig has been disabled because Spring Boot's spring-boot-starter-mail 
// automatically configures a JavaMailSender bean using the spring.mail properties 
// defined in application.yml / application-server.yml.
// 
// Manual configuration with @Value placeholders like ${spring.mail.properties.mail.smtp.auth}
// fails to resolve since those properties are mapped hierarchically as a Java Map 
// rather than individual flat properties, preventing the custom JavaMailSender bean 
// from initializing and causing UnsatisfiedDependencyException.
public class MailConfig {
}
