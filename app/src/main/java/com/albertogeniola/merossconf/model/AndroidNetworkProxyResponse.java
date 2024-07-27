package com.albertogeniola.merossconf.ui.fragments.pair;

import com.albertogeniola.merosslib.NetworkResponse;

public class AndroidNetworkProxyResponse extends NetworkResponse {
    private final int mCode;
    private final String mBody;

    AndroidNetworkProxyResponse(int code, String body) {
        this.mCode = code;
        this.mBody = body;
    }

    @Override
    public int code() {
        return this.mCode;
    }

    @Override
    public String body() {
        return this.mBody;
    }
}
