package org.flashlightdc.flashlight.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bill_count_snapshots")
public class BillCountSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "congress")
    private Integer congress;

    @Column(name = "bill_count")
    private Long billCount;

    @Column(name = "snapshot_at")
    private LocalDateTime snapshotAt;
}