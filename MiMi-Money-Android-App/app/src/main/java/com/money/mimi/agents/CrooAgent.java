package com.money.mimi.agents;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Normalized CROO agent data used by the MiMi marketplace UI. */
public final class CrooAgent {
    private static final Pattern UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private final String id;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final boolean online;
    private final long minimumPriceMicroUsdc;
    private final long completedOrders;
    private final double completionRate;
    private final String averageDelivery;
    private final List<String> tags;
    private final List<CrooService> services;

    CrooAgent(String id, String name, String description, String imageUrl, boolean online,
              long minimumPriceMicroUsdc, long completedOrders, double completionRate,
              String averageDelivery, List<String> tags, List<CrooService> services) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.online = online;
        this.minimumPriceMicroUsdc = Math.max(0L, minimumPriceMicroUsdc);
        this.completedOrders = Math.max(0L, completedOrders);
        this.completionRate = Math.max(0d, Math.min(100d, completionRate));
        this.averageDelivery = averageDelivery;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public boolean isOnline() { return online; }
    public long getMinimumPriceMicroUsdc() { return minimumPriceMicroUsdc; }
    public long getCompletedOrders() { return completedOrders; }
    public double getCompletionRate() { return completionRate; }
    public String getAverageDelivery() { return averageDelivery; }
    public List<String> getTags() { return tags; }
    public List<CrooService> getServices() { return services; }

    public String getFormattedMinimumPrice() {
        BigDecimal value = BigDecimal.valueOf(minimumPriceMicroUsdc, 6).setScale(6, RoundingMode.DOWN).stripTrailingZeros();
        return value.toPlainString() + " USDC";
    }

    public static boolean isValidId(String value) {
        return value != null && UUID.matcher(value.trim()).matches();
    }
}
