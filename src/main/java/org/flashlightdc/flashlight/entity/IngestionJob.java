// entity/IngestionJob.java
package org.flashlightdc.flashlight.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ingestion_jobs")
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "congress")
    private Integer congress;

    @Column(name = "current_offset")
    private Integer currentOffset;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "phase")
    @Enumerated(EnumType.STRING)
    private JobPhase phase;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Setter
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public enum JobStatus { RUNNING, PAUSED, COMPLETED, FAILED }
    public enum JobPhase  { LIST, DETAIL, SUMMARY }


}