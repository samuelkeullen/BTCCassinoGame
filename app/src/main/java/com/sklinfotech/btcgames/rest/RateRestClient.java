package com.sklinfotech.btcgames.rest;

import android.content.Context;

import com.sklinfotech.btcgames.lib.JSONRateResponse;
import com.sklinfotech.btcgames.settings.CurrencySettings;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;

public class RateRestClient extends RestClient {

  private static RateRestClient instance;

  public static RateRestClient getInstance(final Context ctx) {
    if (instance == null) {
      synchronized (RateRestClient.class) {
        if (instance == null) {
          instance = new RateRestClient(ctx);
        }
      }
    }
    instance.ctx = ctx;
    return instance;
  }

  private RateRestClient(final Context ctx) {
    super(ctx);
  }

  public JSONRateResponse getRate() throws IOException {
    final InputStreamReader is = getInputStreamReader(
      CurrencySettings.getInstance(ctx).getCurrencyLowerCase() + "usd",
      null,
      CurrencySettings.getInstance(ctx).getAccountKey());
    return new Gson().fromJson(is, JSONRateResponse.class);
  }
}
