package cn.nuosi.andoroid.testdrawline.SelectableTextView;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.style.LineBackgroundSpan;
import android.widget.TextView;

/**
 * 自定义带分割线的背景
 */
public class MyBackgroundSpan implements LineBackgroundSpan {

    private Paint mPaint = new Paint();
    /**
     * Rectangle used for drawing background.
     */
    private final RectF mRectangle = new RectF();
    private TextView mTextView;
    private int mStart;
    private int mEnd;
    private int mPadding;//textview的pading值
    private int mRadius;//圆角的半径

    public MyBackgroundSpan(TextView textView, int color, int start, int end) {
        this.mTextView = textView;
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(color);
        this.mStart = start;
        this.mEnd = end;
    }

    public MyBackgroundSpan(int color, int start, int end) {
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(color);
        this.mStart = start;
        this.mEnd = end;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint p, int left, int right, int top, int baseline,
                               int bottom, CharSequence text, int start, int end, int lnum) {
        if (mEnd > mStart) {

            final CharSequence part = text.subSequence(start, end);
            final int trimmedLength = TextUtils.getTrimmedLength(part);
            final String trimmedText = part.toString().trim();
            // skip empty parts
            if (TextUtils.isEmpty(trimmedText)) {
                return;
            }
            // do not add background to lines that ends with spaces
            if (trimmedLength != part.length()) {
                final int trimmedLengthStart = getTrimmedLengthStart(part);
                final int trimmedLengthEnd = getTrimmedLengthEnd(part, trimmedLengthStart);
                start = start + trimmedLengthStart;
                end = end - trimmedLengthEnd;
            }
            final int startInText = start < mStart ? mStart : start;
            final int endInText = end > mEnd ? mEnd : end;
            // skip empty parts
            if (startInText >= endInText) {
                return;
            }
            float l = p.measureText(text, start, startInText);
            float r = l + p.measureText(text, startInText, endInText);
            float t = top;
            float b = baseline + p.descent() + mPadding;
            mRectangle.set(l - mPadding, t - mPadding, r + mPadding, b + mPadding);
            canvas.drawRoundRect(mRectangle, mRadius, mRadius, mPaint);
        }
    }

    /**
     * Get number of space characters from the beginning of text.
     *
     * @param text any text
     * @return number of space characters from text beginning
     */
    private int getTrimmedLengthStart(@NonNull CharSequence text) {
        int len = text.length();

        int start = 0;
        while (start < len && text.charAt(start) <= ' ') {
            start++;
        }
        return start;
    }

    /**
     * Get number of space characters from the end of text.
     *
     * @param text  any text
     * @param start number of space characters from beginning of text
     * @return number of space characters from the end of text
     */
    private int getTrimmedLengthEnd(@NonNull CharSequence text, int start) {
        int end = text.length();
        while (end > start && text.charAt(end - 1) <= ' ') {
            end--;
        }

        return text.length() - end;
    }
}