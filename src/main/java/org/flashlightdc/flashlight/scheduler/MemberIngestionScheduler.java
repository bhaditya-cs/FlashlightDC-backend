package org.flashlightdc.flashlight.scheduler;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.MemberDto;
import org.flashlightdc.flashlight.dto.MemberListResponse;
import org.flashlightdc.flashlight.entity.IngestionJob;
import org.flashlightdc.flashlight.repository.IngestionJobRepository;
import org.flashlightdc.flashlight.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MemberIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(MemberIngestionScheduler.class);
    private static final int PAGE_SIZE = 250;
    private static final int CONGRESS = 119;
    private static final int DELAY_MS = 1000;

    private final CongressApiClient congressApiClient;
    private final MemberService memberService;
    private final IngestionJobRepository ingestionJobRepository;

    public MemberIngestionScheduler(CongressApiClient congressApiClient,
                                    MemberService memberService,
                                    IngestionJobRepository ingestionJobRepository) {
        this.congressApiClient = congressApiClient;
        this.memberService = memberService;
        this.ingestionJobRepository = ingestionJobRepository;
    }

    @Scheduled(cron = "${ingestion.member.cron}")
    public void ingestMembers() {
        recoverStuckJobs();

        boolean alreadyComplete = ingestionJobRepository
                .existsByJobTypeAndCongressAndStatusAndPhase(
                        "MEMBERS", CONGRESS,
                        IngestionJob.JobStatus.COMPLETED,
                        IngestionJob.JobPhase.LIST
                );

        if (alreadyComplete) {
            log.info("Member ingestion already complete, skipping run");
            return;
        }

        log.info("Starting member ingestion for congress {}", CONGRESS);

        IngestionJob job = ingestionJobRepository
                .findByJobTypeAndCongressAndStatusAndPhase(
                        "MEMBERS", CONGRESS,
                        IngestionJob.JobStatus.PAUSED,
                        IngestionJob.JobPhase.LIST
                )
                .orElseGet(this::createNewJob);

        job.setStatus(IngestionJob.JobStatus.RUNNING);
        job.setUpdatedAt(LocalDateTime.now());
        ingestionJobRepository.save(job);

        try {
            runMemberIngestion(job);
        } catch (Exception e) {
            log.error("Member ingestion failed at offset {}", job.getCurrentOffset(), e);
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);
        }
    }

    private void recoverStuckJobs() {
        ingestionJobRepository
                .findByJobTypeAndCongressAndStatus(
                        "MEMBERS", CONGRESS, IngestionJob.JobStatus.RUNNING
                )
                .ifPresent(job -> {
                    log.warn("Found stuck RUNNING member job at offset {}, resetting to PAUSED",
                            job.getCurrentOffset());
                    job.setStatus(IngestionJob.JobStatus.PAUSED);
                    job.setUpdatedAt(LocalDateTime.now());
                    ingestionJobRepository.save(job);
                });
    }

    private void runMemberIngestion(IngestionJob job) throws InterruptedException {
        int offset = job.getCurrentOffset();

        while (true) {
            log.info("MEMBERS — fetching offset={} congress={}", offset, CONGRESS);

            MemberListResponse response = congressApiClient
                    .getMembers(CONGRESS, PAGE_SIZE, offset)
                    .block();

            if (response == null || response.members() == null || response.members().isEmpty()) {
                log.info("MEMBERS — empty response at offset {}, marking complete", offset);
                markComplete(job);
                break;
            }

            if (job.getTotalCount() == null) {
                job.setTotalCount(response.pagination().count());
                log.info("MEMBERS — total members to ingest: {}", response.pagination().count());
            }

            for (MemberDto member : response.members()) {
                try {
                    memberService.saveMember(member);
                } catch (Exception e) {
                    log.warn("Failed to save member {}", member.bioguideId(), e);
                }
            }

            offset += PAGE_SIZE;
            job.setCurrentOffset(offset);
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);

            log.info("MEMBERS — saved offset={} / total={}", offset, job.getTotalCount());

            if (offset >= response.pagination().count()) {
                log.info("MEMBERS ingestion complete. Total: {}", response.pagination().count());
                markComplete(job);
                break;
            }

            Thread.sleep(DELAY_MS);
        }
    }

    private void markComplete(IngestionJob job) {
        job.setStatus(IngestionJob.JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        ingestionJobRepository.save(job);
    }

    private IngestionJob createNewJob() {
        IngestionJob job = new IngestionJob();
        job.setJobType("MEMBERS");
        job.setCongress(CONGRESS);
        job.setPhase(IngestionJob.JobPhase.LIST);
        job.setCurrentOffset(0);
        job.setStatus(IngestionJob.JobStatus.PAUSED);
        job.setStartedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return ingestionJobRepository.save(job);
    }
}