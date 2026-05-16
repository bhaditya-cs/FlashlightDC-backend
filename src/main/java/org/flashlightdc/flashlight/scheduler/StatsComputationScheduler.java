package org.flashlightdc.flashlight.scheduler;

import org.flashlightdc.flashlight.entity.StatCosponsorAverages;
import org.flashlightdc.flashlight.entity.StatPartyBreakdown;
import org.flashlightdc.flashlight.entity.StatStateLegislation;
import org.flashlightdc.flashlight.repository.StatCosponsorAveragesRepository;
import org.flashlightdc.flashlight.repository.StatPartyBreakdownRepository;
import org.flashlightdc.flashlight.repository.StatStateLegislationRepository;
import org.flashlightdc.flashlight.repository.StatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class StatsComputationScheduler {

    private static final Logger log = LoggerFactory.getLogger(StatsComputationScheduler.class);

    @Value("${ingestion.congress}")
    private int congress;

    private final StatsRepository statsRepository;
    private final StatPartyBreakdownRepository partyBreakdownRepository;
    private final StatStateLegislationRepository stateLegislationRepository;
    private final StatCosponsorAveragesRepository cosponsorAveragesRepository;

    public StatsComputationScheduler(StatsRepository statsRepository, StatPartyBreakdownRepository statPartyBreakdownRepository,
                                     StatStateLegislationRepository statStateLegislationRepository, StatCosponsorAveragesRepository statCosponsorAveragesRepository) {
        this.statsRepository = statsRepository;
        this.partyBreakdownRepository = statPartyBreakdownRepository;
        this.stateLegislationRepository = statStateLegislationRepository;
        this.cosponsorAveragesRepository = statCosponsorAveragesRepository;

    }

    @Scheduled(cron = "${stats.computation.cron}")
    public void computeStats() {
        log.info("Starting stats computation for congress {}", congress);
        computePartyBreakdown();
        computeStateLegislation();
        computeCosponsorAverages();
        log.info("Stats computation complete for congress {}", congress);
    }

    @Transactional
    public void computePartyBreakdown() {
        try {
            log.info("Computing party breakdown...");
            List<Object[]> rows = statsRepository.getPartyBreakdown(congress);
            List<StatPartyBreakdown> stats = rows.stream()
                    .map(row -> {
                        StatPartyBreakdown stat = new StatPartyBreakdown();
                        stat.setCongress(congress);
                        stat.setParty((String) row[0]);
                        stat.setBillCount(((Number) row[1]).longValue());
                        stat.setComputedAt(LocalDateTime.now());
                        return stat;
                    })
                    .toList();
            partyBreakdownRepository.deleteByCongress(congress);
            partyBreakdownRepository.saveAll(stats);
            log.info("Party breakdown computed — {} parties", stats.size());
        } catch (Exception e) {
            log.error("Failed to compute party breakdown", e);
        }
    }

    @Transactional
    public void computeStateLegislation() {
        try {
            log.info("Computing state legislation...");
            List<Object[]> rows = statsRepository.getStateLegislation(congress);
            List<StatStateLegislation> stats = rows.stream()
                    .map(row -> {
                        StatStateLegislation stat = new StatStateLegislation();
                        stat.setCongress(congress);
                        stat.setState((String) row[0]);
                        stat.setBillCount(((Number) row[1]).longValue());
                        stat.setComputedAt(LocalDateTime.now());
                        return stat;
                    })
                    .toList();
            stateLegislationRepository.deleteByCongress(congress);
            stateLegislationRepository.saveAll(stats);
            log.info("State legislation computed — {} states", stats.size());
        } catch (Exception e) {
            log.error("Failed to compute state legislation", e);
        }
    }

    @Transactional
    public void computeCosponsorAverages() {
        try {
            log.info("Computing cosponsor averages...");
            Object[] row = statsRepository.getCosponsorAverages(congress);
            long bipartisanBills = statsRepository.getBipartisanBillCount(congress);

            StatCosponsorAverages stat = new StatCosponsorAverages();
            stat.setCongress(congress);
            stat.setAvgCosponsorsPerBill(BigDecimal.valueOf(((Number) row[0]).doubleValue()));
            stat.setMaxCosponsors(((Number) row[1]).longValue());
            stat.setBillsWithCosponsors(((Number) row[2]).longValue());
            stat.setTotalBills(((Number) row[3]).longValue());
            stat.setBipartisanBills(bipartisanBills);
            stat.setComputedAt(LocalDateTime.now());

            cosponsorAveragesRepository.deleteByCongress(congress);
            cosponsorAveragesRepository.save(stat);
            log.info("Cosponsor averages computed — avg={} bipartisan={}",
                    stat.getAvgCosponsorsPerBill(), bipartisanBills);
        } catch (Exception e) {
            log.error("Failed to compute cosponsor averages", e);
        }
    }
}