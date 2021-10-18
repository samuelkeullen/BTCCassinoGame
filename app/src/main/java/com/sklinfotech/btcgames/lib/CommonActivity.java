package com.sklinfotech.btcgames.lib;

import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.FragmentActivity;


public class CommonActivity extends FragmentActivity {
  public Handler mHandler;
  static String TAG = "CommonActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mHandler = new Handler();
  }

  @Override
  public void onPause() {
    super.onPause();
    ((CommonApplication) this.getApplication()).abortNetAsyncTasks();
  }

  @Override
  public void onResume() {
    super.onResume();
  }
}