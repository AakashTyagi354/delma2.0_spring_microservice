package com.delma.userservice.config;

import com.delma.userservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


// This filter chain is before controller layer
@Configuration
@RequiredArgsConstructor
public class webSecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
//        httpSecurity
//                .csrf(csrfConfig -> csrfConfig.disable())
//                .formLogin(form -> form.disable())
//                .sessionManagement(sessionConfig ->
//                        sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//
//        httpSecurity.authorizeHttpRequests(auth -> auth
//                .requestMatchers("/public/**", "/auth/**").permitAll()
//                .anyRequest().authenticated()
//                .requestMatchers("/admin/**").authenticated()
//        );
//
//        return httpSecurity.build();
//    }
//    @Bean
//    UserDetailsService userDetailsService(){
//        UserDetails user1 = User.withUsername("admin")
//                .password(passwordEncoder.encode("password"))
//                .roles("ADMIN")
//                .build();
//        UserDetails user2 = User.withUsername("doctor")
//                .password(passwordEncoder.encode("password"))
//                .roles("DOCTOR")
//                .build();
//
//        return new InMemoryUserDetailsManager(user1, user2);
//    }


}
