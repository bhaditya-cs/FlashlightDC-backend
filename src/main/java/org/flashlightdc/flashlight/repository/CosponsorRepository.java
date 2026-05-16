package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.Cosponsor;
import org.flashlightdc.flashlight.entity.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CosponsorRepository extends JpaRepository<Cosponsor, Long> {
    List<Cosponsor> findByBill_Id(Long billId);
    List<Cosponsor> findByMember_BioguideId(String bioguideId);


    List<Cosponsor> findByBill_IdAndMember_BioguideId(Long billId, String bioguideId);

    void deleteByBillId(Long billId);
}