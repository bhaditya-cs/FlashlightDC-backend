package org.flashlightdc.flashlight.scheduler;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.BillSummaryDto;
import org.flashlightdc.flashlight.dto.CosponsorListResponse;
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
public class BillIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillIngestionScheduler.class);
    private static final int PAGE_SIZE = 250;
    private static final int DETAIL_PAGE_SIZE = 50;
    private static final int SUMMARY_PAGE_SIZE = 10;
    private static final int DELAY_MS = 1000;
    private static final int SUMMARY_DELAY_MS = 3500;

    @Value("${ingestion.congress}")
    private int congress;

    @Value("${ingestion.bill.refresh:false}")
    private boolean refresh;

    private final CongressApiClient congressApiClient;
    private final BillService billService;
    private final SummarizationService summarizationService;
    private final IngestionJobRepository ingestionJobRepository;

    public BillIngestionScheduler(CongressApiClient congressApiClient,
                                  BillService billService,
                                  SummarizationService summarizationService,
                                  IngestionJobRepository ingestionJobRepository) {
        this.congressApiClient = congressApiClient;
        this.billService = billService;
        this.summarizationService = summarizationService;
        this.ingestionJobRepository = ingestionJobRepository;
    }

    @Scheduled(cron = "${ingestion.bill.cron}")
    public void ingest() {
        recoverStuckJobs();

        if (refresh) {
            log.info("Refresh enabled — deleting old ingestion jobs for BILLS congress {}", congress);
            ingestionJobRepository.deleteByJobTypeAndCongress("BILLS", congress);
        }

        boolean summaryComplete = ingestionJobRepository
                .existsByJobTypeAndCongressAndStatusAndPhase(
                        "BILLS", congress,
                        IngestionJob.JobStatus.COMPLETED,
                        IngestionJob.JobPhase.SUMMARY
                );

        if (summaryComplete) {
            log.info("Bill ingestion (including summary) already complete, skipping run");
            return;
        }

        boolean detailComplete = ingestionJobRepository
                .existsByJobTypeAndCongressAndStatusAndPhase(
                        "BILLS", congress,
                        IngestionJob.JobStatus.COMPLETED,
                        IngestionJob.JobPhase.DETAIL
                );

        if (detailComplete) {
            runPhase(IngestionJob.JobPhase.SUMMARY);
            return;
        }

        boolean listComplete = ingestionJobRepository
                .existsByJobTypeAndCongressAndStatusAndPhase(
                        "BILLS", congress,
                        IngestionJob.JobStatus.COMPLETED,
                        IngestionJob.JobPhase.LIST
                );

        if (!listComplete) {
            runPhase(IngestionJob.JobPhase.LIST);
        } else {
            runPhase(IngestionJob.JobPhase.DETAIL);
        }
    }

    private void recoverStuckJobs() {
        List<IngestionJob> stuckJobs = ingestionJobRepository
                .findByJobTypeAndCongressAndStatus(
                        "BILLS", congress, IngestionJob.JobStatus.RUNNING
                );

        for (IngestionJob job : stuckJobs) {
            log.warn("Found stuck RUNNING job id={} at offset {}, resetting to PAUSED",
                    job.getId(), job.getCurrentOffset());
            job.setStatus(IngestionJob.JobStatus.PAUSED);
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);
        }
    }

    private void runPhase(IngestionJob.JobPhase phase) {
        log.info("Starting BILLS {} phase for congress {}", phase, congress);

        IngestionJob job = ingestionJobRepository
                .findByJobTypeAndCongressAndStatusAndPhase(
                        "BILLS", congress,
                        IngestionJob.JobStatus.PAUSED,
                        phase
                )
                .orElseGet(() -> createNewJob(phase));

        job.setStatus(IngestionJob.JobStatus.RUNNING);
        job.setUpdatedAt(LocalDateTime.now());
        ingestionJobRepository.save(job);

        try {
            if (phase == IngestionJob.JobPhase.LIST) {
                runListPhase(job);
            } else if (phase == IngestionJob.JobPhase.DETAIL) {
                runDetailPhase(job);
            } else {
                runSummaryPhase(job);
            }
        } catch (Exception e) {
            log.error("BILLS {} phase failed at offset {}", phase, job.getCurrentOffset(), e);
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);
        }
    }

    private void runListPhase(IngestionJob job) throws InterruptedException {
        int offset = job.getCurrentOffset();

        while (true) {
            log.info("LIST phase — fetching offset={} congress={}", offset, congress);

            BillListResponse response = congressApiClient
                    .getBills(congress, PAGE_SIZE, offset)
                    .block();

            if (response == null || response.bills() == null || response.bills().isEmpty()) {
                log.info("LIST phase — empty response at offset {}, marking complete", offset);
                markComplete(job);
                break;
            }

            if (job.getTotalCount() == null) {
                job.setTotalCount(response.pagination().count());
                log.info("LIST phase — total bills to ingest: {}", response.pagination().count());
            }

            for (BillSummaryDto bill : response.bills()) {
                try {
                    billService.saveBill(bill);
                } catch (Exception e) {
                    log.warn("Failed to save bill summary {}/{}/{}",
                            bill.congress(), bill.type(), bill.number(), e);
                }
            }

            offset += PAGE_SIZE;
            job.setCurrentOffset(offset);
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);

            log.info("LIST phase — saved offset={} / total={}", offset, job.getTotalCount());

            if (offset >= response.pagination().count()) {
                log.info("LIST phase complete. Total bills ingested: {}", response.pagination().count());
                markComplete(job);
                break;
            }

            Thread.sleep(DELAY_MS);
        }
    }

    private void runDetailPhase(IngestionJob job) throws InterruptedException {
        int offset = job.getCurrentOffset();

        while (true) {
            log.info("DETAIL phase — processing bills from offset={}", offset);

            Page<Bill> page = billService.findByCongressPaginated(
                    congress, PageRequest.of(offset / DETAIL_PAGE_SIZE, DETAIL_PAGE_SIZE)
            );

            if (page.isEmpty()) {
                log.info("DETAIL phase complete — no more bills to enrich");
                markComplete(job);
                break;
            }

            for (Bill bill : page.getContent()) {
                try {
                    int billNumber = Integer.parseInt(bill.getBillNumber());
                    BillDetailResponse detail = congressApiClient
                            .getBill(bill.getCongress(), bill.getBillType(), billNumber)
                            .block();

                    if (detail != null && detail.bill() != null) {
                        billService.saveBill(detail.bill());
                    }

                    Thread.sleep(DELAY_MS);

                    // fetch and save cosponsors
                    try {
                        CosponsorListResponse cosponsorResponse = congressApiClient
                                .getCosponsors(bill.getCongress(), bill.getBillType(), billNumber)
                                .block();
                        log.info("test {}", cosponsorResponse);
                        if (cosponsorResponse != null && cosponsorResponse.cosponsors() != null) {
                            billService.saveCosponsors(
                                    bill.getCongress(),
                                    bill.getBillType(),
                                    bill.getBillNumber(),
                                    cosponsorResponse.cosponsors()
                            );
                            log.info("Saved {} cosponsors for bill {}/{}/{}",
                                    cosponsorResponse.cosponsors().size(),
                                    bill.getCongress(), bill.getBillType(), bill.getBillNumber());
                        }
                    } catch (Exception e) {
                        log.warn("Failed cosponsor fetch for bill {}/{}/{}",
                                bill.getCongress(), bill.getBillType(), bill.getBillNumber(), e);
                    }

                    Thread.sleep(DELAY_MS);

                    // generate AI summary if not already present
                    try {
                        if (bill.getSummary() == null || bill.getSummary().isEmpty()) {
                            SummaryResponse response = summarizationService
                                    .summarizeBillBlocking(
                                            bill.getCongress(),
                                            bill.getBillType(),
                                            String.valueOf(billNumber)
                                    );
                            log.info("Summarized bill {}/{}/{}: status={}",
                                    bill.getCongress(), bill.getBillType(),
                                    billNumber, response.getStatus());
                            Thread.sleep(SUMMARY_DELAY_MS);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to summarize bill {}/{}/{}",
                                bill.getCongress(), bill.getBillType(), bill.getBillNumber(), e);
                    }

                } catch (NumberFormatException e) {
                    log.warn("Skipping bill with non-numeric number: {}", bill.getBillNumber());
                } catch (Exception e) {
                    log.warn("Failed detail fetch for bill {}/{}/{}",
                            bill.getCongress(), bill.getBillType(), bill.getBillNumber(), e);
                }
            }

            offset += DETAIL_PAGE_SIZE;
            job.setCurrentOffset(offset);
            job.setUpdatedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);

            log.info("DETAIL phase — enriched offset={} / total={}",
                    offset, job.getTotalCount());

            if (!page.hasNext()) {
                log.info("DETAIL phase complete.");
                markComplete(job);
                break;
            }

            Thread.sleep(DELAY_MS * 2);
        }
    }

    private void runSummaryPhase(IngestionJob job) throws InterruptedException {
        int offset = job.getCurrentOffset();

        while (true) {
            log.info("SUMMARY phase — processing bills from offset={}", offset);

            Page<Bill> page = billService.findByCongressAndSummaryIsNull(
                    congress, PageRequest.of(offset / SUMMARY_PAGE_SIZE, SUMMARY_PAGE_SIZE)
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

                Thread.sleep(SUMMARY_DELAY_MS);
            }

            offset += SUMMARY_PAGE_SIZE;
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

            Thread.sleep(SUMMARY_DELAY_MS);
        }
    }

    private void markComplete(IngestionJob job) {
        job.setStatus(IngestionJob.JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        ingestionJobRepository.save(job);
    }

    private IngestionJob createNewJob(IngestionJob.JobPhase phase) {
        IngestionJob job = new IngestionJob();
        job.setJobType("BILLS");
        job.setCongress(congress);
        job.setPhase(phase);
        job.setCurrentOffset(0);
        job.setStatus(IngestionJob.JobStatus.PAUSED);
        job.setStartedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return ingestionJobRepository.save(job);
    }
}
