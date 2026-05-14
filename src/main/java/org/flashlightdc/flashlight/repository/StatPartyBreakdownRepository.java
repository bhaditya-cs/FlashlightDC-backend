package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.StatPartyBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StatPartyBreakdownRepository extends JpaRepository<StatPartyBreakdown, Long> {
    List<StatPartyBreakdown> findByCongress(Integer congress);

    @Transactional
    void deleteByCongress(Integer congress);
}
