package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseReportAccessService {
    private final ExpenseReportRepository expenseReportRepository;

    public ExpenseReportAccessService(ExpenseReportRepository expenseReportRepository) {
        this.expenseReportRepository = expenseReportRepository;
    }

    @Transactional(readOnly = true)
    public void requireOwnership(String sessionId, Long localUserId) {
        if (!expenseReportRepository.existsBySessionIdAndOwnerId(sessionId, localUserId)) {
            throw new ProcessingException(
                "No report found for the provided session ID",
                HttpStatus.NOT_FOUND,
                "SESSION_NOT_FOUND"
            );
        }
    }
}
