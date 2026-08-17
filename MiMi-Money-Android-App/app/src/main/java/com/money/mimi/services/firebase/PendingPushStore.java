package com.money.mimi.services.firebase;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Small durable queue for data-only FCM messages received while no UI socket is running. */
public final class PendingPushStore {
    private static final String PREFS = "mimi_pending_pushes";
    private static final String KEY_QUEUE = "queue";
    private static final int MAX_ITEMS = 50;

    private PendingPushStore() {
    }

    public static synchronized void enqueue(Context context, Map<String, String> data,
                                            boolean group) {
        JSONArray queue = readQueue(context);
        JSONObject item = new JSONObject();
        try {
            item.put("group", group);
            item.put("receivedAt", System.currentTimeMillis());
            JSONObject payload = new JSONObject();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                payload.put(entry.getKey(), entry.getValue());
            }
            item.put("payload", payload);
            JSONArray bounded = new JSONArray();
            int first = Math.max(0, queue.length() - (MAX_ITEMS - 1));
            for (int i = first; i < queue.length(); i++) bounded.put(queue.opt(i));
            bounded.put(item);
            preferences(context).edit().putString(KEY_QUEUE, bounded.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public static synchronized List<JSONObject> drain(Context context) {
        JSONArray queue = readQueue(context);
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item != null) result.add(item);
        }
        preferences(context).edit().remove(KEY_QUEUE).apply();
        return result;
    }

    private static JSONArray readQueue(Context context) {
        String json = preferences(context).getString(KEY_QUEUE, "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
