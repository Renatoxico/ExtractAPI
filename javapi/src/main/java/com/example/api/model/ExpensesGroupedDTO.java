package com.example.api.model;


import java.math.BigDecimal;

public class ExpensesGroupedDTO {
    private String expenseName;
    private BigDecimal total;
    private Long instances;

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

    public ExpensesGroupedDTO (String expenseName, BigDecimal total, Long instances) {
        this.expenseName = expenseName;
        this.total = total;
        this.instances = instances;
    }
}
