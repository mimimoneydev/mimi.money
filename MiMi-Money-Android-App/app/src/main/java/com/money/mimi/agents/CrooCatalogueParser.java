package com.money.mimi.agents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.net.URI;

/** Defensive parser and distribution policy for untrusted marketplace metadata. */
public final class CrooCatalogueParser {
    private static final int MAX_NAME = 120;
    private static final int MAX_DESCRIPTION = 2400;
    private static final int MAX_DETAIL = 3000;
    private static final Set<String> APPROVED_TAGS = new HashSet<>(Arrays.asList(
            "defi-trading", "data-analytics", "research-report", "development-code",
            "automation-workflow", "content-creative", "social-community"
    ));

    private CrooCatalogueParser() {}

    public static CrooCataloguePage parsePage(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray source = root.optJSONArray("agents");
        if (source == null) throw new JSONException("Missing agents array");
        ArrayList<CrooAgent> agents = new ArrayList<>();
        for (int i = 0; i < source.length(); i++) {
            JSONObject raw = source.optJSONObject(i);
            CrooAgent agent = parseAgent(raw, false);
            if (agent != null) agents.add(agent);
        }
        rankAgents(agents);
        return new CrooCataloguePage(agents, safeInt(root.opt("total"), agents.size()));
    }

    public static CrooAgent parseDetail(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject raw = root.optJSONObject("agent");
        CrooAgent agent = parseAgent(raw, true);
        if (agent == null) throw new JSONException("Invalid agent record");
        return agent;
    }

    public static CrooCataloguePage filter(CrooCataloguePage page, String search, String tag) {
        if (page == null) return new CrooCataloguePage(Collections.emptyList(), 0);
        String query = clean(search, 80).toLowerCase(Locale.US);
        String safeTag = tag == null ? "" : tag.trim().toLowerCase(Locale.US);
        ArrayList<CrooAgent> filtered = new ArrayList<>();
        for (CrooAgent agent : page.getAgents()) {
            boolean matchesText = query.isEmpty()
                    || agent.getName().toLowerCase(Locale.US).contains(query)
                    || agent.getDescription().toLowerCase(Locale.US).contains(query);
            boolean matchesTag = safeTag.isEmpty() || agent.getTags().contains(safeTag);
            if (matchesText && matchesTag) filtered.add(agent);
        }
        return new CrooCataloguePage(filtered, filtered.size());
    }

    private static CrooAgent parseAgent(JSONObject raw, boolean includeServices) {
        if (raw == null) return null;
        String id = clean(raw.optString("agentId", ""), 64);
        String name = clean(raw.optString("name", ""), MAX_NAME);
        String status = clean(raw.optString("status", ""), 30).toLowerCase(Locale.US);
        if (!CrooAgent.isValidId(id) || name.isEmpty() || !("active".equals(status) || status.isEmpty())) {
            return null;
        }
        String onlineStatus = clean(raw.optString("onlineStatus", status), 30).toLowerCase(Locale.US);
        boolean online = "online".equals(onlineStatus) || "active".equals(onlineStatus);
        List<String> tags = parseTags(raw.optJSONArray("skillTagSlugs"));
        List<CrooService> services = includeServices
                ? parseServices(raw.optJSONArray("services"))
                : Collections.emptyList();
        return new CrooAgent(
                id,
                name,
                clean(raw.optString("description", ""), MAX_DESCRIPTION),
                safeImageUrl(raw.optString("avatar", "")),
                online,
                safeLong(raw.opt("minServicePrice"), 0L),
                safeLong(raw.opt("completedOrders"), 0L),
                safeDouble(raw.opt("completionRate"), 0d),
                clean(raw.optString("avgDeliveryText", "—"), 40),
                tags,
                services
        );
    }

    private static List<CrooService> parseServices(JSONArray source) {
        ArrayList<CrooService> services = new ArrayList<>();
        if (source == null) return services;
        for (int i = 0; i < source.length() && services.size() < 100; i++) {
            JSONObject raw = source.optJSONObject(i);
            if (raw == null) continue;
            String id = clean(raw.optString("serviceId", ""), 64);
            String name = clean(raw.optString("name", ""), MAX_NAME);
            if (!CrooAgent.isValidId(id) || name.isEmpty()) continue;
            services.add(new CrooService(
                    id,
                    name,
                    clean(raw.optString("description", ""), MAX_DESCRIPTION),
                    safeLong(raw.opt("price"), 0L),
                    safeInt(raw.opt("slaMinutes"), 0),
                    safeLong(raw.opt("orders7d"), 0L),
                    serviceDetail(raw, "requirementText", "requirementSchema"),
                    serviceDetail(raw, "deliverableText", "deliverableSchema")
            ));
        }
        return services;
    }

    private static String serviceDetail(JSONObject raw, String textKey, String schemaKey) {
        String text = clean(raw.optString(textKey, ""), MAX_DETAIL);
        if (!text.isEmpty()) return text;
        String schema = raw.optString(schemaKey, "");
        if (schema == null || schema.trim().isEmpty() || "[]".equals(schema.trim())
                || "{}".equals(schema.trim())) return "";
        try {
            JSONArray fields = new JSONArray(schema);
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < fields.length() && i < 20 && summary.length() < MAX_DETAIL; i++) {
                JSONObject field = fields.optJSONObject(i);
                if (field == null) continue;
                String name = clean(field.optString("name", "field"), 100).replace('_', ' ');
                String type = clean(field.optString("type", "value"), 40);
                String description = clean(field.optString("description", ""), 300);
                if (summary.length() > 0) summary.append('\n');
                summary.append("• ").append(name).append(" (").append(type);
                if (field.optBoolean("required", false)) summary.append(", required");
                summary.append(')');
                if (!description.isEmpty()) summary.append(": ").append(description);
            }
            return clean(summary.toString(), MAX_DETAIL);
        } catch (JSONException ignored) {
            return clean(schema, MAX_DETAIL);
        }
    }

    private static List<String> parseTags(JSONArray source) {
        ArrayList<String> tags = new ArrayList<>();
        if (source == null) return tags;
        for (int i = 0; i < source.length() && tags.size() < 12; i++) {
            String tag = clean(source.optString(i, ""), 60).toLowerCase(Locale.US);
            if (APPROVED_TAGS.contains(tag) && !tags.contains(tag)) tags.add(tag);
        }
        return tags;
    }

    private static void rankAgents(List<CrooAgent> agents) {
        agents.sort(Comparator
                .comparing(CrooAgent::isOnline).reversed()
                .thenComparing(Comparator.comparingLong(CrooAgent::getCompletedOrders).reversed())
                .thenComparing(Comparator.comparingDouble(CrooAgent::getCompletionRate).reversed())
                .thenComparing(CrooAgent::getName, String.CASE_INSENSITIVE_ORDER));
    }

    private static String safeImageUrl(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "object.croo.network".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null ? uri.toASCIIString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String clean(String value, int maxLength) {
        if (value == null) return "";
        String cleaned = value
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ")
                .replaceAll("<[^>]{0,200}>", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength).trim();
    }

    private static long safeLong(Object value, long fallback) {
        try {
            if (value == null || value == JSONObject.NULL) return fallback;
            long parsed = Long.parseLong(String.valueOf(value));
            return parsed < 0L ? fallback : parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int safeInt(Object value, int fallback) {
        long parsed = safeLong(value, fallback);
        return parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;
    }

    private static double safeDouble(Object value, double fallback) {
        try {
            if (value == null || value == JSONObject.NULL) return fallback;
            double parsed = Double.parseDouble(String.valueOf(value));
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
