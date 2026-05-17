package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.dto.BillStatsDto;
import org.flashlightdc.flashlight.dto.CosponsorAveragesDto;
import org.flashlightdc.flashlight.dto.PartyBreakdownDto;
import org.flashlightdc.flashlight.dto.StateLegislationDto;
import org.flashlightdc.flashlight.repository.BillCountSnapshotRepository;
import org.flashlightdc.flashlight.repository.BillRepository;
import org.flashlightdc.flashlight.repository.StatCosponsorAveragesRepository;
import org.flashlightdc.flashlight.repository.StatsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatsService {

    private final StatsRepository statsRepository;
    private final BillRepository billRepository;
    private final BillCountSnapshotRepository snapshotRepository;
    private final StatCosponsorAveragesRepository statCosponsorAveragesRepository;
    public StatsService(StatsRepository statsRepository,
                        BillRepository billRepository,
                        BillCountSnapshotRepository snapshotRepository,
                        StatCosponsorAveragesRepository statCosponsorAveragesRepository) {
        this.statsRepository = statsRepository;
        this.billRepository = billRepository;
        this.snapshotRepository = snapshotRepository;
        this.statCosponsorAveragesRepository = statCosponsorAveragesRepository;
    }

    public List<PartyBreakdownDto> getPartyBreakdown(int congress) {
        return statsRepository.getPartyBreakdown(congress).stream()
                .map(row -> new PartyBreakdownDto(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<StateLegislationDto> getStateLegislation(int congress) {
        return statsRepository.getStateLegislation(congress).stream()
                .map(row -> new StateLegislationDto(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public CosponsorAveragesDto getCosponsorAverages(int congress) {
        return statCosponsorAveragesRepository
                .findTopByCongressOrderByComputedAtDesc(congress)
                .map(s -> new CosponsorAveragesDto(
                        s.getAvgCosponsorsPerBill(),
                        s.getMaxCosponsors(),
                        s.getBillsWithCosponsors(),
                        s.getTotalBills(),
                        s.getBipartisanBills()
                ))
                .orElseGet(() -> {
                    Object[] row = statsRepository.getCosponsorAverages(congress);
                    long bipartisanBills = statsRepository.getBipartisanBillCount(congress);
                    return new CosponsorAveragesDto(
                            BigDecimal.valueOf(((Number) row[0]).doubleValue()),
                            ((Number) row[1]).longValue(),
                            ((Number) row[2]).longValue(),
                            ((Number) row[3]).longValue(),
                            bipartisanBills
                    );
                });
    }

    public BillStatsDto getBillStats(int congress) {
        long currentCount = billRepository.countByCongress(congress);
        long summarizedCount = billRepository.countByCongressAndSummaryIsNotNull(congress);
        long delta = snapshotRepository
                .findTopByCongressAndSnapshotAtBeforeOrderBySnapshotAtDesc(
                        congress, LocalDateTime.now().minusHours(24)
                )
                .map(snapshot -> currentCount - snapshot.getBillCount())
                .orElse(0L);
        return new BillStatsDto(currentCount, summarizedCount, delta);
    }
}
