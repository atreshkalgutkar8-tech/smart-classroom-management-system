
package com.smartclassroom.smart_classroom.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.repository.UserRepository;
import com.smartclassroom.smart_classroom.service.UserService;

@Configuration
public class AdminAccountInitializer {

    @Bean
    CommandLineRunner createAdmin(
            UserService userService,
            UserRepository userRepository) {

        return args -> {

            String adminEmail = "admin@smartclassroom.com";

            if (userRepository.findByEmail(adminEmail).isEmpty()) {

                userService.registerUser(
                    "System Administrator",
                    adminEmail,
                    "Admin@12345",
                    Role.ADMIN
                );

                System.out.println("Admin account created successfully!");

            } else {

                System.out.println("Admin account already exists!");
            }
        };
    }
}
