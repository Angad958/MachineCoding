package com.example.demo.service;

import java.util.Map;

import com.example.demo.models.*;
import com.example.demo.models.Portfolio.StockMetaData;
import com.example.demo.models.User.Role;
import com.example.demo.repository.StockRepository;

public class StockService {
    StockRepository stockRepository;
    PortfolioService portfolioService;
    TransactionService transactionService;
    UserService userService;

    public StockService(StockRepository stockRepository,
            PortfolioService portfolioService, TransactionService transactionService, UserService userService) {
        this.stockRepository = stockRepository;
        this.portfolioService = portfolioService;
        this.transactionService = transactionService;
        this.userService = userService;
    }

    public synchronized void addUser(User user) {
        System.out.println("Adding user with id " + user.getId());
        try {
            userService.addUser(user);
        } catch (Exception e) {
            System.out
                    .println("exception occured while adding user with id " + user.getId() + "Please try again... " + e
                            .getMessage());
        }

    }

    public synchronized void buyStock(int userId, int stockId, int qty) {
        try {
            User user = userService.getUser(userId);
            Stock stock = stockRepository.getStockById(stockId);
            if (user == null) {
                System.err.println("user doesn't exist with id " + userId);
                return;
            }
            transactionService.createTransaction(user, stockId, qty, stock.getCurrentPrice(), "BUY");
            userService.updateUserBalance(user, stockId, qty, "BUY");
            portfolioService.updatePortfolio(user, stockId, qty, "BUY");
        } catch (Exception e) {
            System.err.println("error occured while buying stock " + stockId + " error : " + e.getMessage());
        }

    }

    public synchronized void sellStock(int userId, int stockId, int qty) {
        try {
            System.out.println("Selling stock " + stockId + " By user "+ userId + " Qty : "+qty);
            User user = userService.getUser(userId);
            Stock stock = stockRepository.getStockById(stockId);
            Portfolio portfolio = portfolioService.gePortfolio(userId);
            if (user == null || portfolio == null || !isHaveEnoughQauntity(portfolio, stockId, qty)) {
                System.err.println("user doesn't exist with id || doesn't have enough qty " + userId);
                return;
            }
            transactionService.createTransaction(user, stockId, qty, stock.getCurrentPrice(), "SELL");
            userService.updateUserBalance(user, stockId, qty, "SELL");
            portfolioService.updatePortfolio(user, stockId, qty, "SELL");
        } catch (Exception e) {
            System.err.println("error occured while selling stock " + stockId + " error : " + e.getMessage());
        }
    }

    private boolean isHaveEnoughQauntity(Portfolio portfolio, int stockId, int qty) {
        Map<Integer, StockMetaData> holdings = portfolio.getHoldings();
        StockMetaData stockMetaData = holdings.get(stockId);
        if (stockMetaData == null) {
            System.err.println("User " + portfolio.getUser_id() + " doesn't hold stock " + stockId);
            return false;
        }
        if (stockMetaData.getQty() < qty) {
            System.err.println("User " + portfolio.getUser_id()
                    + " Doesn't have enough quantity to sell please sell less than or equal to "
                    + stockMetaData.getQty());
            return false;
        }

        return true;
    }

    public synchronized void addStock(Stock stock) {
        stockRepository.addStock(stock);
    }

    public void viewPortfolio(int userId) {
        portfolioService.viewPortfolioByUserId(userId);
    }

    public synchronized void updateStockPrice(int userId, int stockId, int price) {
        User user = userService.getUser(userId);
        if (user == null || user.getRole() != Role.ADMIN) {
            System.err.println("user doesn't exist with id or doesn't have permission " + userId);
            return;
        }
        try {
            stockRepository.updatePrice(stockId, price);
        } catch (Exception e) {
            System.err.println("error occured while updating stock " + stockId + " error : " + e.getMessage());
        }

    }
}
