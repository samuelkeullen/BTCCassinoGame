package com.sklinfotech.btcgames.settings;

import android.content.Context;

import com.sklinfotech.btcgames.lib.Bitcoin;

public class DiceSettings {

    private static DiceSettings instance;
    private final Context ctx;

    public static DiceSettings getInstance(final Context ctx) {
        if (instance == null) {
            synchronized (DiceSettings.class) {
                if (instance == null) {
                    instance = new DiceSettings(ctx);
                }
            }
        }
        return instance;
    }

    private DiceSettings(final Context ctx) {
        this.ctx = ctx;
    }

    public long getCreditValue() {
        return Bitcoin.stringAmountToLong(
            CurrencySettings.getInstance(ctx).getValueBasedOnCurrency("0.001", "0.0001"));
    }
}
