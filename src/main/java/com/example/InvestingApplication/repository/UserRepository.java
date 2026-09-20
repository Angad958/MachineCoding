package com.example.InvestingApplication.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.InvestingApplication.models.*;

public class UserRepository {
    Map<Integer, User> userDatabase = new ConcurrentHashMap<>();

    public UserRepository(Map<Integer, User> userDatabase) {
        this.userDatabase = userDatabase;
    }

    public synchronized void addUser(User user) {
        System.out.println("User added with id " + user.getId());
        userDatabase.putIfAbsent(user.getId(), user);
    }

    public synchronized void removeUser(User user) {
        System.out.println("User removed with id " + user.getId());
        userDatabase.remove(user.getId());
    }

    public synchronized void updateUserBalance(User user, float newBalance) {

        User tempValue = userDatabase.get(user.getId());
        tempValue.setBalance(newBalance);
        System.out.println("User balance updated with id " + user.getId());
    }

    public User getUser(int id) {
        User tempValue = userDatabase.get(id);
        return tempValue;
    }
}
