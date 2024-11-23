package com.example.iot_lab7_20212607.models;

import java.util.ArrayList;
import java.util.List;

public class BusLine {
    private String id;
    private String name;
    private String companyId;
    private double unitPrice;
    private double subscriptionPrice;
    private List<String> imageUrls;

    public BusLine(String name, String companyId, double unitPrice, double subscriptionPrice) {
        this.name = name;
        this.companyId = companyId;
        this.unitPrice = unitPrice;
        this.subscriptionPrice = subscriptionPrice;
        this.imageUrls = new ArrayList<>();
    }

    public BusLine() {
        this.imageUrls = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubscriptionPrice() {
        return subscriptionPrice;
    }

    public void setSubscriptionPrice(double subscriptionPrice) {
        this.subscriptionPrice = subscriptionPrice;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}

