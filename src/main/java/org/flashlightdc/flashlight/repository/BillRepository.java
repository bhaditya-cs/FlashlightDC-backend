package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT b.originChamber, COUNT(b) FROM Bill b WHERE b.congress = :congress GROUP BY b.originChamber")
    List<Object[]> countByOriginChamberGrouped(@Param("congress") Integer congress);

    @Query("SELECT b.policyArea, COUNT(b) FROM Bill b WHERE b.congress = :congress AND b.policyArea IS NOT NULL GROUP BY b.policyArea ORDER BY COUNT(b) DESC")
    List<Object[]> countByPolicyAreaGrouped(@Param("congress") Integer congress);

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.congress = :congress AND b.introducedDate >= :startDate AND b.introducedDate < :endDate")
    long countByCongressAndIntroducedDateBetween(@Param("congress") Integer congress, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.congress = :congress AND b.latestActionDate >= :since")
    long countByCongressAndLatestActionDateSince(@Param("congress") Integer congress, @Param("since") String since);
}