package com.sklinfotech.btcgames.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.sklinfotech.btcgames.BuildConfig;
import com.sklinfotech.btcgames.R;
import com.sklinfotech.btcgames.lib.CreateAccountTask;
import com.sklinfotech.btcgames.settings.CurrencySettings;
import com.sklinfotech.btcgames.tasks.NetVerifyAccountTask;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

  CreateAccountTask mCreateAccountTask;
  NetVerifyAccountTask mNetVerifyAccountTask;
  ProgressDialog mVerifyAccountDialog;

  public void updateValues() {
    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

    if (CurrencySettings.getInstance(this).getAccountKey() == null) {
      findPreference("account_key").setSummary("ERRO: Chave da conta está NULL");
    } else {
      findPreference("account_key").setSummary(CurrencySettings.getInstance(this).getAccountKey());
    }

    if (sharedPref.getBoolean("sound_enable", true)) {
      findPreference("sound_enable").setSummary("O som está ligado");
    } else {
      findPreference("sound_enable").setSummary("O som está desligado");
    }
    findPreference("version").setSummary(BuildConfig.VERSION_NAME);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    final SettingsActivity that = this;
    super.onCreate(savedInstanceState);

    // TB - Doesn't seem to do anything for settings screens...?
        /*
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);  
        */
    addPreferencesFromResource(R.xml.preferences);

    mNetVerifyAccountTask = null;
    mVerifyAccountDialog = null;
    updateValues();

    Preference editAccount = (Preference) findPreference("account_key");
    editAccount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        // Check if it's valid
        final String accountKey = (String) newValue;

        // The account_key in preferences hasn't been set yet, so therefore any net calls at this point will still
        // use the old account key value.
        // So delay this a bit and then check
        // TB TODO - This is sloppy
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          public void run() {
            mVerifyAccountDialog = ProgressDialog.show(that, "", "Verifificando chave da conta...", true);
            mNetVerifyAccountTask = new NetVerifyAccountTask(that, accountKey, () -> {
              if (mVerifyAccountDialog != null) {
                mVerifyAccountDialog.dismiss();
                mVerifyAccountDialog = null;
              }
            });
            mNetVerifyAccountTask.execute(Long.valueOf(0));
          }
        }, 200);

        // Return false so that the preferences do not get set yet.
        // Then in NetVerifyAccountTask we update the key once it's been verified.
        return false;
      }
    });

    Preference newAccount = (Preference) findPreference("new_account");
    newAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      // TB TODO - Not sure why this @Override results in an error saying onPreferenceClick is not overridden...
      // @Override
      public boolean onPreferenceClick(Preference arg0) {
        //code for what you want it to do
        // Log.v("PREFERENCE", "CLICKED!!!");

        AlertDialog.Builder builder = new AlertDialog.Builder(that);
        builder.setMessage("Você tem certeza que deseja criar uma nova conta? Você vai perder o acesso a sua conta atual.")
            .setCancelable(false)
            .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
              }
            })
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                // TB TODO
                // Actually get the new account_key! Share code with MainActivity that also does this.
                // Be sure to reset the deposit_address in settings + the BitcoinGames instance!
                dialog.cancel();
                mCreateAccountTask = new CreateAccountTask(that);
                mCreateAccountTask.execute((long) 0);
              }
            });
        AlertDialog alert = builder.create();
        alert.show();

        return true;
      }
    });

    Preference viewSourceCode = (Preference) findPreference("view_source_code");
    viewSourceCode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      // TB TODO - Not sure why this @Override results in an error saying onPreferenceClick is not overridden...
      // @Override
      public boolean onPreferenceClick(Preference arg0) {
        Toast.makeText(that, "Coming soon!", Toast.LENGTH_SHORT).show();
        return true;
      }
    });

    Preference emailUs = (Preference) findPreference("email_us");
    emailUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      // TB TODO - Not sure why this @Override results in an error saying onPreferenceClick is not overridden...
      // @Override
      public boolean onPreferenceClick(Preference arg0) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        String aEmailList[] = {"sklinfotechnologies@gmail.com"};
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
        emailIntent.setType("plain/text");
        startActivity(emailIntent);
        return true;
      }
    });

    Preference importAccount = (Preference) findPreference("import_account");
    importAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      // TB TODO - Not sure why this @Override results in an error saying onPreferenceClick is not overridden...
      // @Override
      public boolean onPreferenceClick(Preference arg0) {

        // TB TODO - Integrate scanning library directly into project?
        // http://damianflannery.wordpress.com/2011/06/13/integrate-zxing-barcode-scanner-into-your-android-app-natively-using-eclipse/

        IntentIntegrator integrator = new IntentIntegrator(that);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
        return true;
      }
    });
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    Log.v("onActivityResult", "yo");
    //Activity.RESULT_OK;
    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
    boolean scanOK = false;
    if (result != null) {
      String contents = result.getContents();
      if (contents != null) {
        //showDialog(R.string.result_succeeded, result.toString());
        //Log.v("SCAN_RESULT", contents);
        scanOK = true;
        int header = contents.indexOf("account_key:");
        if (header >= 0) {
          String accountKey = contents.substring(header + "account_key:".length());
          CreateAccountTask.setAccountKeyInPreferences(this, accountKey);
          Toast.makeText(this, "Chave de conta importada!", Toast.LENGTH_SHORT).show();
          updateValues();
        } else {
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setMessage(String.format("Isso não é um QR code de account_key valido. Por favor vá a pagina Android em %s e escaneie o QR code listado abaixo \"IMPORT YOUR WEB ACCOUNT\".", CurrencySettings.getInstance(this).getServerName()))
              .setCancelable(false)
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                }
              });
          AlertDialog alert = builder.create();
          alert.show();
        }
      } else {
        //showDialog(R.string.result_failed, getString(R.string.result_failed_why));
      }
    }

    if (!scanOK) {
      Toast.makeText(this, "Erro ao escanear o QR code", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updateValues();
  }


}