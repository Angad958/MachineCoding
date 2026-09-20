package com.example.demo.repository;

import com.example.demo.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionRepository {
    Map<Integer, Transaction> TransactionDatabase = new ConcurrentHashMap<>();
    Map<Integer, List<Transaction>> userTransactionIndex = new ConcurrentHashMap<>();
    AtomicInteger transactionIdSequence = new AtomicInteger(0);

    public TransactionRepository(Map<Integer, Transaction> TransactionDatabase) {
        this.TransactionDatabase = TransactionDatabase;
    }

    public int nextTransactionId() {
        return transactionIdSequence.incrementAndGet();
    }

    public synchronized void addTransaction(Transaction transaction, int userId) {
        System.out.println("Transaction added with id" + transaction.getId());
        TransactionDatabase.putIfAbsent(transaction.getId(), transaction);
        userTransactionIndex
                .computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>())
                .add(transaction);
    }

    public synchronized void removeTransaction(Transaction transaction) {
        System.out.println("Transaction removed with id" + transaction.getId());
        TransactionDatabase.remove(transaction.getId());
        List<Transaction> userTransactions = userTransactionIndex.get(transaction.getUser_id());
        if (userTransactions != null) {
            userTransactions.remove(transaction);
        }
    }

    public synchronized List<Transaction> getAllUserTransactionById(int userId) {
        System.out.println("Transaction fetched updated with id" + userId);
        return userTransactionIndex.getOrDefault(userId, Collections.emptyList());
    }
}
