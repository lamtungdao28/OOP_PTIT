package com.example.oopPTIT.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.oopPTIT.security.CompatiblePasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public CompatiblePasswordEncoder passwordEncoder() {
        return new CompatiblePasswordEncoder();
    }
}
