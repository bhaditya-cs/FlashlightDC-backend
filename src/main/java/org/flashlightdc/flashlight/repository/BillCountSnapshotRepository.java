package org.flashlightdc.flashlight.repository;


import org.flashlightdc.flashlight.entity.BillCountSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BillCountSnapshotRepository extends JpaRepository<BillCountSnapshot, Long> {

    Optional<BillCountSnapshot> findTopByCongressOrderBySnapshotAtDesc(Integer congress);

    Optional<BillCountSnapshot> findTopByCongressAndSnapshotAtBeforeOrderBySnapshotAtDesc(
            Integer congress, LocalDateTime before
    );
}
