package com.sklinfotech.btcgames.leanplum;

import android.content.Context;

import com.leanplum.Leanplum;

import static com.sklinfotech.btcgames.lib.BitcoinGames.RUN_ENVIRONMENT;
import static com.sklinfotech.btcgames.lib.BitcoinGames.RunEnvironment.PRODUCTION;

public class LeanplumService {

  private Context ctx;
  private LeanplumConfig leanplumConfig;

  private static LeanplumService instance;

  public static LeanplumService getInstance(final Context ctx) {
    if (instance == null) {
      synchronized (LeanplumService.class) {
        if (instance == null) {
          instance = new LeanplumService(ctx);
        }
      }
    }
    instance.ctx = ctx;
    return instance;
  }

  private LeanplumService(final Context ctx) {
    leanplumConfig = new LeanplumConfig(ctx);
  }

  public void initialize() {
    if (RUN_ENVIRONMENT == PRODUCTION) {
      Leanplum.setAppIdForProductionMode(leanplumConfig.getAppId(), leanplumConfig.getProdKey());
    } else {
      Leanplum.setAppIdForDevelopmentMode(leanplumConfig.getAppId(), leanplumConfig.getDevKey());
    }
    Leanplum.trackAllAppScreens();
    Leanplum.start(ctx);
  }
}
