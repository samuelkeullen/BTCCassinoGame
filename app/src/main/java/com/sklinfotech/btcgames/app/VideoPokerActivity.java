package com.sklinfotech.btcgames.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sklinfotech.btcgames.R;
import com.sklinfotech.btcgames.lib.Bitcoin;
import com.sklinfotech.btcgames.lib.BitcoinGames;
import com.sklinfotech.btcgames.lib.CommonActivity;
import com.sklinfotech.btcgames.lib.JSONReseedResult;
import com.sklinfotech.btcgames.lib.JSONVideoPokerDealResult;
import com.sklinfotech.btcgames.lib.JSONVideoPokerDoubleDealerResult;
import com.sklinfotech.btcgames.lib.JSONVideoPokerDoublePickResult;
import com.sklinfotech.btcgames.lib.JSONVideoPokerHoldResult;
import com.sklinfotech.btcgames.lib.JSONVideoPokerUpdateResult;
import com.sklinfotech.btcgames.lib.NetAsyncTask;
import com.sklinfotech.btcgames.lib.Poker;
import com.sklinfotech.btcgames.lib.PokerFactory;
import com.sklinfotech.btcgames.rest.PokerRestClient;
import com.sklinfotech.btcgames.settings.CurrencySettings;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

public class VideoPokerActivity extends GameActivity {

    class VideoPokerGameState extends GameState {
        final static int WAIT_USER_DEAL = 0;
        final static int WAIT_USER_HOLD = 1;
        final static int WAIT_USER_PICK_CARD = 2;
    }

    class AutoMode {
        final static int STANDARD = 0;
        final static int HOLD_ONLY = 1;
    }

    class AutoDoubleDown {
        final static int NEVER = 0;
        final static int SOMETIMES = 1;
        final static int ALWAYS = 2;
    }

    private int mDoubleDownHeldCard = -1;
    private int mDoubleDownLevel = -1;
    private String mDoubleDownServerSeedHash;
    private int mPrize = -1;
    private int mHandEval = 0;
    private int mPaytable = 0;
    private int mBetSize;
    long mProgressiveJackpot;
    int mAutoMode;
    int mAutoSpeed;
    int mAutoDoubleDown;
    private NetReseedTask mNetReseedTask;
    private NetDealTask mNetDealTask;
    private NetHoldTask mNetHoldTask;
    private NetDoubleDealerTask mNetDoubleDealerTask;
    private NetDoublePickTask mNetDoublePickTask;
    private NetUpdateTask mNetUpdateTask;
    final private String TAG = "VideoPokerActivity";
    private JSONVideoPokerDealResult mDealResult;
    private JSONVideoPokerHoldResult mHoldResult;
    private CardHolder[] mCardHolders;
    private LinearLayout mPayout;
    private ViewGroup mPayoutHolder;
    private Button mDealButton;
    private Button mAutoButton;
    private Button mPaytablesButton;
    private Button mDoubleButton;
    private int mSoundCardDeal;
    private int mSoundCoinPay;
    private int mSoundBoop;
    private int mSoundWinDouble;
    private int mSoundWinOnDeal;
    private int mSoundWin;
    private int mSoundDealFive;
    private Poker mPoker;
    private boolean mCurrentHoldSlideAction;


    final private int NUM_CARDS = 5;
    final private String VP_SETTING_CREDIT_VALUE = "vp_credit_value";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_videopoker);

        Log.v(TAG, "Starting free memory: " + MemoryUsage.getFreeMemory(this));

        configureFlashingDepositButton(TAG);

        BitcoinGames bvc = BitcoinGames.getInstance(this);
        mGameState = VideoPokerGameState.WAIT_USER_DEAL;
        mPoker = PokerFactory.getPoker(0);
        //mPoker.test_recommend_hold();
        mBetSize = 1;
        mProgressiveJackpot = -1;
        mAutoMode = AutoMode.STANDARD;
        mAutoSpeed = AutoSpeed.MEDIUM;
        mAutoDoubleDown = AutoDoubleDown.SOMETIMES;

        //mPayout = (ViewGroup) findViewById( R.id.payout );
        mPayout = null;
        mPayoutHolder = findViewById(R.id.payout_holder);
        mDealButton = findViewById(R.id.deal_button);
        mAutoButton = findViewById(R.id.auto_button);
        mPaytablesButton = findViewById(R.id.paytables_button);
        mDoubleButton = findViewById(R.id.double_button);

        // Starting value (0.001) gets set in GameActivity::onCreate()
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mCreditValue = sharedPref.getLong(VP_SETTING_CREDIT_VALUE, mCreditValue);
        updateSatoshiButton(mCreditValue);

        mSoundCardDeal = mSoundPool.load(this, R.raw.carddeal, 1);
        mSoundCoinPay = mSoundPool.load(this, R.raw.coinpay, 1);
        mSoundBoop = mSoundPool.load(this, R.raw.boop, 1);
        mSoundWinDouble = mSoundPool.load(this, R.raw.slot_machine_win_22, 1);
        mSoundWinOnDeal = mSoundPool.load(this, R.raw.slot_machine_bet_10, 1);
        mSoundWin = mSoundPool.load(this, R.raw.win1, 1);
        mSoundDealFive = mSoundPool.load(this, R.raw.deal_five, 1);

        addCardBitmapsToCache();
        mBitmapCache.addBitmap(R.drawable.card_2c_wild);
        mBitmapCache.addBitmap(R.drawable.card_2d_wild);
        mBitmapCache.addBitmap(R.drawable.card_2s_wild);
        mBitmapCache.addBitmap(R.drawable.card_2h_wild);

        mCardHolders = new CardHolder[NUM_CARDS];
        mCardHolders[0] = new CardHolder(this, mBitmapCache, R.id.card_holder0, 0);
        mCardHolders[1] = new CardHolder(this, mBitmapCache, R.id.card_holder1, 1);
        mCardHolders[2] = new CardHolder(this, mBitmapCache, R.id.card_holder2, 2);
        mCardHolders[3] = new CardHolder(this, mBitmapCache, R.id.card_holder3, 3);
        mCardHolders[4] = new CardHolder(this, mBitmapCache, R.id.card_holder4, 4);

        for (int i = 0; i < NUM_CARDS; i++) {
            mCardHolders[i].mCardContainer.setOnTouchListener(cardHolderTouchListener);
        }

        updateControls();
        updateCredits(mUseFakeCredits ? bvc.mFakeIntBalance : bvc.mIntBalance);

        Log.v(TAG, "Ending free memory: " + MemoryUsage.getFreeMemory(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBitmapCache.clear();
    }

    void timeUpdate() {
        super.timeUpdate();
        if (canDeal() || canHold()) {
            mDealButton.setBackgroundResource(mBlinkOn ? R.drawable.button_yellow_bright : R.drawable.button_yellow);
        }
        if (canDoubleDown()) {
            mDoubleButton.setTextColor(Color.WHITE);
        }

        if (mPayout != null && mHandEval > 0) {
            int textColor = mBlinkOn ? ContextCompat.getColor(this, R.color.blue_green) : Color.WHITE;
            // TB TODO - Blink this yo
            ViewGroup payoutRow = mPayout.findViewWithTag(mHandEval);

            TextView nameColumn = payoutRow.findViewWithTag("name-column");
            nameColumn.setTextColor(textColor);
        }
    }

    int getCardResourceFromCard(String cardName) {
        if (mPoker.is_joker_game() && cardName.charAt(0) == '2') {
            char suit = cardName.charAt(1);
            switch (suit) {
                case 's':
                    return R.drawable.card_2s_wild;
                case 'd':
                    return R.drawable.card_2d_wild;
                case 'h':
                    return R.drawable.card_2h_wild;
                case 'c':
                    return R.drawable.card_2c_wild;
                default:
                    Log.e(TAG, "Bad 2X card passed to getCardResourceFromCard");
            }
        }
        return super.getCardResourceFromCard(cardName);
    }

    void resetAllCards() {
        for (int i = 0; i < NUM_CARDS; i++) {
            mCardHolders[i].resetCard();
        }
    }

    private void resetPayoutTableTextColor(int oldHandEval) {
        if (oldHandEval == 0) {
            return;
        }
        ViewGroup payoutRow = mPayout.findViewWithTag(oldHandEval);
        TextView nameColumn = payoutRow.findViewWithTag("name-column");
        nameColumn.setTextColor(ContextCompat.getColor(this, R.color.vp_payout_text));
    }

    OnTouchListener prizeColumnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!canDeal()) {
                return false;
            }

            // TB - I hackily added 1000 to the tag numbers to avoid a collision with payout row tags.
            mBetSize = (Integer) v.getTag() - 1000;
            mTextBet.setText(getString(R.string.bet_amount, mBetSize));
            playSound(mSoundBoop);
            constructPayouts();
            return false;
        }
    };

    private void constructPayouts() {
        String[] names = mPoker.hand_names_caps;
        int[][] payouts = mPoker.payouts;
        final int oneDP = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        final int itemHeight = (mPayoutHolder.getHeight() - 4 * oneDP) / (names.length - 1);

        Log.v(TAG, "CONSTRUCT PAYOUTS!");

        mPayoutHolder.removeAllViews();

        FrameLayout payoutBorder = new FrameLayout(this);
        payoutBorder.setBackgroundResource(R.drawable.payout_border);
        FrameLayout.LayoutParams payoutBorderLayout = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemHeight * (names.length - 1) + 4 * oneDP);
        mPayoutHolder.addView(payoutBorder, payoutBorderLayout);

        mPayout = new LinearLayout(this);
        mPayout.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams payoutLayout = new FrameLayout.LayoutParams(mPayoutHolder.getWidth() - 4 * oneDP, itemHeight * (names.length - 1));
        payoutLayout.gravity = Gravity.CENTER;
        payoutBorder.addView(mPayout, payoutLayout);

        // TB - Minus 1 from length since we are not adding NOTHING to the paytable.
        // TB - Minus 20 from height to account for padding in payout.
        // TB TODO - Not sure why -10 works, since the total vertical padding is 20... Should look into this further.
        // int itemHeight = (mPayout.getLayoutParams().height - 10) / (names.length-1);
        //int itemHeight = (mPayoutHolder.getLayoutParams().height - 2*oneDP) / (names.length-1);
        for (int i = names.length - 1; i > 0; i--) {
            ViewGroup row = (ViewGroup) getLayoutInflater().inflate(R.layout.vp_payout_item, null);
            row.setTag(i);

            int regularBG = ContextCompat.getColor(this, R.color.vp_payout_bg);
            int selectedBG = ContextCompat.getColor(this, R.color.blue_green);

            TextView nameView = row.findViewById(R.id.name);
            nameView.setText(names[i]);
            nameView.setClickable(false);
            nameView.setTag("name-column");

            TextView prize0 = row.findViewById(R.id.prize0);
            prize0.setText(String.valueOf(payouts[0][i]));
            prize0.setBackgroundColor(mBetSize == 1 ? selectedBG : regularBG);
            prize0.setTextColor(mBetSize == 1 ? Color.WHITE : ContextCompat.getColor(this, R.color.vp_payout_text));
            prize0.setTag(1001);
            prize0.setOnTouchListener(prizeColumnTouchListener);

            TextView prize1 = row.findViewById(R.id.prize1);
            prize1.setText(String.valueOf(payouts[1][i]));
            prize1.setBackgroundColor(mBetSize == 2 ? selectedBG : regularBG);
            prize1.setTextColor(mBetSize == 2 ? Color.WHITE : ContextCompat.getColor(this, R.color.vp_payout_text));
            prize1.setTag(1002);
            prize1.setOnTouchListener(prizeColumnTouchListener);

            TextView prize2 = row.findViewById(R.id.prize2);
            prize2.setText(String.valueOf(payouts[2][i]));
            prize2.setBackgroundColor(mBetSize == 3 ? selectedBG : regularBG);
            prize2.setTextColor(mBetSize == 3 ? Color.WHITE : ContextCompat.getColor(this, R.color.vp_payout_text));
            prize2.setTag(1003);
            prize2.setOnTouchListener(prizeColumnTouchListener);

            TextView prize3 = row.findViewById(R.id.prize3);
            prize3.setText(String.valueOf(payouts[3][i]));
            prize3.setBackgroundColor(mBetSize == 4 ? selectedBG : regularBG);
            prize3.setTextColor(mBetSize == 4 ? Color.WHITE : ContextCompat.getColor(this, R.color.vp_payout_text));
            prize3.setTag(1004);
            prize3.setOnTouchListener(prizeColumnTouchListener);

            TextView prize4 = row.findViewById(R.id.prize4);
            if (i == names.length - 1 && mProgressiveJackpot >= 0) {
                prize4.setText(getProgressiveJackpotString(mProgressiveJackpot));
            } else {
                prize4.setText(String.valueOf(payouts[4][i]));
            }
            prize4.setBackgroundColor(mBetSize == 5 ? selectedBG : regularBG);
            prize4.setTextColor(mBetSize == 5 ? Color.WHITE : ContextCompat.getColor(this, R.color.vp_payout_text));
            prize4.setTag(1005);
            prize4.setOnTouchListener(prizeColumnTouchListener);

            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemHeight);

            mPayout.addView(row, layout);

            for (int c = 0; c < row.getChildCount(); c++) {
                // The gray side color borders are separate Views, so they should be excluded from these text operations
                if (row.getChildAt(c).getClass() == TextView.class) {
                    TextView text = (TextView) row.getChildAt(c);
                    text.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemHeight);
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mIsGameBusy || mIsWaitingForServer || mGameState == VideoPokerGameState.WAIT_USER_HOLD || mGameState == VideoPokerGameState.WAIT_USER_PICK_CARD) {
                showEarlyExitDialog();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
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
            mNetReseedTask.executeParallel(0L);
        }

        mNetUpdateTask = new NetUpdateTask(this);
        mNetUpdateTask.executeParallel(0L);

        timeUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTimeUpdateRunnable);
        mHandler.removeCallbacks(mCountUpRunnable);
        setAuto(false);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Need to post this so that the UI elements are correctly sized from scaleContents.
        // Otherwise all the size getting commands will return the old values...
        mHandler.post(this::constructPayouts);
    }

    public void onCreditSatoshi(View button) {
        if (!canCreditSatoshi()) {
            return;
        }

        final String currency = CurrencySettings.getInstance(this).getCurrencyUpperCase();
        final CreditItem[] items = new CreditItem[]{
            new CreditItem(String.format("1 CREDIT = 0.05 %s", currency), String.format("Win over 200 %s!", currency), Bitcoin.stringAmountToLong("0.05")),
            new CreditItem(String.format("1 CREDIT = 0.01 %s", currency), String.format("Win over 40 %s!", currency), Bitcoin.stringAmountToLong("0.01")),
            new CreditItem(String.format("1 CREDIT = 0.005 %s", currency), String.format("Win over 20 %s!", currency), Bitcoin.stringAmountToLong("0.005")),
            new CreditItem(String.format("1 CREDIT = 0.001 %s", currency), String.format("Win over 4 %s!", currency), Bitcoin.stringAmountToLong("0.001")),
            new CreditItem(String.format("1 CREDIT = 0.0001 %s", currency), String.format("Win over 0.4 %s!", currency), Bitcoin.stringAmountToLong("0.0001"))};
        showCreditDialog(VP_SETTING_CREDIT_VALUE, items);
    }

    @Override
    public void handleCreditSatoshiChanged() {

        // Gotta reset the jackpot until we get the new value
        mProgressiveJackpot = -1;
        updateProgressiveJackpot(mProgressiveJackpot);

        // Get the progressive jackpot
        mNetUpdateTask = new NetUpdateTask(this);
        mNetUpdateTask.execute(Long.valueOf(0));
    }

    public void holdCard(int index, boolean value) {
        if (mIsWaitingForServer || mIsGameBusy) {
            return;
        }

        playSound(mSoundBoop);
        Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vb.vibrate(25);

        CardHolder holder = mCardHolders[index];
        if (mGameState == VideoPokerGameState.WAIT_USER_HOLD) {
            holder.holdCard(value);
        } else if (mGameState == VideoPokerGameState.WAIT_USER_PICK_CARD) {
            if (index == 0) {
                return;
            }
            holder.holdCard(value);
            mDoubleDownHeldCard = index;
            mNetDoublePickTask = new NetDoublePickTask(this);
            mNetDoublePickTask.executeParallel();
        }
    }

    OnTouchListener cardHolderTouchListener = new OnTouchListener() {
        //@Override
        public boolean onTouch(View v, MotionEvent event) {

            if (!canHold() && !canPickCard()) {
                return false;
            }

            // onTouch + finger move will continue sending the touch command to the same view, so we
            // need to check the touch coords to determine what card he is touching.

            int leftBlackBarWidth = (mContainer.getWidth() - mContents.getWidth()) / 2;
            // Can't just use holder.getWidth() since that doesn't take margins into account.
            int holderAreaWidth = mContents.getWidth() / 5;
            int index = (int) ((event.getRawX() - leftBlackBarWidth) / holderAreaWidth);
            Log.d(TAG, "onTouch!" + index);
            CardHolder holder = mCardHolders[index];

            // Must be within the Y coord of the holder

            int[] holderLocation = new int[2];
            holder.mCardContainer.getLocationOnScreen(holderLocation);
            if (event.getRawY() < holderLocation[1] || event.getRawY() > holderLocation[1] + holder.mCardContainer.getHeight()) {
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mCurrentHoldSlideAction = !holder.mIsHeld;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    // Change heldness, if it's not already changed (so it won't keep changing back and forth)
                    if (mCurrentHoldSlideAction != holder.mIsHeld) {
                        holdCard(index, mCurrentHoldSlideAction);
                    }
                    break;
            }

            return true;
        }
    };

    void handleNotEnoughCredits() {
        super.showDepositDialog(R.color.bitcoin_games_videopoker);
        setAuto(false);
    }

    public void onDeposit(View button) {
        showDepositDialog(R.color.bitcoin_games_videopoker, true);
    }

    public void onDeal(View button) {
        if (mGameState == VideoPokerGameState.WAIT_USER_DEAL) {
            if (!canDeal()) {
                return;
            }
            BitcoinGames bvc = BitcoinGames.getInstance(this);

            if ((mUseFakeCredits ? bvc.mFakeIntBalance : bvc.mIntBalance) - mCreditValue < 0) {
                handleNotEnoughCredits();
                return;
            }

            if (mServerSeedHash == null) {
                // TB TODO - Get another hash? Or maybe we're waiting for it still?
                return;
            }

            Log.v("onDeal", Long.toString(System.currentTimeMillis()));
            mNetDealTask = new NetDealTask(this);
            mNetDealTask.executeParallel();
        } else if (mGameState == VideoPokerGameState.WAIT_USER_HOLD) {
            if (!canHold()) {
                return;
            }
            Log.v("onHold", Long.toString(System.currentTimeMillis()));
            mNetHoldTask = new NetHoldTask(this);
            mNetHoldTask.executeParallel();
        }
    }

    private boolean canDeal() {
        if (mIsWaitingForServer || mIsGameBusy) {
            return false;
        }
        // TB TODO - Should this also check if you have enough credits?
        // If so, then canAuto() and canPaytable() need to not depend on canDeal().
        return (mGameState == VideoPokerGameState.WAIT_USER_DEAL);
    }

    private boolean canHold() {
        if (mIsWaitingForServer || mIsGameBusy) {
            return false;
        }
        return (mGameState == VideoPokerGameState.WAIT_USER_HOLD);
    }

    private boolean canPickCard() {
        if (mIsWaitingForServer || mIsGameBusy) {
            return false;
        }
        return (mGameState == VideoPokerGameState.WAIT_USER_PICK_CARD);
    }

    private boolean canDoubleDown() {
        if (mIsWaitingForServer || mIsGameBusy) {
            return false;
        }
        if (mHoldResult == null) {
            return false;
        }
        if (!mPoker.can_double_down(mHoldResult.hand_eval)) {
            return false;
        }
        // TB TODO - Should use the Poker function! (it checks the handeval as well)
        return (mGameState == VideoPokerGameState.WAIT_USER_DEAL && mPrize > 0 && mDoubleDownLevel <= 2);
    }

    private boolean canCreditSatoshi() {
        return canDeal();
    }

    private boolean canAuto() {
        return canDeal() || canHold();
    }

    private boolean canPaytables() {
        return canDeal();
    }

    public void updateControls() {
        if (canDeal() || canHold()) {
            mDealButton.setBackgroundResource(R.drawable.button_yellow_bright);
            mDoubleButton.setTextColor(Color.WHITE);
        } else {
            mDealButton.setBackgroundResource(R.drawable.button_yellow);
            mDoubleButton.setTextColor(Color.GRAY);
        }
        if (canDoubleDown()) {
            mDoubleButton.setTextColor(Color.WHITE);
        } else {
            mDoubleButton.setTextColor(Color.GRAY);
        }

        if (mIsAutoOn) {
            mAutoButton.setBackgroundResource(R.drawable.button_red);
            mAutoButton.setTextColor(Color.WHITE);
        } else if (canAuto()) {
            mAutoButton.setBackgroundResource(R.drawable.button_blue_green);
            mAutoButton.setTextColor(Color.WHITE);
        } else {
            mAutoButton.setBackgroundResource(R.drawable.button_dark);
            mAutoButton.setTextColor(Color.GRAY);
        }

        if (canPaytables()) {
            mPaytablesButton.setTextColor(Color.WHITE);
            mPaytablesButton.setBackgroundResource(R.drawable.button_cyan);
        } else {
            mPaytablesButton.setTextColor(Color.GRAY);
            mPaytablesButton.setBackgroundResource(R.drawable.button_dark);
        }
    }

    private void doAuto() {
        if (!mIsAutoOn) {
            return;
        }

        Random r = new Random();

        switch (mGameState) {
            case VideoPokerGameState.WAIT_USER_DEAL:
                // Start at true, and then find reasons to not double down.
                boolean shouldDoubleDown = true;
                if (!canDoubleDown()) {
                    shouldDoubleDown = false;
                } else {
                    switch (mAutoDoubleDown) {
                        case AutoDoubleDown.NEVER:
                            shouldDoubleDown = false;
                            break;
                        case AutoDoubleDown.SOMETIMES:
                            if (r.nextInt(2) == 0) {
                                shouldDoubleDown = false;
                            }
                            break;
                        case AutoDoubleDown.ALWAYS:
                            // pass
                    }
                }
                if (mAutoMode == AutoMode.HOLD_ONLY) {
                    shouldDoubleDown = false;
                }

                if (shouldDoubleDown) {
                    onDouble(null);
                } else {
                    onDeal(null);
                }
                break;
            case VideoPokerGameState.WAIT_USER_HOLD:
                onDeal(null);
                break;
            case VideoPokerGameState.WAIT_USER_PICK_CARD:
                // TB TODO - Pick a random card!
                int hold = r.nextInt(4) + 1;
                holdCard(hold, true);
                break;
        }
    }

    private void checkAuto() {
        if (mIsGameBusy) {
            Log.v(TAG, "Error: checkAuto() called while game is busy.");
            return;
        }
        if (mIsWaitingForServer) {
            Log.v(TAG, "Error: checkAuto() called while waiting for server.");
            return;
        }
        if (!mIsAutoOn) {
            return;
        }

        // Hold cards immediately, but then wait to actually deal
        if (mGameState == VideoPokerGameState.WAIT_USER_HOLD) {
            // TB TODO - hold suggest mode
            String[] cards = new String[5];
            for (int i = 0; i < 5; i++) {
                cards[i] = mCardHolders[i].mCard;
            }

            int[] holds = mPoker.recommend_hold(cards);

            // TB TEMP TEST - Check for dumb holding
            if (holds[0] == 1 && holds[1] == 1 && holds[2] == 1 && holds[3] == 1 && holds[4] == 1) {
                if (!mPoker.is_hand_ok_to_hold_all_cards(mDealResult.hand_eval)) {
                    Log.v("YO", "Hand eval = " + mDealResult.hand_eval);
                    Log.v("YO", "HAND_STRAIGHT=" + mPoker.HAND_STRAIGHT);
                    mPoker.log_cards(cards);
                    mPoker.recommend_hold(cards);
                    throw new RuntimeException("Bad hold!!!");
                }
            }

            for (int i = 0; i < 5; i++) {
                holdCard(i, holds[i] == 1);
            }
        }

        if (mAutoMode == AutoMode.HOLD_ONLY) {
            return;
        }

        int delay = getDelayFromAutoSpeed(mAutoSpeed);

        // When you initially start auto mode, it should immediately jump into action.
        // It shouldn't just initially sit there for 1-2 seconds, since that seems unresponsive.
        if (mIsFirstAutoAction) {
            delay = 0;
            mIsFirstAutoAction = false;
        }

        mHandler.postDelayed(new Runnable() {
            public void run() {
                doAuto();
            }
        }, delay);
    }

    public void setAuto(boolean auto) {
        mIsAutoOn = auto;
        updateControls();
        if (auto) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mIsFirstAutoAction = true;
            checkAuto();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void onAuto(View button) {
        if (mIsAutoOn) {
            setAuto(false);
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.vp_auto);
        dialog.setTitle("Autoplay Settings");


        final Spinner modeSpinner = (Spinner) dialog.findViewById(R.id.mode_spinner);
        final Spinner speedSpinner = (Spinner) dialog.findViewById(R.id.speed_spinner);
        final Spinner doubleDownSpinner = (Spinner) dialog.findViewById(R.id.doubledown_spinner);

        modeSpinner.setSelection(mAutoMode);
        speedSpinner.setSelection(mAutoSpeed);
        doubleDownSpinner.setSelection(mAutoDoubleDown);

        Button playButton = (Button) dialog.findViewById(R.id.play_button);
        playButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mAutoMode = modeSpinner.getSelectedItemPosition();
                mAutoSpeed = speedSpinner.getSelectedItemPosition();
                mAutoDoubleDown = doubleDownSpinner.getSelectedItemPosition();
                setAuto(true);
                dialog.dismiss();
            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void onPaytables(View button) {
        // TB - This ordering looks nicer than the official paytable numbering, so we need to manually set the paytable below
        final String[] items = {"Jacks or Better", "Deuces Wild", "Bonus Poker", "Double Bonus", "Dbl Dbl Bonus", "Tens or Better", "Bonus Deluxe"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("More games");
        builder.setItems(items, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {
                // TB TODO - Actually change the friggen paytable!
                // TB TODO - Prettier dialog? Could use the art from the web site?
                //Toast.makeText(getApplicationContext(), "Coming soon: " + items[item], Toast.LENGTH_SHORT).show();
                switch (item) {
                    case 0:
                        mPaytable = 0;
                        break;
                    case 1:
                        mPaytable = 5;
                        break;
                    case 2:
                        mPaytable = 2;
                        break;
                    case 3:
                        mPaytable = 3;
                        break;
                    case 4:
                        mPaytable = 4;
                        break;
                    case 5:
                        mPaytable = 1;
                        break;
                    case 6:
                        mPaytable = 6;
                        break;
                    default:
                        Log.v(TAG, "Bad game selected");
                        break;
                }
                mPoker = PokerFactory.getPoker(mPaytable);
                constructPayouts();
            }

        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void onDouble(View button) {
        if (!canDoubleDown()) {
            return;
        }
        long cost = (mCreditValue * mPrize);
        BitcoinGames bvc = BitcoinGames.getInstance(this);
        if ((mUseFakeCredits ? bvc.mFakeIntBalance : bvc.mIntBalance) - cost < 0) {
            Toast.makeText(this, R.string.not_enough_credits, Toast.LENGTH_SHORT).show();
            return;
        }

        mNetDoubleDealerTask = new NetDoubleDealerTask(this);
        mNetDoubleDealerTask.executeParallel();
    }

    String getProgressiveJackpotString(long progressiveJackpot) {
        int bestHand = mPoker.hand_names.length - 1;
        float val = mPoker.get_hand_prize_amount(5, bestHand) + (float) (progressiveJackpot / 10000.0);
        return String.format(Locale.getDefault(), "%.2f", val);
    }

    void updateProgressiveJackpot(long progressiveJackpot) {
        Log.v("Jackpot", Long.toString(progressiveJackpot));
        // This way to often gets nullpointer when multitasking with app.
        if (mPayout == null) {
            return;
        }
        ViewGroup payoutRow = mPayout.findViewWithTag(mPoker.hand_names.length - 1);
        TextView prize4 = payoutRow.findViewById(R.id.prize4);
        prize4.setText(getProgressiveJackpotString(progressiveJackpot));
    }

    class ShowCardsRunnable implements Runnable {
        String[] mCards;
        // Index into the new cards we're to show
        int mCardIndex;
        // Position in the hand that we're at
        int mHandPosition;
        Runnable mFinishedCallback;

        ShowCardsRunnable(String[] cards, Runnable finishedCallback) {
            mCardIndex = 0;
            // Set to -1 so that the first time we hit run, it gets set to 0.
            mHandPosition = -1;
            mFinishedCallback = finishedCallback;
            mCards = cards;
        }

        public void run() {

            if (mCards.length == 0) {
                mFinishedCallback.run();
                return;
            }

            while (true) {
                mHandPosition++;
                if (mHandPosition == NUM_CARDS) {
                    mFinishedCallback.run();
                    return;
                }

                // Game crashed trying to dereference null mCards[mCardIndex]
                // So check here if something is weird, and then print out a bunch of stuff
                if (mCards[mCardIndex] == null) {
                    Log.v("CRASH", "Gonna crash");
                    Log.v("CRASH", "mHandPosition" + mHandPosition);
                    for (int i = 0; i < mCards.length; i++) {
                        Log.v("mCards", mCards[i]);
                    }
                    for (int i = 0; i < NUM_CARDS; i++) {
                        Log.v("holders", mCardHolders[0].mCard);
                    }
                }

                CardHolder holder = mCardHolders[mHandPosition];
                if (holder.mIsShowingBack) {
                    holder.showCard(mCards[mCardIndex]);
                    playSound(mSoundCardDeal);
                    mCardIndex++;

                    if (mCardIndex == mCards.length) {
                        mFinishedCallback.run();
                        return;
                    }
                    int delay = 100;
                    mHandler.postDelayed(this, delay);
                    return;
                }
            }

        }
    }

    class NetUpdateTask extends NetAsyncTask<Long, Void, JSONVideoPokerUpdateResult> {

        NetUpdateTask(CommonActivity a) {
            super(a);
        }

        public JSONVideoPokerUpdateResult go(Long... v) throws IOException {
            int last = 999999999;
            int chatlast = 999999999;
            return PokerRestClient.getInstance(mActivity).videoPokerUpdate(last, chatlast, mCreditValue);
        }

        public void onSuccess(JSONVideoPokerUpdateResult result) {
            mProgressiveJackpot = result.progressive_jackpot;
            updateProgressiveJackpot(mProgressiveJackpot);
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
            return PokerRestClient.getInstance(mActivity).videoPokerReseed();
        }

        public void onSuccess(JSONReseedResult result) {
            mServerSeedHash = result.server_seed_hash;
            updateControls();
            checkConnectingAlert();
            if (mConnectingDialog == null && mAutodeal) {
                onDeal(null);
            }
        }
    }

    class NetDealTask extends NetAsyncTask<Long, Void, JSONVideoPokerDealResult> {

        NetDealTask(CommonActivity a) {
            super(a);
            // TB TEMP TEST - Keep this running so that if the task is interrupted (phone call, etc), that the result
            // will still be shown.
            mAllowAbort = false;
            updateCredits((mUseFakeCredits ? mBVC.mFakeIntBalance : mBVC.mIntBalance) - (mBetSize * mCreditValue));

            stopCountUpWins();
            mIsWaitingForServer = true;
            mIsGameBusy = true;
            Log.v("NetDealTask", Long.toString(System.currentTimeMillis()));
            resetAllCards();
            playSound(mSoundCoinPay);
            updateWin(0, false);

            // TB - Credits are now dirty (so don't update credits with whatever we get from a balance update, since it will be incorrect)
            mCreditsAreDirty = true;
            updateControls();
            resetPayoutTableTextColor(mHandEval);
            mHandEval = 0;
        }

        public JSONVideoPokerDealResult go(Long... v) throws IOException {
            String serverSeedHash = mServerSeedHash;
            // TB TODO - Randomize this!
            return PokerRestClient.getInstance(mActivity).videoPokerDeal(mBetSize, mPaytable, mCreditValue, serverSeedHash, getClientSeed(), mUseFakeCredits);
        }

        @Override
        public void onSuccess(final JSONVideoPokerDealResult result) {
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
            mDealResult = result;
            ShowCardsRunnable showCards = new ShowCardsRunnable(result.cards, new Runnable() {
                public void run() {
                    if (result.hand_eval > 0) {
                        playSound(mSoundWinOnDeal);
                        mHandEval = result.hand_eval;
                    }
                    mGameState = VideoPokerGameState.WAIT_USER_HOLD;
                    mIsGameBusy = false;
                    updateControls();
                    for (int i = 0; i < NUM_CARDS; i++) {
                        mCardHolders[i].enableGrayHoldImage();
                    }
                    checkAuto();
                }
            });
            showCards.run();

            mBVC.mIntBalance = result.intbalance;
            mBVC.mFakeIntBalance = result.fake_intbalance;
            updateCredits(mUseFakeCredits ? result.fake_intbalance : result.intbalance);
            mProgressiveJackpot = result.progressive_jackpot;
            updateProgressiveJackpot(mProgressiveJackpot);
        }

        @Override
        public void onError(final JSONVideoPokerDealResult result) {
            setAuto(false);
            mIsGameBusy = false;
            updateControls();
        }

        @Override
        public void onDone() {
            mIsWaitingForServer = false;

            // TB - Credits are now clean. We can display the intbalance we get from the server again.
            mCreditsAreDirty = false;
        }
    }

    class NetHoldTask extends NetAsyncTask<Long, Void, JSONVideoPokerHoldResult> {

        String mHolds;

        NetHoldTask(CommonActivity a) {
            super(a);
            // TB TEMP TEST - Keep this running so that if the task is interrupted (phone call, etc), that the result
            // will still be shown.
            mAllowAbort = false;
            mIsWaitingForServer = true;
            mIsGameBusy = true;
            Log.v("NetHoldTask", Long.toString(System.currentTimeMillis()));
            mHolds = "";
            for (int i = 0; i < NUM_CARDS; i++) {
                CardHolder holder = mCardHolders[i];
                if (holder.mIsHeld) {
                    mHolds += "1";
                } else {
                    holder.showCard("back");
                    mHolds += "0";
                }
                holder.disableGrayHoldImage();
            }
            updateControls();
        }

        public JSONVideoPokerHoldResult go(Long... v) throws IOException {
            //String holds = "01100";
            return PokerRestClient.getInstance(mActivity).videoPokerHold(mDealResult.game_id, mHolds, mServerSeedHash);
        }

        @Override
        public void onSuccess(final JSONVideoPokerHoldResult result) {
            mHoldResult = result;
            mServerSeedHash = result.server_seed_hash;

            // TB TODO - Sometimes result.cards is null (screwed up new connection?), resulting in the game crashing in ShowCardsRunnable.

            ShowCardsRunnable showCards = new ShowCardsRunnable(result.cards, new Runnable() {
                public void run() {
                    if (result.hand_eval > 0) {
                        playSound(mSoundWin);

                        // This sets the credits to dirty, so changing mBVC.mIntBalance a little later won't mess with the count up.
                        long delta = mCreditValue;
                        if (mIsAutoOn) {
                            delta = result.prize * mCreditValue;
                        }
                        startCountUpWins(result.prize * mCreditValue, (mUseFakeCredits ? result.fake_intbalance : result.intbalance) - (result.prize * mCreditValue), mCreditValue);

                        mDoubleDownLevel = 0;
                        mDoubleDownServerSeedHash = result.double_down_server_seed_hash;

                        resetPayoutTableTextColor(mHandEval);
                        mHandEval = result.hand_eval;

                        if (mPoker.is_jackpot(result.hand_eval)) {
                            setAuto(false);
                        }
                    } else {
                        //mTextWin.setText( "" );
                        updateWin(0, false);
                    }
                    mPrize = result.prize;
                    mBVC.mIntBalance = result.intbalance;
                    mBVC.mFakeIntBalance = result.fake_intbalance;
                    updateCredits(mUseFakeCredits ? result.fake_intbalance : result.intbalance);

                    mGameState = VideoPokerGameState.WAIT_USER_DEAL;
                    mIsGameBusy = false;
                    updateControls();
                    checkAuto();
                }
            });
            showCards.run();
        }

        @Override
        public void onError(final JSONVideoPokerHoldResult result) {
            mIsGameBusy = false;
        }

        @Override
        public void onDone() {
            mIsWaitingForServer = false;
        }
    }

    class NetDoubleDealerTask extends NetAsyncTask<Long, Void, JSONVideoPokerDoubleDealerResult> {

        NetDoubleDealerTask(CommonActivity a) {
            super(a);

            // TB TEMP TEST - Keep this running so that if the task is interrupted (phone call, etc), that the result
            // will still be shown.
            mAllowAbort = false;
            long cost = (mCreditValue * mPrize);
            updateCredits((mUseFakeCredits ? mBVC.mFakeIntBalance : mBVC.mIntBalance) - cost);

            mIsWaitingForServer = true;
            mIsGameBusy = true;
            resetAllCards();

            updateWin(mPrize * 2 * mCreditValue, true);
            // TB - Credits are now dirty (so don't update credits with whatever we get from a balance update, since it will be incorrect)
            mCreditsAreDirty = true;
            updateControls();
            mCardHolders[0].showDealer();
        }

        public JSONVideoPokerDoubleDealerResult go(Long... v) throws IOException {
            // TB TODO - This must support all rounds of double down!
            return PokerRestClient.getInstance(mActivity).videoPokerDoubleDealer(mDealResult.game_id, mDoubleDownServerSeedHash, getClientSeed(), mDoubleDownLevel);
        }

        @Override
        public void onSuccess(final JSONVideoPokerDoubleDealerResult result) {

            if (result.error != null) {
                // This will happen if the server seed has expired. Just scrap the game and move on.
                if (result.error.contains("This double game has already been played")) {
                    mIsGameBusy = false;
                    setAuto(false);
                    mServerSeedHash = null;
                    mHoldResult = null;
                    resetAllCards();
                    showConnectingDialog();
                    mNetReseedTask = new NetReseedTask(mActivity, false);
                    mNetReseedTask.execute(Long.valueOf(0));
                } else {
                    Log.e(TAG, "Unknown error returned by server:" + result.error);
                }
                return;
            }

            String[] cards = new String[]{result.dealer_card};
            ShowCardsRunnable showCards = new ShowCardsRunnable(cards, new Runnable() {
                public void run() {
                    mGameState = VideoPokerGameState.WAIT_USER_PICK_CARD;
                    mIsGameBusy = false;
                    updateControls();
                    checkAuto();
                }

            });
            showCards.run();
            mBVC.mIntBalance = result.intbalance;
            mBVC.mFakeIntBalance = result.fake_intbalance;
            updateCredits(mUseFakeCredits ? result.fake_intbalance : result.intbalance);
        }

        @Override
        public void onError(final JSONVideoPokerDoubleDealerResult result) {
            mIsGameBusy = false;
        }

        @Override
        public void onDone() {
            mIsWaitingForServer = false;
            mCreditsAreDirty = false;
        }
    }

    class NetDoublePickTask extends NetAsyncTask<Long, Void, JSONVideoPokerDoublePickResult> {

        NetDoublePickTask(CommonActivity a) {
            super(a);
            // TB TEMP TEST - Keep this running so that if the task is interrupted (phone call, etc), that the result
            // will still be shown.
            mAllowAbort = false;
            mIsWaitingForServer = true;
            mIsGameBusy = true;
            updateControls();
        }

        public JSONVideoPokerDoublePickResult go(Long... v) throws IOException {
            return PokerRestClient.getInstance(mActivity).videoPokerDoublePick(mDealResult.game_id, mDoubleDownLevel, mDoubleDownHeldCard);
        }

        @Override
        public void onSuccess(final JSONVideoPokerDoublePickResult result) {

            // TB - Surely there's a better way to chop off the top card?
            String[] cards = new String[]{result.cards[1], result.cards[2], result.cards[3], result.cards[4]};
            ShowCardsRunnable showCards = new ShowCardsRunnable(cards, new Runnable() {
                public void run() {

                    if (result.prize > 0) {
                        mDoubleDownLevel++;
                        // mPrize *= 2;
                        mPrize = result.prize;
                        playSound(mSoundWinDouble);
                        mDoubleDownServerSeedHash = result.double_down_server_seed_hash;
                    } else {
                        mPrize = 0;
                    }
                    updateWin(mPrize * mCreditValue, false);

                    mBVC.mIntBalance = result.intbalance;
                    mBVC.mFakeIntBalance = result.fake_intbalance;
                    updateCredits(mUseFakeCredits ? result.fake_intbalance : result.intbalance);
                    mGameState = VideoPokerGameState.WAIT_USER_DEAL;
                    mIsGameBusy = false;
                    updateControls();
                    checkAuto();
                }
            });
            showCards.run();
        }

        @Override
        public void onError(final JSONVideoPokerDoublePickResult result) {
            mIsGameBusy = false;
        }

        @Override
        public void onDone() {
            mIsWaitingForServer = false;
        }
    }
}

class CardHolder {
    public TextView mHoldText;
    public ImageView mCardImage;
    public boolean mIsHeld;
    public int mIndex;
    public VideoPokerActivity mActivity;
    public View mCardContainer;
    private final String TAG = "CardHolder";
    public boolean mIsShowingBack;
    private BitmapCache mBitmapCache;
    public String mCard;
    private boolean mGrayHoldImageEnabled;

    //public CardHolder( Activity a, int id )
    CardHolder(VideoPokerActivity a, BitmapCache bitmapCache, int containerResourceID, int index) {
        mActivity = a;
        mBitmapCache = bitmapCache;
        mCardContainer = a.findViewById(containerResourceID);
        mCardContainer.setTag(index);

        mCardImage = mCardContainer.findViewById(R.id.card0);
        showCard("back");

        mHoldText = mCardContainer.findViewById(R.id.hold0);

        mIndex = index;
        mIsHeld = false;
        resetCard();
    }

    void showCard(final String cardName) {
        mCard = cardName;
        int r = mActivity.getCardResourceFromCard(cardName);

        Bitmap src = mBitmapCache.getBitmap(r);
        RoundedBitmapDrawable dr =
            RoundedBitmapDrawableFactory.create(mActivity.getResources(), src);

        int screenLayoutSize = mActivity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        if (screenLayoutSize > Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            dr.setCornerRadius(10.0f);
        } else {
            dr.setCornerRadius(25.0f);
        }

        mCardImage.setImageDrawable(dr);

        mIsShowingBack = cardName.equals("back");
    }

    void resetCard() {
        mIsHeld = false;
        mHoldText.setVisibility(View.INVISIBLE);
        showCard("back");
    }

    void enableGrayHoldImage() {
        mGrayHoldImageEnabled = true;
        mHoldText.setTextColor(Color.GRAY);
        holdCard(false);
    }

    void disableGrayHoldImage() {
        mGrayHoldImageEnabled = false;
        if (!mIsHeld) {
            mHoldText.setVisibility(View.INVISIBLE);
        }
    }

    void holdCard(boolean value) {
        mIsHeld = value;
        mHoldText.setText(R.string.vp_hold);
        if (mIsHeld) {
            mHoldText.setBackgroundResource(R.drawable.hold_border);
            mHoldText.setTextColor(ContextCompat.getColor(mActivity, R.color.hold_color));
            mHoldText.setVisibility(View.VISIBLE);
        } else if (mGrayHoldImageEnabled) {
            mHoldText.setBackground(null);
            mHoldText.setTextColor(Color.GRAY);
            mHoldText.setVisibility(View.VISIBLE);
        } else {
            mHoldText.setVisibility(View.INVISIBLE);
        }
    }

    void showDealer() {
        mHoldText.setText(R.string.vp_dealer);
        mHoldText.setBackgroundResource(R.drawable.hold_border);
        mHoldText.setTextColor(ContextCompat.getColor(mActivity, R.color.hold_color));
        mHoldText.setVisibility(View.VISIBLE);
    }
}