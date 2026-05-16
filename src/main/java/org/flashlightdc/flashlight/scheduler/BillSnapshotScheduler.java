package org.flashlightdc.flashlight.scheduler;


import org.flashlightdc.flashlight.entity.BillCountSnapshot;
import org.flashlightdc.flashlight.repository.BillCountSnapshotRepository;
import org.flashlightdc.flashlight.repository.BillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BillSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillSnapshotScheduler.class);

    @Value("${ingestion.congress}")
    private int congress;

    private final BillRepository billRepository;
    private final BillCountSnapshotRepository snapshotRepository;

    public BillSnapshotScheduler(BillRepository billRepository,
                                 BillCountSnapshotRepository snapshotRepository) {
        this.billRepository = billRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Scheduled(cron = "${stats.snapshot.cron}")
    public void snapshot() {
        log.info("Taking bill count snapshot for congress {}", congress);
        long count = billRepository.countByCongress(congress);
        BillCountSnapshot snapshot = new BillCountSnapshot();
        snapshot.setCongress(congress);
        snapshot.setBillCount(count);
        snapshot.setSnapshotAt(LocalDateTime.now());
        snapshotRepository.save(snapshot);
        log.info("Saved bill count snapshot: {} bills", count);
    }
}