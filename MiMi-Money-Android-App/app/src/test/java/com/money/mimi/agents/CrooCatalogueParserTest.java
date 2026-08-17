package com.money.mimi.agents;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrooCatalogueParserTest {
    private static final String ID_A = "b6c8cc34-0d3e-46dc-9b9d-816a3659dcad";
    private static final String ID_B = "e05abaea-a586-4954-bbcf-d5c93127a214";

    @Test
    public void normalizesPricesModeratesImagesAndRanksOnlineFirst() throws Exception {
        String json = "{\"agents\":["
                + agent(ID_A, "Offline", "offline", "9000000", "999", "https://evil.example/a.png") + ","
                + agent(ID_B, "<b>Online</b>", "online", "100000", "2", "https://object.croo.network/a.png")
                + "],\"total\":\"2\"}";

        CrooCataloguePage page = CrooCatalogueParser.parsePage(json);

        assertEquals(2, page.getTotal());
        assertEquals("Online", page.getAgents().get(0).getName());
        assertEquals("0.1 USDC", page.getAgents().get(0).getFormattedMinimumPrice());
        assertEquals("", page.getAgents().get(1).getImageUrl());
        assertEquals("data-analytics", page.getAgents().get(0).getTags().get(0));
    }

    @Test
    public void rejectsInvalidAndInactiveListings() throws Exception {
        String invalid = agent("not-a-uuid", "Bad", "online", "1", "1", "");
        String inactive = agent(ID_A, "Hidden", "online", "1", "1", "")
                .replace("\"status\":\"active\"", "\"status\":\"suspended\"");
        CrooCataloguePage page = CrooCatalogueParser.parsePage(
                "{\"agents\":[" + invalid + "," + inactive + "],\"total\":2}");
        assertTrue(page.getAgents().isEmpty());
    }

    @Test
    public void parsesServiceAndConvertsSchemaToSafeSummary() throws Exception {
        String service = "{\"serviceId\":\"022c38ad-0be9-4ee1-8f76-d645cb182010\","
                + "\"name\":\"Wallet report\",\"price\":\"2500000\",\"slaMinutes\":30,"
                + "\"orders7d\":\"4\",\"requirementText\":\"\","
                + "\"requirementSchema\":\"[{\\\"name\\\":\\\"wallet_address\\\",\\\"type\\\":\\\"string\\\",\\\"required\\\":true,\\\"description\\\":\\\"Public wallet\\\"}]\","
                + "\"deliverableText\":\"A report\"}";
        String detail = "{\"agent\":" + agent(ID_A, "Agent", "online", "2500000", "4", "")
                .replace("}", ",\"services\":[" + service + "]}") + "}";

        CrooAgent agent = CrooCatalogueParser.parseDetail(detail);
        CrooService parsed = agent.getServices().get(0);

        assertEquals("2.5 USDC", parsed.getFormattedPrice());
        assertTrue(parsed.getRequirements().contains("wallet address"));
        assertTrue(parsed.getRequirements().contains("required"));
        assertFalse(parsed.getRequirements().contains("<"));
    }

    @Test(expected = JSONException.class)
    public void failsClosedWhenAgentsArrayIsMissing() throws Exception {
        CrooCatalogueParser.parsePage("{\"total\":1}");
    }

    @Test
    public void cachedSearchFiltersByTextAndApprovedCategory() throws Exception {
        String json = "{\"agents\":["
                + agent(ID_A, "AlphaTrack", "online", "100000", "10", "") + ","
                + agent(ID_B, "Research Writer", "online", "200000", "5", "")
                    .replace("data-analytics", "research-report")
                + "],\"total\":2}";
        CrooCataloguePage page = CrooCatalogueParser.parsePage(json);

        CrooCataloguePage searched = CrooCatalogueParser.filter(page, "alpha", "data-analytics");
        CrooCataloguePage wrongCategory = CrooCatalogueParser.filter(page, "alpha", "research-report");

        assertEquals(1, searched.getAgents().size());
        assertEquals("AlphaTrack", searched.getAgents().get(0).getName());
        assertTrue(wrongCategory.getAgents().isEmpty());
    }

    private static String agent(String id, String name, String online, String price,
                                String orders, String avatar) {
        return "{\"agentId\":\"" + id + "\",\"name\":\"" + name + "\","
                + "\"description\":\"Useful agent\",\"avatar\":\"" + avatar + "\","
                + "\"status\":\"active\",\"onlineStatus\":\"" + online + "\","
                + "\"minServicePrice\":\"" + price + "\",\"completedOrders\":\"" + orders + "\","
                + "\"completionRate\":99.5,\"avgDeliveryText\":\"10 min\","
                + "\"skillTagSlugs\":[\"data-analytics\",\"unapproved-tag\"]}";
    }
}
