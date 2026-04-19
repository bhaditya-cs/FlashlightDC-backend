package org.flashlightdc.flashlight.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sponsors")
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_by_request")
    private boolean isByRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bioguide_id")
    private Member member;
}
