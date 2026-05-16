package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// repository/BillRepository.java
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByCongressAndBillTypeAndBillNumber(
            Integer congress, String billType, String billNumber
    );

    List<Bill> findByCongress(Integer congress);

    List<Bill> findByPolicyArea(String policyArea);

    List<Bill> findByOriginChamber(String originChamber);

    Page<Bill> findByCongress(Integer congress, Pageable pageable);

    Page<Bill> findByPolicyAreaAndCongress(String policyArea, Integer congress, Pageable pageable);

    Page<Bill> findByCongressAndSummaryIsNull(Integer congress, Pageable pageable);

    long countByCongress(Integer congress);

    long countByCongressAndSummaryIsNotNull(Integer congress);
}