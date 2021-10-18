package com.sklinfotech.btcgames.lib;

class JSONNotifyTransaction {
  public double amount;
  public String txid;
  public boolean credited;
}

public class JSONBalanceResult extends JSONBaseResult {
  public int shutdown_time;
  public JSONNotifyTransaction notify_transaction;
  public long intbalance;
  public long fake_intbalance;
  public String sender_address;
  public boolean unconfirmed;
}
