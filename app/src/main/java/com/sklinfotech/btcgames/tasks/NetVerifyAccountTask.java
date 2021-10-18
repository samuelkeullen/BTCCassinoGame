package com.sklinfotech.btcgames.tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import com.sklinfotech.btcgames.lib.CreateAccountTask;
import com.sklinfotech.btcgames.lib.JSONBalanceResult;
import com.sklinfotech.btcgames.lib.NetBalanceTask;
import com.sklinfotech.btcgames.rest.AccountRestClient;

import java.io.IOException;
import java.util.function.Consumer;

public class NetVerifyAccountTask extends NetBalanceTask {

  private String mAccountKey;
  private Consumer<JSONBalanceResult> onSuccessCallback;
  private Runnable onDoneCallback;

  public NetVerifyAccountTask(final Activity a, final String accountKey) {
    this(a, accountKey, ignored -> {}, () -> {});
  }

  public NetVerifyAccountTask(
      final Activity a, final String accountKey, final Runnable onDoneCallback) {
    this(a, accountKey, ignored -> {}, onDoneCallback);
  }

  public NetVerifyAccountTask(
      final Activity a, final String accountKey, final Consumer<JSONBalanceResult> onSuccessCallback) {
    this(a, accountKey, onSuccessCallback, () -> {});
  }

  public NetVerifyAccountTask(
      final Activity a,
      final String accountKey,
      final Consumer<JSONBalanceResult> onSuccessCallback,
      final Runnable onDoneCallback) {
    super(a);
    this.mAccountKey = accountKey;
    this.onSuccessCallback = onSuccessCallback;
    this.onDoneCallback = onDoneCallback;
    this.mShowDialogOnError = false;
  }

  @Override
  public JSONBalanceResult go(Long... v) throws IOException {
    return AccountRestClient.getInstance(mActivity).isAccountKeyValid(mAccountKey);
  }

  @Override
  public void onError(JSONBalanceResult result) {
    super.onError(result);

    new AlertDialog.Builder(mActivity)
      .setMessage("This account key is not valid. Please check the value and try again.")
      .setCancelable(false)
      .setPositiveButton("OK", (dialog, id) -> dialog.cancel())
      .create()
      .show();
  }

  @Override
  public void onSuccess(JSONBalanceResult result) {
    super.onSuccess(result);

    CreateAccountTask.setAccountKeyInPreferences(mActivity, mAccountKey);
    Toast.makeText(mActivity, "Account key has been updated.", Toast.LENGTH_SHORT).show();
    onSuccessCallback.accept(result);
  }

  public void onDone() {
    super.onDone();
    this.onDoneCallback.run();
  }
}