package com.dpower.util;

import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;

/**
 * 二维码工具类
 */
public class QRCodeUtils {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    /**
     * 创建二维码
     * @param content 二维码内容
     * @param widthPix 二维码的宽
     * @param heightPix 二维码的高
     * @param margin 白边大小，取值范围0~4
     * @return
     * @throws WriterException
     */
    public static Bitmap createQRCode(String content, int widthPix, int heightPix, int margin)
            throws WriterException {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        if (margin < 0 || margin > 4) {
            margin = 1;
        }
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();;
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        /* 二维码的纠错级别(排错率),4个级别：
		 L (7%)、
		 M (15%)、
		 Q (25%)、
		 H (30%)(最高H)
		 纠错信息同样存储在二维码中，纠错级别越高，纠错信息占用的空间越多，那么能存储的有用讯息就越少；共有四级；
		 选择M，扫描速度快。
		 */
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        // 二维码边界空白大小 0,1,2,3,4 (4为默认,最大)
        hints.put(EncodeHintType.MARGIN, margin);
        BitMatrix matrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, widthPix, heightPix, hints);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 二维码添加logo
     * @param QRCode 二维码图片
     * @param logo logo图片
     * @param logoMargin logo图片的外边框
     * @param isRoundCorner logo图片是否圆角(logo需要边框的情况下此参数才有效)
     * */
    public static Bitmap QRCodeAddLogo(Bitmap QRCode, Bitmap logo, int logoMargin, boolean isRoundCorner) {
        if (QRCode == null || logo == null) {
            return null;
        }
        int QRCodeWidth = QRCode.getWidth();
        int QRCodeHeight = QRCode.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();
        if (QRCodeWidth == 0 || QRCodeHeight == 0
                || logoWidth == 0 || logoHeight == 0) {
            return null;
        }
        if (logoMargin > 0) {
            if (isRoundCorner) {
                // 圆角logo
                Bitmap newLogo = Bitmap.createBitmap(logoWidth, logoHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(newLogo);
                Paint paint = new Paint();
                Rect rect = new Rect(0, 0, logoWidth, logoHeight);
                RectF rectF = new RectF(rect);
                paint.setAntiAlias(true);
                canvas.drawARGB(0, 0, 0, 0);
                paint.setColor(Color.WHITE);
                canvas.drawRoundRect(rectF, logoWidth / 6, logoHeight / 6, paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(logo, rect, rect, paint);
                canvas.save();
                canvas.restore();
                logo = newLogo;
                if (!newLogo.isRecycled()) {
                    newLogo.isRecycled();
                }
            }
            int newW = logoWidth + logoMargin * 2;
            int newH = logoHeight + logoMargin * 2;
            Bitmap newLogo = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newLogo);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            if (isRoundCorner) {
                // 圆角矩形
                paint.setStyle(Paint.Style.FILL);
                paint.setAntiAlias(true);// 设置画笔的锯齿效果
                canvas.drawRoundRect(new RectF(0, 0, newW, newH), newW / 6, newH / 6, paint);
            } else {
                // 普通矩形
                canvas.drawRect(new Rect(0, 0, newW, newH), paint);
            }
            canvas.drawBitmap(logo, logoMargin, logoMargin, null);
            canvas.save();
            canvas.restore();
            logo = newLogo;
            if (!newLogo.isRecycled()) {
                newLogo.isRecycled();
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(QRCodeWidth, QRCodeHeight, Bitmap.Config.ARGB_8888);
        // logo缩放
        float sx  = QRCodeWidth * 1.0f / 4 / logoWidth;
        float sy  = QRCodeHeight * 1.0f / 4 / logoHeight;
        // 居中
        int x = (QRCodeWidth - logoWidth) / 2;
        int y = (QRCodeHeight - logoHeight) / 2;
        float px = QRCodeWidth / 2;
        float py = QRCodeHeight / 2;
        // 右下角
//		int x = QRCodeWidth  - logoWidth - logoMargin;
//		int y = QRCodeHeight - logoHeight - logoMargin;
//        float px = QRCodeWidth - logoMargin;
//        float py = QRCodeHeight - logoMargin;
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(QRCode, 0, 0, null);
        canvas.scale(sx, sy, px, py);
        canvas.drawBitmap(logo, x, y, null);
        canvas.save();
        canvas.restore();
        return bitmap;
    }

    /**
     * 识别二维码
     * @param bitmap 图片
     * @return
     */
    public static Result scanImage(Bitmap bitmap) {
        if(bitmap == null){
            return null;
        }
        Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8"); //设置二维码内容的编码
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        try {
            return reader.decode(binaryBitmap, hints);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 识别二维码
     * @param path 图片路径
     * @return
     */
    public static Result scanImage(String path) {
        if(TextUtils.isEmpty(path)){
            return null;
        }
        Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8"); //设置二维码内容的编码

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, 400,
                400);
        options.inPurgeable = true;// 同时设置才会有效
        options.inInputShareable = true;// 当系统内存不够时候图片自动被回收
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        try {
            return reader.decode(binaryBitmap, hints);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static  int calculateInSampleSize(
            BitmapFactory.Options options, int viewWidth, int viewHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > viewHeight || width > viewWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > viewHeight
                    && (halfWidth / inSampleSize) > viewWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
