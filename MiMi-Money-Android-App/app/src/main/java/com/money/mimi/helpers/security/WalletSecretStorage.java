package com.money.mimi.helpers.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

public final class WalletSecretStorage {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String PREF_SECURE_STORAGE = "WALLET_SECURE_STORAGE";
    private static final String KEY_WRAPPED_AES = "KEY_WRAPPED_AES";
    private static final String KEY_FALLBACK_AES = "KEY_FALLBACK_AES";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "mimi_wallet_secret_key";
    private static final String VALUE_PREFIX = "enc_v1";

    private static SecretKey cachedSecretKey;

    private WalletSecretStorage() {
    }

    public static boolean isEncryptedValue(String value) {
        return value != null && value.startsWith(VALUE_PREFIX + ":");
    }

    public static synchronized String encrypt(Context context, String clearText) {
        if (clearText == null) return null;
        try {
            SecretKey secretKey = getOrCreateSecretKey(context.getApplicationContext());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(clearText.getBytes(UTF_8));
            return VALUE_PREFIX + ":" + encode(cipher.getIV()) + ":" + encode(encrypted);
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized String decrypt(Context context, String encryptedValue) {
        if (TextUtils.isEmpty(encryptedValue)) return null;
        if (!isEncryptedValue(encryptedValue)) return encryptedValue;
        try {
            String[] parts = encryptedValue.split(":", 3);
            if (parts.length != 3) return null;
            SecretKey secretKey = getOrCreateSecretKey(context.getApplicationContext());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(decode(parts[1])));
            byte[] decrypted = cipher.doFinal(decode(parts[2]));
            return new String(decrypted, UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey getOrCreateSecretKey(Context context) throws Exception {
        if (cachedSecretKey != null) return cachedSecretKey;

        SharedPreferences prefs = context.getSharedPreferences(PREF_SECURE_STORAGE, Context.MODE_PRIVATE);
        String fallbackKey = prefs.getString(KEY_FALLBACK_AES, null);
        if (!TextUtils.isEmpty(fallbackKey)) {
            cachedSecretKey = new SecretKeySpec(decode(fallbackKey), "AES");
            return cachedSecretKey;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                cachedSecretKey = loadOrCreateKeystoreBackedKey(context, prefs);
                return cachedSecretKey;
            } catch (Exception ignored) {
                // Fall back to app-private key storage on devices where AndroidKeyStore is unavailable.
            }
        }

        cachedSecretKey = loadOrCreateFallbackKey(prefs);
        return cachedSecretKey;
    }

    private static SecretKey loadOrCreateKeystoreBackedKey(Context context, SharedPreferences prefs) throws Exception {
        ensureKeyPair(context);

        String wrappedKey = prefs.getString(KEY_WRAPPED_AES, null);
        if (!TextUtils.isEmpty(wrappedKey)) {
            return new SecretKeySpec(unwrapKey(decode(wrappedKey)), "AES");
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();

        boolean stored = prefs.edit()
                .putString(KEY_WRAPPED_AES, encode(wrapKey(secretKey.getEncoded())))
                .remove(KEY_FALLBACK_AES)
                .commit();

        if (!stored) throw new IllegalStateException("Unable to persist wrapped wallet key");
        return secretKey;
    }

    private static SecretKey loadOrCreateFallbackKey(SharedPreferences prefs) throws Exception {
        String rawKey = prefs.getString(KEY_FALLBACK_AES, null);
        if (!TextUtils.isEmpty(rawKey)) {
            return new SecretKeySpec(decode(rawKey), "AES");
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        boolean stored = prefs.edit().putString(KEY_FALLBACK_AES, encode(secretKey.getEncoded())).commit();
        if (!stored) throw new IllegalStateException("Unable to persist fallback wallet key");
        return secretKey;
    }

    private static void ensureKeyPair(Context context) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) return;

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", KEYSTORE_PROVIDER);
        keyPairGenerator.initialize(buildLegacyKeyPairSpec(context));
        keyPairGenerator.generateKeyPair();
    }

    private static java.security.spec.AlgorithmParameterSpec buildLegacyKeyPairSpec(Context context) throws Exception {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 25);

        Class<?> builderClass = Class.forName("android.security.KeyPairGeneratorSpec$Builder");
        Object builder = builderClass.getConstructor(Context.class).newInstance(context);
        invokeBuilder(builderClass, builder, "setAlias", String.class, KEY_ALIAS);
        invokeBuilder(builderClass, builder, "setSubject", X500Principal.class, new X500Principal("CN=Mimi Wallet,O=Mimi"));
        invokeBuilder(builderClass, builder, "setSerialNumber", BigInteger.class, BigInteger.ONE);
        invokeBuilder(builderClass, builder, "setStartDate", Date.class, start.getTime());
        invokeBuilder(builderClass, builder, "setEndDate", Date.class, end.getTime());
        Method buildMethod = builderClass.getMethod("build");
        return (java.security.spec.AlgorithmParameterSpec) buildMethod.invoke(builder);
    }

    private static void invokeBuilder(Class<?> builderClass, Object builder, String methodName, Class<?> paramType, Object value) throws Exception {
        Method method = builderClass.getMethod(methodName, paramType);
        method.invoke(builder, value);
    }

    private static byte[] wrapKey(byte[] rawKey) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        Certificate certificate = keyStore.getCertificate(KEY_ALIAS);
        if (certificate == null) throw new IllegalStateException("Missing wallet key certificate");
        PublicKey publicKey = certificate.getPublicKey();
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(rawKey);
    }

    private static byte[] unwrapKey(byte[] wrappedKey) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
        if (privateKey == null) throw new IllegalStateException("Missing wallet private key");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(wrappedKey);
    }

    private static String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private static byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }
}