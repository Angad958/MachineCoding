package com.example.InvestingApplication.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.example.InvestingApplication.models.*;
import com.example.InvestingApplication.models.Portfolio.StockMetaData;
import com.example.InvestingApplication.repository.PortfolioRepository;
import com.example.InvestingApplication.repository.StockRepository;
import com.example.InvestingApplication.repository.UserRepository;

public class PortfolioService {
    PortfolioRepository portfolioRepository;
    UserRepository userRepository;
    StockRepository stockRepository;

    public PortfolioService(PortfolioRepository portfolioRepository, StockRepository stockRepository,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
    }

    public void updatePortfolio(User user, int stockId, int qty, String type) {
        try {
            Portfolio currentPortfolio = portfolioRepository.getPortfolio(user.getId());
            Map<Integer, StockMetaData> tempHoldings = currentPortfolio.getHoldings();
            if (type == "BUY")
                updateAvgPriceAndQty(tempHoldings, stockId, qty);
            else {
                updateQty(tempHoldings, stockId, qty);
            }

        } catch (Exception e) {
            System.err.println("Error occur while fetching portfolio " + e);
        }

    }

    public Portfolio gePortfolio(int userId) {
        Portfolio portfolio = null;
        try {
            portfolio = portfolioRepository.getPortfolio(userId);
        } catch (Exception e) {
            System.err.println("User doesn't have portfolio " + userId);
        }
        return portfolio;
    }

    private void updateAvgPriceAndQty(Map<Integer, StockMetaData> tempHoldings, int stockId, int qty) {

        try {
            Stock currenStock = stockRepository.getStockById(stockId);
            StockMetaData stockMetaData = tempHoldings.get(stockId);
            if (stockMetaData == null) {
                tempHoldings.put(stockId, new StockMetaData(currenStock.getCurrentPrice(), qty));
                return;
            }

            float currentStockPrice = currenStock.getCurrentPrice();
            float currentAvgPrice = stockMetaData.getAvgPrice();
            int currentQty = stockMetaData.getQty();
            float finalAvgPrice = ((currentAvgPrice * currentQty) + (currentStockPrice * qty)) / (currentQty + qty);
            int finalQty = currentQty + qty;
            stockMetaData.setAvgPrice(finalAvgPrice);
            stockMetaData.setQty(finalQty);
            tempHoldings.put(stockId, stockMetaData);
        } catch (Exception e) {
            System.err.println("No stock found with id : " + stockId);
        }

    }

    private void updateQty(Map<Integer, StockMetaData> tempHoldings, int stockId, int qty) {
        try {
            StockMetaData stockMetaData = tempHoldings.get(stockId);
            int currentQty = stockMetaData.getQty();
            int finalQty = currentQty - qty;
            stockMetaData.setQty(finalQty);
            tempHoldings.put(stockId, stockMetaData);
        } catch (Exception e) {
            System.err.println("No stock found with id : " + stockId);
        }
    }

    public void viewPortfolioByUserId(int userId) {
        try {
            Portfolio portfolio = portfolioRepository.getPortfolio(userId);
            printPortfolio(portfolio);
        } catch (Exception e) {
            System.err.println("No portfolio found with user_id : " + userId);
        }

    }

    public void printPortfolio(Portfolio portfolio) {
        try {
            User user = userRepository.getUser(portfolio.getUser_id());
            System.out.println("User Portfolio :: name :  " + user.getName() + " Balance: " + user.getBalance());
            float currentMarketValue = 0, currentPortfolioValue = 0;
            float profit = 0;
            Map<Integer, StockMetaData> holdings = portfolio.getHoldings();

            for (Map.Entry<Integer, Portfolio.StockMetaData> e : holdings.entrySet()) {
                System.out
                        .println("StockId: " + e.getKey() + " AvgPrice : " + e.getValue().getAvgPrice() + " Qty : " + e
                                .getValue().getQty());
                currentMarketValue = currentMarketValue + (stockRepository.getStockById(
                        e.getKey())).getCurrentPrice() * e.getValue().getQty();
                currentPortfolioValue = currentPortfolioValue + e.getValue().getAvgPrice() * e.getValue().getQty();
            }
            profit = currentMarketValue - currentPortfolioValue;
            System.out.println("Current Market Portflio Value : " + currentMarketValue);
            System.out.println("Current Portflio Value : " + currentPortfolioValue);
            System.out.println("Profit : " + profit);
        } catch (Exception e) {
            System.err.println("error occurred while viewing portfolio and error is : " + e);
        }

    }

}
