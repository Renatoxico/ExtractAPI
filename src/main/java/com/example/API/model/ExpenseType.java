package com.example.API.model;

import java.util.List;

public class ExpenseType {
    private String token;
    private String type;

    public ExpenseType(String type, String token) {
        this.token = token;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public static List<ExpenseType> load () {
        List<ExpenseType> types = List.of();
        types.add(new ExpenseType("Streaming", "netflix"));
        types.add(new ExpenseType("Streaming", "hbo"));
        types.add(new ExpenseType("Streaming", "globo"));
        types.add(new ExpenseType("Streaming", "disney"));
        types.add(new ExpenseType("Streaming", "hulu"));
        types.add(new ExpenseType("iFood", "ifood"));
        types.add(new ExpenseType("Uber", "uber"));
        return types;
    }
}
