package com.sklinfotech.btcgames.app;

import androidx.lifecycle.Observer;
import androidx.annotation.Nullable;

import com.sklinfotech.util.Currency;

import java.util.function.Consumer;

class CurrencyChangeListener implements Observer<Currency> {

  private Consumer<Currency> callback;

  CurrencyChangeListener(final Consumer<Currency> callback) {
    this.callback = callback;
  }

  @Override
  public void onChanged(@Nullable Currency currency) {
    callback.accept(currency);
  }
}