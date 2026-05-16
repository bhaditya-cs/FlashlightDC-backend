package org.flashlightdc.flashlight.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "cosponsors")
public class Cosponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_original")
    private boolean isOriginal;

    @Column(name = "sponsored_date")
    private String sponsoredDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    @JsonIgnore
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bioguide_id")
    @JsonBackReference
    private Member member;

    @JsonProperty("memberName")
    public String getMemberName() {
        return member != null ? member.getName() : null;
    }

    @JsonProperty("memberBioguideId")
    public String getMemberBioguideId() {
        return member != null ? member.getBioguideId() : null;
    }

    @JsonProperty("memberParty")
    public String getMemberParty() {
        return member != null ? member.getPartyName() : null;
    }

    @JsonProperty("memberState")
    public String getMemberState() {
        return member != null ? member.getState() : null;
    }
}
