package com.sklinfotech.btcgames.lib;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QrCode {
  public static Bitmap encodeAsBitmap(String str) throws WriterException {
    int white = 0xFFFFFFFF;
    int black = 0xFF000000;
    int WIDTH = 500;
    BitMatrix result;
    Bitmap bitmap = null;
    try {
      result = new MultiFormatWriter().encode(str,
          BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);

      int w = result.getWidth();
      int h = result.getHeight();
      int[] pixels = new int[w * h];
      for (int y = 0; y < h; y++) {
        int offset = y * w;
        for (int x = 0; x < w; x++) {
          pixels[offset + x] = result.get(x, y) ? black : white;
        }
      }
      bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, 500, 0, 0, w, h);
    } catch (Exception iae) {
      iae.printStackTrace();
      return null;
    }
    return bitmap;
  }
}