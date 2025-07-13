package com.radioip.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import java.util.*;

@Configuration
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

    // === Utilisateurs locaux ===
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsService(PasswordEncoder encoder, IceWatchConfig config) {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password(encoder.encode(config.getAdminPassword()))
                .roles("ADMIN", "USER")
                .build(),
            User.withUsername("enseignant")
                .password(encoder.encode(config.getEnseignantPassword()))
                .roles("USER")
                .build()
        );
    }
    // === Contexte LDAP ===
    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        return new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBase);
    }

    // === Mapper : ROLE_USER à tout LDAP membre d'au moins un groupe ===
    @Bean
    public GrantedAuthoritiesMapper ldapAuthoritiesMapper() {
        return (authorities) -> {
            if (authorities != null && !authorities.isEmpty()) {
                Set<GrantedAuthority> mapped = new HashSet<>();
                mapped.add(() -> "ROLE_USER");
                return mapped;
            }
            // Pas de groupe → login refusé
            return Collections.emptySet();
        };
    }

    // === Authentification combinée LDAP + mémoire ===
    @Bean
    public AuthenticationManager authenticationManager(
            InMemoryUserDetailsManager memoryAuth,
            DefaultSpringSecurityContextSource contextSource,
            GrantedAuthoritiesMapper ldapAuthoritiesMapper
    ) throws Exception {
        // Local
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(memoryAuth);
        daoProvider.setPasswordEncoder(passwordEncoder());

        // LDAP
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[]{userDnPattern});

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, "ou=Groups");
        authoritiesPopulator.setIgnorePartialResultException(true);

        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
        ldapProvider.setAuthoritiesMapper(ldapAuthoritiesMapper);

        return new ProviderManager(Arrays.asList(daoProvider, ldapProvider));
    }

    // === Règles de sécurité ===
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Ajoute ça pour voir si c’est le CSRF qui bloque
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/token", "/favicon.ico", "/css/**", "/js/**", "/login.html").permitAll()
                .requestMatchers("/dashboard.html").hasRole("ADMIN")
                .requestMatchers("/index.html").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(login -> login
            .loginPage("/login.html")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/index.html", true)
            .failureUrl("/login.html?error")
            .permitAll()
        )

            .logout(logout -> logout.permitAll())
            .authenticationManager(authManager)
            .headers(headers -> headers.frameOptions().disable());

        return http.build();
    }

}
