package com.sklinfotech.btcgames.tasks;

import android.app.Activity;

import com.sklinfotech.btcgames.lib.JSONRateResponse;
import com.sklinfotech.btcgames.lib.NetAsyncTask;
import com.sklinfotech.btcgames.rest.RateRestClient;

import java.io.IOException;
import java.util.function.Consumer;

public class RateTask extends NetAsyncTask<Long, Void, JSONRateResponse> {

  private final Consumer<Double> successCallback;

  public RateTask(final Activity a, final Consumer<Double> successCallback) {
    super(a);
    this.successCallback = successCallback == null ? ignored -> {} : successCallback;
  }

  @Override
  public JSONRateResponse go(Long... v) throws IOException {
    return RateRestClient.getInstance(mContext).getRate();
  }

  @Override
  public void onSuccess(final JSONRateResponse response) {
    if (response.err == null) {
      successCallback.accept(response.rate);
    }
  }
}
