package com.sklinfotech.btcgames.leanplum;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class LeanplumConfig {

  private static final String TAG = LeanplumConfig.class.getSimpleName();
  private static final String LEANPLUM_FILE_NAME = "leanplum.properties";

  private String appId;
  private String prodKey;
  private String devKey;

  LeanplumConfig(final Context ctx) {
    try (final InputStream inputStream = ctx.getAssets().open(LEANPLUM_FILE_NAME)) {
      final Properties properties = new Properties();
      properties.load(inputStream);
      this.appId = properties.getProperty("leanplum.appId");
      this.prodKey = properties.getProperty("leanplum.prodKey");
      this.devKey = properties.getProperty("leanplum.devKey");
    } catch (IOException e) {
      Log.e(TAG, String.format("Cannot load %s file.", LEANPLUM_FILE_NAME), e);

      this.appId = "";
      this.prodKey = "";
      this.devKey = "";
    }
  }

  String getAppId() {
    return appId;
  }

  String getProdKey() {
    return prodKey;
  }

  String getDevKey() {
    return devKey;
  }
}