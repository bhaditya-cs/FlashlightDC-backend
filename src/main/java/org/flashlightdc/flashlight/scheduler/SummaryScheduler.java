package org.flashlightdc.flashlight.scheduler;

import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.entity.IngestionJob;
import org.flashlightdc.flashlight.repository.IngestionJobRepository;
import org.flashlightdc.flashlight.service.BillService;
import org.flashlightdc.flashlight.service.SummarizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SummaryScheduler.class);
    private static final int PAGE_SIZE = 10;
    private static final int DELAY_MS = 3500;

    @Value("${ingestion.congress}")
    private int congress;

    private final BillService billService;
    private final SummarizationService summarizationService;
    private final IngestionJobRepository ingestionJobRepository;

    public SummaryScheduler(BillService billService,
                            SummarizationService summarizationService,
                            IngestionJobRepository ingestionJobRepository) {
        this.billService = billService;
        this.summarizationService = summarizationService;
        this.ingestionJobRepository = ingestionJobRepository;
    }

    @Scheduled(cron = "${ingestion.summary.cron}")
    public void summarize() {
        recoverStuckJobs();

        // Quick check: are there any bills that need summaries?
        long pending = billService.findByCongressAndSummaryIsNull(
                congress, PageRequest.of(0, 1)
        ).getTotalElements();

        if (pending == 0) {
            log.debug("No bills with missing summaries for congress {}, nothing to do", congress);
            return;
        }

        log.info("Found {} bills with missing summaries for congress {}, starting catch-up", pending, congress);

        IngestionJob job = ingestionJobRepository
                .findByJobTypeAndCongressAndStatusAndPhase(
                        "BILLS", congress,
                        IngestionJob.JobStatus.PAUSED,
                        IngestionJob.JobPhase.SUMMARY
                )
                .orElseGet(this::createNewJob);

        job.setStatus(IngestionJob.JobStatus.RUNNING);
        job.setUpdatedAt(LocalDateTime.now());
        ingestionJobRepository.save(job);

        try {
            runSummaryIngestion(job);
        } catch (Exception e) {
            log.error("Summary ingestion failed at offset {}", job.getCurrentOffset(), e);
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);
        }
    }

    private void recoverStuckJobs() {
        List<IngestionJob> stuckJobs = ingestionJobRepository
                .findByJobTypeAndCongressAndStatus(
                        "BILLS", congress, IngestionJob.JobStatus.RUNNING
                );

        for (IngestionJob job : stuckJobs) {
            if (job.getPhase() != IngestionJob.JobPhase.SUMMARY) {
                continue;
            }
            log.warn("Found stuck RUNNING summary job id={} at offset {}, resetting to PAUSED",
                    job.getId(), job.getCurrentOffset());
            job.setStatus(IngestionJob.JobStatus.PAUSED);
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);
        }
    }

    private void runSummaryIngestion(IngestionJob job) throws InterruptedException {
        int offset = job.getCurrentOffset();

        while (true) {
            log.info("SUMMARY phase — processing bills from offset={}", offset);

            Page<Bill> page = billService.findByCongressAndSummaryIsNull(
                    congress, PageRequest.of(offset / PAGE_SIZE, PAGE_SIZE)
            );

            if (page.isEmpty()) {
                log.info("SUMMARY phase complete — no more bills to summarize");
                markComplete(job);
                break;
            }

            if (job.getTotalCount() == null) {
                job.setTotalCount((int) page.getTotalElements());
                log.info("SUMMARY phase — total bills to summarize: {}", page.getTotalElements());
            } else {
                job.setTotalCount((int) page.getTotalElements());
            }

            for (Bill bill : page.getContent()) {
                try {
                    SummaryResponse response = summarizationService
                            .summarizeBillBlocking(
                                    bill.getCongress(),
                                    bill.getBillType(),
                                    bill.getBillNumber()
                            );

                    log.info("Summarized bill {}/{}/{}: status={}",
                            bill.getCongress(), bill.getBillType(),
                            bill.getBillNumber(), response.getStatus());

                } catch (Exception e) {
                    log.warn("Failed to summarize bill {}/{}/{}",
                            bill.getCongress(), bill.getBillType(),
                            bill.getBillNumber(), e);
                }

                Thread.sleep(DELAY_MS);
            }

            offset += PAGE_SIZE;
            job.setCurrentOffset(offset);
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);

            log.info("SUMMARY phase — processed offset={} / total={}",
                    offset, job.getTotalCount());

            if (!page.hasNext()) {
                log.info("SUMMARY phase complete.");
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
        job.setJobType("BILLS");
        job.setCongress(congress);
        job.setPhase(IngestionJob.JobPhase.SUMMARY);
        job.setCurrentOffset(0);
        job.setStatus(IngestionJob.JobStatus.PAUSED);
        job.setStartedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return ingestionJobRepository.save(job);
    }
}
