package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.StatStateLegislation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StatStateLegislationRepository extends JpaRepository<StatStateLegislation, Long> {
    List<StatStateLegislation> findByCongress(Integer congress);

    @Transactional
    void deleteByCongress(Integer congress);
}