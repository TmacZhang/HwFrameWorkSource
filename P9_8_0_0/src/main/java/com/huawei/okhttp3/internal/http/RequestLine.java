package com.huawei.okhttp3.internal.http;

import com.huawei.okhttp3.HttpUrl;
import com.huawei.okhttp3.Request;
import java.net.Proxy.Type;

public final class RequestLine {
    private RequestLine() {
    }

    public static String get(Request request, Type proxyType) {
        StringBuilder result = new StringBuilder();
        result.append(request.method());
        result.append(' ');
        if (includeAuthorityInRequestLine(request, proxyType)) {
            result.append(request.url());
        } else {
            result.append(requestPath(request.url()));
        }
        result.append(" HTTP/1.1");
        return result.toString();
    }

    private static boolean includeAuthorityInRequestLine(Request request, Type proxyType) {
        return !request.isHttps() && proxyType == Type.HTTP;
    }

    public static String requestPath(HttpUrl url) {
        String path = url.encodedPath();
        String query = url.encodedQuery();
        return query != null ? path + '?' + query : path;
    }
}
