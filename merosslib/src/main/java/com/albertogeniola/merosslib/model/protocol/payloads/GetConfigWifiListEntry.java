package com.albertogeniola.merosslib.model.protocol.payloads;

import com.albertogeniola.merosslib.model.Cipher;
import com.albertogeniola.merosslib.model.Encryption;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

import lombok.Getter;

@Getter
public class GetConfigWifiListEntry {
    @SerializedName("ssid")
    private String base64ssid;

    @SerializedName("bssid")
    private String bssid;

    @SerializedName("signal")
    private Double signal;

    @SerializedName("channel")
    private Integer channel;

    @SerializedName("encryption")
    private Encryption encryption;

    @SerializedName("cipher")
    private Cipher cipher;

    public String getSsid() {
        if (base64ssid != null) {
            return new String( Base64.decodeBase64(base64ssid.getBytes()), StandardCharsets.UTF_8);
        }
        return null;
    }
}
