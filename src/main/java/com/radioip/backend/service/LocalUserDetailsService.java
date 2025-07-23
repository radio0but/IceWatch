package com.radioip.backend.service;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;


import org.springframework.stereotype.Service;
import com.radioip.backend.model.LocalUser; // <-- si LocalUser est dans .model
import com.radioip.backend.repository.LocalUserRepository; // <-- ton repo
import org.springframework.security.core.GrantedAuthority;


@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final LocalUserRepository repo;

    public LocalUserDetailsService(LocalUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LocalUser user = repo.findById(username)
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + username));

        List<GrantedAuthority> authorities = Arrays.stream(user.getRoles().split(","))
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
            .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }
}

