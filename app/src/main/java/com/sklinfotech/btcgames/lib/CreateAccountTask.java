package com.sklinfotech.btcgames.lib;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.sklinfotech.btcgames.rest.AccountRestClient;
import com.sklinfotech.btcgames.settings.CurrencySettings;

import java.io.IOException;
import java.util.function.Consumer;

public class CreateAccountTask extends NetAsyncTask<Long, Void, JSONCreateAccountResult> {

    private ProgressDialog mAlert;
    final static String TAG = "CreateAccountTask";
    private Consumer<String> onSuccessCallback;

    public CreateAccountTask(final Activity a) {
        this(a, ignored -> {
        });
    }

    public CreateAccountTask(final Activity a, final Consumer<String> onSuccessCallback) {
        super(a);
        this.onSuccessCallback = onSuccessCallback;
        this.mAlert = ProgressDialog.show(mActivity, "", "Creating anonymous account...", true);
    }

    public JSONCreateAccountResult go(Long... v) throws IOException {
        return AccountRestClient.getInstance(mActivity).createAccount();
    }

    public void onDone() {
        mAlert.cancel();
    }

    public void onSuccess(JSONCreateAccountResult result) {
        // TB TODO - This will crash if result.account_key is null for some reason.
        Log.v(TAG, "New account created!");
        Log.v(TAG, result.account_key);

        Toast.makeText(mActivity, "New account created!", Toast.LENGTH_SHORT).show();
        setAccountKeyInPreferences(mActivity, result.account_key);
        this.onSuccessCallback.accept(result.account_key);
    }

    public static void setAccountKeyInPreferences(Activity a, String key) {
        CurrencySettings.getInstance(a).setAccountKey(key);

        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(a).edit();
        editor.putString("deposit_address", null);
        editor.putString("last_withdraw_address", null);
        editor.apply();

        BitcoinGames bvc = BitcoinGames.getInstance(a);
        bvc.mIntBalance = -1;
        bvc.mFakeIntBalance = -1;
    }
}
