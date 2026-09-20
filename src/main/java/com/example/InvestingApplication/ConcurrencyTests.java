package com.example.InvestingApplication;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

import com.example.InvestingApplication.models.Portfolio;
import com.example.InvestingApplication.models.Stock;
import com.example.InvestingApplication.models.Transaction;
import com.example.InvestingApplication.models.User;
import com.example.InvestingApplication.models.Portfolio.StockMetaData;
import com.example.InvestingApplication.models.User.Role;
import com.example.InvestingApplication.repository.PortfolioRepository;
import com.example.InvestingApplication.repository.StockRepository;
import com.example.InvestingApplication.repository.TransactionRepository;
import com.example.InvestingApplication.repository.UserRepository;
import com.example.InvestingApplication.service.PortfolioService;
import com.example.InvestingApplication.service.StockService;
import com.example.InvestingApplication.service.TransactionService;
import com.example.InvestingApplication.service.UserService;

/**
 * Concurrency tests for the investing services. Each scenario builds a fresh world, hits it from many threads and
 * checks invariants. Scenarios run ROUNDS times because races are probabilistic.
 *
 * Run: java com.example.demo.ConcurrencyTests (exit code 1 if any scenario fails)
 */
public class ConcurrencyTests {

	private static final int ROUNDS = 20;
	private static final PrintStream REAL_OUT = System.out;
	private static final PrintStream REAL_ERR = System.err;
	private static ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

	/** A fresh, isolated set of repositories and services. */
	static class World {
		final UserRepository userRepository = new UserRepository(new ConcurrentHashMap<>());
		final StockRepository stockRepository = new StockRepository(new ConcurrentHashMap<>());
		final PortfolioRepository portfolioRepository = new PortfolioRepository(new ConcurrentHashMap<>());
		final TransactionRepository transactionRepository = new TransactionRepository(new ConcurrentHashMap<>());
		final PortfolioService portfolioService = new PortfolioService(portfolioRepository, stockRepository,
				userRepository);
		final TransactionService transactionService = new TransactionService(transactionRepository);
		final UserService userService = new UserService(userRepository, stockRepository, portfolioRepository);
		final StockService stockService = new StockService(stockRepository, portfolioService, transactionService,
				userService);

		void addUser(int id, float balance, Role role) {
			stockService.addUser(new User(id, "user" + id, balance, role));
		}

		void addStock(int id, float price) {
			stockService.addStock(new Stock(id, "S" + id, price, 1_000_000));
		}

		float balance(int userId) {
			return userService.getUser(userId).getBalance();
		}

		int qty(int userId, int stockId) {
			Portfolio p = portfolioService.gePortfolio(userId);
			StockMetaData m = p == null ? null : p.getHoldings().get(stockId);
			return m == null ? 0 : m.getQty();
		}

		float sumOfTransactions(int userId) {
			float total = 0;
			for (Transaction t : transactionRepository.getAllUserTransactionById(userId)) {
				total += t.getAmount();
			}
			return total;
		}
	}

	public static void main(String[] args) {
		List<Boolean> results = new ArrayList<>();

		results.add(scenario("1. Concurrent buys, same user + stock: nothing lost", () -> {
			World w = new World();
			w.addUser(1, 1_000_000, Role.CUSTOMER);
			w.addStock(10, 100);
			int threads = 8, perThread = 200;
			runConcurrently(threads, t -> {
				for (int i = 0; i < perThread; i++) {
					w.stockService.buyStock(1, 10, 1);
				}
			});
			int total = threads * perThread;
			List<Transaction> txs = w.transactionRepository.getAllUserTransactionById(1);
			long distinctIds = txs.stream().map(Transaction::getId).distinct().count();
			List<String> problems = new ArrayList<>();
			if (w.qty(1, 10) != total)
				problems.add("qty " + w.qty(1, 10) + " != " + total);
			if (Math.abs(w.balance(1) - (1_000_000 - total * 100f)) > 0.5)
				problems.add("balance " + w.balance(1) + " != " + (1_000_000 - total * 100f));
			if (txs.size() != total)
				problems.add("transactions " + txs.size() + " != " + total);
			if (distinctIds != txs.size())
				problems.add("duplicate transaction ids: " + distinctIds + " distinct of " + txs.size());
			return problems;
		}));

		results.add(scenario("2. Buy race on balance: never overspend", () -> {
			World w = new World();
			w.addUser(1, 1000, Role.CUSTOMER); // affords exactly 10 shares at 100
			w.addStock(10, 100);
			runConcurrently(32, t -> w.stockService.buyStock(1, 10, 1));
			List<String> problems = new ArrayList<>();
			if (w.qty(1, 10) != 10)
				problems.add("bought " + w.qty(1, 10) + " shares, expected exactly 10");
			if (w.balance(1) != 0)
				problems.add("balance " + w.balance(1) + ", expected 0");
			return problems;
		}));

		results.add(scenario("3. Concurrent buys and sells net out", () -> {
			World w = new World();
			w.addUser(1, 1_000_000, Role.CUSTOMER);
			w.addStock(10, 100);
			w.stockService.buyStock(1, 10, 1000); // 100k spent, holding 1000
			runConcurrently(8, t -> {
				for (int i = 0; i < 100; i++) {
					if (t % 2 == 0)
						w.stockService.buyStock(1, 10, 1);
					else
						w.stockService.sellStock(1, 10, 1);
				}
			});
			List<String> problems = new ArrayList<>();
			if (w.qty(1, 10) != 1000)
				problems.add("qty " + w.qty(1, 10) + " != 1000");
			if (Math.abs(w.balance(1) - 900_000) > 0.5)
				problems.add("balance " + w.balance(1) + " != 900000");
			return problems;
		}));

		results.add(scenario("4. Oversell race: never sell more than held", () -> {
			World w = new World();
			w.addUser(1, 1_000_000, Role.CUSTOMER);
			w.addStock(10, 100);
			w.stockService.buyStock(1, 10, 100); // holds 100, balance 990000
			runConcurrently(16, t -> w.stockService.sellStock(1, 10, 10)); // 160 requested, only 100 held
			List<String> problems = new ArrayList<>();
			if (w.qty(1, 10) != 0)
				problems.add("qty " + w.qty(1, 10) + ", expected 0 (negative means oversold)");
			if (Math.abs(w.balance(1) - 1_000_000) > 0.5)
				problems.add("balance " + w.balance(1) + " != 1000000");
			return problems;
		}));

		results.add(scenario("5. Price changes mid-trade: transaction, balance, portfolio agree", () -> {
			World w = new World();
			w.addUser(1, 1_000_000, Role.CUSTOMER);
			w.addUser(2, 0, Role.ADMIN);
			w.addStock(10, 100);
			AtomicBoolean stop = new AtomicBoolean();
			Thread flipper = new Thread(() -> {
				int price = 100;
				while (!stop.get()) {
					price = price == 100 ? 200 : 100;
					w.stockService.updateStockPrice(2, 10, price);
				}
			});
			flipper.start();
			try {
				runConcurrently(4, t -> {
					for (int i = 0; i < 250; i++) {
						w.stockService.buyStock(1, 10, 1);
					}
				});
			} finally {
				stop.set(true);
				flipper.join();
			}
			float debited = 1_000_000 - w.balance(1);
			float transactionTotal = w.sumOfTransactions(1);
			StockMetaData m = w.portfolioService.gePortfolio(1).getHoldings().get(10);
			float costBasis = m.getAvgPrice() * m.getQty();
			List<String> problems = new ArrayList<>();
			if (Math.abs(transactionTotal - debited) > 0.5)
				problems.add("transactions total " + transactionTotal + " but balance was debited " + debited);
			if (Math.abs(costBasis - transactionTotal) > 10)
				problems.add("portfolio cost basis " + costBasis + " but transactions total " + transactionTotal);
			return problems;
		}));

		results.add(scenario("6. Concurrent addUser: each user gets their own portfolio", () -> {
			World w = new World();
			int n = 50;
			runConcurrently(n, t -> w.addUser(t + 1, 1000, Role.CUSTOMER));
			List<Integer> wrong = new ArrayList<>();
			for (int id = 1; id <= n; id++) {
				Portfolio p = w.portfolioService.gePortfolio(id);
				if (p == null || p.getUser_id() != id)
					wrong.add(id);
			}
			List<String> problems = new ArrayList<>();
			if (!wrong.isEmpty())
				problems.add(wrong.size() + " of " + n + " users resolve to a portfolio owned by someone else (e.g. user "
						+ wrong.get(0) + ")");
			return problems;
		}));

		results.add(scenario("7. Readers while writers add new holdings: no errors", () -> {
			World w = new World();
			w.addUser(1, 10_000_000, Role.CUSTOMER);
			for (int s = 1; s <= 20; s++)
				w.addStock(s, 100);
			AtomicBoolean stop = new AtomicBoolean();
			List<Thread> readers = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				Thread r = new Thread(() -> {
					while (!stop.get())
						w.stockService.viewPortfolio(1);
				});
				readers.add(r);
				r.start();
			}
			try {
				runConcurrently(4, t -> {
					for (int rep = 0; rep < 10; rep++)
						for (int s = 1; s <= 20; s++)
							w.stockService.buyStock(1, s, 1);
				});
			} finally {
				stop.set(true);
				for (Thread r : readers)
					r.join();
			}
			List<String> problems = new ArrayList<>();
			String errors = errBuffer.toString();
			if (errors.contains("error occurred while viewing"))
				problems.add("a reader hit an error: " + errors.lines().findFirst().orElse(""));
			for (int s = 1; s <= 20; s++)
				if (w.qty(1, s) != 40) {
					problems.add("stock " + s + " qty " + w.qty(1, s) + " != 40");
					break;
				}
			return problems;
		}));

		long failed = results.stream().filter(ok -> !ok).count();
		REAL_OUT.println("\n" + (results.size() - failed) + "/" + results.size() + " scenarios passed");
		System.exit(failed == 0 ? 0 : 1);
	}

	/** Runs the body ROUNDS times with service logging silenced. The body returns a list of problems (empty = pass). */
	private static boolean scenario(String name, Callable<List<String>> body) {
		int failures = 0;
		String firstProblem = null;
		for (int round = 0; round < ROUNDS; round++) {
			List<String> problems;
			silence();
			try {
				problems = body.call();
			} catch (Throwable t) {
				problems = List.of("threw " + t);
			} finally {
				restore();
			}
			if (!problems.isEmpty()) {
				failures++;
				if (firstProblem == null)
					firstProblem = String.join("; ", problems);
			}
		}
		if (failures == 0) {
			REAL_OUT.println("PASS  " + name + "  (" + ROUNDS + "/" + ROUNDS + " rounds)");
		} else {
			REAL_OUT.println("FAIL  " + name + "  (" + failures + "/" + ROUNDS + " rounds failed)");
			REAL_OUT.println("        first failure: " + firstProblem);
		}
		return failures == 0;
	}

	/** Starts all threads behind a latch so they hit the code at the same moment, then waits for them. */
	private static void runConcurrently(int threads, IntConsumer task) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch go = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			final int id = i;
			futures.add(pool.submit((Callable<Void>) () -> {
				ready.countDown();
				go.await();
				task.accept(id);
				return null;
			}));
		}
		ready.await();
		go.countDown();
		pool.shutdown();
		if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
			pool.shutdownNow();
			throw new IllegalStateException("timed out (deadlock?)");
		}
		for (Future<?> f : futures)
			f.get(); // rethrows anything a worker threw
	}

	private static void silence() {
		errBuffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(OutputStream.nullOutputStream()));
		System.setErr(new PrintStream(errBuffer, true));
	}

	private static void restore() {
		System.setOut(REAL_OUT);
		System.setErr(REAL_ERR);
	}
}
