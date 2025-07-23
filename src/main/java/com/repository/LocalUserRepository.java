package com.radioip.backend.repository;

import com.radioip.backend.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalUserRepository extends JpaRepository<LocalUser, String> {
    // Tu peux ajouter des méthodes personnalisées ici si nécessaire
}
