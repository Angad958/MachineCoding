package com.example.InvestingApplication;

import java.util.concurrent.ConcurrentHashMap;

import com.example.InvestingApplication.models.Stock;
import com.example.InvestingApplication.models.User;
import com.example.InvestingApplication.models.User.Role;
import com.example.InvestingApplication.repository.PortfolioRepository;
import com.example.InvestingApplication.repository.StockRepository;
import com.example.InvestingApplication.repository.TransactionRepository;
import com.example.InvestingApplication.repository.UserRepository;
import com.example.InvestingApplication.service.PortfolioService;
import com.example.InvestingApplication.service.StockService;
import com.example.InvestingApplication.service.TransactionService;
import com.example.InvestingApplication.service.UserService;

public class InvestingApplication {

	public static void main(String[] args) {
		User user1 = new User(1, "Angad", 10000, Role.CUSTOMER);
		User user3 = new User(3, "RAM JI", 40000, Role.CUSTOMER);
		User user2 = new User(2, "Geetika", 20000, Role.ADMIN);
		Stock stock1 = new Stock(10, "RL", 110, 10000);
		Stock stock2 = new Stock(11, "ML", 210, 10000);
		Stock stock3 = new Stock(12, "ANL", 150, 10000);
		Stock stock4 = new Stock(13, "BOL", 170, 10000);
		Stock stock5 = new Stock(14, "MLI", 180, 10000);
		Stock stock6 = new Stock(15, "RLB", 130, 10000);
		// One shared instance of each repository, so all services see the same data
		UserRepository userRepository = new UserRepository(new ConcurrentHashMap<>());
		StockRepository stockRepository = new StockRepository(new ConcurrentHashMap<>());
		PortfolioRepository portfolioRepository = new PortfolioRepository(new ConcurrentHashMap<>());
		TransactionRepository transactionRepository = new TransactionRepository(new ConcurrentHashMap<>());

		PortfolioService portfolioService = new PortfolioService(portfolioRepository, stockRepository, userRepository);
		TransactionService transactionService = new TransactionService(transactionRepository);
		UserService userService = new UserService(userRepository, stockRepository, portfolioRepository);
		StockService stockService = new StockService(stockRepository, portfolioService, transactionService,
				userService);

		stockService.addUser(user1);
		stockService.addUser(user2);
		stockService.addUser(user3);

		stockService.addStock(stock1);
		stockService.addStock(stock2);
		stockService.addStock(stock3);
		stockService.addStock(stock4);
		stockService.addStock(stock5);
		stockService.addStock(stock6);

		stockService.buyStock(1, 10, 10);
		stockService.buyStock(1, 15, 40);
		stockService.buyStock(1, 12, 10);

		stockService.buyStock(3, 13, 200);
		stockService.buyStock(2, 11, 50);
		stockService.viewPortfolio(1);
		stockService.updateStockPrice(2, 10, 170);
		stockService.viewPortfolio(1);

		// State so far: user1 bal 2200 (10@110, 10@150, 40@130), stock 10 price = 70,
		// user2 bal 9500 (50 of stock 11), user3 bal 6000 (200 of stock 13)

		section("1. Buy more of an existing stock -> weighted avg price");
		// 10 more of stock 10 at 70: avg = (110*10 + 70*10) / 20 = 90, qty 20, balance 2200 - 700 = 1500
		stockService.buyStock(1, 10, 10);
		stockService.viewPortfolio(1);

		section("2. Partial sell");
		// sell 10 of stock 15 at 130: qty 40 -> 30, balance 1500 + 1300 = 2800
		stockService.sellStock(1, 15, 10);
		stockService.viewPortfolio(1);

		section("3. Insufficient funds");
		// 100 * 210 = 21000 > 6000: should be rejected, user3 unchanged (balance 6000)
		stockService.buyStock(3, 11, 100);
		stockService.viewPortfolio(3);

		section("4. Unknown stock / unknown user");
		// both should print an error and change nothing
		stockService.buyStock(1, 99, 1);
		stockService.buyStock(99, 10, 1);

		section("5. View other portfolios");
		stockService.viewPortfolio(2); // 50 of stock 11
		stockService.viewPortfolio(3); // 200 of stock 13
		stockService.viewPortfolio(99); // no portfolio -> error

		section("6. Price update permissions");
		// admin updating a stock that doesn't exist -> error
		stockService.updateStockPrice(2, 99, 50);
		// customer updating a price -> should be rejected, stock 10 must stay at 70
		stockService.updateStockPrice(1, 10, 1);
		stockService.viewPortfolio(1);

		section("7. Invalid sells");
		// selling more than held (stock 12 held: 10) -> should be rejected
		stockService.sellStock(1, 12, 1000);
		// selling a stock the user doesn't hold at all -> should be rejected
		stockService.sellStock(1, 13, 1);
		stockService.viewPortfolio(1);

	}

	private static void section(String title) {
		System.out.println("\n========== " + title + " ==========");
	}

}
