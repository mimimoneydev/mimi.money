package com.money.mimi.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CrooCataloguePage {
    private final List<CrooAgent> agents;
    private final int total;

    CrooCataloguePage(List<CrooAgent> agents, int total) {
        this.agents = Collections.unmodifiableList(new ArrayList<>(agents));
        this.total = Math.max(this.agents.size(), total);
    }

    public List<CrooAgent> getAgents() { return agents; }
    public int getTotal() { return total; }
}
