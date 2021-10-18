package com.sklinfotech.btcgames.lib;

import android.util.Log;

public class PokerFactory {
  final static String TAG = "Pokerfactory";

  static public Poker getPoker(int paytable) {
    Poker poker = null;
    switch (paytable) {
      case 0:
        poker = new PokerJacksOrBetter();
        break;
      case 1:
        poker = new PokerTensOrBetter();
        break;
      case 2:
        poker = new PokerBonus();
        break;
      case 3:
        poker = new PokerDoubleBonus();
        break;
      case 4:
        poker = new PokerDoubleDoubleBonus();
        break;
      case 5:
        poker = new PokerDeucesWild();
        break;
      case 6:
        poker = new PokerBonusDeluxe();
        break;
      default:
        Log.v(TAG, "Bad game selected");
        break;

    }
    return poker;
  }
}
