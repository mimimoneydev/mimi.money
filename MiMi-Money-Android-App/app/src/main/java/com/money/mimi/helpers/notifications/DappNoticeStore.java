package com.money.mimi.helpers.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.money.mimi.models.notifications.DappNotice;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DappNoticeStore {
    public static final String ACTION_NOTICES_CHANGED = "com.money.mimi.action.DAPP_NOTICES_CHANGED";

    private static final String NOTICE_BASE_URL = "https://notice.mimi.money";
    private static final String PREF_NAME = "mimi_dapp_notices";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 100;

    private DappNoticeStore() {
    }

    public static synchronized DappNotice saveFromFcm(Context context, Map<String, String> data) {
        DappNotice notice = fromFcm(data);
        if (notice == null) {
            return null;
        }
        save(context, notice);
        return notice;
    }

    public static DappNotice fromFcm(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        String title = firstValue(data, "title", "notification_title", "subject");
        String message = firstValue(data, "msg", "message", "body", "notification_body");
        String type = firstValue(data, "type", "notice_type");
        String link = firstValue(data, "link", "url", "click_action_url");
        String image = extractImageUrl(data);

        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(message)) {
            return null;
        }
        if (TextUtils.isEmpty(title)) {
            title = "MiMi Money";
        }
        if (TextUtils.isEmpty(message)) {
            message = "";
        }

        DappNotice notice = new DappNotice(
                String.valueOf(System.currentTimeMillis()),
                emptyToDefault(type, ""),
                title,
                message,
                emptyToDefault(link, ""),
                normalizeImageUrl(image),
                System.currentTimeMillis(),
                false
        );
        return notice;
    }

    public static String extractImageUrl(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        String image = firstValue(data, imageKeys());
        if (!TextUtils.isEmpty(image)) {
            return normalizeImageUrl(image);
        }

        for (String value : data.values()) {
            String nestedImage = firstImageFromJson(value);
            if (!TextUtils.isEmpty(nestedImage)) {
                return normalizeImageUrl(nestedImage);
            }
        }
        return "";
    }

    public static String normalizeImageUrl(String value) {
        String image = value == null ? "" : value.trim();
        if (image.isEmpty() || "null".equalsIgnoreCase(image)) {
            return "";
        }
        image = image.replace(" ", "%20");
        if (image.startsWith("//")) {
            return "https:" + image;
        }
        if (isHttpUrl(image)) {
            return image;
        }
        if (image.startsWith("/")) {
            return NOTICE_BASE_URL + image;
        }
        return NOTICE_BASE_URL + "/" + image;
    }

    public static synchronized void save(Context context, DappNotice notice) {
        if (context == null || notice == null) {
            return;
        }
        saveInternal(context, notice);
        notifyChanged(context);
    }

    public static synchronized List<DappNotice> getNotices(Context context) {
        ArrayList<DappNotice> result = new ArrayList<>();
        if (context == null) {
            return result;
        }
        JSONArray array = readArray(context);
        for (int i = 0; i < array.length(); i++) {
            DappNotice notice = fromJson(array.optJSONObject(i));
            if (notice != null) {
                result.add(notice);
            }
        }
        return result;
    }

    public static synchronized int getUnreadCount(Context context) {
        int count = 0;
        for (DappNotice notice : getNotices(context)) {
            if (!notice.isRead()) {
                count++;
            }
        }
        return count;
    }

    public static synchronized void markAllRead(Context context) {
        if (context == null) {
            return;
        }
        JSONArray array = readArray(context);
        JSONArray next = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) {
                try {
                    item.put("read", true);
                } catch (Exception ignored) {
                }
                next.put(item);
            }
        }
        prefs(context).edit().putString(KEY_ITEMS, next.toString()).apply();
        notifyChanged(context);
    }

    public static synchronized void clear(Context context) {
        if (context == null) {
            return;
        }
        prefs(context).edit().remove(KEY_ITEMS).apply();
        notifyChanged(context);
    }

    private static void saveInternal(Context context, DappNotice notice) {
        JSONArray current = readArray(context);
        JSONArray next = new JSONArray();
        next.put(toJson(notice));
        for (int i = 0; i < current.length() && next.length() < MAX_ITEMS; i++) {
            JSONObject item = current.optJSONObject(i);
            if (item != null) {
                next.put(item);
            }
        }
        prefs(context).edit().putString(KEY_ITEMS, next.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static JSONArray readArray(Context context) {
        try {
            return new JSONArray(prefs(context).getString(KEY_ITEMS, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static JSONObject toJson(DappNotice notice) {
        JSONObject object = new JSONObject();
        try {
            object.put("id", notice.getId());
            object.put("type", notice.getType());
            object.put("title", notice.getTitle());
            object.put("message", notice.getMessage());
            object.put("link", notice.getLink());
            object.put("image", notice.getImage());
            object.put("receivedAt", notice.getReceivedAt());
            object.put("read", notice.isRead());
        } catch (Exception ignored) {
        }
        return object;
    }

    private static DappNotice fromJson(JSONObject object) {
        if (object == null) {
            return null;
        }
        return new DappNotice(
                object.optString("id"),
                object.optString("type"),
                object.optString("title"),
                object.optString("message"),
                object.optString("link"),
                object.optString("image"),
                object.optLong("receivedAt"),
                object.optBoolean("read")
        );
    }

    private static String firstValue(Map<String, String> data, String... keys) {
        for (String key : keys) {
            String value = data.get(key);
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private static String[] imageKeys() {
        return new String[]{
                "image",
                "imageUrl",
                "image_url",
                "imageURL",
                "img",
                "imgUrl",
                "img_url",
                "picture",
                "pictureUrl",
                "picture_url",
                "photo",
                "photoUrl",
                "photo_url",
                "thumbnail",
                "thumb",
                "bigPicture",
                "big_picture",
                "largeIcon",
                "large_icon",
                "icon",
                "media",
                "mediaUrl",
                "media_url",
                "attachment",
                "attachmentUrl",
                "attachment_url",
                "banner",
                "bannerUrl",
                "banner_url"
        };
    }

    private static String firstImageFromJson(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return "";
        }
        try {
            Object root = trimmed.startsWith("{") ? new JSONObject(trimmed) : new JSONArray(trimmed);
            return firstImageFromJsonNode(root, 0);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String firstImageFromJsonNode(Object node, int depth) {
        if (node == null || depth > 5) {
            return "";
        }
        try {
            if (node instanceof JSONObject) {
                JSONObject object = (JSONObject) node;
                for (String key : imageKeys()) {
                    String value = object.optString(key, "");
                    if (!TextUtils.isEmpty(value) && !"null".equalsIgnoreCase(value)) {
                        return value;
                    }
                }
                Iterator<String> keys = object.keys();
                while (keys.hasNext()) {
                    String nested = firstImageFromJsonNode(object.opt(keys.next()), depth + 1);
                    if (!TextUtils.isEmpty(nested)) {
                        return nested;
                    }
                }
            } else if (node instanceof JSONArray) {
                JSONArray array = (JSONArray) node;
                for (int i = 0; i < array.length(); i++) {
                    String nested = firstImageFromJsonNode(array.opt(i), depth + 1);
                    if (!TextUtils.isEmpty(nested)) {
                        return nested;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String emptyToDefault(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("https://") || value.startsWith("http://");
    }

    private static void notifyChanged(Context context) {
        Intent intent = new Intent(ACTION_NOTICES_CHANGED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }
}
