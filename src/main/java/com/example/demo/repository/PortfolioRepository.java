package com.example.demo.repository;

import com.example.demo.models.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PortfolioRepository {
    Map<Integer, Portfolio> portfolioDatabase = new ConcurrentHashMap<>();

    public PortfolioRepository(Map<Integer, Portfolio> portfolioDatabase) {
        this.portfolioDatabase = new ConcurrentHashMap<>();
    }

    public synchronized void addPortfolio(Portfolio portfolio) {
        System.out.println("Portfolio added with id" + portfolio.getId());
        portfolioDatabase.putIfAbsent(portfolio.getId(), portfolio);
    }

    public synchronized void removePortfolio(Portfolio portfolio) {
        System.out.println("Portfolio removed with id" + portfolio.getId());
        portfolioDatabase.remove(portfolio.getId());
    }

    public synchronized void updatePortfolio(Portfolio portfolio) {

        portfolioDatabase.put(portfolio.getId(), portfolio);

        System.out.println("Portfolio balance updated with id" + portfolio.getId());
    }

    public synchronized Portfolio getPortfolio(int userId) throws Exception {
        Portfolio currenPortfolio = portfolioDatabase.get(userId);

        if (currenPortfolio == null) {
            throw new Exception("No portfolio for the userId: " + userId);
        }
        return currenPortfolio;
    }
}
