package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.api.v2.CategoryCorrectionRequest;
import io.github.renatoxico.extract.api.v2.ManualExpenseRequest;
import io.github.renatoxico.extract.api.v2.ReportResponse;
import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.service.ExpenseEditingService;
import io.github.renatoxico.extract.service.V2ReportMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/reports/{reportId}/expenses")
public class ExpenseEditingController {
    private final ExpenseEditingService service;
    private final V2ReportMapper mapper;

    public ExpenseEditingController(ExpenseEditingService service, V2ReportMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PatchMapping("/category")
    public ReportResponse correctCategory(@PathVariable String reportId,
                                         @RequestBody CategoryCorrectionRequest request,
                                         @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return mapper.toResponse(service.correctCategory(reportId, principal.localUserId(), request));
    }

    @DeleteMapping("/{expenseId}")
    public ReportResponse removeExpense(@PathVariable String reportId, @PathVariable Long expenseId,
                                       @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return mapper.toResponse(service.removeExpense(reportId, principal.localUserId(), expenseId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse addExpense(@PathVariable String reportId, @RequestBody ManualExpenseRequest request,
                                    @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return mapper.toResponse(service.addExpense(reportId, principal.localUserId(), request));
    }
}
