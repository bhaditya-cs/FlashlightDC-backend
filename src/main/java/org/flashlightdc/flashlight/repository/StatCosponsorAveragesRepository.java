package org.flashlightdc.flashlight.repository;


import org.flashlightdc.flashlight.entity.StatCosponsorAverages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface StatCosponsorAveragesRepository extends JpaRepository<StatCosponsorAverages, Long> {
    Optional<StatCosponsorAverages> findTopByCongressOrderByComputedAtDesc(Integer congress);

    @Transactional
    void deleteByCongress(Integer congress);
}
