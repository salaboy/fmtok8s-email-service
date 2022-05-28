package com.salaboy.conferences.email.dev;

import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("dev")
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, TracerAutoConfiguration.class,
        ReactiveManagementWebSecurityAutoConfiguration.class})
@Configuration
public class DevProfile {

    @Bean
    public WebClient getWebClient() {
        return WebClient.builder().build();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf().disable()
                .authorizeExchange()
                .anyExchange().permitAll()
                .and()
                .build();
    }
}
