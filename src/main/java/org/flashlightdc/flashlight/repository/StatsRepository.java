package org.flashlightdc.flashlight.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StatsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> getPartyBreakdown(int congress) {
        return entityManager.createNativeQuery("""
            SELECT m.party_name, COUNT(s.id)
            FROM sponsors s
            JOIN members m ON s.bioguide_id = m.bioguide_id
            JOIN bills b ON s.bill_id = b.id
            WHERE b.congress = :congress
            AND m.party_name IS NOT NULL
            GROUP BY m.party_name
            ORDER BY COUNT(s.id) DESC
            """)
                .setParameter("congress", congress)
                .getResultList();
    }

    public List<Object[]> getStateLegislation(int congress) {
        return entityManager.createNativeQuery("""
            SELECT m.state, COUNT(s.id)
            FROM sponsors s
            JOIN members m ON s.bioguide_id = m.bioguide_id
            JOIN bills b ON s.bill_id = b.id
            WHERE b.congress = :congress
            AND m.state IS NOT NULL
            GROUP BY m.state
            ORDER BY COUNT(s.id) DESC
            """)
                .setParameter("congress", congress)
                .getResultList();
    }

    public Object[] getCosponsorAverages(int congress) {
        return (Object[]) entityManager.createNativeQuery("""
            SELECT
                ROUND(AVG(cosponsor_count), 2),
                MAX(cosponsor_count),
                SUM(CASE WHEN cosponsor_count > 0 THEN 1 ELSE 0 END),
                COUNT(*)
            FROM (
                SELECT b.id, COUNT(c.id) as cosponsor_count
                FROM bills b
                LEFT JOIN cosponsors c ON b.id = c.bill_id
                WHERE b.congress = :congress
                GROUP BY b.id
            ) bill_cosponsor_counts
            """)
                .setParameter("congress", congress)
                .getSingleResult();
    }

    public long getBipartisanBillCount(int congress) {
        return ((Number) entityManager.createNativeQuery("""
        SELECT COUNT(DISTINCT b.id)
        FROM bills b
        WHERE b.congress = :congress
        AND EXISTS (
            SELECT 1
            FROM (
                SELECT m.party_name
                FROM sponsors s
                JOIN members m ON s.bioguide_id = m.bioguide_id
                WHERE s.bill_id = b.id
                UNION ALL
                SELECT m.party_name
                FROM cosponsors c
                JOIN members m ON c.bioguide_id = m.bioguide_id
                WHERE c.bill_id = b.id
            ) parties
            WHERE parties.party_name IN ('Democratic', 'Democrat', 'D')
        )
        AND EXISTS (
            SELECT 1
            FROM (
                SELECT m.party_name
                FROM sponsors s
                JOIN members m ON s.bioguide_id = m.bioguide_id
                WHERE s.bill_id = b.id
                UNION ALL
                SELECT m.party_name
                FROM cosponsors c
                JOIN members m ON c.bioguide_id = m.bioguide_id
                WHERE c.bill_id = b.id
            ) parties
            WHERE parties.party_name IN ('Republican', 'R')
        )
        """)
                .setParameter("congress", congress)
                .getSingleResult()).longValue();
    }
}