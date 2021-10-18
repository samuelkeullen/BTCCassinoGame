//
// TB TODO - Does not work for postDelayed it seems... They do not make it to handleMessage...
// 
package com.sklinfotech.btcgames.app;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Stack;

public class PausableHandler extends Handler {
  private Stack<Message> mMessageStack = new Stack<Message>();
  private boolean mIsPaused = false;

  public synchronized void pause() {
    mIsPaused = true;
  }

  public synchronized void resume() {
    mIsPaused = false;
    while (!mMessageStack.empty()) {
      sendMessageAtFrontOfQueue(mMessageStack.pop());
    }
  }

  @Override
  public void handleMessage(Message msg) {

    if (mIsPaused) {
      Log.v("handleMessage", "Delaying message...");
      mMessageStack.push(Message.obtain(msg));
      return;
    }

    // otherwise handle message as normal
    // ...
    Log.v("handleMessage", "Gonna handle this message!");
    super.handleMessage(msg);
  }

}
