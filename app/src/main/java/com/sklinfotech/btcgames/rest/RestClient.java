package com.sklinfotech.btcgames.rest;

import android.content.Context;
import android.util.Log;

import com.sklinfotech.btcgames.settings.CurrencySettings;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

abstract class RestClient {

  protected Context ctx;

  RestClient(final Context ctx) {
    this.ctx = ctx;
  }

  String tag() {
    return this.getClass().getSimpleName();
  }

  private final int CONNECT_TIMEOUT = 6000;
  private final int READ_TIMEOUT = 6000;

  InputStreamReader getInputStreamReader(String path, String params, String accountKey) throws IOException {
    String p = path;
    if (params != null) {
      p += "?" + params;
    }

    HttpURLConnection conn = requestGET(p, accountKey);
    return new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
  }

  private InputStreamReader getInputStreamReader(String path) throws IOException {
    return getInputStreamReader(path, null, null);
  }

  private InputStreamReader postInputStreamReader(String path, String params, String accountKey) throws IOException {
    HttpURLConnection conn = requestPOST(path, accountKey);

    // POST
    conn.setDoOutput(true);
    if (params != null) {
      OutputStreamWriter wr = new OutputStreamWriter(
              conn.getOutputStream());
      wr.write(params);
      wr.flush();
      wr.close();
    }

    // Standard
    conn.setConnectTimeout(CONNECT_TIMEOUT);
    conn.setReadTimeout(READ_TIMEOUT);
    conn.setInstanceFollowRedirects(true);

    return new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
  }

  private HttpURLConnection requestGET(String path, String accountKey) throws IOException {
    return connect("GET", path, accountKey);
  }

  private HttpURLConnection requestPOST(String path, String accountKey) throws IOException {
    return connect("POST", path, accountKey);
  }

  private HttpURLConnection connect(String method, String path, String accountKey) throws IOException {
    URL u = new URL(CurrencySettings.getInstance(ctx).getServerAddress() + "/" + path);
    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
    Log.v(method, u.toString());
    conn.setRequestMethod(method);
    conn.setConnectTimeout(CONNECT_TIMEOUT);
    conn.setReadTimeout(READ_TIMEOUT);
    // conn.setInstanceFollowRedirects(true);

    // TB TODO - Support password encrypted accounts
    if (accountKey != null) {
      conn.setRequestProperty("Cookie", "account_key=" + accountKey);
    }

    return conn;
  }

  String encodeKeyValuePair(String key, String value) throws UnsupportedEncodingException {
    if (value == null) {
      return "";
    }

    return URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
  }

  String encodeKeyValuePair(String key, long value) throws UnsupportedEncodingException {
    return encodeKeyValuePair(key, Long.toString(value));
  }

  String encodeKeyValuePair(String key, int value) throws UnsupportedEncodingException {
    return encodeKeyValuePair(key, Integer.toString(value));
  }

  String encodeKeyValuePair(String key, boolean value) throws UnsupportedEncodingException {
    return encodeKeyValuePair(key, Boolean.toString(value));
  }

  protected String getAccountKey() {
    return CurrencySettings.getInstance(ctx).getAccountKey();
  }
}
