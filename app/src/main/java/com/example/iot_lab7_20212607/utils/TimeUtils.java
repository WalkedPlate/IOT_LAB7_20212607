package com.example.iot_lab7_20212607.utils;

public class TimeUtils {
    public static long calculateTripDuration(long entryTime, long exitTime) {
        return (exitTime - entryTime) / (60 * 1000); // Duración en minutos
    }

    public static double calculateCashbackPercentage(long tripDuration) {
        return tripDuration < Constants.CASHBACK_THRESHOLD_MINUTES ?
                Constants.CASHBACK_HIGH_PERCENTAGE : Constants.CASHBACK_LOW_PERCENTAGE;
    }
}