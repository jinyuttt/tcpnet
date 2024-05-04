package org.cd.httpnet;

import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClient {
    private static OkHttpClient client = null;
    private static final String DEFAULT_MEDIA_TYPE = "application/json; charset=utf-8";

    private static final int CONNECT_TIMEOUT = 5;

    private static final int READ_TIMEOUT = 7;

    private static final String GET = "GET";

    private static final String POST = "POST";



    private static OkHttpClient getInstance() {
        if (client == null) {
            synchronized (OkHttpClient.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return client;
    }


    /**
     * get
     *
     * @param url
     * @return
     */
    public static String doGet(String url) {

        try {

            Request request = new Request.Builder().url(url).get().build();
            // 创建一个请求
            Response response = getInstance().newCall(request).execute();
            if (response.isSuccessful()) {
                int httpCode = response.code();
                String result;
                ResponseBody body = response.body();
                if (body != null) {
                    result = body.string();

                } else {
                    response.close();
                    throw new RuntimeException("exception in OkHttpUtil,response body is null");
                }
                return result;
            }
            return "";

        } catch (Exception ex) {

            return "";
        }
    }


    /**
     * @param url
     * @param postBody
     * @param mediaType
     * @return
     */
    public static String doPost(String url, String postBody, String mediaType) {
        try {
            long startTime = System.currentTimeMillis();
            MediaType createMediaType = MediaType.parse(mediaType == null ? DEFAULT_MEDIA_TYPE : mediaType);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(createMediaType, postBody))
                    .build();

            Response response = getInstance().newCall(request).execute();
            if (response.isSuccessful()) {

                int httpCode = response.code();
                String result;
                ResponseBody body = response.body();
                if (body != null) {
                    result = body.string();

                } else {
                    response.close();
                    throw new IOException("exception in OkHttpUtil,response body is null");
                }
                return result;
            }
            return "";
        } catch (Exception ex) {

            return "";
        }
    }

    /**
     * @param url
     * @param parameterMap
     * @return
     */
    public static String doPost(String url, Map<String, String> parameterMap) {
        try {
            long startTime = System.currentTimeMillis();
            List<String> parameterList = new ArrayList<>();
            FormBody.Builder builder = new FormBody.Builder();
            if (parameterMap.size() > 0) {
                for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
                    String parameterName = entry.getKey();
                    String value = entry.getValue();
                    builder.add(parameterName, value);
                    parameterList.add(parameterName + ":" + value);
                }
            }
            FormBody formBody = builder.build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            Response response = getInstance().newCall(request).execute();
            if (response.isSuccessful()) {
                String result;
                int httpCode = response.code();
                ResponseBody body = response.body();
                if (body != null) {
                    result = body.string();
                } else {
                    response.close();
                    throw new IOException("exception in OkHttpUtil,response body is null");
                }
                return result;
            }
            return "";

        } catch (Exception ex) {
            return "";
        }
    }
}

