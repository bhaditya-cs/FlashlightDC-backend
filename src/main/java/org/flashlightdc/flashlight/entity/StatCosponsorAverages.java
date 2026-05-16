package org.flashlightdc.flashlight.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "stat_cosponsor_averages")
public class StatCosponsorAverages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "congress")
    private Integer congress;

    @Column(name = "avg_cosponsors_per_bill")
    private BigDecimal avgCosponsorsPerBill;

    @Column(name = "max_cosponsors")
    private Long maxCosponsors;

    @Column(name = "bills_with_cosponsors")
    private Long billsWithCosponsors;

    @Column(name = "bipartisan_bills")
    private Long bipartisanBills;

    @Column(name = "total_bills")
    private Long totalBills;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;
}