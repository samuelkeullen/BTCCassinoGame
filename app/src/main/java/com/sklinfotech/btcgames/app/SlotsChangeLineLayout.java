package com.sklinfotech.btcgames.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class SlotsChangeLineLayout extends LinearLayout {

  public SlotsChangeLineLayout(Context context) {
    super(context);
  }

  public SlotsChangeLineLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SlotsChangeLineLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SlotsChangeLineLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  SlotsActivity slots;

  public void setSlots(SlotsActivity slots) {
    this.slots = slots;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    // do whatever you want with the event
    // and return true so that children don't receive it
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (!slots.canChangeLines()) {
        return true;
      }
      // Log.v(TAG, String.format("%d,%d    --  %f,%f", mLinesButton.getWidth(), mLinesButton.getHeight(), event.getX(), event.getY()) );
      int centerX = slots.mLinesButton.getWidth() / 2;
      if (event.getX() > centerX) {
        slots.mLines += 1;
        if (slots.mLines > SlotsActivity.MAX_LINES) {
          slots.mLines = SlotsActivity.MAX_LINES;
        }
      } else {
        slots.mLines -= 1;
        if (slots.mLines < 1) {
          slots.mLines = 1;
        }
      }

      slots.handleLinesChanged();
    }
    return true;
  }
}
