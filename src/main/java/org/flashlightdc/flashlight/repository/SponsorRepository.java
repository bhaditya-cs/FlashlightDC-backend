package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.Sponsor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {

    List<Sponsor> findByBill_Id(Long billId);

    List<Sponsor> findByMember_BioguideId(String bioguideId);

    List<Sponsor> findByBill_IdAndMember_BioguideId(Long billId, String bioguideId);

    Page<Sponsor> findByMember_BioguideId(String bioguideId, Pageable pageable);


    void deleteByBillId(Long billId);
}
