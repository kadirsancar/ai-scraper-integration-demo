package com.kadir.aipage.repository;

import com.kadir.aipage.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdOrderByCheckedAtAsc(Long productId);

    List<PriceHistory> findTop10ByProductIdOrderByCheckedAtDesc(Long productId);

}