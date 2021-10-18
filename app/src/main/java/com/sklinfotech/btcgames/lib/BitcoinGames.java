package com.sklinfotech.btcgames.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sklinfotech.btcgames.BuildConfig;
import com.sklinfotech.btcgames.settings.CurrencySettings;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

public class BitcoinGames {

    // Change RUN_ENVIRONMENT to production before releasing
    public class RunEnvironment {
        public final static int PRODUCTION = 0;
        public final static int LOCAL = 1;
        public final static int EMULATOR = 2;
    }

    public static final int RUN_ENVIRONMENT;

    static {
        if (BuildConfig.DEBUG) {
            RUN_ENVIRONMENT = RunEnvironment.LOCAL;
        } else {
            RUN_ENVIRONMENT = RunEnvironment.PRODUCTION;
        }
    }

    public long mIntBalance;
    public long mFakeIntBalance;
    public boolean mUnconfirmed;

    public String mLastWithdrawAddress;

    private static BitcoinGames mInstance = null;

    // Prevent public access
    private BitcoinGames() {
        // TB TODO - Generate a new account, and then store it on the phone.
        // Let the user specify his account key as well, if he's transferring
        // his account.
        mIntBalance = -1;
        mFakeIntBalance = -1;

        if (BitcoinGames.RUN_ENVIRONMENT == BitcoinGames.RunEnvironment.EMULATOR
            || BitcoinGames.RUN_ENVIRONMENT == BitcoinGames.RunEnvironment.LOCAL) {
            trustEveryone();
        }
    }

    public static BitcoinGames getInstance(final Context ctx) {

        // TB TODO - Could do settings stuff here???
        // accountkey, etc, etc
        // SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        // String syncConnPref = sharedPref.getString(SettingsActivity.KEY_PREF_SYNC_CONN, "");

        if (mInstance == null) {
            mInstance = new BitcoinGames();
        }

        final String currencyPrefix = CurrencySettings.getInstance(ctx).getCurrencyLowerCase();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        mInstance.mLastWithdrawAddress = sharedPref.getString(currencyPrefix + "_last_withdraw_address", null);

        // TB - Don't remember the deposit address, so we don't run into the same problem of people
        // being stuck with an old address that is no longer valid.
        // mInstance.mDepositAddress = sharedPref.getString("deposit_address", null);

        return mInstance;
    }

    // TB - Radically insecure. Should never call this on production!
    private void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context
                .getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }
}
