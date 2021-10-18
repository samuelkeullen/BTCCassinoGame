package com.sklinfotech.btcgames.settings;

import android.app.ProgressDialog;
import android.util.Log;

import com.sklinfotech.btcgames.R;
import com.sklinfotech.btcgames.app.GameActivity;
import com.sklinfotech.btcgames.lib.CommonActivity;
import com.sklinfotech.btcgames.lib.JSONBitcoinAddressResult;
import com.sklinfotech.btcgames.lib.NetAsyncTask;
import com.sklinfotech.btcgames.rest.AccountRestClient;

import java.io.IOException;
import java.util.function.Consumer;

class NetBitcoinAddressTask extends NetAsyncTask<Long, Void, JSONBitcoinAddressResult> {

  private static final String TAG = NetBitcoinAddressTask.class.getSimpleName();

  private ProgressDialog mAlert;
  private Consumer<JSONBitcoinAddressResult> onSuccessCallback;

  NetBitcoinAddressTask(final CommonActivity activity, final Consumer<JSONBitcoinAddressResult> onSuccessCallback) {
    super(activity);
    Log.v(TAG, "NetBitcoinAddressTask go!");
    mAlert = ProgressDialog.show(activity, "", activity.getString(R.string.deposit_message_retrieving_dep_address), true);
    this.onSuccessCallback = onSuccessCallback;
  }

  @Override
  public void onDone() {
    mAlert.cancel();
  }

  @Override
  public JSONBitcoinAddressResult go(Long... v) throws IOException {
    Log.v(TAG, "deposit check go!");
    return AccountRestClient.getInstance(mActivity).getBitcoinAddress();
  }

  @Override
  public void onSuccess(JSONBitcoinAddressResult result) {
    Log.v(TAG, "deposit check success!");
    onSuccessCallback.accept(result);
  }

  @Override
  public void onError(JSONBitcoinAddressResult r) {
    mShowDialogOnError = false;
    GameActivity.handleCriticalConnectionError(mActivity);
  }
}