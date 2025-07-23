package com.radioip.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import com.radioip.backend.model.LocalUser; // <-- selon où est ta classe
import com.radioip.backend.repository.LocalUserRepository;


@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final LocalUserRepository repo;
    private final PasswordEncoder encoder;

    public AdminUserController(LocalUserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createUser(@RequestParam String username,
                                             @RequestParam String password,
                                             @RequestParam String role) {
        if (repo.existsById(username)) return ResponseEntity.badRequest().body("Utilisateur déjà existant");

        LocalUser user = new LocalUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRoles(role.equals("ADMIN") ? "ADMIN,USER" : "USER");

        repo.save(user);
        return ResponseEntity.ok("Utilisateur créé");
    }
    
    @DeleteMapping("/delete/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        if (!repo.existsById(username)) return ResponseEntity.notFound().build();

        // Récupérer tous les utilisateurs
        List<LocalUser> users = repo.findAll();
        long adminCount = users.stream().filter(u -> u.getRoles().contains("ADMIN")).count();

        // Si c'est un admin et qu'il est le dernier, refuser la suppression
        LocalUser user = repo.findById(username).get();
        if (user.getRoles().contains("ADMIN") && adminCount <= 1) {
            return ResponseEntity.status(403).body("Impossible de supprimer le dernier administrateur");
        }

        repo.deleteById(username);
        return ResponseEntity.ok("Utilisateur supprimé");
    }


    @GetMapping("/list")
    public List<LocalUser> listUsers() {
        return repo.findAll();
    }
}

