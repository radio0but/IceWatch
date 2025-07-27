package com.radioip.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SlotInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String day;   // ex: dimanche
    private String hour;  // ex: 14

    @Column(columnDefinition = "TEXT")
    private String audioInfo;

    @Column(columnDefinition = "TEXT")
    private String videoInfo;
}
