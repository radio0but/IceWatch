package com.radioip.backend.repository;

import com.radioip.backend.model.SlotInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SlotInfoRepository extends JpaRepository<SlotInfo, Long> {

    @Query("SELECT s FROM SlotInfo s WHERE LOWER(s.day) = LOWER(:day) AND s.hour = :hour")
    Optional<SlotInfo> findByDayAndHour(@Param("day") String day, @Param("hour") String hour);
}
