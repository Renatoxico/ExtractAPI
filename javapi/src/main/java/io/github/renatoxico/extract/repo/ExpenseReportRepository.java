package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.ExpenseReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, String> {
    boolean existsByIdAndOwnerId(String reportId, Long ownerId);
}
