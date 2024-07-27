package com.albertogeniola.merossconf.model;

import android.net.ConnectivityManager;
import android.net.Network;

import androidx.annotation.NonNull;

import com.albertogeniola.merosslib.NetworkProxy;
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
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class AndroidNetworkProxy extends NetworkProxy {
    private final Network network;

    public AndroidNetworkProxy(Network network) {
        this.network = network;
    }

    @Override
    public NetworkResponse post(URL url, String contentType, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (this.network != null ? this.network.openConnection(url) : url.openConnection());

        connection.setConnectTimeout(15);
        connection.setReadTimeout(15);
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);
        connection.setRequestProperty("Content-Type", contentType);
        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                out, StandardCharsets.UTF_8));
        writer.write(body);
        writer.flush();
        connection.connect();
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

        return new AndroidNetworkProxyResponse(code, response);
    }

    @Override
    public NetworkResponse post(URL url, HashMap<String, String> headers, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (this.network != null ? this.network.openConnection(url) : url.openConnection());

        connection.setConnectTimeout(15);
        connection.setReadTimeout(15);
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);
        headers.forEach(connection::setRequestProperty);
        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                out, StandardCharsets.UTF_8));
        writer.write(body);
        writer.flush();
        connection.connect();
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

        return new AndroidNetworkProxyResponse(code, response);
    }
}
