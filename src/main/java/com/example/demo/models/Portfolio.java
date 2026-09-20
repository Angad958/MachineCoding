package com.example.demo.models;

import java.util.*;

public class Portfolio {
    int id;
    int user_id;
    Map<Integer, StockMetaData> holdings;

    public static class StockMetaData {

        float avgPrice;
        int qty;

        public StockMetaData(float avgPrice, int qty) {
            this.avgPrice = avgPrice;
            this.qty = qty;
        }

        public float getAvgPrice() {
            return avgPrice;
        }

        public void setAvgPrice(float avgPrice) {
            this.avgPrice = avgPrice;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }
    }

    public Portfolio(int id, int user_id, Map<Integer, Portfolio.StockMetaData> holdings) {
        this.id = id;
        this.user_id = user_id;
        this.holdings = holdings;
    }

    public int getId() {
        return id;
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

    public Map<Integer, StockMetaData> getHoldings() {
        return holdings;
    }

    public void setHoldings(Map<Integer, StockMetaData> holdings) {
        this.holdings = holdings;
    }
}
