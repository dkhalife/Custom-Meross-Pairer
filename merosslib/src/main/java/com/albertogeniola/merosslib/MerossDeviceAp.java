package com.albertogeniola.merosslib;

import com.albertogeniola.merosslib.model.protocol.Message;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiList;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAll;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigKey;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigKeyResponse;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigWifi;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigWifiResponse;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import lombok.Getter;


public class MerossDeviceAp implements Serializable {

    @Getter
    private final String ip;
    @Getter
    private final String cloudKey;
    private final OkHttpClient client;
    private final Gson g = Utils.getGson();

    public MerossDeviceAp(String ip, String cloudKey) {
        this.ip = ip;
        this.cloudKey = cloudKey;
        this.client = new OkHttpClient();
        this.client.setConnectTimeout(15, TimeUnit.SECONDS);
        this.client.setReadTimeout(15, TimeUnit.SECONDS);
    }

    public MerossDeviceAp() {
        this("10.10.10.1", "");
    }

    public MessageGetSystemAllResponse getConfig() throws IOException {
        Message message = MessageGetSystemAll.BuildNew();
        return this.sendMessage(message, MessageGetSystemAllResponse.class);
    }

    public MessageGetConfigWifiListResponse scanWifi() throws IOException {
        Message message = MessageGetConfigWifiList.BuildNew();
        return this.sendMessage(message, MessageGetConfigWifiListResponse.class);
    }

    public void setConfigKey(String hostname, int port, String key, String userId ) throws IOException {
        Message message = MessageSetConfigKey.BuildNew(hostname, port, key, userId);
        this.sendMessage(message, MessageSetConfigKeyResponse.class);
    }

    public void setConfigWifi(GetConfigWifiListEntry wifiConfig, String base64password) throws IOException {
        Message message = MessageSetConfigWifi.BuildNew(wifiConfig.getBase64ssid(), base64password, wifiConfig.getBssid(), wifiConfig.getChannel(), wifiConfig.getCipher(), wifiConfig.getEncryption());
        this.sendMessage(message, MessageSetConfigWifiResponse.class);
    }

    private <T> T sendMessage(Message message, Class<T> type) throws IOException {
        message.sign(cloudKey); // Signature is not verified when pairing!

        String jj = g.toJson(message);
        RequestBody msg = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jj.getBytes(StandardCharsets.UTF_8));
        Request request = new Request.Builder()
                .url("http://" + ip + "/config")
                .addHeader("Content-Type", "application/json")
                .post(msg)
                .build();
        Response response = client.newCall(request).execute();

        if (response.code() != 200 ) {
            throw new IOException("Invalid response code (" + response.code() + ") received from Meross Device");
        }

        ResponseBody body = response.body();
        T retValue = g.fromJson(body.string(), type);
        body.close();
        return retValue;
    }
}
