package com.example.InvestingApplication.service;

import com.example.InvestingApplication.models.*;
import com.example.InvestingApplication.repository.TransactionRepository;

public class TransactionService {
    TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public int createTransaction(User user, int stockId, int qty, float price, String type) throws Exception {
        int id = transactionRepository.nextTransactionId();
        float amount = price * qty;
        if (type == "SELL") {
            amount = -1 * amount;
        }
        if (user.getBalance() < amount) {
            throw new Exception("User has insufficient funds " + user.getId());
        }
        Transaction newTransaction = new Transaction(id, user.getId(), amount, stockId, type);
        transactionRepository.addTransaction(newTransaction, user.getId());
        return id;
    }

}
