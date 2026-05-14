package org.flashlightdc.flashlight.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "stat_party_breakdown")
public class StatPartyBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "congress")
    private Integer congress;

    @Column(name = "party")
    private String party;

    @Column(name = "bill_count")
    private Long billCount;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;
}