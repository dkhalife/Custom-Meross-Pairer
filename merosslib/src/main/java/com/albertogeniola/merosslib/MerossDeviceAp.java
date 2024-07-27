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

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import lombok.Getter;


public class MerossDeviceAp implements Serializable {

    @Getter
    private final String ip = "10.10.10.1";
    @Getter
    private final String cloudKey = "";
    private final NetworkProxy client;
    private final Gson g = Utils.getGson();

    public MerossDeviceAp(NetworkProxy client) {
        this.client = client;
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

        String msg = g.toJson(message);
        NetworkResponse response = client.post(new URL("http://" + ip + "/config"), "application/json", msg);

        if (response.code() != 200 ) {
            throw new IOException("Invalid response code (" + response.code() + ") received from Meross Device");
        }

        return g.fromJson(response.body(), type);
    }
}
