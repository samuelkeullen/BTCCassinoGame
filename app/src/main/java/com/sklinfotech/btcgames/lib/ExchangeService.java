package com.sklinfotech.btcgames.lib;

import android.app.Activity;

import com.sklinfotech.btcgames.tasks.RateTask;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class ExchangeService {

  private static ExchangeService instance;

  private Activity activity;

  public static ExchangeService getInstance(final Activity activity) {
    if (instance == null) {
      synchronized (ExchangeService.class) {
        if (instance == null) {
          instance = new ExchangeService();
        }
      }
    }
    instance.activity = activity;
    return instance;
  }

  private ExchangeService() {
  }

  void toFiat(final double cryptoValue, final Consumer<BigDecimal> successCallback) {
    new RateTask(activity, rate -> successCallback.accept(convertCryptoToFiat(cryptoValue, rate))).execute();
  }

  private static BigDecimal convertCryptoToFiat(final double cryptoValue, final double rate) {
    return BigDecimal.valueOf(cryptoValue).multiply(BigDecimal.valueOf(rate)).setScale(2, BigDecimal.ROUND_HALF_UP);
  }
}
