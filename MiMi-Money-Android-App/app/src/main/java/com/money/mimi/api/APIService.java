package com.money.mimi.api;


import android.content.Context;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.money.mimi.BuildConfig;
import com.money.mimi.helpers.PreferenceManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.realm.RealmObject;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by Abderrahim El imame on 27/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class APIService {

    protected final Context context;

    public static APIService with(Context context) {
        return new APIService(context);
    }

    public APIService(Context context) {
        this.context = context;
    }

    private static Gson gson = new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getDeclaringClass().equals(RealmObject.class);
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();


    public <S> S RootService(Class<S> serviceClass, String baseUrl) {

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.dns(hostname -> {
            try {
                return Dns.SYSTEM.lookup(hostname);
            } catch (UnknownHostException e) {
                return Arrays.asList(
                        InetAddress.getByName("8.8.8.8"),
                        InetAddress.getByName("8.8.4.4")
                );
            }
        });
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();

            String token = PreferenceManager.getToken(context);
            if (token != null && token.startsWith("debug-token")) {
                token = null;
            }

            Request.Builder requestBuilder = original.newBuilder()
                    .header("token", token)
                    .method(original.method(), original.body());
            if (original.body() == null || original.body().contentType() == null
                    || !original.body().contentType().type().equals("multipart")) {
                requestBuilder.header("Content-Type", "application/json");
            }
            Request request = requestBuilder.build();
            // Customize or return the response
            return chain.proceed(request);
        });

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        OkHttpClient client = httpClient
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        Retrofit builder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();


        return builder.create(serviceClass);
    }


    public  <S> S AuthService(Class<S> serviceClass, String baseUrl) {

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();

            // Customize the request
            Request request = original.newBuilder()
                    .method(original.method(), original.body())
                    .build();

            // Customize or return the response
            return chain.proceed(request);
        });

        // Wallet auth uses real backend responses in debug.

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        OkHttpClient client = httpClient
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(50, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(interceptor)
                .build();
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client);

        Retrofit adapter = builder.build();

        return adapter.create(serviceClass);
    }
}
