package com.example.demo.models;
public class Transaction {
    int id;
    int user_id;
    float amount;
    int stock_id;
    String type;
    public int getId() {
        return id;
    }
    public Transaction(int id, int user_id, float amount, int stock_id, String type) {
        this.id = id;
        this.user_id = user_id;
        this.amount = amount;
        this.stock_id = stock_id;
        this.type = type;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getUser_id() {
        return user_id;
    }
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
    public float getAmount() {
        return amount;
    }
    public void setAmount(float amount) {
        this.amount = amount;
    }
    public int getStock_id() {
        return stock_id;
    }
    public void setStock_id(int stock_id) {
        this.stock_id = stock_id;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
