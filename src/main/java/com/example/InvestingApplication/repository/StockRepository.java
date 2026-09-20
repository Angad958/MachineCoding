package com.example.InvestingApplication.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.InvestingApplication.models.*;

public class StockRepository {
    Map<Integer, Stock> stockDatabase = new ConcurrentHashMap<>();

    public StockRepository(Map<Integer, Stock> stockDatabase) {
        this.stockDatabase = stockDatabase;
    }

    public synchronized void addStock(Stock stock) {
        System.out.println("Stock added with id " + stock.getId());
        stockDatabase.putIfAbsent(stock.getId(), stock);
    }

    public synchronized void removeStock(Stock stock) {
        System.out.println("Stock removed with id " + stock.getId());
        stockDatabase.remove(stock.getId());
    }

    public synchronized Stock getStockById(int stockId) throws Exception {
        Stock stock = stockDatabase.get(stockId);
        if (stock == null) {
            throw new Exception("No stock found with id: " + stockId);
        }
        return stock;
    }

    public synchronized void updatePrice(int stockId, float newPrice) throws Exception{
        System.out.println("Stock price updated with id " + stockId);
        Stock tempValue = stockDatabase.get(stockId);
        if (tempValue == null) {
            throw new Exception("No stock found with id: " + stockId);
        }
        tempValue.setCurrentPrice(newPrice);
    }
}
