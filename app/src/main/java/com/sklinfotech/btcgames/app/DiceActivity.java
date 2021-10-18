package com.sklinfotech.btcgames.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.sklinfotech.btcgames.R;
import com.sklinfotech.btcgames.lib.BitcoinGames;
import com.sklinfotech.btcgames.lib.CommonActivity;
import com.sklinfotech.btcgames.lib.Dice;
import com.sklinfotech.btcgames.lib.JSONDiceRulesetResult;
import com.sklinfotech.btcgames.lib.JSONDiceThrowResult;
import com.sklinfotech.btcgames.lib.JSONDiceUpdateResult;
import com.sklinfotech.btcgames.lib.JSONReseedResult;
import com.sklinfotech.btcgames.lib.NetAsyncTask;
import com.sklinfotech.btcgames.rest.DiceRestClient;
import com.sklinfotech.btcgames.settings.DiceSettings;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DiceActivity extends GameActivity {

    class DiceGameState extends GameState {
        final static int WAIT_USER_THROW = 0;
    }

    class AutoStrategy {
        final static int REPEAT_BET = 0;
        final static int MARTINGALE = 1;
    }

    class AutoTarget {
        final static int HIGH = 0;
        final static int LOW = 1;
    }

    class ThrowHint {
        final static int NONE = 0;
        final static int HIGH = 1;
        final static int LOW = 2;
    }

    class LastGameResult {
        final static int NOTHING = 0;
        final static int WIN = 1;
        final static int LOSE = 2;
    }

    final static int MIN_BET = 1;
    final static int MAX_BET = 1000;

    class DirtyControls {
        boolean mPayoutValueText;
        boolean mPayoutSeekBar;
        boolean mChanceValueText;
        boolean mChanceSeekBar;
        boolean mAmountValueText;
        boolean mAmountSeekBar;
        boolean mProfitValueText;
    }

    DirtyControls mDirtyControls;

    Dice mDice;
    int mAutoStrategy;
    int mAutoSpeed;
    int mAutoTarget;

    private TextView mJackpot5Text;
    private TextView mJackpot6Text;

    private LinearLayout mLuckyNumberActual;
    private TextView mLuckyNumberDirection;
    private TextView mLuckyNumberGoal;

    private TextView[] mLuckyNumberActuals;

    private SeekBar mPayoutSeekbar;
    private TextView mPayoutValueText;
    private SeekBar mChanceSeekbar;
    private TextView mChanceValueText;
    private SeekBar mAmountSeekbar;
    private TextView mAmountValueText;
    private TextView mProfitValueText;
    private int mAmountValue;
    private double mPayoutValue;
    private double mChanceValue;
    private double mProfitValue;
    private String mTargetValue;
    private View mErrorContainer;
    private View mResultContainer;
    private TextView mErrorText;
    private Button mAutoButton;

    private Button mRollHighButton;
    private Button mRollLowButton;

    private NetRulesetTask mNetRulesetTask;
    private NetReseedTask mNetReseedTask;
    private NetUpdateTask mNetUpdateTask;
    private NetThrowTask mNetThrowTask;
    public ShowLuckyNumberRunnable mShowLuckyNumberRunnable;

    final private String TAG = "DiceActivity";

    private int mSoundCoinPay;
    private int mSoundBoop;
    private int mSoundWin;
    private int mSoundWinJackpot;
    JSONDiceThrowResult mThrowResult;
    private JSONDiceRulesetResult mRuleset;

    // These get calculated whenn the ruleset is returned.
    private double mRulesetMaximumChance;
    private double mRulesetMinimumChance;

    private int mThrowHint;
    private int mLuckyNumberDirectionBackgroundResource;

    // This is needed to prevent a stack overflow from the controls getting updated by other controls,
    // and then updating other controls indefinitely.
    private boolean mIsUserInputError;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_dice);

        configureFlashingDepositButton(TAG);

        BitcoinGames bvc = BitcoinGames.getInstance(this);
        mTimeUpdateDelay = 50;
        mCreditValue = DiceSettings.getInstance(this).getCreditValue();

        mGameState = DiceGameState.WAIT_USER_THROW;
        mDice = new Dice();
        mAutoStrategy = AutoStrategy.REPEAT_BET;
        mAutoSpeed = AutoSpeed.MEDIUM;
        mAutoTarget = AutoTarget.HIGH;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mShowLuckyNumberRunnable = null;
        mThrowHint = ThrowHint.HIGH;
        mIsUserInputError = false;
        mDirtyControls = new DirtyControls();

        mJackpot5Text = findViewById(R.id.jackpot5);
        mJackpot6Text = findViewById(R.id.jackpot6);

        mRollHighButton = findViewById(R.id.roll_high);
        mRollLowButton = findViewById(R.id.roll_low);

        mAutoButton = findViewById(R.id.auto_button);

        mLuckyNumberActual = findViewById(R.id.lucky_number_actual);
        mLuckyNumberDirection = findViewById(R.id.lucky_number_direction);
        mLuckyNumberGoal = findViewById(R.id.lucky_number_goal);

        mLuckyNumberActuals = new TextView[7];
        mLuckyNumberActuals[0] = findViewById(R.id.lucky_number_actual0);
        mLuckyNumberActuals[1] = findViewById(R.id.lucky_number_actual1);
        mLuckyNumberActuals[2] = findViewById(R.id.lucky_number_actual2);
        mLuckyNumberActuals[3] = findViewById(R.id.lucky_number_actual3);
        mLuckyNumberActuals[4] = findViewById(R.id.lucky_number_actual4);
        mLuckyNumberActuals[5] = findViewById(R.id.lucky_number_actual5);
        mLuckyNumberActuals[6] = findViewById(R.id.lucky_number_actual6);

        mPayoutSeekbar = findViewById(R.id.payout_seekbar);
        mPayoutValueText = findViewById(R.id.payout_valuetext);
        mChanceSeekbar = findViewById(R.id.chance_seekbar);
        mChanceValueText = findViewById(R.id.chance_valuetext);
        mAmountSeekbar = findViewById(R.id.amount_seekbar);
        mAmountValueText = findViewById(R.id.amount_valuetext);
        mProfitValueText = findViewById(R.id.profit_valuetext);

        mErrorContainer = findViewById(R.id.error_container);
        mResultContainer = findViewById(R.id.result_container);
        mErrorText = findViewById(R.id.error_text);

        mAmountValue = 1;
        mPayoutValue = 2;
        mChanceValue = 49.5;

        mLuckyNumberDirectionBackgroundResource = R.drawable.result_box;

        mSoundCoinPay = mSoundPool.load(this, R.raw.coinpay, 1);
        mSoundBoop = mSoundPool.load(this, R.raw.boop, 1);
        mSoundWin = mSoundPool.load(this, R.raw.win1, 1);
        mSoundWinJackpot = mSoundPool.load(this, R.raw.slot_machine_win_19, 1);

        mPayoutSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mRuleset == null) {
                    return;
                }

                // Changing one seek bar will change the other, so we don't want to keep calling onProgressChanged for each change.
                if (!fromUser) {
                    return;
                }

                double quadraticProgress = getQuadraticEasedProgress(progress);
                mPayoutValue = mRuleset.result.minimum_payout + ((mRuleset.result.maximum_payout - mRuleset.result.minimum_payout) * quadraticProgress);
                mPayoutValue /= 100000000;

                mDirtyControls.mPayoutValueText = true;
                handlePayoutChange();
                hideVirtualKeyboard(seekBar);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mChanceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mRuleset == null) {
                    return;
                }
                // Changing one seek bar will change the other, so we don't want to keep calling onProgressChanged for each change.
                if (!fromUser) {
                    return;
                }

                // TB TODO - get from ruleset!
                double minChance = 0.99;
                double maxChance = 97.0;

                double quadraticProgress = getQuadraticEasedProgress(progress);
                mChanceValue = minChance + ((maxChance - minChance) * quadraticProgress);

                mDirtyControls.mChanceValueText = true;
                handleChanceChange();
                hideVirtualKeyboard(seekBar);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mAmountSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= 0) {
                    progress = 1;
                }

                // Changing one seek bar will change the other, so we don't want to keep calling onProgressChanged for each change.
                if (!fromUser) {
                    return;
                }

                double quadraticProgress = getSlowQuadraticEasedInProgress(progress);
                mAmountValue = (int) (MIN_BET + ((MAX_BET - MIN_BET) * quadraticProgress));

                mDirtyControls.mAmountValueText = true;
                handleAmountChange();
                hideVirtualKeyboard(seekBar);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mRollHighButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mThrowHint = ThrowHint.HIGH;
                updateControls();
                return false;
            }
        });
        mRollLowButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mThrowHint = ThrowHint.LOW;
                updateControls();
                return false;
            }
        });

        updateCredits(mUseFakeCredits ? bvc.mFakeIntBalance : bvc.mIntBalance);
    }

    void hideVirtualKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    String prettyDouble4(final double d) {
        return String.format(Locale.getDefault(), "%.4f", d);
    }

    double getQuadraticEasedProgress(int progress) {

        // Quadratic easing in/out, so that the edges of the seekbar move the value slower.
        // This assumes that the slider goes from 0 to 1000
        double t = progress / 1000.0;
        if (t < 0.5) {
            return 2 * t * t;
        } else {
            t = 1 - t;
            return 1.0 - (2 * t * t);
        }

    }

    // We need to get the correct progress value to use when repositioning the seekbars.
    // Since the corresponding values ease in/out quadratically, we need to map back to that.
    // Pos is from 0-1
    double getLinearProgressFromQuadratic(double pos) {
        if (pos < 0.5) {
            return Math.sqrt(pos / 2.0);
        } else {
            return 1 - Math.sqrt(-(pos - 1) / 2);
        }
    }

    double getSlowQuadraticEasedInProgress(int progress) {
        double t = progress / 1000.0;
        return t * t;
    }

    double getLinearProgressFromSlowQuadraticEasedIn(double pos) {
        return Math.sqrt(pos);
    }

    void setPayoutValueText() {
        mPayoutValueText.setText(prettyDouble4(mPayoutValue));
    }

    void setPayoutSeekbar() {
        // This division sucks
        double minPayout = mRuleset.result.minimum_payout / 100000000;
        double maxPayout = mRuleset.result.maximum_payout / 100000000;
        double linearPos = (mPayoutValue - minPayout) / (maxPayout - minPayout);
        if (linearPos > 1) {
            linearPos = 1;
        } else if (linearPos < 0) {
            linearPos = 0;
        }
        double quadPos = getLinearProgressFromQuadratic(linearPos);
        mPayoutSeekbar.setProgress((int) (quadPos * 1000));
    }

    void setChanceValueText() {
        mChanceValueText.setText(prettyDouble4(mChanceValue));
    }

    void setChanceSeekbar() {
        // TB TODO - get from ruleset!
        double minChance = 0.99;
        double maxChance = 97.0;
        double linearPos = (mChanceValue - minChance) / (maxChance - minChance);
        if (linearPos > 1) {
            linearPos = 1;
        } else if (linearPos < 0) {
            linearPos = 0;
        }
        double quadPos = getLinearProgressFromQuadratic(linearPos);
        mChanceSeekbar.setProgress((int) (quadPos * 1000));
    }

    void setAmountSeekbar() {
        double linearPos = (double) (mAmountValue - 1) / (double) (MAX_BET - MIN_BET);
        if (linearPos > 1) {
            linearPos = 1;
        } else if (linearPos < 0) {
            linearPos = 0;
        }
        double quadPos = getLinearProgressFromSlowQuadraticEasedIn(linearPos);
        mAmountSeekbar.setProgress((int) (quadPos * 1000));
    }

    void recalculateProfit() {
        mProfitValue = this.mPayoutValue * mAmountValue - mAmountValue;
        mDirtyControls.mProfitValueText = true;
    }

    void handlePayoutChange() {
        mChanceValue = (mRuleset.result.player_return / 1000000) / mPayoutValue;

        mDirtyControls.mChanceValueText = true;
        mDirtyControls.mChanceSeekBar = true;
        recalculateProfit();
        updateControls();
    }

    void handleChanceChange() {
        mPayoutValue = (mRuleset.result.player_return / 1000000) / mChanceValue;
        mDirtyControls.mPayoutValueText = true;
        mDirtyControls.mPayoutSeekBar = true;

        recalculateProfit();
        updateControls();
    }

    void handleAmountChange() {
        recalculateProfit();
        updateControls();
    }

    void handleProfitChange() {
        // TB TODO - Amount gets clobbered to int, which then makes the profit not guaranteed...
        mAmountValue = (int) (mProfitValue / (mPayoutValue - 1.0));

        mDirtyControls.mAmountValueText = true;
        mDirtyControls.mAmountSeekBar = true;
        updateControls();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void timeUpdate() {
        super.timeUpdate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mServerSeedHash == null) {
            // If the seed happens to have expired when he returns, that's OK because we'll get a new seed
            // when need_seed is returned when dealing the game.
            mNetReseedTask = new NetReseedTask(this, false);
            mNetReseedTask.executeParallel(Long.valueOf(0));
        }

        mNetUpdateTask = new NetUpdateTask(this);
        mNetUpdateTask.executeParallel(Long.valueOf(0));

        // TB - Kind of silly to be getting this multiple times???
        mNetRulesetTask = new NetRulesetTask(this);
        mNetRulesetTask.execute(Long.valueOf(0));

        timeUpdate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mIsGameBusy || mIsWaitingForServer) {
                showEarlyExitDialog();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTimeUpdateRunnable);
        mHandler.removeCallbacks(mCountUpRunnable);
        mHandler.removeCallbacks(mShowLuckyNumberRunnable);
        setAuto(false);
    }

    void handleNotEnoughCredits() {
        super.showDepositDialog(R.color.bitcoin_games_dice);
        setAuto(false);
    }

    public void onDeposit(View button) {
        showDepositDialog(R.color.bitcoin_games_dice, true);
    }

    public void onThrow(String target) {
        if (mGameState == DiceGameState.WAIT_USER_THROW) {
            if (!canThrow()) {
                return;
            }
            BitcoinGames bvc = BitcoinGames.getInstance(this);

            if ((mUseFakeCredits ? bvc.mFakeIntBalance : bvc.mIntBalance) - (mAmountValue * mCreditValue) < 0) {
                handleNotEnoughCredits();
                return;
            }

            if (mServerSeedHash == null) {
                // TB TODO - Get another hash? Or maybe we're waiting for it still?
                return;
            }

            mTargetValue = target;
            mNetThrowTask = new NetThrowTask(this);
            mNetThrowTask.executeParallel();
        }
    }

    public void onRollHigh(View button) {
        // Set the throw hint again just in case the button touch handler didn't trigger for some reason
        mThrowHint = ThrowHint.HIGH;
        onThrow("high");
    }

    public void onRollLow(View button) {
        // Set the throw hint again just in case the button touch handler didn't trigger for some reason
        mThrowHint = ThrowHint.LOW;
        onThrow("low");
    }

    private boolean canThrow() {
        if (mIsWaitingForServer || mIsGameBusy) {
            return false;
        }

        if (mIsUserInputError) {
            return false;
        }

        return (mGameState == DiceGameState.WAIT_USER_THROW);
    }

    private boolean canAuto() {
        return canThrow();
    }

    void setLuckyNumberDirectionBackgroundResource(int resource) {
        // This is insane.
        // When you call setBackgroundResource, the padding info is lost. So you need to rebuild it.
        if (resource != mLuckyNumberDirectionBackgroundResource) {
            int bottom = mLuckyNumberDirection.getPaddingBottom();
            int top = mLuckyNumberDirection.getPaddingTop();
            int right = mLuckyNumberDirection.getPaddingRight();
            int left = mLuckyNumberDirection.getPaddingLeft();
            mLuckyNumberDirection.setBackgroundResource(resource);
            mLuckyNumberDirection.setPadding(left, top, right, bottom);
            mLuckyNumberDirectionBackgroundResource = resource;
        }
    }

    public void updateControls() {
        if (mRuleset == null) {
            return;
        }

        if (canThrow()) {
            mRollHighButton.setBackgroundResource(R.drawable.button_purple);
            mRollLowButton.setBackgroundResource(R.drawable.button_purple);
            mRollHighButton.setTextColor(Color.WHITE);
            mRollLowButton.setTextColor(Color.WHITE);
        } else {
            mRollHighButton.setBackgroundResource(R.drawable.button_dark);
            mRollLowButton.setBackgroundResource(R.drawable.button_dark);
            mRollHighButton.setTextColor(Color.GRAY);
            mRollLowButton.setTextColor(Color.GRAY);
        }

        if (mIsAutoOn) {
            mAutoButton.setBackgroundResource(R.drawable.button_red);
        } else if (canAuto()) {
            mAutoButton.setBackgroundResource(R.drawable.button_blue_green);
        } else {
            mAutoButton.setBackgroundResource(R.drawable.button_dark);
        }
        mTextBet.setText(getString(R.string.bet_amount, mAmountValue));


        if (mDirtyControls.mPayoutValueText) {
            setPayoutValueText();
            mDirtyControls.mPayoutValueText = false;
        }
        if (mDirtyControls.mPayoutSeekBar) {
            setPayoutSeekbar();
            mDirtyControls.mPayoutSeekBar = false;
        }
        if (mDirtyControls.mChanceValueText) {
            setChanceValueText();
            mDirtyControls.mChanceValueText = false;
        }
        if (mDirtyControls.mChanceSeekBar) {
            setChanceSeekbar();
            mDirtyControls.mChanceSeekBar = false;
        }
        if (mDirtyControls.mAmountValueText) {
            mAmountValueText.setText(String.valueOf(mAmountValue));
            mDirtyControls.mAmountValueText = false;
        }
        if (mDirtyControls.mAmountSeekBar) {
            setAmountSeekbar();
            mDirtyControls.mAmountSeekBar = false;
        }
        if (mDirtyControls.mProfitValueText) {
            mProfitValueText.setText(prettyDouble4(mProfitValue));
            mDirtyControls.mProfitValueText = false;
        }

        long intBetChance = (long) (mChanceValue * 10000);
        long intPayout = (long) (mPayoutValue * 100000000);

        int luckyNumberDirectionBackgroundResource = R.drawable.result_box;
        if (mThrowHint == ThrowHint.NONE) {
            // What goes here?
            if (mThrowResult != null) {
                // Go back to showing the result of the throw!
                if (mThrowResult.target.equalsIgnoreCase("high")) {
                    mLuckyNumberDirection.setText(">");
                } else {
                    mLuckyNumberDirection.setText("<");
                }

                drawLuckyNumberGoal(mThrowResult.target.equalsIgnoreCase("high") ? 999999 - mThrowResult.chance : mThrowResult.chance);

                if (mThrowResult.intwinnings > 0) {
                    luckyNumberDirectionBackgroundResource = R.drawable.result_box_win;
                }
                drawLuckyNumberActual(mThrowResult.lucky_number, 7);

            } else {
                // TB TODO - Set the correct digits
                drawLuckyNumberActual(0, 0);
                mLuckyNumberDirection.setText(">");
                drawLuckyNumberGoal(495000);
            }
        } else if (mThrowHint == ThrowHint.HIGH) {
            mLuckyNumberDirection.setText(">");
            drawLuckyNumberGoal(mDice.getWinCutoff(true, intBetChance));
            drawLuckyNumberActual(0, 0);
        } else if (mThrowHint == ThrowHint.LOW) {
            mLuckyNumberDirection.setText("<");
            drawLuckyNumberGoal(mDice.getWinCutoff(false, intBetChance));
            drawLuckyNumberActual(0, 0);
        }

        setLuckyNumberDirectionBackgroundResource(luckyNumberDirectionBackgroundResource);

        mChanceValueText.setTextColor(ContextCompat.getColor(this, R.color.blue_green));
        mPayoutValueText.setTextColor(ContextCompat.getColor(this, R.color.blue_green));
        mAmountValueText.setTextColor(ContextCompat.getColor(this, R.color.blue_green));
        mProfitValueText.setTextColor(ContextCompat.getColor(this, R.color.blue_green));

        mIsUserInputError = false;
        String errorString = "";
        if (intPayout < mRuleset.result.minimum_payout) {
            mIsUserInputError = true;
            errorString = "Chance can not be greater than " + prettyDouble4(mRulesetMaximumChance);
            mChanceValueText.setTextColor(Color.RED);
        } else if (intPayout > mRuleset.result.maximum_payout) {
            mIsUserInputError = true;
            errorString = "Chance can not be smaller than " + prettyDouble4(mRulesetMinimumChance);
            mChanceValueText.setTextColor(Color.RED);
        }

        if (mAmountValue < 1) {
            mIsUserInputError = true;
            errorString = "Bet amount must be greater than 0";
            mAmountValueText.setTextColor(Color.RED);
        }

        // No need to check for a whole number bet amount like the JS code since mAmountValue is a whole number.
        if (mProfitValue * mCreditValue > mRuleset.result.maximum_profit) {
            mIsUserInputError = true;
            errorString = "Profit can not be bigger than " + (mRuleset.result.maximum_profit / mCreditValue) + " credits";
            mAmountValueText.setTextColor(Color.RED);
        }

        if (mIsUserInputError) {
            mErrorContainer.setVisibility(View.VISIBLE);
            mResultContainer.setVisibility(View.GONE);
            mErrorText.setText(errorString);
        } else {
            mErrorContainer.setVisibility(View.GONE);
            mResultContainer.setVisibility(View.VISIBLE);
        }

        String s = "> " + formatLuckyNumber(mDice.getWinCutoff(true, intBetChance));
        mRollHighButton.setText(getString(R.string.dice_roll_high_button, s));
        s = "< " + formatLuckyNumber(mDice.getWinCutoff(false, intBetChance));
        mRollLowButton.setText(getString(R.string.dice_roll_low_button, s));
    }

    private void doAuto(int lastGameResult) {
        if (!mIsAutoOn) {
            return;
        }

        // We update the value here instead of the game's onSuccess handler so that the bet amount first
        // changes right before the new bet is initiated.
        if (mAutoStrategy == AutoStrategy.MARTINGALE) {
            if (lastGameResult == LastGameResult.WIN) {
                mAmountValue = 1;
            } else if (lastGameResult == LastGameResult.LOSE) {
                mAmountValue *= 2;
                if (mAmountValue > MAX_BET) {
                    // TB TODO - Parameter to specify whether the amount should reset or stay at max...???
                    mAmountValue = MAX_BET;
                }

                // Check that profit is not over the max. If so, reduce the bet amount.
                recalculateProfit();
                if (mProfitValue * mCreditValue > mRuleset.result.maximum_profit) {
                    mProfitValue = mRuleset.result.maximum_profit / mCreditValue;
                    handleProfitChange();
                }
            }
            mDirtyControls.mAmountValueText = true;
            mDirtyControls.mAmountSeekBar = true;
            handleAmountChange();
        }

        if (mAutoTarget == AutoTarget.HIGH) {
            mThrowHint = ThrowHint.HIGH;
            onThrow("high");
        } else {
            mThrowHint = ThrowHint.LOW;
            onThrow("low");
        }
    }

    private void checkAuto(final int lastGameResult) {
        if (mIsGameBusy) {
            Log.e(TAG, "Error: checkAuto() called while game is busy.");
            return;
        }
        if (mIsWaitingForServer) {
            Log.e(TAG, "Error: checkAuto() called while waiting for server.");
            return;
        }
        if (!mIsAutoOn) {
            return;
        }

        int delay = getDelayFromAutoSpeed(mAutoSpeed);

        // When you initially start auto mode, it should immediately jump into action.
        // It shouldn't just initially sit there for 1-2 seconds, since that seems unresponsive.
        if (mIsFirstAutoAction) {
            delay = 0;
            mIsFirstAutoAction = false;
        }

        mHandler.postDelayed(() -> doAuto(lastGameResult), delay);
    }

    public void setAuto(boolean auto) {
        mIsAutoOn = auto;
        updateControls();
        if (auto) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mIsFirstAutoAction = true;
            checkAuto(LastGameResult.NOTHING);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void onAuto(View button) {


        // TB TODO DICE


        if (mIsAutoOn) {
            setAuto(false);
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.d_auto);
        dialog.setTitle("Autoplay Settings");

        final Spinner strategySpinner = dialog.findViewById(R.id.strategy_spinner);
        final Spinner speedSpinner = dialog.findViewById(R.id.speed_spinner);
        final Spinner targetSpinner = dialog.findViewById(R.id.target_spinner);

        strategySpinner.setSelection(mAutoStrategy);
        speedSpinner.setSelection(mAutoSpeed);
        targetSpinner.setSelection(mAutoTarget);

        Button playButton = dialog.findViewById(R.id.play_button);
        playButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mAutoStrategy = strategySpinner.getSelectedItemPosition();
                mAutoSpeed = speedSpinner.getSelectedItemPosition();
                mAutoTarget = targetSpinner.getSelectedItemPosition();

                if (mAutoStrategy == AutoStrategy.MARTINGALE) {
                    // TB TODO - Should this reset to 1? Or remember your starting bet?
                    mAmountValue = 1;

                    mDirtyControls.mAmountValueText = true;
                    mDirtyControls.mAmountSeekBar = true;
                    handleAmountChange();
                }
                setAuto(true);
                dialog.dismiss();
            }
        });

        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    String getProgressiveJackpotString(long progressiveJackpot) {
        // The jackpot returned is in 10000ths of a credit
        if (mRuleset == null) {
            return "XXX";
        }

        float val = (float) (progressiveJackpot / 10000.0);
        return String.format(Locale.getDefault(), "%.2f", val);
    }

    void updateProgressiveJackpot(final Map<String, Integer> prog) {
        int jp5 = Optional.ofNullable(prog.get("5")).orElse(0);
        int jp6 = Optional.ofNullable(prog.get("6")).orElse(0);
        if (jp5 > 0) {
            mJackpot5Text.setText(getProgressiveJackpotString(jp5));
        }
        if (jp6 > 0) {
            mJackpot6Text.setText(getProgressiveJackpotString(jp6));
        }
    }

    @Override
    boolean shouldConnectingDialogShow() {
        boolean showDialog = super.shouldConnectingDialogShow();
        if (showDialog) {
            return true;
        }

        return mRuleset == null;
    }

    void drawLuckyNumberActual(long luckyNumber, int numVisibleDigits) {

        String luckyString = "";
        if (luckyNumber < 100000) {
            luckyString += "0";
        }
        luckyString += Integer.toString((int) (luckyNumber / 10000));
        luckyString += ".";
        luckyString += Long.toString((luckyNumber % 10000));
        while (luckyString.length() < 7) {
            luckyString += "0";
        }

        for (int i = 0; i < numVisibleDigits; i++) {
            char ch = luckyString.charAt(i);
            if (ch == '7') {
                mLuckyNumberActuals[i].setTextColor(ContextCompat.getColor(this, R.color.blue_green));
            } else {
                mLuckyNumberActuals[i].setTextColor(Color.WHITE);
            }
            mLuckyNumberActuals[i].setText(String.valueOf(luckyString.charAt(i)));
        }

        for (int k = numVisibleDigits; k < 7; k++) {
            mLuckyNumberActuals[k].setTextColor(Color.GRAY);
            if (k != 2) {
                mLuckyNumberActuals[k].setText("X");
            }
        }
    }

    String formatLuckyNumber(long goal) {
        return String.format(Locale.getDefault(), "%07.4f", (goal / 10000.0));
    }

    void drawLuckyNumberGoal(long goal) {
        mLuckyNumberGoal.setText(formatLuckyNumber(goal));
    }

    class ShowLuckyNumberRunnable implements Runnable {
        // Position in the reels
        long mLuckyNumber;
        Runnable mFinishedCallback;
        int mDigitIndex;

        ShowLuckyNumberRunnable(long luckyNumber, Runnable finishedCallback) {
            mFinishedCallback = finishedCallback;
            mLuckyNumber = luckyNumber;
            mDigitIndex = 1;
        }

        public void run() {
            // Instantly show the digits if in auto mode
            if (mIsAutoOn) {
                mFinishedCallback.run();
                return;
            }

            drawLuckyNumberActual(mLuckyNumber, mDigitIndex);

            if (mDigitIndex == 7) {
                mFinishedCallback.run();
                return;
            }

            mDigitIndex += 1;
            final int delay = 50;
            mHandler.postDelayed(this, delay);
        }
    }

    class NetUpdateTask extends NetAsyncTask<Long, Void, JSONDiceUpdateResult> {

        NetUpdateTask(CommonActivity a) {
            super(a);
        }

        public JSONDiceUpdateResult go(Long... v) throws IOException {
            int last = 999999999;
            int chatlast = 999999999;

            return DiceRestClient.getInstance(mActivity).diceUpdate(last, chatlast, mCreditValue);
        }

        public void onSuccess(JSONDiceUpdateResult result) {
            updateProgressiveJackpot(result.progressive_jackpots);
        }
    }

    class NetReseedTask extends NetAsyncTask<Long, Void, JSONReseedResult> {
        boolean mAutodeal;

        NetReseedTask(Activity a, boolean autodeal) {
            super(a);
            // If true, the game will try to automatically deal after getting this seed.
            // This should happen if the server returns need_seed when the user hit's DRAW.
            // He shouldn't have to hit Draw again after getting the seed.
            mAutodeal = autodeal;
        }

        public JSONReseedResult go(Long... v) throws IOException {
            return DiceRestClient.getInstance(mActivity).diceReseed();
        }

        public void onSuccess(JSONReseedResult result) {
            mServerSeedHash = result.server_seed_hash;
            updateControls();
            checkConnectingAlert();
            if (mConnectingDialog == null && mAutodeal) {
                onThrow(mTargetValue);
            }
        }
    }

    class NetThrowTask extends NetAsyncTask<Long, Void, JSONDiceThrowResult> {

        boolean mIsFreeSpin;

        NetThrowTask(CommonActivity a) {
            super(a);
            // TB TEMP TEST - Keep this running so that if the task is interrupted (phone call, etc), that the result
            // will still be shown.
            mAllowAbort = false;

            updateCredits((mUseFakeCredits ? mBVC.mFakeIntBalance : mBVC.mIntBalance) - (mAmountValue * mCreditValue));

            // TB TEMP TEST - There's probably a better place to put this?
            stopCountUpWins();
            updateWin(0, false);

            mIsWaitingForServer = true;
            mIsGameBusy = true;
            playSound(mSoundCoinPay);

            // TB - Credits are now dirty (so don't update credits with whatever we get from a balance update, since it will be incorrect)
            mCreditsAreDirty = true;
            updateControls();
        }

        public JSONDiceThrowResult go(Long... v) throws IOException {
            String serverSeedHash = mServerSeedHash;

            long bet = mAmountValue * mCreditValue;
            long payout = (long) (mPayoutValue * 100000000);
            return DiceRestClient.getInstance(mActivity).diceThrow(serverSeedHash, getClientSeed(), bet, payout, mTargetValue, mUseFakeCredits);
        }

        @Override
        public void onSuccess(final JSONDiceThrowResult result) {
            if (result.error != null) {
                mIsGameBusy = false;
                setAuto(false);
                if (result.error.contains("need_seed")) {
                    mServerSeedHash = null;
                    showConnectingDialog();
                    mNetReseedTask = new NetReseedTask(mActivity, true);
                    mNetReseedTask.execute(Long.valueOf(0));
                } else {
                    Log.e(TAG, "Unknown error returned by server:" + result.error);
                    handleError(result, String.format("Error from server: %s", result.error));
                }
                updateControls();
                return;
            }
            mThrowResult = result;
            mServerSeedHash = result.server_seed_hash;

            mBVC.mIntBalance = result.intbalance;
            mBVC.mFakeIntBalance = result.fake_intbalance;

            updateProgressiveJackpot(result.progressive_jackpots);

            mShowLuckyNumberRunnable = new ShowLuckyNumberRunnable(result.lucky_number, new Runnable() {

                public void run() {
                    if (result.progressive_win > 0) {
                        playSound(mSoundWinJackpot);
                    } else if (result.intwinnings > 0) {
                        playSound(mSoundWin);
                    }

                    if (result.intwinnings > 0) {
                        long delta;
                        long quotient = result.intwinnings / mCreditValue;
                        if (quotient < 50) {
                            delta = mCreditValue;
                        } else if (quotient < 500) {
                            delta = mCreditValue * 5;
                        } else {
                            delta = quotient * 350;
                        }

                        if (mIsAutoOn) {
                            delta = result.intwinnings;
                        }

                        startCountUpWins(result.intwinnings, (mUseFakeCredits ? result.fake_intbalance : result.intbalance) - result.intwinnings, delta);
                    }

                    mIsGameBusy = false;

                    int lastGameResult = LastGameResult.LOSE;
                    if (result.intwinnings > 0) {
                        lastGameResult = LastGameResult.WIN;
                    }
                    updateControls();

                    checkAuto(lastGameResult);
                }
            });
            mShowLuckyNumberRunnable.run();
        }

        @Override
        public void onError(final JSONDiceThrowResult result) {
            mIsGameBusy = false;
            setAuto(false);
            updateControls();
        }

        @Override
        public void onDone() {
            mIsWaitingForServer = false;

            // TB - Credits are now clean. We can display the intbalance we get from the server again.
            mCreditsAreDirty = false;
            mThrowHint = ThrowHint.NONE;
        }
    }

    class NetRulesetTask extends NetAsyncTask<Long, Void, JSONDiceRulesetResult> {

        NetRulesetTask(CommonActivity a) {
            super(a);
            mIsWaitingForServer = true;
        }

        public JSONDiceRulesetResult go(Long... v) throws IOException {
            return DiceRestClient.getInstance(mActivity).diceRuleset();
        }

        @Override
        public void onSuccess(final JSONDiceRulesetResult result) {
            mRuleset = result;

            mRulesetMaximumChance = 100.0 * mRuleset.result.player_return / mRuleset.result.minimum_payout;
            mRulesetMinimumChance = 100.0 * mRuleset.result.player_return / mRuleset.result.maximum_payout;

            checkConnectingAlert();
            mDirtyControls.mPayoutSeekBar = true;
            mDirtyControls.mPayoutValueText = true;
            mDirtyControls.mChanceSeekBar = true;
            mDirtyControls.mChanceValueText = true;
            handlePayoutChange();
        }

        @Override
        public void onDone() {
            mIsWaitingForServer = false;
        }
    }


}

