package com.albertogeniola.merossconf;

import static com.albertogeniola.merossconf.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.albertogeniola.merosslib.Utils;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class AndroidPreferencesManager {
    private static final String PREFS_CONFS = "com.albertogeniola.merossconf.shared_preferences";
    private static final String PREFS_WIFI_CREDS = "com.albertogeniola.merossconf.shared_preferences.wifi_creds";
    private static final String KEY_MQTT_CONF = "mqtt";
    private static final String KEY_HTTP_CONF = "http";
    private static final Gson g = Utils.getGson();

    public static void storeNewMqttConfiguration(Context context, MqttConfiguration conf) {
        List<MqttConfiguration> allConfs = loadAllMqttConfigurations(context);
        allConfs.removeIf(c -> c.getName().toLowerCase().trim().compareTo(conf.getName().toLowerCase().trim()) == 0);
        allConfs.add(conf);

        SharedPreferences settings = context.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String json = g.toJson(allConfs);
        editor.putString(KEY_MQTT_CONF, json);
        editor.apply();
    }

    public static List<MqttConfiguration> loadAllMqttConfigurations(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        List<MqttConfiguration> res = null;
        try {
            res = g.fromJson(settings.getString(KEY_MQTT_CONF, null), new TypeToken<List<MqttConfiguration>>() {}.getType());
        } catch (Exception e) {
            // Error while loading configurations
            Log.e(LOG_TAG, "Error while loading stored MQTT Configurations. This error is ignored.");
        }
        if (res == null)
            return new ArrayList<>();
        else
            return res;

    }

    public static void storeHttpCredentials(Context c, ApiCredentials httpCredentials) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String json = g.toJson(httpCredentials);
        editor.putString(KEY_HTTP_CONF, json);
        editor.apply();
    }

    public static void removeHttpCredentials(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(KEY_HTTP_CONF);
        editor.apply();
    }

    @Nullable
    public static String getWifiStoredPassword(@NonNull Context c, @NonNull String bssid) {
        return c.getSharedPreferences(PREFS_WIFI_CREDS, Context.MODE_PRIVATE).getString(bssid.trim().toLowerCase(), null);
    }

    public static void storeWifiStoredPassword(@NonNull Context c, @NonNull String bssid, @NonNull String password) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS_WIFI_CREDS, Context.MODE_PRIVATE).edit();
        e.putString(bssid.trim().toLowerCase(), password);
        e.apply();
    }

    @Nullable
    public static ApiCredentials loadHttpCredentials(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        return g.fromJson(settings.getString(KEY_HTTP_CONF, null), ApiCredentials.class);
    }
}
