package com.example.iot_lab7_20212607.models;

public class Transaction {
    private String userId;
    private String busLineId;
    private String type;
    private double amount;
    private long timestamp;
    private double cashback;

    public Transaction() {
    }

    public Transaction(String userId, String busLineId, String type, double amount, long timestamp, double cashback) {
        this.userId = userId;
        this.busLineId = busLineId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.cashback = cashback;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBusLineId() {
        return busLineId;
    }

    public void setBusLineId(String busLineId) {
        this.busLineId = busLineId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getCashback() {
        return cashback;
    }

    public void setCashback(double cashback) {
        this.cashback = cashback;
    }
}
