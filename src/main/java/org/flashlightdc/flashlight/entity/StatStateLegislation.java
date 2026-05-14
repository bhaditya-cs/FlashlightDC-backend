package org.flashlightdc.flashlight.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "stat_state_legislation")
public class StatStateLegislation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "congress")
    private Integer congress;

    @Column(name = "state")
    private String state;

    @Column(name = "bill_count")
    private Long billCount;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;
}
