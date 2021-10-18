package com.sklinfotech.btcgames.rest;

import android.content.Context;
import android.util.Log;

import com.sklinfotech.btcgames.lib.JSONDiceRulesetResult;
import com.sklinfotech.btcgames.lib.JSONDiceThrowResult;
import com.sklinfotech.btcgames.lib.JSONDiceUpdateResult;
import com.sklinfotech.btcgames.lib.JSONReseedResult;
import com.sklinfotech.btcgames.settings.CurrencySettings;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;

public class DiceRestClient extends RestClient {

  private static DiceRestClient instance;

  public static DiceRestClient getInstance(final Context ctx) {
    if (instance == null) {
      synchronized (DiceRestClient.class) {
        if (instance == null) {
          instance = new DiceRestClient(ctx);
        }
      }
    }
    instance.ctx = ctx;
    return instance;
  }

  private DiceRestClient(final Context ctx) {
    super(ctx);
  }

  public JSONDiceRulesetResult diceRuleset() throws IOException {
    InputStreamReader is = getInputStreamReader("dice/ruleset", null, null);
    return new Gson().fromJson(is, JSONDiceRulesetResult.class);
  }

  public JSONReseedResult diceReseed() throws IOException {
    InputStreamReader is = getInputStreamReader("dice/reseed", null, CurrencySettings.getInstance(ctx).getAccountKey());
    return new Gson().fromJson(is, JSONReseedResult.class);
  }

  public JSONDiceThrowResult diceThrow(
      String serverSeedHash, String clientSeed, long bet, long payout, String target, boolean useFakeCredits) throws IOException {
    String params = encodeKeyValuePair("server_seed_hash", serverSeedHash);
    params += "&" + encodeKeyValuePair("client_seed", clientSeed);
    params += "&" + encodeKeyValuePair("bet", bet);
    params += "&" + encodeKeyValuePair("payout", payout);
    params += "&" + encodeKeyValuePair("target", target);
    params += "&" + encodeKeyValuePair("use_fake_credits", useFakeCredits);

    Log.v(tag(), params);

    InputStreamReader is = getInputStreamReader("dice/throw", params, CurrencySettings.getInstance(ctx).getAccountKey());
    return new Gson().fromJson(is, JSONDiceThrowResult.class);
  }

  public JSONDiceUpdateResult diceUpdate(int last, int chatlast, long creditValue) throws IOException {
    String params = encodeKeyValuePair("last", last);
    params += "&" + encodeKeyValuePair("chatlast", chatlast);
    params += "&" + encodeKeyValuePair("credit_btc_value", creditValue);

    InputStreamReader is = getInputStreamReader("dice/update", params, CurrencySettings.getInstance(ctx).getAccountKey());
    return new Gson().fromJson(is, JSONDiceUpdateResult.class);
  }
}
