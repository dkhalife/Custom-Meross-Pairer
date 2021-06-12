package com.albertogeniola.merossconf;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;

import androidx.core.app.ActivityCompat;

import com.albertogeniola.merosslib.model.Cipher;
import com.albertogeniola.merosslib.model.Encryption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.regex.Pattern;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class AndroidUtils {
    public static boolean isLocationEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            // This is Deprecated in API 28
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return  (mode != Settings.Secure.LOCATION_MODE_OFF);

        }
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

    public static Boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static boolean validateBaseUrl(String url) {
        return Pattern.matches("^(http|https)\\:\\/\\/([\\_\\-a-zA-Z0-9\\.]+)(\\:[0-9]+)?$", url);
    }


    public static int dpToPx(Context ctx, int dp) {
        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static boolean checkPermissions(final Context ctx, String[] permissions) {
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(ctx, p) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
}
