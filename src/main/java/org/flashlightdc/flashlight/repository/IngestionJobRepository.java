package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {

    Optional<IngestionJob> findByJobTypeAndCongressAndStatusAndPhase(
            String jobType, Integer congress,
            IngestionJob.JobStatus status,
            IngestionJob.JobPhase phase
    );

    boolean existsByJobTypeAndCongressAndStatusAndPhase(
            String jobType, Integer congress,
            IngestionJob.JobStatus status,
            IngestionJob.JobPhase phase
    );

    Optional<IngestionJob> findByJobTypeAndCongressAndStatus(
            String jobType, Integer congress, IngestionJob.JobStatus status
    );

    Optional<IngestionJob> findByJobTypeAndCongressAndPhase(
            String jobType, Integer congress, IngestionJob.JobPhase phase
    );

    void deleteByJobTypeAndCongressAndPhase(
            String jobType, Integer congress, IngestionJob.JobPhase phase
    );
}