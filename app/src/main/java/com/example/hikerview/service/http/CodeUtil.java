package com.example.hikerview.service.http;

import android.text.TextUtils;
import android.util.Log;

import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.StringUtil;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.request.PostRequest;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.FormBody;
import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2018/12/2
 * 时间：At 15:00
 */
public class CodeUtil {
    private static final String TAG = "CodeUtil";

    public static void get(String url, OnCodeGetListener listener) {
        get(url, "UTF-8", null, listener);
    }

    public static void get(String url, final String charset, Map<String, String> headers, final OnCodeGetListener listener) {
        long start = System.currentTimeMillis();
        url = url.replace(" ", "");
        url = StringUtil.decodeConflictStr(url);
        if (headers != null) {
            for (String key : headers.keySet()) {
                headers.put(key, StringUtil.decodeConflictStr(headers.get(key)));
            }
        }
        if (url.startsWith("hiker://files/")) {
            String fileName = StringUtil.trimBlanks(url.replace("hiker://files/", ""));
            File file = new File(SettingConfig.rootDir + File.separator + fileName);
            if (file.exists()) {
                listener.onSuccess(FileUtil.fileToString(file.getAbsolutePath()));
            } else {
                listener.onFailure(404, url + "文件不存在");
            }
            return;
        } else if (url.startsWith("file://") || url.startsWith("/")) {
            dealFileByPath(url, listener);
            return;
        } else if (url.startsWith("hiker://")) {
            dealFileByHiker(url, listener);
            return;
        }
        String charsetCode = null;
        if (!TextUtils.isEmpty(charset) && !"UTF-8".equals(charset)) {
            charsetCode = charset;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.put(entry.getKey(), entry.getValue());
            }
        }
        OkGo.<String>get(url)
                .headers(httpHeaders).execute(new CharsetStringCallback(charsetCode) {
            @Override
            public void onSuccess(com.lzy.okgo.model.Response<String> response) {
                long end = System.currentTimeMillis();
                Timber.d("codeUtil, fetch: consume=%s", (end - start));
                listener.onSuccess(response.body());
            }

            @Override
            public void onError(com.lzy.okgo.model.Response<String> response) {
                super.onError(response);
                listener.onFailure(response.code(), response.getException().toString());
            }
        });
    }

    private static void dealFileByHiker(String url, OnCodeGetListener listener) {
        if (url.startsWith("hiker://assets/")) {
            listener.onSuccess(HikerRuleUtil.getAssetsFileByHiker(url));
        }
        HikerRuleUtil.getRulesByHiker(url, listener);
    }

    private static void dealFileByPath(String url, OnCodeGetListener listener) {
        url = url.replace("file://", "");
        File file = new File(url);
        if (file.exists()) {
            listener.onSuccess(FileUtil.fileToString(url));
        } else {
            listener.onFailure(404, url + "文件不存在");
        }
    }

    public static void post(String url, HttpParams params, OnCodeGetListener listener) {
        post(url, params, "UTF-8", null, listener);
    }

    public static void post(String url, HttpParams params, final String charset, Map<String, String> headers, final OnCodeGetListener listener) {
        url = url.replace(" ", "");
        url = StringUtil.decodeConflictStr(url);
        if (headers != null) {
            for (String key : headers.keySet()) {
                headers.put(key, StringUtil.decodeConflictStr(headers.get(key)));
            }
        }
        if (url.startsWith("file://") || url.startsWith("/")) {
            dealFileByPath(url, listener);
            return;
        }
        String charsetCode = null;
        if (!TextUtils.isEmpty(charset) && !"UTF-8".equals(charset)) {
            charsetCode = charset;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.put(entry.getKey(), entry.getValue());
            }
        }
//        JSEngine.getInstance().setUrl(url);
        PostRequest<String> request = OkGo.post(url);
        for (String key : params.urlParamsMap.keySet()) {
            if ("JsonBody".equals(key)) {
                Log.d(TAG, "post: " + params.urlParamsMap.get(key).get(0));
                request.upJson(params.urlParamsMap.get(key).get(0));
                params.urlParamsMap.remove(key);
                break;
            }
        }
        Map<String, String> map = new HashMap<>();
        for (String key : params.urlParamsMap.keySet()) {
            if (params.urlParamsMap.get(key) != null && params.urlParamsMap.get(key).size() > 0) {
                map.put(key, StringUtil.decodeConflictStr(params.urlParamsMap.get(key).get(0)));
            }
        }
        request.params(map);
        if (StringUtil.isNotEmpty(charsetCode) && !"UTF-8".equalsIgnoreCase(charsetCode)
                && !map.isEmpty()) {
            FormBody.Builder bodyBuilder = new FormBody.Builder(Charset.forName(charset));
            for (String key : request.getParams().urlParamsMap.keySet()) {
                List<String> urlValues = request.getParams().urlParamsMap.get(key);
                if(CollectionUtil.isNotEmpty(urlValues)){
                    for (String value : urlValues) {
                        bodyBuilder.add(key, value);
                    }
                }
            }
            request.upRequestBody(bodyBuilder.build());
            request.removeAllParams();
        }
        request.headers(httpHeaders)
                .execute(new CharsetStringCallback(charsetCode) {
                    @Override
                    public void onSuccess(com.lzy.okgo.model.Response<String> response) {

                        listener.onSuccess(response.body());
                    }

                    @Override
                    public void onError(com.lzy.okgo.model.Response<String> response) {
                        super.onError(response);
                        listener.onFailure(response.code(), response.getException().toString());
                    }
                });

    }


    public interface OnCodeGetListener {
        void onSuccess(String s);

        void onFailure(int errorCode, String msg);
    }
}
