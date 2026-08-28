package com.kadir.aipage.repository;

import com.kadir.aipage.entity.ScraperLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScraperLogRepository extends JpaRepository<ScraperLog, Long> {
}