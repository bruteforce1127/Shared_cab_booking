package com.kucp1127.sharedcabbooking.domain.enums;


public enum CabType {
    SEDAN(4, 3, 100),           // 4 passengers, 3 luggage, 100kg max
    SUV(6, 5, 150),             // 6 passengers, 5 luggage, 150kg max
    VAN(8, 8, 200),             // 8 passengers, 8 luggage, 200kg max
    PREMIUM_SEDAN(4, 3, 100);   // Premium sedan with same capacity

    private final int maxPassengers;
    private final int maxLuggageCount;
    private final double maxLuggageWeightKg;

    CabType(int maxPassengers, int maxLuggageCount, double maxLuggageWeightKg) {
        this.maxPassengers = maxPassengers;
        this.maxLuggageCount = maxLuggageCount;
        this.maxLuggageWeightKg = maxLuggageWeightKg;
    }

    public int getMaxPassengers() {
        return maxPassengers;
    }

    public int getMaxLuggageCount() {
        return maxLuggageCount;
    }

    public double getMaxLuggageWeightKg() {
        return maxLuggageWeightKg;
    }
}
