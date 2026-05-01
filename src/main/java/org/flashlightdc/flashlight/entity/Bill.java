package org.flashlightdc.flashlight.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "bills", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"congress", "bill_type", "bill_number"})
})
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "congress")
    private Integer congress;

    @Column(name = "bill_number")
    private String billNumber;

    @Column(name = "bill_type")
    private String billType;

    @Column(name = "title", length = 2000)
    private String title;

    @Column(name = "origin_chamber")
    private String originChamber;

    @Column(name = "introduced_date")
    private String introducedDate;

    @Column(name = "latest_action_date")
    private String latestActionDate;

    @Column(name = "latest_action_text", length = 2000)
    private String latestActionText;

    @Column(name = "policy_area")
    private String policyArea;

    @Column(name = "url", length = 500)
    private String url;

    @Lob
    @Column(name = "summary")
    private String summary;

    @Column(name = "summary_updated_at")
    private LocalDateTime summaryUpdatedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sponsor> sponsors = new ArrayList<>();
}
