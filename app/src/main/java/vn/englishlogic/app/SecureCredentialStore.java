package vn.englishlogic.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureCredentialStore {
    private static final String KEY_ALIAS = "english_logic_device_credential_v1";
    private static final String PREFERENCES = "english_logic_secure_credentials";
    private static final String CREDENTIAL_KEY = "encrypted_device_credential";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SharedPreferences preferences;
    private final SecureRandom secureRandom = new SecureRandom();

    SecureCredentialStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    String createOpaqueToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.encodeToString(token, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    boolean save(String accountId, String deviceId, String deviceToken) {
        if (accountId == null || !accountId.startsWith("account:") || !isUuid(deviceId) || !isSafeToken(deviceToken)) return false;
        try {
            JSONObject payload = new JSONObject();
            payload.put("accountId", accountId);
            payload.put("deviceId", deviceId);
            payload.put("deviceToken", deviceToken);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            byte[] envelope = new byte[1 + iv.length + encrypted.length];
            envelope[0] = (byte) iv.length;
            System.arraycopy(iv, 0, envelope, 1, iv.length);
            System.arraycopy(encrypted, 0, envelope, 1 + iv.length, encrypted.length);
            return preferences.edit().putString(
                CREDENTIAL_KEY,
                Base64.encodeToString(envelope, Base64.NO_WRAP)
            ).commit();
        } catch (Exception error) {
            return false;
        }
    }

    String load() {
        String stored = preferences.getString(CREDENTIAL_KEY, null);
        if (stored == null || stored.isEmpty()) return "";
        try {
            byte[] envelope = Base64.decode(stored, Base64.DEFAULT);
            if (envelope.length < 14) throw new IllegalStateException("Credential envelope is incomplete");
            int ivLength = envelope[0] & 0xff;
            if (ivLength < 12 || ivLength > 16 || envelope.length <= 1 + ivLength) {
                throw new IllegalStateException("Credential IV is invalid");
            }
            byte[] iv = new byte[ivLength];
            byte[] encrypted = new byte[envelope.length - 1 - ivLength];
            System.arraycopy(envelope, 1, iv, 0, ivLength);
            System.arraycopy(envelope, 1 + ivLength, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            String cleartext = new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
            JSONObject parsed = new JSONObject(cleartext);
            if (!parsed.optString("accountId").startsWith("account:")
                || !isUuid(parsed.optString("deviceId"))
                || !isSafeToken(parsed.optString("deviceToken"))) {
                throw new IllegalStateException("Credential payload is invalid");
            }
            return parsed.toString();
        } catch (Exception error) {
            preferences.edit().remove(CREDENTIAL_KEY).apply();
            return "";
        }
    }

    boolean hasCredential() {
        return !load().isEmpty();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build());
        return generator.generateKey();
    }

    private boolean isUuid(String value) {
        return value != null && value.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    }

    private boolean isSafeToken(String value) {
        return value != null && value.matches("^[A-Za-z0-9._~-]{32,512}$");
    }
}
