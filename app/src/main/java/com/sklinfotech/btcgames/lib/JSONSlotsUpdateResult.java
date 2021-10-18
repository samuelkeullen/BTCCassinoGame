package com.sklinfotech.btcgames.lib;


public class JSONSlotsUpdateResult extends JSONBaseResult {
  public int players_online;
  public int games_played;
  public long btc_winnings;
  public int progressive_jackpots_won;
  // TB TODO - chatlog
  // TB TODO - leaderboard

  // Slots only
  public long progressive_jackpot;
}
