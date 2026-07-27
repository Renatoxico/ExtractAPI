package com.example.api.repo;

import com.example.api.model.ExpenseReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, String> {
    boolean existsBySessionIdAndOwnerId(String sessionId, Long ownerId);
}
