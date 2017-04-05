package cn.nuosi.andoroid.testdrawline.SelectableTextView;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.Layout;
import android.text.style.LineBackgroundSpan;
import android.widget.TextView;

/**
 * 指定文字单独添加不同颜色的下划线
 */
public class MyLineBackgroundSpan implements LineBackgroundSpan {

    private Paint mPaint;
    private TextView mTextView;
    private int start;
    private int end;

    public MyLineBackgroundSpan(TextView textView, int color, int start, int end) {
        this.mPaint = new Paint();
        this.mPaint.setColor(color);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(TextLayoutUtil.dp2px(textView.getContext(), 1.5f));
        this.mTextView = textView;
        this.start = start;
        this.end = end;
    }

    public void setSelect(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint p, int left, int right, int top, int baseline,
                               int bottom, CharSequence text, int start, int end, int lnum) {
        if (this.end > this.start) {
            // 获取该ClickableSpan的坐标
            Layout layout = mTextView.getLayout();
            int topLine = layout.getLineForOffset(this.start);
            int bottomLine = layout.getLineForOffset(this.end);
            Rect bound = new Rect();
            float yAxisBottom = bound.bottom;//字符底部y坐标
            float xAxisLeft = layout.getPrimaryHorizontal(this.start);//字符左边x坐标
            float xAxisRight = layout.getSecondaryHorizontal(this.end);//字符右边x坐标
            for (int i = topLine; i <= bottomLine; i++) {
                Path path = new Path();
                if (i == topLine) {
                    path.moveTo(layout.getLineLeft(i) + xAxisLeft, layout.getLineBottom(i));
                } else {
                    path.moveTo(layout.getLineLeft(i), layout.getLineBottom(i));
                }
                if (i == bottomLine) {
                    path.lineTo(xAxisRight, layout.getLineBottom(i));
                } else {
                    path.lineTo(layout.getLineRight(i), layout.getLineBottom(i));
                }
                canvas.drawPath(path, mPaint);
            }
        }
    }
}
