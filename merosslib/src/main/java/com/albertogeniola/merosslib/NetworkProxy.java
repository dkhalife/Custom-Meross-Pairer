package com.albertogeniola.merosslib;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;

public abstract class NetworkProxy {
    public abstract NetworkResponse post(URL url, String contentType, String body) throws IOException;

    public abstract NetworkResponse post(URL url, HashMap<String, String> headers, String body) throws IOException;
}
