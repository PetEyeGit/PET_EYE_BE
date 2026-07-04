package com.sang.sourcepattern.config;

import com.sang.sourcepattern.entity.Role;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.repository.RoleRepository;
import com.sang.sourcepattern.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer {

    @Bean
    ApplicationRunner applicationRunner(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            log.info("Initializing application data...");

            // 1. Create Roles if they don't exist
            if (roleRepository.findByName("ADMIN").isEmpty()) {
                roleRepository.save(Role.builder().name("ADMIN").description("Administrator").build());
            }
            if (roleRepository.findByName("USER").isEmpty()) {
                roleRepository.save(Role.builder().name("USER").description("Regular User").build());
            }
            if (roleRepository.findByName("SHOP_OWNER").isEmpty()) {
                roleRepository.save(Role.builder().name("SHOP_OWNER").description("Shop Owner").build());
            }
            if (roleRepository.findByName("STAFF").isEmpty()) {
                roleRepository.save(Role.builder().name("STAFF").description("Shop Staff").build());
            }

            // 2. Create Default Admin if it doesn't exist
            if (userRepository.findByEmail("vudinhsang0403@gmail.com").isEmpty()) {
                Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);

                User admin = User.builder()
                        .email("vudinhsang0403@gmail.com")
                        .password(passwordEncoder.encode("admin@12345"))
                        .fullName("System Admin")
                        .active(true)
                        .roles(roles)
                        .build();

                userRepository.save(admin);
                log.info("Default admin created: vudinhsang0403@gmail.com / admin@12345");
            }

            log.info("Application data initialization complete.");
        };
    }
}
