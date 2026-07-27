package com.example.api.service;

import com.example.api.exception.ProcessingException;
import com.example.api.model.AppUser;
import com.example.api.model.ExpenseReport;
import com.example.api.repo.AppUserRepository;
import com.example.api.repo.ExpenseReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseReportAccessService {
    private final ExpenseReportRepository expenseReportRepository;
    private final AppUserRepository appUserRepository;

    public ExpenseReportAccessService(
        ExpenseReportRepository expenseReportRepository,
        AppUserRepository appUserRepository
    ) {
        this.expenseReportRepository = expenseReportRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public void registerOwnership(String sessionId, Long localUserId) {
        AppUser owner = appUserRepository.getReferenceById(localUserId);
        expenseReportRepository.save(new ExpenseReport(sessionId, owner));
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
