package com.radioip.backend.config;

import com.radioip.backend.service.LocalUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.*;

@Configuration
@EnableMethodSecurity
public class Security_Config {

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    @Value("${spring.ldap.user-dn-patterns}")
    private String userDnPattern;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        return new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBase);
    }

    @Bean
    public GrantedAuthoritiesMapper ldapAuthoritiesMapper() {
        return authorities -> {
            if (authorities != null && !authorities.isEmpty()) {
                Set<GrantedAuthority> mapped = new HashSet<>();
                mapped.add(() -> "ROLE_USER");
                return mapped;
            }
            return Collections.emptySet();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            LocalUserDetailsService userService,
            PasswordEncoder encoder,
            DefaultSpringSecurityContextSource contextSource,
            GrantedAuthoritiesMapper ldapAuthoritiesMapper
    ) {
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(userService);
        daoProvider.setPasswordEncoder(encoder);

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[]{userDnPattern});

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, "ou=Groups");
        authoritiesPopulator.setIgnorePartialResultException(true);

        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
        ldapProvider.setAuthoritiesMapper(ldapAuthoritiesMapper);

        return new ProviderManager(List.of(daoProvider, ldapProvider));
    }

    @Bean
    public AuthenticationSuccessHandler successHandler(IceWatchConfig config) {
        return (HttpServletRequest request, HttpServletResponse response, Authentication auth) -> {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

            if (config.isDisableLogin() && isAdmin) {
                response.sendRedirect("/dashboard");
            } else {
                response.sendRedirect("/index");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager authManager,
            IceWatchConfig config
    ) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> {
            auth
                // 🔓 Publics (toujours)
                .requestMatchers("/auth/token", "/favicon.ico", "/css/**", "/js/**", "/login.html", "/login").permitAll();

            if (config.isDisableLogin()) {
                // 🟢 Accès libre à index, mais dashboard nécessite login
                auth
                    .requestMatchers("/", "/index").permitAll()
                    .requestMatchers("/dashboard").authenticated()
                    .anyRequest().permitAll();
            } else {
                // 🔐 Index et dashboard nécessitent login
                auth
                    .requestMatchers("/dashboard").hasRole("ADMIN")
                    .requestMatchers("/", "/index").authenticated()
                    .anyRequest().permitAll();
            }
        });

        http.formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login") // 🔐 nécessaire pour POST
                .successHandler(successHandler(config))
                .failureUrl("/login?error")
                .permitAll()
        );

        http.logout(logout -> logout.permitAll())
            .authenticationManager(authManager)
            .headers(headers -> headers.frameOptions().disable());

        return http.build();
    }
}
