package com.example.InvestingApplication.service;

import java.util.concurrent.ConcurrentHashMap;

import com.example.InvestingApplication.models.Portfolio;
import com.example.InvestingApplication.models.Stock;
import com.example.InvestingApplication.models.User;
import com.example.InvestingApplication.repository.*;

public class UserService {
    UserRepository userRepository;
    StockRepository stockRepository;
    PortfolioRepository portfolioRepository;

    public UserService(UserRepository userRepository, StockRepository stockRepository) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
    }

    public UserService(UserRepository userRepository, StockRepository stockRepository,
            PortfolioRepository portfolioRepository) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(int userId) {
        return userRepository.getUser(userId);
    }

    public void addUser(User user) {
        userRepository.addUser(user);
        portfolioRepository.addPortfolio(new Portfolio(user.getId(), user.getId(),new ConcurrentHashMap<>()));
    }

    public void updateUserBalance(User user, int stockId, int qty, String type) {
        try {
            Stock stock = stockRepository.getStockById(stockId);
            float totalStockPrice = (stock.getCurrentPrice() * qty);
            if (type == "BUY") {
                totalStockPrice = -1 * totalStockPrice;
            }

            userRepository.updateUserBalance(user, user.getBalance() + totalStockPrice);
        } catch (Exception e) {

        }
    }

}
