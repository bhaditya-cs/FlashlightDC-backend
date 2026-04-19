package org.flashlightdc.flashlight.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "terms")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chamber")
    private String chamber;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bioguide_id")
    private Member member;
}
