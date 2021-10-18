package com.sklinfotech.btcgames.rest;

import android.content.Context;

import com.sklinfotech.btcgames.lib.JSONBlackjackCommandResult;
import com.sklinfotech.btcgames.lib.JSONBlackjackRulesetResult;
import com.sklinfotech.btcgames.lib.JSONReseedResult;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;

public class BlackjackRestClient extends RestClient {

  private static BlackjackRestClient instance;

  public static BlackjackRestClient getInstance(final Context ctx) {
    if (instance == null) {
      synchronized (BlackjackRestClient.class) {
        if (instance == null) {
          instance = new BlackjackRestClient(ctx);
        }
      }
    }
    instance.ctx = ctx;
    return instance;
  }

  private BlackjackRestClient(final Context ctx) {
    super(ctx);
  }

  public JSONReseedResult blackjackReseed() throws IOException {
    InputStreamReader is = getInputStreamReader("blackjack/reseed", null, getAccountKey());
    return new Gson().fromJson(is, JSONReseedResult.class);
  }

  public JSONBlackjackRulesetResult blackjackRuleset() throws IOException {
    InputStreamReader is = getInputStreamReader("blackjack/ruleset", null, null);
    return new Gson().fromJson(is, JSONBlackjackRulesetResult.class);
  }

  public JSONBlackjackCommandResult blackjackDeal(long bet, long progressiveBet, String serverSeedHash, String clientSeed, boolean useFakeCredits) throws IOException {
    String params = encodeKeyValuePair("bet", bet);
    params += "&" + encodeKeyValuePair("progressive_bet", progressiveBet);
    params += "&" + encodeKeyValuePair("server_seed_hash", serverSeedHash);
    params += "&" + encodeKeyValuePair("client_seed", clientSeed);
    params += "&" + encodeKeyValuePair("use_fake_credits", useFakeCredits);

    InputStreamReader is = getInputStreamReader("blackjack/deal", params, getAccountKey());
    return new Gson().fromJson(is, JSONBlackjackCommandResult.class);
  }

  public JSONBlackjackCommandResult blackjackCommand(String url, String gameID, int handIndex) throws IOException {
    String params = encodeKeyValuePair("game_id", gameID);
    params += "&" + encodeKeyValuePair("hand_index", handIndex);
    InputStreamReader is = getInputStreamReader(url, params, getAccountKey());
    return new Gson().fromJson(is, JSONBlackjackCommandResult.class);
  }

  public JSONBlackjackCommandResult blackjackStand(String gameID, int handIndex) throws IOException {
    return blackjackCommand("blackjack/stand", gameID, handIndex);
  }

  public JSONBlackjackCommandResult blackjackHit(final String accountKey, String gameID, int handIndex) throws IOException {
    return blackjackCommand("blackjack/stand", gameID, handIndex);
  }

  public JSONBlackjackCommandResult blackjackSplit(final String accountKey, String gameID, int handIndex) throws IOException {
    return blackjackCommand("blackjack/split", gameID, handIndex);
  }

  public JSONBlackjackCommandResult blackjackDouble(final String accountKey, String gameID, int handIndex) throws IOException {
    return blackjackCommand("blackjack/double", gameID, handIndex);
  }

  public JSONBlackjackCommandResult blackjackInsurance(final String accountKey, String gameID, int handIndex) throws IOException {
    return blackjackCommand("blackjack/insurance", gameID, handIndex);
  }
}
