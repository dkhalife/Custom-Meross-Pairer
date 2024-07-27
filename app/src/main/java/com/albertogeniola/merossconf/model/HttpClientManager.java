package com.albertogeniola.merossconf.model;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import com.albertogeniola.merosslib.MerossHttpClient;
import com.albertogeniola.merosslib.NetworkProxy;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.DeviceInfo;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class HttpClientManager {
    // Singleton pattern
    private static HttpClientManager mClientManager;
    public static HttpClientManager getInstance() {
        if (mClientManager == null) {
            mClientManager = new HttpClientManager();
        }
        return mClientManager;
    }

    // Instance attributes
    private MerossHttpClient mClient;

    private HttpClientManager() {
        mClient = new MerossHttpClient(null, new AndroidNetworkProxy(null));
    }

    public void loadFromCredentials(ApiCredentials creds, NetworkProxy network) {
        mClient = new MerossHttpClient(creds, network);
    }

    public void asyncLogin(final String serverUrl, final String username, final String password, Callback<ApiCredentials> callback) {
        if (mClient == null) {
            throw new IllegalStateException("HttpClient has not been loaded yet.");
        }

        try {
            mClient.login(serverUrl, username, password);
            callback.onSuccess(mClient.getCredentials());
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void asyncLogout(Callback<Void> callback) {
        if (mClient == null)
            throw new IllegalStateException("HttpClient has not been loaded yet");

        try {
            mClient.logout();
            callback.onSuccess(null);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void asyncListDevices(Callback<List<DeviceInfo>> callback) {
        if (mClient == null)
            throw new IllegalStateException("HttpClient has not been loaded yet");

        try {
            callback.onSuccess(mClient.listDevices());
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(Exception exception);
    }
}
