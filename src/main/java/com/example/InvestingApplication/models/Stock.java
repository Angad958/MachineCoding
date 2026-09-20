package com.example.InvestingApplication.models;

public class Stock {

    int id;
    String symbol;
    float currentPrice;
    int qty;
    public Stock() {
    }

   

    public Stock(int id, String symbol, float currentPrice, int qty) {
        this.id = id;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.qty = qty;
    }



    @Override
    public String toString() {
        return "Stocks [id=" + id + ", symbol=" + symbol + ", currentPrice=" + currentPrice + "]";
    }



    public int getId() {
        return id;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }



    public int getQty() {
        return qty;
    }



    public void setId(int id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(float currentPrice) {
        this.currentPrice = currentPrice;
    }

}
