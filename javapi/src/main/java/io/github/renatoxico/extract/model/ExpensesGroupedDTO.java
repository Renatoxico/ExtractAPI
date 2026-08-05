package io.github.renatoxico.extract.model;


import java.math.BigDecimal;

public class ExpensesGroupedDTO {
    private String expenseName;
    private BigDecimal total;
    private Long instances;
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getInstances() {
        return instances;
    }

    public void setInstances(Long instances) {
        this.instances = instances;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public ExpensesGroupedDTO (String expenseName, BigDecimal total, Long instances, String category) {
        this.expenseName = expenseName;
        this.total = total;
        this.instances = instances;
        this.category = category;
    }
}
