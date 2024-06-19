package com.albertogeniola.merossconf;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.util.DisplayMetrics;

import androidx.core.location.LocationManagerCompat;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;


public class AndroidUtils {
    public static boolean isLocationEnabled(Context context)
    {
        return LocationManagerCompat.isLocationEnabled((LocationManager) context.getSystemService(Context.LOCATION_SERVICE));
    }

    public static String getConnectedWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
            return null;

        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getBSSID() == null) {
            return null;
        } else {
            return info.getSSID().replaceAll("\"","");
        }
    }

    public static String getWifiNameFromSsid(WifiSsid ssid) {
        if (ssid == null) {
            return "";
        }

        return new String(ssid.getBytes(), StandardCharsets.UTF_8);
    }

    public static Boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static Boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi != null && mWifi.isConnected();
    }

    public static boolean validateBaseUrl(String url) {
        return Pattern.matches("^(http|https)://([_\\-a-zA-Z0-9.]+)(:[0-9]+)?$", url);
    }

    public static int dpToPx(Context ctx, int dp) {
        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
