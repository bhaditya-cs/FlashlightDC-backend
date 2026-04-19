package org.flashlightdc.flashlight.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "members")
public class Member {

    @Id
    @Column(name = "bioguide_id")
    private String bioguideId;

    @Column(name = "name")
    private String name;

    @Column(name = "party_name")
    private String partyName;

    @Column(name = "state")
    private String state;

    @Column(name = "district")
    private String district;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "attribution", length = 500)
    private String attribution;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Term> terms = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Sponsor> sponsorships = new ArrayList<>();
}
