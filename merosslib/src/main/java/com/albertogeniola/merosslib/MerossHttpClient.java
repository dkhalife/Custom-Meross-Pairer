package com.albertogeniola.merosslib;

import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.ApiResponse;
import com.albertogeniola.merosslib.model.http.DeviceInfo;
import com.albertogeniola.merosslib.model.http.ErrorCodes;
import com.albertogeniola.merosslib.model.http.LoginResponseData;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiException;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiInvalidCredentialsException;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiTokenException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import lombok.NonNull;
import lombok.SneakyThrows;


public class MerossHttpClient implements Serializable {
    // Static attributes
    private final static Logger l = Logger.getLogger(MerossHttpClient.class.getName());

    private static final Gson g =  Utils.getGson();
    private static final String NOONCE_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOGIN_PATH = "/v1/Auth/Login";
    private static final String DEVICE_LIST = "/v1/Device/devList";
    private static final String LOGOUT_PATH = "/v1/Profile/logout";
    private static final String SECRET = "23x17ahWarFH6w29";
    private static final HashMap<String, Object> DEFAULT_PARAMS = new HashMap<>();

    // Class attributes
    private ApiCredentials mCredentials;
    private final NetworkProxy mClient;

    public MerossHttpClient() {
        this(null, null);
    }

    public MerossHttpClient(ApiCredentials creds, NetworkProxy client) {
        this.mCredentials = creds;
        this.mClient = client;
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    public void login(String apiUrl, String username, String password) throws IOException, HttpApiException {
        HashMap<String, Object> data = new HashMap<>();
        data.put("email", username);
        data.put("password", password);
        LoginResponseData result = authenticatedPost( apiUrl+LOGIN_PATH, data, null, LoginResponseData.class);

        this.mCredentials = new ApiCredentials(
                apiUrl,
                result.getToken(),
                result.getUserId(),
                result.getEmail(),
                result.getKey(),
                new Date()
        );
    }

    public List<DeviceInfo> listDevices() throws IOException, HttpApiException {
        HashMap<String, Object> data = new HashMap<>();
        TypeToken<?> typeToken = TypeToken.getParameterized(List.class, DeviceInfo.class);
        return authenticatedPost( mCredentials.getApiServer()+DEVICE_LIST, data, this.mCredentials.getToken(), typeToken.getType());
    }

    public void logout() throws HttpApiException, IOException {
        if (mCredentials == null) {
            throw new IllegalStateException("Invalid logout operation: this client is not logged in.");
        }
        authenticatedPost(mCredentials.getApiServer()+LOGOUT_PATH, null, mCredentials.getToken(), Object.class);
    }

    private static String generateNonce() {
        StringBuilder result = new StringBuilder(16);
        Random random = new Random();
        for (int i = 0; i< 16; i++) {
            char randomChar = NOONCE_ALPHABET.charAt(random.nextInt(NOONCE_ALPHABET.length()));
            result.append(randomChar);
        }
        return result.toString();
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    @SneakyThrows({UnsupportedEncodingException.class, NoSuchAlgorithmException.class})
    private <T> T authenticatedPost(@NonNull String url, HashMap<String, Object> data, String httpToken, Type dataType) throws IOException, HttpApiException {

        String nonce = generateNonce();
        long timestampMillis = new Date().getTime();
        String params = new String(Base64.encodeBase64(g.toJson(data == null ? DEFAULT_PARAMS : data ).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        // Generate the md5-hash (called signature)
        MessageDigest m = MessageDigest.getInstance("md5");
        String dataToSign = SECRET + timestampMillis + nonce + params;
        m.update(dataToSign.getBytes(StandardCharsets.UTF_8));
        String md5hash = toHexString(m.digest());

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("params", params);
        payload.put("sign", md5hash);
        payload.put("timestamp", timestampMillis);
        payload.put("nonce", nonce);

        String requestData = g.toJson(payload);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Authorization",  httpToken == null ? "Basic" : "Basic " + httpToken);
        headers.put("vender", "Meross");
        headers.put("AppVersion", "1.3.0");
        headers.put("AppLanguage", "EN");
        headers.put("User-Agent", "okhttp/3.6.0");

        NetworkResponse response = mClient.post(new URL(url), headers, requestData);
        String strdata = response.body();
        l.fine("HTTP Response, STATUS_CODE: "+response.code()+", BODY: "+strdata);
        if (response.code() != 200) {
            l.severe("Bad HTTP Response code: " + response.code() );
        }

        TypeToken<?> token = TypeToken.getParameterized(ApiResponse.class, dataType);
        ApiResponse<T> responseData = g.fromJson(strdata, token.getType());

        return switch (responseData.getApiStatus()) {
            case CODE_NO_ERROR -> responseData.getData();
            case CODE_WRONG_CREDENTIALS, CODE_UNEXISTING_ACCOUNT ->
                    throw new HttpApiInvalidCredentialsException(responseData.getApiStatus());
            case CODE_TOKEN_ERROR, CODE_TOKEN_EXPIRED, CODE_TOKEN_INVALID, CODE_TOO_MANY_TOKENS ->
                    throw new HttpApiTokenException(responseData.getApiStatus());
            default -> {
                l.severe("API Code was unknown. Passing CODE_ERROR_GENERIC to the handler.");
                throw new HttpApiException(responseData.getApiStatus() == null ? ErrorCodes.CODE_GENERIC_ERROR : responseData.getApiStatus());
            }
        };
    }

    public ApiCredentials getCredentials() {
        return mCredentials;
    }
}
