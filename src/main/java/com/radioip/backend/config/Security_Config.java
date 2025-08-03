package com.radioip.backend.config;

import com.radioip.backend.security.LoginRateLimitFilter;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.*;

@Configuration
@EnableMethodSecurity
public class Security_Config {

    @Value("${spring.ldap.urls:#{null}}")
    private String ldapUrl;

    @Value("${spring.ldap.base:#{null}}")
    private String ldapBase;

    @Value("${spring.ldap.user-dn-patterns:#{null}}")
    private String userDnPattern;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            LocalUserDetailsService userService,
            PasswordEncoder encoder
    ) {
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(userService);
        daoProvider.setPasswordEncoder(encoder);

        if (ldapUrl != null && !ldapUrl.isBlank() &&
            ldapBase != null && !ldapBase.isBlank() &&
            userDnPattern != null && !userDnPattern.isBlank()) {

            DefaultSpringSecurityContextSource contextSource =
                    new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBase);
            contextSource.afterPropertiesSet();

            BindAuthenticator authenticator = new BindAuthenticator(contextSource);
            authenticator.setUserDnPatterns(new String[]{userDnPattern});

            DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                    new DefaultLdapAuthoritiesPopulator(contextSource, "ou=Groups");
            authoritiesPopulator.setIgnorePartialResultException(true);

            LdapAuthenticationProvider ldapProvider =
                    new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
            ldapProvider.setAuthoritiesMapper(ldapAuthoritiesMapper());

            return new ProviderManager(List.of(daoProvider, ldapProvider));
        }

        return new ProviderManager(List.of(daoProvider));
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
            IceWatchConfig config,
            LoginRateLimitFilter loginRateLimitFilter // ← injection du filtre
    ) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> {
            auth
                .requestMatchers("/auth/token", "/favicon.ico", "/css/**", "/js/**", "/login.html", "/login").permitAll()
                .requestMatchers("/**.html").denyAll();

            if (config.isDisableLogin()) {
                auth
                    .requestMatchers("/", "/index").permitAll()
                    .requestMatchers("/dashboard").authenticated()
                    .anyRequest().permitAll();
            } else {
                auth
                    .requestMatchers("/dashboard").hasRole("ADMIN")
                    .requestMatchers("/", "/index").authenticated()
                    .anyRequest().permitAll();
            }
        });

        http.formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler(config))
                .failureUrl("/login?error")
                .permitAll()
        );

        http.logout(logout -> logout.permitAll())
            .authenticationManager(authManager)
            .headers(headers -> headers.frameOptions().disable());

        // Ajoute le filtre anti-brute-force AVANT l'authentification Spring
        http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
