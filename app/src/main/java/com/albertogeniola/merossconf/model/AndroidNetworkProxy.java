package com.albertogeniola.merossconf.ui.fragments.pair;

import android.net.ConnectivityManager;
import android.net.Network;

import com.albertogeniola.merossconf.model.HttpClientManager;import com.albertogeniola.merosslib.NetworkProxy;
import com.albertogeniola.merosslib.NetworkResponse;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class AndroidNetworkProxy extends NetworkProxy {
    private final Network network;

    public AndroidNetworkProxy(Network network) {
        if (network != null) {
            this.network = network;
        } else {
            ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
            Network currentNetwork = connectivityManager.getActiveNetwork();
        }
    }

    @Override
    public NetworkResponse post(URL url, String contentType, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) this.network.openConnection(url);
        connection.setConnectTimeout(15);
        connection.setReadTimeout(15);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);

        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                out, StandardCharsets.UTF_8));
        writer.write(body);
        writer.flush();

        int code = connection.getResponseCode();
        String response = null;
        if (code ==  200) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                builder.append(line);
            }
            response = builder.toString();
        }

        return new HttpClientManager.AndroidNetworkProxyResponse(code, response);
    }

    @Override
    public NetworkResponse post(URL url, HashMap<String, String> headers, String body) throws IOException {
        return null;
    }
}
