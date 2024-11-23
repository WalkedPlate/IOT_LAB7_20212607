package com.example.iot_lab7_20212607.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private String email;
    private String role;  // "OPERATIONAL" o "COMPANY"
    private double balance;  // Soolo para usuarios operativos
    private List<String> subscriptions;  // IDs de líneas suscritas

    public User() {
    }

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.balance = 50.0;  // Le asignamos un saldo inicial
        this.subscriptions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<String> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<String> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
