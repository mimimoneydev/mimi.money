package com.money.mimi.agents;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Read-only CROO catalogue repository with bounded parsing and offline cache fallback. */
public final class CrooCatalogueRepository {
    private static final String API_ORIGIN = "https://api.croo.network";
    private static final String AGENTS_PATH = "/backend/v1/public/agents";
    private static final String PREFS = "mimi_croo_catalogue";
    private static final String CACHE_BODY = "agents_page_body";
    private static final String CACHE_TIME = "agents_page_time";
    private static final long FRESH_CACHE_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long MAX_STALE_CACHE_MS = TimeUnit.DAYS.toMillis(7);
    private static final long MAX_RESPONSE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_PAGE_SIZE = 50;

    private static volatile CrooCatalogueRepository instance;

    private final SharedPreferences cache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService parserExecutor = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(false)
            .build();

    public interface PageCallback {
        void onSuccess(CrooCataloguePage page, boolean fromStaleCache);
        void onError();
    }

    public interface AgentCallback {
        void onSuccess(CrooAgent agent);
        void onError();
    }

    private CrooCatalogueRepository(Context context) {
        cache = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static CrooCatalogueRepository get(Context context) {
        if (instance == null) {
            synchronized (CrooCatalogueRepository.class) {
                if (instance == null) instance = new CrooCatalogueRepository(context);
            }
        }
        return instance;
    }

    @Nullable
    public Call loadAgents(int page, int pageSize, String search, String tag,
                           boolean forceNetwork, PageCallback callback) {
        int safePage = Math.max(1, Math.min(100, page));
        int safePageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
        String safeSearch = bounded(search, 80);
        String safeTag = bounded(tag, 60);
        boolean defaultRequest = safePage == 1 && safeSearch.isEmpty() && safeTag.isEmpty();
        boolean cacheEligible = safePage == 1;

        String cached = cache.getString(CACHE_BODY, null);
        long cacheAge = System.currentTimeMillis() - cache.getLong(CACHE_TIME, 0L);
        if (!forceNetwork && defaultRequest && cached != null && cacheAge >= 0L && cacheAge <= FRESH_CACHE_MS) {
            parseCached(cached, "", "", false, callback);
            return null;
        }

        HttpUrl.Builder url = HttpUrl.parse(API_ORIGIN + AGENTS_PATH).newBuilder()
                .addQueryParameter("page", String.valueOf(safePage))
                .addQueryParameter("page_size", String.valueOf(safePageSize))
                .addQueryParameter("sort", "most_orders");
        if (!safeSearch.isEmpty()) url.addQueryParameter("search", safeSearch);
        if (!safeTag.isEmpty()) url.addQueryParameter("tags", safeTag);

        Request request = new Request.Builder()
                .url(url.build())
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "MiMi-Money-Android/1.1")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) fallbackToCache(cacheEligible, safeSearch, safeTag, callback);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response safeResponse = response) {
                    if (!safeResponse.isSuccessful()) {
                        fallbackToCache(cacheEligible, safeSearch, safeTag, callback);
                        return;
                    }
                    ResponseBody body = safeResponse.body();
                    if (body == null || body.contentLength() > MAX_RESPONSE_BYTES) {
                        fallbackToCache(cacheEligible, safeSearch, safeTag, callback);
                        return;
                    }
                    String json = body.string();
                    if (json.length() > MAX_RESPONSE_BYTES) {
                        fallbackToCache(cacheEligible, safeSearch, safeTag, callback);
                        return;
                    }
                    parserExecutor.execute(() -> {
                        try {
                            CrooCataloguePage parsed = CrooCatalogueParser.parsePage(json);
                            if (defaultRequest) {
                                cache.edit().putString(CACHE_BODY, json)
                                        .putLong(CACHE_TIME, System.currentTimeMillis()).apply();
                            }
                            postSuccess(callback, parsed, false);
                        } catch (JSONException ignored) {
                            fallbackToCache(cacheEligible, safeSearch, safeTag, callback);
                        }
                    });
                } catch (IOException ignored) {
                    fallbackToCache(cacheEligible, safeSearch, safeTag, callback);
                }
            }
        });
        return call;
    }

    @Nullable
    public Call loadAgent(String agentId, AgentCallback callback) {
        if (!CrooAgent.isValidId(agentId)) {
            mainHandler.post(callback::onError);
            return null;
        }
        HttpUrl url = HttpUrl.parse(API_ORIGIN + AGENTS_PATH + "/" + agentId.trim());
        Request request = new Request.Builder().url(url).get()
                .header("Accept", "application/json")
                .header("User-Agent", "MiMi-Money-Android/1.1")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(callback::onError);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response safeResponse = response) {
                    ResponseBody body = safeResponse.body();
                    if (!safeResponse.isSuccessful() || body == null || body.contentLength() > MAX_RESPONSE_BYTES) {
                        mainHandler.post(callback::onError);
                        return;
                    }
                    String json = body.string();
                    if (json.length() > MAX_RESPONSE_BYTES) {
                        mainHandler.post(callback::onError);
                        return;
                    }
                    parserExecutor.execute(() -> {
                        try {
                            CrooAgent agent = CrooCatalogueParser.parseDetail(json);
                            mainHandler.post(() -> callback.onSuccess(agent));
                        } catch (JSONException ignored) {
                            mainHandler.post(callback::onError);
                        }
                    });
                } catch (IOException ignored) {
                    mainHandler.post(callback::onError);
                }
            }
        });
        return call;
    }

    private void fallbackToCache(boolean cacheEligible, String search, String tag, PageCallback callback) {
        if (!cacheEligible) {
            mainHandler.post(callback::onError);
            return;
        }
        String cached = cache.getString(CACHE_BODY, null);
        long cacheAge = System.currentTimeMillis() - cache.getLong(CACHE_TIME, 0L);
        if (cached == null || cacheAge < 0L || cacheAge > MAX_STALE_CACHE_MS) {
            mainHandler.post(callback::onError);
            return;
        }
        parseCached(cached, search, tag, true, callback);
    }

    private void parseCached(String json, String search, String tag, boolean stale, PageCallback callback) {
        parserExecutor.execute(() -> {
            try {
                CrooCataloguePage page = CrooCatalogueParser.parsePage(json);
                if (!search.isEmpty() || !tag.isEmpty()) page = CrooCatalogueParser.filter(page, search, tag);
                postSuccess(callback, page, stale);
            } catch (JSONException ignored) {
                mainHandler.post(callback::onError);
            }
        });
    }

    private void postSuccess(PageCallback callback, CrooCataloguePage page, boolean stale) {
        mainHandler.post(() -> callback.onSuccess(page, stale));
    }

    private static String bounded(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
