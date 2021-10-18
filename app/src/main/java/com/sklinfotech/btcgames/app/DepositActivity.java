package com.sklinfotech.btcgames.app;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sklinfotech.btcgames.R;
import com.sklinfotech.btcgames.lib.Bitcoin;
import com.sklinfotech.btcgames.lib.BitcoinGames;
import com.sklinfotech.btcgames.lib.CommonActivity;
import com.sklinfotech.btcgames.lib.JSONBalanceResult;
import com.sklinfotech.btcgames.lib.NetBalanceTask;
import com.sklinfotech.btcgames.lib.QrCode;
import com.sklinfotech.btcgames.settings.CurrencySettings;
import com.google.zxing.WriterException;

public class DepositActivity extends CommonActivity {

    private String depositAddress;

    TextView mBalance;
    TextView mUnconfirmedWarning;
    TextView mTitle;
    TextView mDepositAddress;
    Button mExternalApp;
    ImageView mQrCodeImage;

    private final static String TAG = DepositActivity.class.getSimpleName();
    final static int REQUEST_CODE_DEPOSIT_APP = 0;
    DepositNetBalanceTask mNetBalanceTask;

    boolean mWillReturnFromDeposit;
    ProgressDialog mWaitingForDepositAlert;

    public void updateValues() {

        BitcoinGames bvc = BitcoinGames.getInstance(this);
        if (bvc.mIntBalance != -1) {
            String balance = Bitcoin.longAmountToStringChopped(bvc.mIntBalance);
            mBalance.setText(getString(R.string.bitcoin_balance, balance, CurrencySettings.getInstance(this).getCurrencyUpperCase()));
        } else {
            mBalance.setText(CurrencySettings.getInstance(this).getCurrencyUpperCase());
        }

        if (bvc.mUnconfirmed) {
            mUnconfirmedWarning.setVisibility(View.VISIBLE);
        } else {
            mUnconfirmedWarning.setVisibility(View.GONE);
        }

        if (depositAddress == null) {
            CurrencySettings.getInstance(this).retrieveAddress(this, address -> {
                depositAddress = address;
                updateAddressOnUI();
            });
        } else {
            updateAddressOnUI();
        }
        // TB TODO - Enable/disable external app button depending on whether an address exists yet
        // TB TODO - Enable/disable external app button if no bitcoin intent handler app exists
    }

    private void updateAddressOnUI() {
        if (depositAddress == null) {
            mDepositAddress.setText(getString(R.string.main_connecting));
            if (mQrCodeImage != null) mQrCodeImage.setVisibility(View.GONE);
        } else {
            mDepositAddress.setText(depositAddress);
            if (mQrCodeImage != null) {
                try {
                    Bitmap bmp = QrCode.encodeAsBitmap(depositAddress.toString());
                    mQrCodeImage.setImageBitmap(bmp);
                    mQrCodeImage.setVisibility(View.VISIBLE);
                } catch (WriterException e) {
                    Log.e(TAG, "QR-code was not generated: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_deposit);

        mBalance = (TextView) findViewById(R.id.balance);
        mUnconfirmedWarning = (TextView) findViewById(R.id.unconfirmed_warning);
        mTitle = (TextView) findViewById(R.id.title);
        mDepositAddress = (TextView) findViewById(R.id.deposit_address);
        mExternalApp = (Button) findViewById(R.id.external_app_button);
        mQrCodeImage = (ImageView) findViewById(R.id.deposit_qr_code_image);
        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        Typeface robotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");

        mNetBalanceTask = null;
        mTitle.setText(R.string.deposit);
        mTitle.setTypeface(robotoLight);

        // TB TODO - Should store/retrieve this address in the preferences storage, and then just
        // set it to null whenever the user account changes (indicating that we must get the deposit address)
        updateValues();

        // Sucky hack
        mWillReturnFromDeposit = false;
        mWaitingForDepositAlert = null;
    }

    public void timeUpdate() {
        if (CurrencySettings.getInstance(this).getAccountKey() != null) {
            Log.v(TAG, CurrencySettings.getInstance(this).getAccountKey());
            mNetBalanceTask = new DepositNetBalanceTask(this);
            mNetBalanceTask.executeParallel(Long.valueOf(0));
        }

        // Every 5 seconds
        final int timeUpdateDelay = 5000;
        mHandler.postDelayed(this::timeUpdate, timeUpdateDelay);
    }


    @Override
    public void onResume() {
        super.onResume();

        // Sucky hack since we can't rely on onActivityResult() being called after a deposit is made. :(
        Log.v(TAG, "onResume: " + mWillReturnFromDeposit);
        if (mWillReturnFromDeposit) {
            mWaitingForDepositAlert = ProgressDialog.show(this, "", "Checking for new deposit...\nThis usually takes just a few seconds.", true);
            mWillReturnFromDeposit = false;

            // If the alert hasn't been dismissed because of a new deposit, just kill it after a while so that the user is not stuck.
            final int delay = 7000;
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (mWaitingForDepositAlert != null) {
                        Log.v(TAG, "Dismissing2");
                        mWaitingForDepositAlert.dismiss();
                    }
                }
            }, delay);
        }

        timeUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "OnPause");
        mHandler.removeCallbacks(this::timeUpdate);
    }

    public void handleBlockchainCrash() {
      /*
      final Activity that = this;
		Handler handler = new Handler();
		handler.postDelayed( new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(that);
				builder.setMessage("Blockchain seems to have crashed. Try setting up your Blockchain account from the app before trying to deposit coins.")
				       .setCancelable(false)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				    	   public void onClick(DialogInterface dialog, int id) {
				               dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.create();	        
				alert.show(); 
			}
		}, 500 ); 
		*/
    }


    // TB TODO - This is getting immediately called when startActivityForResult is called... because android:launchMode in the bitcoin app is android:launchMode="singleTask",
    // which apparently causes this. So could just run the balance checker or something...
    // So for now I'm just calling startActivity(), which doesn't cause onPause -> onResume -> onPause -> SendCoinsExternalActivity -> onResume...
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_DEPOSIT_APP) {
            Log.e(TAG, "Got a activity request code that we didn't specify!");
            return;
        }
        if (resultCode == RESULT_CANCELED) {
            Log.v(TAG, "resultCode == RESULT_CANCELED!");
            handleBlockchainCrash();
            return;
        }
        Log.v(TAG, "resultCode == " + resultCode);

        // TB TODO - Finish this activity so we pop back to the main screen (on successful deposit)
    }

    public void handleMissingExternalApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.deposit_missing_wallet_message)
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    String url = getString(R.string.deposit_missing_wallet_app_url);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        AlertDialog alert = builder.create();
        alert.show();

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onDepositAddress(View button) {
        if (depositAddress != null) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Bitcoin Address", depositAddress);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "Deposit address has been copied to your clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    public void onExternalApp(View button) {
        // TB TODO - Verify that an external app actually exists!!!
        // TB TODO - Get correct deposit address from service call
        if (depositAddress != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(depositAddress));

            try {
                // TB TODO - This will immediately call onActivityResult because android:launchMode in the bitcoin app is android:launchMode="singleTask",
                // which apparently causes this. So could just run the balance checker or something...
                // So for now just call startActivity and hack in a progress alert when you return... sucky...
                //startActivityForResult( intent, REQUEST_CODE_DEPOSIT_APP );
                startActivity(intent);

                // TB - This hack sucks, since we don't know what the user actually did in the other app. If he didn't deposit anything, we won't know!
                mWillReturnFromDeposit = true;
            } catch (ActivityNotFoundException e) {
                handleMissingExternalApp();
            }
        }
    }

    class DepositNetBalanceTask extends NetBalanceTask {

        DepositNetBalanceTask(CommonActivity a) {
            super(a);
        }

        public void onSuccess(JSONBalanceResult result) {

            if (mWaitingForDepositAlert != null) {
                if (result.notify_transaction != null) {
                    // Ditch the progress dialog so that the new deposit box can appear instead.
                    Log.v(TAG, "Dismissing!");
                    mWaitingForDepositAlert.dismiss();
                }
            }

            super.onSuccess(result);
            updateValues();
        }

        @Override
        public void onDone() {
            // TB TODO - Kill waiting for depositalert!
        }

        @Override
        public void onUserConfirmNewBalance() {
            // Get out of the deposit screen now that the deposit is done...
            mActivity.finish();
        }
    }
}