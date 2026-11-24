
package com.smartclassroom.smart_classroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Public pages
                .requestMatchers(
                    "/register",
                    "/login",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                // Teacher pages
                .requestMatchers("/teacher/**")
                .hasRole("TEACHER")

                // Parent pages
                .requestMatchers("/parent/**")
                .hasRole("PARENT")

                // Student pages
                .requestMatchers("/student/**")
                .hasRole("STUDENT")

                // Administrator pages
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")

                // All remaining pages require login
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )

            .logout(Customizer.withDefaults());

        return http.build();
    }
}
