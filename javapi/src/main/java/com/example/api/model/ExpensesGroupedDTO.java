package com.example.api.model;



public class ExpensesGroupedDTO {
    private String expenseName;
    private Double total;
    private Long instances;

    public Long getInstances() {
        return instances;
    }

    public void setInstances(Long instances) {
        this.instances = instances;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public ExpensesGroupedDTO (String expenseName, Double total, Long instances) {
        this.expenseName = expenseName;
        this.total = total;
        this.instances = instances;
    }
}
