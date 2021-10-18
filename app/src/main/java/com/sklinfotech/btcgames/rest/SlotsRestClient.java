package com.sklinfotech.btcgames.rest;

import android.content.Context;

import com.sklinfotech.btcgames.lib.JSONReseedResult;
import com.sklinfotech.btcgames.lib.JSONSlotsPullResult;
import com.sklinfotech.btcgames.lib.JSONSlotsRulesetResult;
import com.sklinfotech.btcgames.lib.JSONSlotsUpdateResult;
import com.sklinfotech.btcgames.settings.CurrencySettings;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;

public class SlotsRestClient extends RestClient {

  private static SlotsRestClient instance;

  public static SlotsRestClient getInstance(final Context ctx) {
    if (instance == null) {
      synchronized (SlotsRestClient.class) {
        if (instance == null) {
          instance = new SlotsRestClient(ctx);
        }
      }
    }
    instance.ctx = ctx;
    return instance;
  }

  private SlotsRestClient(final Context ctx) {
    super(ctx);
  }

  public JSONSlotsRulesetResult slotsRuleset() throws IOException {
    String params = null;
    InputStreamReader is = getInputStreamReader("slots/ruleset", params, null);
    return new Gson().fromJson(is, JSONSlotsRulesetResult.class);
  }

  public JSONReseedResult slotsReseed() throws IOException {
    InputStreamReader is = getInputStreamReader("slots/reseed", null, CurrencySettings.getInstance(ctx).getAccountKey());
    return new Gson().fromJson(is, JSONReseedResult.class);
  }

  public JSONSlotsPullResult slotsPull(int lines, long creditBTCValue, String serverSeedHash, String clientSeed, boolean useFakeCredits) throws IOException {
    String params = encodeKeyValuePair("num_lines", lines);
    params += "&" + encodeKeyValuePair("credit_btc_value", creditBTCValue);
    params += "&" + encodeKeyValuePair("server_seed_hash", serverSeedHash);
    params += "&" + encodeKeyValuePair("client_seed", clientSeed);
    params += "&" + encodeKeyValuePair("use_fake_credits", useFakeCredits);

    InputStreamReader is = getInputStreamReader("slots/pull", params, CurrencySettings.getInstance(ctx).getAccountKey());
    return new Gson().fromJson(is, JSONSlotsPullResult.class);
  }

  public JSONSlotsUpdateResult slotsUpdate(int last, int chatlast, long creditBTCValue) throws IOException {
    String params = encodeKeyValuePair("last", last);
    params += "&" + encodeKeyValuePair("chatlast", chatlast);
    params += "&" + encodeKeyValuePair("credit_btc_value", creditBTCValue);

    InputStreamReader is = getInputStreamReader("slots/update", params, CurrencySettings.getInstance(ctx).getAccountKey());
    return new Gson().fromJson(is, JSONSlotsUpdateResult.class);
  }
}
