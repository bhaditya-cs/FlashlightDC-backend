package org.flashlightdc.flashlight.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "terms")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @JsonIgnore
    private Member member;
}
