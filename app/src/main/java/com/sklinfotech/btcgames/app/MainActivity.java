package com.sklinfotech.btcgames.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.sklinfotech.btcgames.BuildConfig;
import com.sklinfotech.btcgames.R;
import com.sklinfotech.btcgames.lib.Bitcoin;
import com.sklinfotech.btcgames.lib.BitcoinGames;
import com.sklinfotech.btcgames.lib.CommonActivity;
import com.sklinfotech.btcgames.lib.JSONAndroidAppVersionResult;
import com.sklinfotech.btcgames.lib.JSONBalanceResult;
import com.sklinfotech.btcgames.lib.NetAsyncTask;
import com.sklinfotech.btcgames.lib.NetBalanceTask;
import com.sklinfotech.btcgames.rest.SettingsRestClient;
import com.sklinfotech.btcgames.settings.CurrencySettings;

import java.io.IOException;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class MainActivity extends CommonActivity {

    boolean mFakeCredits = true;
    TextView mBalance;
    TextView mTestLocalWarning;
    ImageButton mVideoPoker;
    ImageButton mBlackjack;
    ImageButton mSlots;
    Button mDeposit;
    Button mSettings;
    TextView mLogoText;
    Button mCashOut;
    Button mShare;
    MainNetBalanceTask mNetBalanceTask;
    NetAndroidAppVersionTask mAndroidAppVersionTask;
    Handler mHandler;
    final static String TAG = "MainActivity";
    long mLastNetBalanceCheck;
    boolean mBlinkOn;
    final static int BLINK_DELAY = 500;
    final static String SETTING_ANDROID_APP_VERSION_CHECK = "android_app_version_check";
    Typeface mRobotoLight;
    Typeface mRobotoBold;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // TB TEMP TEST - For now always reset the preferences (true) so that we
        // can get first time players working.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // TB - These below lines reset everything to defaults... good for
        // testing first time players
        /*
         * PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit
         * (); PreferenceManager.setDefaultValues(this, R.xml.preferences,
         * true);
         */

        mBalance = findViewById(R.id.balance);
        mTestLocalWarning = findViewById(R.id.test_local_warning);
        mVideoPoker = findViewById(R.id.videopoker_button);
        mBlackjack = findViewById(R.id.blackjack_button);
        mSlots = findViewById(R.id.slots_button);
        mDeposit = findViewById(R.id.deposit_button);
        mCashOut = findViewById(R.id.cashout_button);
        mShare = findViewById(R.id.share_button);
        mSettings = findViewById(R.id.settings_button);
        mLogoText = findViewById(R.id.bitcoin_com_logo_text);
        mRobotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        mRobotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");

        mBalance.setTypeface(mRobotoBold);
        mDeposit.setTypeface(mRobotoLight);
        mCashOut.setTypeface(mRobotoLight);
        mSettings.setTypeface(mRobotoLight);
        mShare.setTypeface(mRobotoLight);

        mNetBalanceTask = null;
        mAndroidAppVersionTask = null;
        mHandler = new Handler();
        mLastNetBalanceCheck = 0;
        mBlinkOn = false;

        if (BitcoinGames.RUN_ENVIRONMENT == BitcoinGames.RunEnvironment.PRODUCTION) {
            mTestLocalWarning.setVisibility(View.GONE);
        }

        final Context self = this;
        ((RadioButton) findViewById(CurrencySettings.getInstance(self).getValueBasedOnCurrency(R.id.radioBch, R.id.radioBtc))).setChecked(true);
        ((RadioGroup) findViewById(R.id.radioCurrency)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mBalance.setText(getString(R.string.loading));
                CurrencySettings.getInstance(self).reload(((RadioButton) findViewById(checkedId)).getText().toString());
                if (CurrencySettings.getInstance(self).getAccountKey() == null) {
                    startActivity(new Intent(self, CreateAccountActivity.class));
                }
            }
        });
    }

    public void timeUpdate() {
        mBlinkOn = !mBlinkOn;

        BitcoinGames bvc = BitcoinGames.getInstance(this);
        long now = System.currentTimeMillis();
        if (now - mLastNetBalanceCheck > 5000) {
            mLastNetBalanceCheck = now;
            if (CurrencySettings.getInstance(this).getAccountKey() != null) {
                mNetBalanceTask = new MainNetBalanceTask(this);
                mNetBalanceTask.execute(0L);
            }
        }

        if (bvc.mIntBalance == 0 && mBlinkOn) {
            mDeposit.setTypeface(mRobotoBold);
        } else {
            mDeposit.setTypeface(mRobotoLight);
        }

        mFakeCredits = bvc.mIntBalance == 0;

        mHandler.postDelayed(this::timeUpdate, BLINK_DELAY);
    }

    public void updateValues(final CurrencySettings currencySettings) {
        BitcoinGames bvc = BitcoinGames.getInstance(this);

        if (bvc.mIntBalance != -1) {
            String balance = Bitcoin.longAmountToStringChopped(bvc.mIntBalance);
            mBalance.setText(getString(R.string.bitcoin_balance, balance, currencySettings.getCurrencyUpperCase()));
        } else {
            mBalance.setText(getString(R.string.main_connecting));
        }
        mLogoText.setText(currencySettings.getValueBasedOnCurrency(R.string.cashgames, R.string.games));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateValues(CurrencySettings.getInstance(this));

        // TB TODO - Is there some better kind of storage I should be using for something like this?
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        long lastCheck = sharedPref.getLong(SETTING_ANDROID_APP_VERSION_CHECK, 0);
        long now = System.currentTimeMillis() / 1000;
        // Check every week
        long APP_CHECK_DELAY = 60 * 60 * 24 * 7;
        if (now - lastCheck > APP_CHECK_DELAY) {
            mAndroidAppVersionTask = new NetAndroidAppVersionTask(this);
            mAndroidAppVersionTask.execute(Long.valueOf(0));
        }

        timeUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(this::timeUpdate);
    }

    public void onShare(View button) {
        // TB TODO - Show a dialog explaining that you will get a referral bonus.
        // Might be tricky to enforce the user zapping the QR code on the Android page. Otherwise no referral bonus... Hmmm
        // TB TODO - Add on referral code to the URL???

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Come play Bitcoin Games");
        intent.putExtra(Intent.EXTRA_TEXT, String.format("Check out the Bitcoin Games Android app. %s/android", CurrencySettings.getInstance(this).getServerAddress()));
        startActivity(Intent.createChooser(intent, "Share Bitcoin Games with friends"));
    }

    public void onVideoPoker(View button) {
        Intent intent = new Intent(this, VideoPokerActivity.class);
        intent.putExtra(GameActivity.KEY_USE_FAKE_CREDITS, mFakeCredits);
        startActivity(intent);
    }

    public void onSlots(View button) {
        Intent intent = new Intent(this, SlotsActivity.class);
        intent.putExtra(GameActivity.KEY_USE_FAKE_CREDITS, mFakeCredits);
        startActivity(intent);
    }

    public void onDice(View button) {
        Intent intent = new Intent(this, DiceActivity.class);
        intent.putExtra(GameActivity.KEY_USE_FAKE_CREDITS, mFakeCredits);
        startActivity(intent);
    }

    public void onBlackjack(View button) {
        Intent intent = new Intent(this, BlackjackActivity.class);
        intent.putExtra(GameActivity.KEY_USE_FAKE_CREDITS, mFakeCredits);
        startActivity(intent);
    }

    public void onCashOut(View button) {
        Intent intent = new Intent(this, CashOutActivity.class);
        startActivity(intent);
    }

    public void onDeposit(View button) {
        Intent intent = new Intent(this, DepositActivity.class);
        startActivity(intent);
    }

    public void onSettings(View button) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    class MainNetBalanceTask extends NetBalanceTask {

        MainNetBalanceTask(CommonActivity a) {
            super(a);
        }

        public void onSuccess(JSONBalanceResult result) {
            super.onSuccess(result);
            updateValues(CurrencySettings.getInstance(mActivity));
        }
    }

    class NetAndroidAppVersionTask extends NetAsyncTask<Long, Void, JSONAndroidAppVersionResult> {

        NetAndroidAppVersionTask(CommonActivity a) {
            super(a);
        }

        public JSONAndroidAppVersionResult go(Long... v) throws IOException {
            mShowDialogOnError = false;
            return SettingsRestClient.getInstance(mActivity).getAndroidAppVersion();
        }

        void showNewVersionDialog(String oldVersion, String newVersion) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            String message = String.format("A new version of Bitcoin Games is now available!\nYour version: %s\nNew version: %s\n\nDo you want to download it?", oldVersion, newVersion);
            builder.setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        final CurrencySettings currencySettings = CurrencySettings.getInstance(mActivity);
                        String url = String.format("%s/android?account_key=%s", currencySettings.getServerAddress(), currencySettings.getAccountKey());
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
        }

        public void onSuccess(JSONAndroidAppVersionResult result) {
            Log.v(TAG, "This is version: " + BuildConfig.VERSION_NAME);
            Log.v(TAG, "Most recent version is: " + result.version);

            if (!BuildConfig.VERSION_NAME.contentEquals(result.version)) {
                // Don't ask again for a while
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);
                long now = System.currentTimeMillis() / 1000;
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(SETTING_ANDROID_APP_VERSION_CHECK, now);
                editor.apply();

                showNewVersionDialog(BuildConfig.VERSION_NAME, result.version);
            }
        }
    }
}
