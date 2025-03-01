package com.example.hikerview.service.parser;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hikerview.constants.UAEnum;
import com.example.hikerview.service.http.CodeUtil;
import com.example.hikerview.utils.StringUtil;
import com.lzy.okgo.model.HttpParams;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 作者：By hdy
 * 日期：On 2018/9/8
 * 时间：At 8:36
 */

public class HttpParser {
    private static final String TAG = "HttpParser";

    public static void parseSearchUrlForHtml(String sourceUrl, @NonNull OnSearchCallBack onSearchCallBack) {
        String wd = sourceUrl.contains("%%") ? "%%" : "%%";
        parseSearchUrlForHtml(wd, sourceUrl, onSearchCallBack);
    }

    public static String parseSearchUrl(String sourceUrl, String wd) {
        String[] d = sourceUrl.split(";");
        if (d.length == 1) {
            return replaceKey(sourceUrl, wd);
        } else if (d.length == 2) {
            return replaceKey(d[0], wd);
        } else {
            wd = encodeUrl(wd, d[2]);
            return replaceKey(d[0], wd);
        }
    }

    private static String replaceKey(String url, String key) {
        if (StringUtil.isEmpty(url) || StringUtil.isEmpty(key)) {
            return url;
        }
        if (url.contains("%%")) {
            return url.replace("%%", key);
        }
        return url.replace("**", key);
    }

    public static void parseSearchUrlForHtml(String wd, String sourceUrl, @NonNull OnSearchCallBack onSearchCallBack) {
        String[] d = sourceUrl.split(";");
        if (d.length == 1) {
            wd = encodeUrl(wd, "UTF-8");
            d[0] = replaceKey(d[0], wd);
            d[0] = parseParamsByJs(d[0]);
            get(d[0], null, null, onSearchCallBack);
        } else if (d.length == 2) {
            if ("get".equals(d[1]) || "*".equals(d[1])) {
                wd = encodeUrl(wd, "UTF-8");
                d[0] = replaceKey(d[0], wd);
                d[0] = parseParamsByJs(d[0]);
                get(d[0], null, getHeaders(sourceUrl), onSearchCallBack);
            } else if ("post".equalsIgnoreCase(d[1])) {
                wd = encodeUrl(wd, "UTF-8");
                d[0] = replaceKey(d[0], wd);
                d[0] = parseParamsByJs(d[0]);
                String[] ss = StringUtil.splitUrlByQuestionMark(d[0]);
                String[] sss;
                if (ss.length > 1) {
                    sss = ss[1].split("&");
                } else {
                    sss = new String[]{};
                }
                HttpParams params = new HttpParams();
                for (int i = 0; i < sss.length; i++) {
                    if (TextUtils.isEmpty(sss[i])) {
                        continue;
                    }
                    String[] kk = sss[i].split("=");
                    if (kk.length >= 2) {
                        params.put(kk[0], kk[1]);
                    }
                }
                post(ss[0], null, getHeaders(sourceUrl), params, onSearchCallBack);
            } else {
                wd = encodeUrl(wd, d[1]);
                d[0] = replaceKey(d[0], wd);
                d[0] = parseParamsByJs(d[0]);
                get(d[0], d[1], getHeaders(sourceUrl), onSearchCallBack);
            }
        } else {
            wd = encodeUrl(wd, d[2]);
            if ("post".equalsIgnoreCase(d[1])) {
                if (StringUtil.isNotEmpty(d[2]) && !"utf-8".equalsIgnoreCase(d[2])) {
                    //后面会自动编码
                    wd = decodeUrl(wd, d[2]);
                }
                d[0] = replaceKey(d[0], wd);
                d[0] = parseParamsByJs(d[0]);
                String[] ss = StringUtil.splitUrlByQuestionMark(d[0]);
                String[] sss;
                if (ss.length > 1) {
                    sss = ss[1].split("&");
                } else {
                    sss = new String[]{};
                }
                HttpParams params = new HttpParams();
                for (int i = 0; i < sss.length; i++) {
                    if (TextUtils.isEmpty(sss[i])) {
                        continue;
                    }
                    String[] kk = sss[i].split("=");
                    if (kk.length >= 2) {
                        params.put(kk[0], kk[1]);
                    }
                }
                post(ss[0], d[2], getHeaders(sourceUrl), params, onSearchCallBack);
            } else {
                d[0] = replaceKey(d[0], wd);
                d[0] = parseParamsByJs(d[0]);
                get(d[0], d[2], getHeaders(sourceUrl), onSearchCallBack);
            }
        }
    }

    /**
     * 修改url，加上ua
     *
     * @param url
     * @param ua
     * @return
     */
    public static String getUrlAppendUA(String url, String ua) {
        if (StringUtil.isEmpty(ua)) {
            return url;
        }
        if (UAEnum.AUTO.getCode().equals(ua)) {
            return url;
        }
        String[] d = url.split(";");
        if (d.length == 1) {
            return url + ";get;UTF-8;{User-Agent@" + getRealUa(ua) + "}";
        } else if (d.length == 2) {
            return url + ";UTF-8;{User-Agent@" + getRealUa(ua) + "}";
        } else if (d.length == 3) {
            return url + ";{User-Agent@" + getRealUa(ua) + "}";
        } else if (url.contains("User-Agent@")) {
            return url;
        } else {
            String header = d[d.length - 1];
            if (!header.startsWith("{") || !header.endsWith("}")) {
                return url;
            }
            header = header.substring(0, header.length() - 1);
            if (header.length() == 1) {
                return StringUtil.arrayToString(d, 0, d.length - 1, ";") + ";{User-Agent@" + getRealUa(ua) + "}";
            } else {
                header = header + "&&User-Agent@" + getRealUa(ua) + ")";
                return StringUtil.arrayToString(d, 0, d.length - 1, ";") + ";" + header;
            }
        }
    }

    private static String getRealUa(String uaCode) {
        UAEnum uaEnum = UAEnum.getByCode(uaCode);
        return uaEnum.getContent().replace(";", "；；");
    }

    /**
     * 参数可以用js处理
     *
     * @param url
     * @return
     */
    private static String parseParamsByJs(String url) {
        String[] urls = StringUtil.splitUrlByQuestionMark(url);
        if (urls.length > 1) {
            String[] ps = urls[1].split("&");
            for (int i = 0; i < ps.length; i++) {
                String[] kv = ps[i].split("=");
                if (kv.length > 1) {
                    String kv1 = StringUtil.arrayToString(kv, 1, "=");
                    String[] vs = kv1.split("\\.js:");
                    if (vs.length > 1) {
                        kv1 = JSEngine.getInstance().evalJS(vs[1], vs[0]);
                    }
                    ps[i] = kv[0] + "=" + kv1;
                }
            }
            url = urls[0] + "?" + StringUtil.listToString(Arrays.asList(ps), "&");
        }
        String[] urls2 = url.split("\\?");
        urls2[0] = getUrlByJs(urls2[0]);
        url = StringUtil.arrayToString(urls2, 0, "?");
        System.out.println(url);
        return url;
    }

    private static String getUrlByJs(String url) {
        String[] vs = url.split("\\.js:");
        if (vs.length > 1) {
            url = JSEngine.getInstance().evalJS(vs[1], vs[0]);
        }
        return url;
    }

    public static void main(String[] args) {
        System.out.println(parseParamsByJs("http://qxkkk.cn/movie.php?m=/fyclass/list.php？？rank=fyyear&page=fypage"));
    }

    /**
     * 参数可以用js处理
     *
     * @param url
     * @return
     */
    private static String parseHeadersByJs(String url) {
        String[] ps = url.split("&&");
        for (int i = 0; i < ps.length; i++) {
            String[] kv = ps[i].split("@");
            if (kv.length > 1) {
                String[] vs = kv[1].split("\\.js:");
                if (vs.length > 1) {
                    kv[1] = JSEngine.getInstance().evalJS(vs[1], vs[0]);
                }
                ps[i] = kv[0] + "@" + kv[1];
            }
        }
        url = StringUtil.listToString(Arrays.asList(ps), "&&");
        return url;
    }

    public static String getCode(String searchUrl) {
        if (TextUtils.isEmpty(searchUrl)) {
            return null;
        }
        String[] d = searchUrl.split(";");
        String code = null;
        if (d.length >= 3) {
            code = d[2];
        }
        if ("*".equals(code)) {
            return "UTF-8";
        }
        return code;
    }

    public static Map<String, String> getHeaders(String searchUrl) {
        if (TextUtils.isEmpty(searchUrl)) {
            return null;
        }
        String[] d = searchUrl.split(";");
        String header = d[d.length - 1];
        if (!header.startsWith("{") || !header.endsWith("}")) {
            return null;
        }
        header = StringUtil.decodeConflictStr(header);
        Map<String, String> headers = new HashMap<>();
        String h = header.substring(1);
        h = h.substring(0, h.length() - 1);
        h = parseHeadersByJs(h);
        String[] hs = h.split("&&");
        for (String h1 : hs) {
            String[] keyValue = h1.split("@");
            if (keyValue.length >= 2) {
                if ("getTimeStamp()".equals(keyValue[1])) {
                    headers.put(keyValue[0], System.currentTimeMillis() + "");
                } else {
                    headers.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return headers;
    }

    public static String getRealUrlFilterHeaders(String searchUrl) {
        if (TextUtils.isEmpty(searchUrl)) {
            return "";
        }
        String[] d = searchUrl.split(";");
        String header = d[d.length - 1];
        if (!header.startsWith("{") || !header.endsWith("}")) {
            return searchUrl;
        }
        return d[0];
    }

    public static void get(String url, @Nullable final String code, @Nullable Map<String, String> headers, @NonNull final OnSearchCallBack onSearchCallBack) {
//        Log.d(TAG, "just get: "+url);
        try {
            url = url.replace(" ", "");
            url = StringUtil.decodeConflictStr(url);
            String finalUrl = url;
            CodeUtil.get(url, code, headers, new CodeUtil.OnCodeGetListener() {
                @Override
                public void onSuccess(String s) {
                    onSearchCallBack.onSuccess(finalUrl, s);
                }

                @Override
                public void onFailure(int errorCode, String msg) {
                    onSearchCallBack.onFailure(errorCode, msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            onSearchCallBack.onFailure(500, e.toString());
        }
    }

    public static void post(String url, @Nullable final String code, @Nullable Map<String, String> headers, HttpParams params, @NonNull final OnSearchCallBack onSearchCallBack) {
//        Log.d(TAG, "just get: "+url);
        try {
            url = url.replace(" ", "");
            url = StringUtil.decodeConflictStr(url);
            String finalUrl = url;
            CodeUtil.post(url, params, code, headers, new CodeUtil.OnCodeGetListener() {
                @Override
                public void onSuccess(String s) {
                    onSearchCallBack.onSuccess(finalUrl, s);
                }

                @Override
                public void onFailure(int errorCode, String msg) {
                    onSearchCallBack.onFailure(111, msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            onSearchCallBack.onFailure(500, e.toString());
        }
    }

    public interface OnSearchCallBack {

        void onSuccess(String url, String s);

        void onFailure(int errorCode, String msg);
    }

    public static String encodeUrl(String str, String code) {//url解码
        if (StringUtil.isEmpty(code) || "UTF-8".equals(code.toUpperCase()) || "*".equals(code)) {
            return str;
        }
        try {
            str = java.net.URLEncoder.encode(str, code);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "encodeUrl: ", e);
        }
        return str;
    }

    public static String encodeUrl(String str) {//url解码
        try {
            str = java.net.URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "encodeUrl: ", e);
        }
        return str;
    }

    public static String decodeUrl(String str, String code) {//url解码
        try {
            str = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
            str = str.replaceAll("\\+", "%2B");
            str = java.net.URLDecoder.decode(str, code);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "decodeUrl: ", e);
        }
        return str;
    }

    public static String getFirstPageUrl(String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        try {
            int page = 1;
            String[] allUrl = url.split(";");
            String[] urls = allUrl[0].split("\\[firstPage=");
            if (urls.length > 1) {
                return urls[1].split("]")[0];
            } else if (urls[0].contains("fypage@")) {
                //fypage@-1@*2@/fyclass
                String[] strings = urls[0].split("fypage@");
                String[] pages = strings[1].split("@");
                for (int i = 0; i < pages.length - 1; i++) {
                    if (pages[i].startsWith("-")) {
                        page = page - Integer.parseInt(pages[i].replace("-", ""));
                    } else if (pages[i].startsWith("+")) {
                        page = page + Integer.parseInt(pages[i].replace("+", ""));
                    } else if (pages[i].startsWith("*")) {
                        page = page * Integer.parseInt(pages[i].replace("*", ""));
                    } else if (pages[i].startsWith("/")) {
                        page = page / Integer.parseInt(pages[i].replace("/", ""));
                    }
                }
                //前缀 + page + 后缀
                return strings[0] + page + pages[pages.length - 1];
            } else {
                return urls[0].replace("fypage", page + "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

}
