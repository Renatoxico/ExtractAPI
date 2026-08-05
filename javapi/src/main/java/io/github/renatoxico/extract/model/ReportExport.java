package io.github.renatoxico.extract.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportExport {
    public ReportExport(List<ExpensesGroupedDTO> smartGroupExpenselist,
        List<NoteableDay> notableDays,
        List<ExpenseDTO> allExpenses,
        List<ExpensesCategories> expensesByCategory,
        ExpenseDTO biggestSingularExpense,
        String sessionToken) {
        this.smartGroupExpenselist = smartGroupExpenselist;
        this.notableDays = notableDays;
        this.allExpenses = allExpenses;
        this.expensesByCategory = expensesByCategory;
        this.biggestSingularExpense = biggestSingularExpense;
        this.sessionToken = sessionToken;
    }

    @JsonProperty("SmartGroupExpenselist")
    private List<ExpensesGroupedDTO> smartGroupExpenselist;

    @JsonProperty("NotableDays")
    private List<NoteableDay> notableDays;

    @JsonProperty("AllExpenses")
    private List<ExpenseDTO> allExpenses;

    @JsonProperty("ExpensesByCategory")
    private List<ExpensesCategories> expensesByCategory;

    @JsonProperty("BiggestSingularExpense")
    private ExpenseDTO biggestSingularExpense;

    @JsonProperty("sessionToken")
    private String sessionToken;
}
