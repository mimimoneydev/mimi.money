package com.money.mimi.agents;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A normalized, display-safe service offered by a CROO agent. */
public final class CrooService {
    private final String id;
    private final String name;
    private final String description;
    private final long priceMicroUsdc;
    private final int slaMinutes;
    private final long orders7d;
    private final String requirements;
    private final String deliverable;

    CrooService(String id, String name, String description, long priceMicroUsdc,
                int slaMinutes, long orders7d, String requirements, String deliverable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priceMicroUsdc = Math.max(0L, priceMicroUsdc);
        this.slaMinutes = Math.max(0, slaMinutes);
        this.orders7d = Math.max(0L, orders7d);
        this.requirements = requirements;
        this.deliverable = deliverable;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getPriceMicroUsdc() { return priceMicroUsdc; }
    public int getSlaMinutes() { return slaMinutes; }
    public long getOrders7d() { return orders7d; }
    public String getRequirements() { return requirements; }
    public String getDeliverable() { return deliverable; }

    public String getFormattedPrice() {
        BigDecimal value = BigDecimal.valueOf(priceMicroUsdc, 6).setScale(6, RoundingMode.DOWN).stripTrailingZeros();
        return value.toPlainString() + " USDC";
    }
}
