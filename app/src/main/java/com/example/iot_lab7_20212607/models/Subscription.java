package com.example.iot_lab7_20212607.models;

public class Subscription {
    private String userId;
    private String busLineId;
    private long startDate;
    private long endDate;

    public Subscription() {
    }

    public Subscription(String userId, String busLineId, long startDate, long endDate) {
        this.userId = userId;
        this.busLineId = busLineId;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }
}