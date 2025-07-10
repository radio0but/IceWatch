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
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import java.util.Arrays;

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

    // === Mémoire (utilisateurs locaux) ===
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN", "USER").build(),
                User.withUsername("enseignant").password(encoder.encode("radio2025")).roles("USER").build()
        );
    }

    // === LDAP context (connexion au serveur LDAP) ===
    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        return new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBase);
    }

    // === Authentification combinée LDAP + mémoire ===
    @Bean
    public AuthenticationManager authenticationManager(
            InMemoryUserDetailsManager memoryAuth,
            DefaultSpringSecurityContextSource contextSource
    ) throws Exception {
        // Utilisateur local
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(memoryAuth);
        daoProvider.setPasswordEncoder(passwordEncoder());

        // LDAP
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[]{userDnPattern});
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(authenticator, new DefaultLdapAuthoritiesPopulator(contextSource, "ou=groups"));

        return new ProviderManager(Arrays.asList(daoProvider, ldapProvider));
    }

    // === Règles de sécurité ===
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/token", "/favicon.ico", "/css/**", "/js/**").permitAll()
                .requestMatchers("/dashboard.html").hasRole("ADMIN")
                .requestMatchers("/index.html").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(login -> login
                .defaultSuccessUrl("/index.html", true)
                .permitAll()
            )
            .logout(logout -> logout.permitAll())
            .authenticationManager(authManager) // 👈 ici on branche le manager
            .headers(headers -> headers.frameOptions().disable());
    
        return http.build();
    }
    
}
