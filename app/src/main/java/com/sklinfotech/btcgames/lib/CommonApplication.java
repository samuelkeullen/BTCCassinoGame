package com.sklinfotech.btcgames.lib;

import android.util.Log;

import com.sklinfotech.btcgames.leanplum.LeanplumService;
import com.leanplum.LeanplumApplication;

import java.util.ArrayList;
import java.util.List;

public class CommonApplication extends LeanplumApplication {

  private static final String TAG = "CommonApplication";
  private static List<NetAsyncTask> mPendingTasks = new ArrayList<>();

  @Override
  public void onCreate() {
    super.onCreate();
    LeanplumService.getInstance(this).initialize();
  }

  public void abortNetAsyncTasks() {
    for (NetAsyncTask task : mPendingTasks) {
      Log.v(TAG, "ABORTING TASK!");
      if (task != null) {
        task.abort();
      }
    }
    mPendingTasks.clear();
  }

  public void addNetAsyncTask(NetAsyncTask task) {
    mPendingTasks.add(task);
  }

  public void removeNetAsyncTask(NetAsyncTask task) {
    boolean found = mPendingTasks.remove(task);
    if (!found) {
      Log.e(TAG, "Trying to remove net async task that's not in the list");
    }
  }

}
