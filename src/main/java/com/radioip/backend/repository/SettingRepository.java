package com.radioip.backend.repository;
import com.radioip.backend.model.Setting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
    Optional<Setting> findByKey(String key);
}
