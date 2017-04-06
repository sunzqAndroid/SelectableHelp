package cn.nuosi.andoroid.testdrawline.SelectableTextView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SparseArrayCompat;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.nuosi.andoroid.testdrawline.DrawLineApplication;
import cn.nuosi.andoroid.testdrawline.FlaotActivity;
import cn.nuosi.andoroid.testdrawline.GreenDaoManager;
import cn.nuosi.andoroid.testdrawline.R;
import cn.nuosi.andoroid.testdrawline.dao.Book;
import cn.nuosi.andoroid.testdrawline.greendao.gen.BookDao;
import cn.nuosi.andoroid.testdrawline.info.BookInfo;

/**
 * Created by Elder on 2017/3/9.
 * 创建可选文本控件的辅助类
 */

public class SelectableTextHelper {

    private static int DEFAULT_SELECTION_LENGTH = 1;
    private static final int DEFAULT_SHOW_DURATION = 100;

    private Context mContext;
    private TextView mTextView;
    /**
     * 保存该书所有标记的集合
     */
    private List<Book> mBookList;

    private BookInfo mBookInfo;
    /**
     * 自定义菜单的布局ID
     */
    private int menuId;
    /**
     * 可变文本的接口非常强大
     */
    private Spannable mSpannable;
    /**
     * 改变文本背景色
     */
    private BackgroundColorSpan mBgSpan;
    /**
     * 比HashMap<Integer,Object>更高效
     */
    private SparseArrayCompat<MyClickableSpan> clickSpanMap;
    /**
     * 比HashMap<Integer,Object>更高效
     */
    private SparseArrayCompat<BackgroundColorSpan> bgSpanMap;

    private int mTouchX;
    private int mTouchY;
    private int[] mLocation = new int[2];

    private int mSelectedColor;
    private int mCursorHandleColor;
    private int mCursorHandleSize;
    /**
     * 记录每次选中后的信息实体类
     */
    private SelectionInfo mSelectionInfo = new SelectionInfo();
    private OnSelectListener mSelectListener;
    /**
     * 弹出Menu窗口
     */
    private OperateWindow mOperateWindow;
    /**
     * 两个选择游标
     */
    private CursorHandle mStartHandle;
    private CursorHandle mEndHandle;
    /**
     * 滚动界面时隐藏选中状态的标志
     */
    private boolean isHideWhenScroll;
    private boolean isHide = true;
    private int length;

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    /**
     * 滑动状态改变时的监听器
     */
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    private int top;
    private int bottom;

    public void setTop(int top) {
        this.top = top;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    private SelectableTextHelper(Builder builder) {
        mTextView = builder.mTextView;
        mContext = mTextView.getContext();
        mSelectedColor = builder.mSelectedColor;
        mCursorHandleColor = builder.mCursorHandleColor;
        mCursorHandleSize = TextLayoutUtil.dp2px(mContext, builder.mCursorHandleSizeInDp);
        menuId = builder.menuId;
        mBookInfo = builder.mBookInfo;
        mBookList = mBookInfo.mBookList;
        init();
    }

    private void init() {
        length = mTextView.getText().length();
        // 由于 TextView 的文本的 BufferType 类型；
        // 是 SPANNABLE 时才可以设置 Span ，实现选中的效果；
        mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);
        // 初始化保存标记对象的集合
        clickSpanMap = new SparseArrayCompat<>();
        bgSpanMap = new SparseArrayCompat<>();
        // 将数据库中的标记全部载入到当前TextView中
        if (mBookList != null) {
            if (mTextView.getText() instanceof Spannable) {
                mSpannable = (Spannable) mTextView.getText();
            }
            for (final Book bean : mBookList) {
                if (length > bean.getStart()) {
                    if (bean.getEnd() >= length) {
                        bean.setEnd(length - 1);
                    }
                    TextPaint textPaint = getPaint(new TextPaint(
                            new Paint(Paint.ANTI_ALIAS_FLAG)), bean.getColor());
                    MyClickableSpan clickSpan = new MyClickableSpan(textPaint) {
                        @Override
                        public void onClick(View widget) {
                            clickSelectSpan(bean.getStart(), bean.getEnd());
                        }
                    };
                    clickSpanMap.append(bean.getStart(), clickSpan);
                    mSpannable.setSpan(clickSpan, bean.getStart(), bean.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    BackgroundColorSpan bgSpan = new BackgroundColorSpan(bean.getColor());
                    bgSpanMap.append(bean.getStart(), bgSpan);
                    mSpannable.setSpan(bgSpan, bean.getStart(), bean.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mTextView.setText(mSpannable);
                } else {
                    //TODO 删除标注
                }
            }
            mTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        mTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSelectView(mTouchX, mTouchY);
                return true;
            }
        });

        mTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTouchX = (int) event.getX();
                mTouchY = (int) event.getY();
                return false;
            }
        });

        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 当TextView有可点击部分时将屏蔽TextView的单击事件
                if (mTextView.getSelectionStart() == -1 && mTextView.getSelectionEnd() == -1) {
                    resetSelectionInfo();
                    hideSelectView();
                }
            }
        });
        // 设置当前TextView关联状态变化时的监听
        mTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                destroy();
            }
        });

        mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                if (isHideWhenScroll) {
                    isHideWhenScroll = false;
                    postShowSelectView(DEFAULT_SHOW_DURATION);
                }
                return true;
            }
        };
        mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);

        mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (!isHideWhenScroll && !isHide) {
                    isHideWhenScroll = true;
                    if (mOperateWindow != null) {
                        mOperateWindow.dismiss();
                    }
                    if (mStartHandle != null) {
                        mStartHandle.dismiss();
                    }
                    if (mEndHandle != null) {
                        mEndHandle.dismiss();
                    }
                }
            }
        };
        mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);

        mOperateWindow = new OperateWindow(mContext, menuId);
    }

    /**
     * 标注信息更新后，重新加载并绘制标注
     *
     * @param bookInfo
     */
    public void updateBookInfo(BookInfo bookInfo) {
        for (Book book : mBookList) {
            MyClickableSpan mClickableSpan = clickSpanMap.get(book.getStart());
            BackgroundColorSpan mbgSpan = bgSpanMap.get(book.getStart());
            mSpannable.removeSpan(mClickableSpan);
            mSpannable.removeSpan(mbgSpan);
            clickSpanMap.delete(mTextView.getSelectionStart());
            bgSpanMap.delete(mTextView.getSelectionStart());
            mTextView.setText(mSpannable);
        }
        mBookList.clear();
        destroy();
        mBookInfo = bookInfo;
        mBookList = mBookInfo.mBookList;
        init();
    }

    /**
     * 改变字体大小后，刷新标注内容的字体大小
     */
    public void changeTextSize() {
        for (Book book : mBookList) {
            MyClickableSpan mClickableSpan = clickSpanMap.get(book.getStart());
            TextPaint textPaint = getPaint(new TextPaint(
                    new Paint(Paint.ANTI_ALIAS_FLAG)), book.getColor());
            mClickableSpan.setTextPaint(textPaint);
        }
        mTextView.setText(mSpannable);
    }

    /**
     * 延迟显示的方法
     *
     * @param duration:设置延迟的时间
     */
    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    /**
     * TextView显示时候的回调
     */
    private final Runnable mShowSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHide) return;
            if (mOperateWindow != null) {
                mOperateWindow.show();
            }
            if (mStartHandle != null) {
                showCursorHandle(mStartHandle);
            }
            if (mEndHandle != null) {
                showCursorHandle(mEndHandle);
            }
        }
    };

    /**
     * 隐藏选中状态的View
     */
    private void hideSelectView() {
        isHide = true;
        if (mStartHandle != null) {
            mStartHandle.dismiss();
        }
        if (mEndHandle != null) {
            mEndHandle.dismiss();
        }
        if (mOperateWindow != null) {
            mOperateWindow.dismiss();
        }
    }

    /**
     * 重置选择状态
     */
    private void resetSelectionInfo() {
        if (mSpannable != null && mBgSpan != null) {
            // 移除背景色
            mSpannable.removeSpan(mBgSpan);
            mBgSpan = null;
        }
    }

    /**
     * 自定义画笔调用的方法
     *
     * @param paint:传入定义好的画笔对象
     * @param color：画笔颜色
     * @return 返回定义好的TextPaint对象
     */
    @NonNull
    private TextPaint getPaint(TextPaint paint, int color) {
        WeakReference<TextPaint> mTextPaint = new WeakReference<>(paint);
        mTextPaint.get().setTextSize(mTextView.getTextSize());
        mTextPaint.get().setColor(mTextView.getContext().getResources().getColor(R.color.text_dark));
        mTextPaint.get().setTypeface(DrawLineApplication.mTypeface);
        return mTextPaint.get();
    }

    /**
     * 显示选中文本时的效果
     *
     * @param x:触碰屏幕时的X坐标
     * @param y:触碰屏幕时的Y坐标
     */
    private void showSelectView(int x, int y) {
        // 重置上一次选中的状态
        hideSelectView();
        resetSelectionInfo();
        isHide = false;
        // 新建左右游标
        if (mStartHandle == null) mStartHandle = new CursorHandle(true);
        if (mEndHandle == null) mEndHandle = new CursorHandle(false);
        // 默认选择一个字符
        int startOffset = TextLayoutUtil.getPreciseOffset(mTextView, x, y);
        int endOffset = startOffset + DEFAULT_SELECTION_LENGTH;

        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        // 边界异常处理
        if (mSpannable == null || startOffset >= mTextView.getText().length()) {
            return;
        }
        selectText(startOffset, endOffset);
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
        mOperateWindow.show();
        // 恢复初始值
        DEFAULT_SELECTION_LENGTH = 1;
    }

    /**
     * 选中文本的方法
     *
     * @param startPos:选择文本时的首字索引值
     * @param endPos:选择文本时的尾字索引值
     */
    private void selectText(int startPos, int endPos) {
        if (startPos != -1) {
            mSelectionInfo.setStart(startPos);
        }
        if (endPos != -1) {
            mSelectionInfo.setEnd(endPos);
        }
        if (mSelectionInfo.getStart() > mSelectionInfo.getEnd()) {
            int temp = mSelectionInfo.getStart();
            mSelectionInfo.setStart(mSelectionInfo.getEnd());
            mSelectionInfo.setEnd(temp);
        }

        if (mSpannable != null) {
            if (mBgSpan == null) {
                mBgSpan = new BackgroundColorSpan(mSelectedColor);
            }
            // 截取选中状态的文本
            mSelectionInfo.setSelectionContent(
                    mSpannable.subSequence(
                            mSelectionInfo.getStart(),
                            mSelectionInfo.getEnd()).toString());
            mSpannable.setSpan(mBgSpan,
                    mSelectionInfo.getStart(), mSelectionInfo.getEnd(),
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            // 设置监听选中接口回调时选择到的文字
            if (mSelectListener != null) {
                mSelectListener.onTextSelected(mSelectionInfo.getSelectionContent());
            }
        }
    }

    /**
     * 实现画线的方法
     */
    private void showUnderLine(final TextPaint paint, int bgColor) {
        // 将划线颜色信息保存到SelectionInfo中
        mSelectionInfo.setColor(bgColor);
        MyClickableSpan mClickableSpan;
        if (mSpannable != null) {
            List<Book> delList = new ArrayList<>();
            for (Book temp : mBookList) {
                if (mSelectionInfo.getStart() >= temp.getStart() && mSelectionInfo.getStart() < temp.getEnd()) {
                    mSelectionInfo.setStart(temp.getStart());
                    if (!TextUtils.isEmpty(temp.getNote())) {
                        mSelectionInfo.setNoteContent((TextUtils.isEmpty(mSelectionInfo.getNoteContent()) ? "" : mSelectionInfo.getNoteContent() + "\n") + temp.getNote());
                    }
                    if (mSelectionInfo.getEnd() <= temp.getEnd()) {
                        mSelectionInfo.setEnd(temp.getEnd());
                    }
                    delList.add(temp);
                } else if (mSelectionInfo.getStart() <= temp.getStart() && mSelectionInfo.getEnd() >= temp.getStart()) {
                    if (!TextUtils.isEmpty(temp.getNote())) {
                        mSelectionInfo.setNoteContent((TextUtils.isEmpty(mSelectionInfo.getNoteContent()) ? "" : mSelectionInfo.getNoteContent() + "\n") + temp.getNote());
                    }
                    if (mSelectionInfo.getEnd() <= temp.getEnd()) {
                        mSelectionInfo.setEnd(temp.getEnd());
                    }
                    delList.add(temp);
                }
            }
            for (Book temp : delList) {
                if (temp.getStart() != mSelectionInfo.getStart()) {
                    delUnderline(temp);
                }
            }
            if (delList.size() > 0) {
                // 重新截取选中状态的文本
                mSelectionInfo.setSelectionContent(
                        mSpannable.subSequence(
                                mSelectionInfo.getStart(),
                                mSelectionInfo.getEnd()).toString());
            }
            if (clickSpanMap.get(this.mSelectionInfo.getStart()) != null) {
                mClickableSpan = clickSpanMap.get(this.mSelectionInfo.getStart());
                mClickableSpan.setTextPaint(paint);
                BackgroundColorSpan mbgSpan = bgSpanMap.get(mSelectionInfo.getStart());
                mSpannable.removeSpan(mbgSpan);
                bgSpanMap.delete(mSelectionInfo.getStart());
                // 更新标记的方法
                updateNote();
            } else {
                mClickableSpan = new MyClickableSpan(paint) {
                    @Override
                    public void onClick(View widget) {
                        clickSelectSpan(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
                    }
                };
                // 将选中状态的信息保存到MyClickableSpan中
                mClickableSpan.setInfo(this.mSelectionInfo);
                // 添加到ClickSpan集合中
                clickSpanMap.append(this.mSelectionInfo.getStart(), mClickableSpan);
                // 将标记信息存入到数据库中
                saveNote(this.mSelectionInfo);
            }
            // 设置点击部分
            mSpannable.setSpan(mClickableSpan,
                    this.mSelectionInfo.getStart(), this.mSelectionInfo.getEnd(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            //设置背景色
            BackgroundColorSpan bgSpan = new BackgroundColorSpan(mSelectionInfo.getColor());
            // 添加到bgSpanMap集合中
            bgSpanMap.append(mSelectionInfo.getStart(), bgSpan);
            //设置点击部分的背景色
            mSpannable.setSpan(bgSpan,
                    mSelectionInfo.getStart(), mSelectionInfo.getEnd(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTextView.setMovementMethod(LinkMovementMethod.getInstance());
            // Refresh
            mTextView.setText(mSpannable);
        }
    }

    /**
     * 更新标记信息的方法
     */
    private void updateNote() {
        BookDao dao = GreenDaoManager.getInstance().getSession().getBookDao();
        int index = mSelectionInfo.getStart();
        Book mDelBook = getBook(index);
        if (mDelBook != null) {
            // 删除原来颜色的标注对象
            mBookList.remove(mDelBook);
            // 将更新后的实体类对象存入集合中
            mDelBook.setColor(mSelectionInfo.getColor());
            mDelBook.setEnd(mSelectionInfo.getEnd());
            if (!TextUtils.isEmpty(mSelectionInfo.getNoteContent())) {
                mDelBook.setNote(mSelectionInfo.getNoteContent());
                mSelectionInfo.setNoteContent("");
            }
            mDelBook.setContent(mSelectionInfo.getSelectionContent());
            mBookList.add(mDelBook);
            //拷贝一份标注信息，存入数据库，这样避免影响当前缓存中的标注信息
            Book newBook = mDelBook.copy();
            newBook.setStart(newBook.getStart() + mBookInfo.startX);
            newBook.setEnd(newBook.getEnd() + mBookInfo.startX);
            // 更新数据库中的信息
            dao.update(newBook);
        }
    }

    /**
     * 给定首字符的索引值返回指定的标注对象
     *
     * @param index:给定查找时的首字索引值
     * @return 返回指定的查找对象
     */
    private Book getBook(int index) {
        for (Book bean : mBookList) {
            if (bean.getStart() == index) {
                return bean;
            }
        }
        return null;
    }

    /**
     * 将标记信息存储到数据库中的方法
     *
     * @param info:当前需要保持的SelectionInfo对象
     */
    private void saveNote(SelectionInfo info) {
        BookDao dao = GreenDaoManager.getInstance().getSession().getBookDao();
        Book book = new Book();
        book.setColor(info.getColor());
        book.setStart(info.getStart() + mBookInfo.startX);
        book.setEnd(info.getEnd() + mBookInfo.startX);
        if (!TextUtils.isEmpty(mSelectionInfo.getNoteContent())) {
            book.setNote(mSelectionInfo.getNoteContent());
            mSelectionInfo.setNoteContent("");
        }
        book.setContent(info.getSelectionContent());
        Book newBook = book.copy();
        newBook.setId(dao.insert(book));
        newBook.setStart(newBook.getStart() - mBookInfo.startX);
        newBook.setEnd(newBook.getEnd() - mBookInfo.startX);
        // 存放在内存的集合中
        mBookList.add(newBook);
    }

    /**
     * 删除数据库中数据的方法
     */
    private void delNote() {
        BookDao dao = GreenDaoManager.getInstance().getSession().getBookDao();
        int index = mSelectionInfo.getStart();
        Book mDelBook = getBook(index);
        if (mDelBook != null) {
            mBookList.remove(mDelBook);
            mDelBook.setStart(mDelBook.getStart() + mBookInfo.startX);
            dao.delete(mDelBook);
        }
    }


    /**
     * 点击画线区域时调用的方法
     */
    private void clickSelectSpan(int start, int end) {
        // 设置TextView高亮部分背景颜色为透明
        mTextView.setHighlightColor(ContextCompat.getColor(mContext,
                android.R.color.transparent));
        // 将点击部分的信息保存到SelectionInfo中
        mSelectionInfo.setStart(start);
        mSelectionInfo.setEnd(end);
        mSelectionInfo.setSelectionContent(mTextView.getText().toString()
                .substring(start, end));
        // 弹出菜单
        isHide = false;
        mOperateWindow.setDel(true);
        // 获取该ClickableSpan的坐标
        Layout layout = mTextView.getLayout();
        int line = layout.getLineForOffset(start);
        // 得到该字符的X坐标
        int offsetX = (int) layout.getPrimaryHorizontal(start);
        // 得到该字符的矩形区域
        Rect rect = new Rect();
        layout.getLineBounds(line, rect);
        // 得到该字符的Y坐标
        int offsetY = rect.top;
        DEFAULT_SELECTION_LENGTH = end - start;
        showSelectView(offsetX, offsetY);
    }

    /**
     * 删除下划线的方法
     */
    private void delUnderline() {
        MyClickableSpan mClickableSpan = clickSpanMap.get(mTextView.getSelectionStart());
        BackgroundColorSpan mbgSpan = bgSpanMap.get(mTextView.getSelectionStart());
        hideSelectView();
        resetSelectionInfo();
        mSpannable.removeSpan(mClickableSpan);
        mSpannable.removeSpan(mbgSpan);
        clickSpanMap.delete(mTextView.getSelectionStart());
        bgSpanMap.delete(mTextView.getSelectionStart());
        mTextView.setText(mSpannable);
        // 从数据库中删除数据
        delNote();
    }

    /**
     * 删除下划线的方法
     */
    private void delUnderline(Book book) {
        MyClickableSpan mClickableSpan = clickSpanMap.get(book.getStart());
        BackgroundColorSpan mbgSpan = bgSpanMap.get(book.getStart());
        mSpannable.removeSpan(mClickableSpan);
        mSpannable.removeSpan(mbgSpan);
        clickSpanMap.delete(book.getStart());
        bgSpanMap.delete(book.getStart());
        mTextView.setText(mSpannable);
        // 从数据库中删除数据
        BookDao dao = GreenDaoManager.getInstance().getSession().getBookDao();
        mBookList.remove(book);
        book.setStart(book.getStart() + mBookInfo.startX);
        dao.delete(book);
    }

    /**
     * 显示游标的方法
     *
     * @param cursorHandle:需要显示的游标对象
     */
    private void showCursorHandle(CursorHandle cursorHandle) {
        Layout layout = mTextView.getLayout();
        int offset = cursorHandle.isLeft ? mSelectionInfo.getStart() : mSelectionInfo.getEnd();
        cursorHandle.show((int) layout.getPrimaryHorizontal(offset),
                layout.getLineBottom(layout.getLineForOffset(offset)));
    }

    /**
     * 设置外部调用的监听接口
     *
     * @param selectListener:供外部传入的监听接口对象
     */
    public void setSelectListener(OnSelectListener selectListener) {
        mSelectListener = selectListener;
    }

    /**
     * 当前TextView销毁时释放资源
     */
    private void destroy() {
        // 释放监听器
        mTextView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        // 取消显示状态
        resetSelectionInfo();
        hideSelectView();
        mStartHandle = null;
        mEndHandle = null;
        mOperateWindow = null;

        if (clickSpanMap != null) {
            clickSpanMap.clear();
            clickSpanMap = null;
        }
        if (bgSpanMap != null) {
            bgSpanMap.clear();
            bgSpanMap = null;
        }
    }

    /**
     * 构建者类用于初始化SelectableTextHelper类
     */
    public static class Builder {
        private TextView mTextView;
        private int mCursorHandleColor = 0xFF1379D6;
        private int mSelectedColor = 0xFFAFE1F4;
        private float mCursorHandleSizeInDp = 24;
        private int menuId;
        private BookInfo mBookInfo;

        public Builder(TextView textView) {
            mTextView = textView;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }

        public Builder setPopMenu(int layoutId) {
            menuId = layoutId;
            return this;
        }

        public Builder setBookInfo(BookInfo bookInfo) {
            mBookInfo = bookInfo;
            return this;
        }

        public SelectableTextHelper build() {
            return new SelectableTextHelper(this);
        }
    }

    /**
     * Operate windows : copy, select all
     */
    private class OperateWindow {

        private PopupWindow mWindow;
        private TextView mDelTv;

        private int mWidth;
        private int mHeight;

        private OperateWindow(final Context context, final int menuId) {
            // 解析弹出的菜单
            final View contentView = LayoutInflater.from(context).inflate(menuId, null);
            contentView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            mWidth = contentView.getMeasuredWidth();
            mHeight = contentView.getMeasuredHeight();
            // 通过PopWindow弹出
            mWindow = new PopupWindow(contentView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, false);
            mWindow.setClippingEnabled(false);

            contentView.findViewById(R.id.tv_copy).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 获取剪贴板管理器
                    ClipboardManager clip = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    // 设置剪贴板内容
                    clip.setPrimaryClip(ClipData.newPlainText(mSelectionInfo.getSelectionContent(), mSelectionInfo.getSelectionContent()));
                    if (mSelectListener != null) {
                        mSelectListener.onTextSelected(mSelectionInfo.getSelectionContent());
                    }
                    // 取消选中状态
                    SelectableTextHelper.this.resetSelectionInfo();
                    SelectableTextHelper.this.hideSelectView();
                }
            });
            contentView.findViewById(R.id.tv_select_all).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideSelectView();
                    selectText(0, mTextView.getText().length());
                    isHide = false;
                    showCursorHandle(mStartHandle);
                    showCursorHandle(mEndHandle);
                    mOperateWindow.show();
                }
            });
            // 设置笔记
            contentView.findViewById(R.id.tv_note).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 直接选择记笔记时默认设置划线
                    hideSelectView();
                    resetSelectionInfo();
                    Book book = getBook(mSelectionInfo.getStart());
                    int color = Color.RED;
                    if (book != null) {
                        color = book.getColor();
                    }
                    TextPaint mTextPaint = getPaint(new TextPaint(
                            new Paint(Paint.ANTI_ALIAS_FLAG)), color);
                    showUnderLine(mTextPaint, color);
                    if (book == null) {
                        book = getBook(mSelectionInfo.getStart());
                    }
                    Book newBook = book.copy();
                    newBook.setId(book.getId());
                    newBook.setStart(newBook.getStart() + mBookInfo.startX);
                    newBook.setEnd(newBook.getEnd() + mBookInfo.startX);
                    // 跳转完成记笔记的功能
                    Intent intent = new Intent(mContext, FlaotActivity.class);
                    intent.putExtra("book", newBook);
                    mContext.startActivity(intent);
                }
            });
            // 设置红色下划线
            contentView.findViewById(R.id.red_color).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideSelectView();
                    resetSelectionInfo();
                    TextPaint mTextPaint = getPaint(new TextPaint(
                            new Paint(Paint.ANTI_ALIAS_FLAG)), Color.RED);
                    showUnderLine(mTextPaint, Color.RED);
                }
            });
            // 设置蓝色下划线
            contentView.findViewById(R.id.blue_color).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideSelectView();
                    resetSelectionInfo();
                    TextPaint mTextPaint = getPaint(new TextPaint(
                            new Paint(Paint.ANTI_ALIAS_FLAG)), Color.BLUE);
                    showUnderLine(mTextPaint, Color.BLUE);
                }
            });
            // 删除下划线逻辑部分
            mDelTv = (TextView) contentView.findViewById(R.id.selectable_delete);
            mDelTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delUnderline();
                    // 设置删除按钮无效
                    setDel(false);
                }
            });
        }


        /**
         * 显示弹窗的方法
         */
        private void show() {
            // 获取在当前窗口内的绝对坐标
            mTextView.getLocationInWindow(mLocation);
            // 定位弹窗位置
            Layout layout = mTextView.getLayout();
            // 得到当前字符段的左边X坐标+Y坐标
            int posX = (int) layout.getPrimaryHorizontal(mSelectionInfo.getStart());
            int posY = layout.getLineTop(layout.getLineForOffset(
                    mSelectionInfo.getStart())) + mLocation[1];
            // 设置边界值
            if (posX <= 0) posX = 16;

            if (posY < top) {//被选中的顶部滑出了上边界
                posY = layout.getLineBottom(layout.getLineForOffset(
                        mSelectionInfo.getEnd())) + mLocation[1];
                if (posY < top) {//被选中的底部出了上边界
                    return;
                }
                posY = posY + 16;
            } else if (posY > bottom) {//被选中的顶部滑出了下边界
                return;
            } else {
                posY = posY - mHeight - 16;
                if (posY < top) {//操作框超出上边界
                    posY = layout.getLineBottom(layout.getLineForOffset(
                            mSelectionInfo.getEnd())) + mLocation[1];
                    if (posY < top) {//被选中的底部出了上边界
                        return;
                    }
                    posY = posY + 16;
                }
            }

            if ((posX + mWidth) > TextLayoutUtil.getScreenWidth(mContext)) {
                posX = TextLayoutUtil.getScreenWidth(mContext) - mWidth - 16;
            }
            if (posY > TextLayoutUtil.getScreenHeight(mContext) - mHeight) {
                posY = TextLayoutUtil.getScreenHeight(mContext) / 2 - mHeight / 2;
                posX = TextLayoutUtil.getScreenWidth(mContext) / 2 - mWidth / 2;
            }
            // 设置阴影效果
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mWindow.setElevation(8f);
            }
            mWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, posX, posY);
        }

        private void dismiss() {
            mWindow.dismiss();
        }

        public boolean isShowing() {
            return mWindow.isShowing();
        }

        /**
         * 设置弹窗菜单是否能够使用删除按钮
         *
         * @param del:是否显示删除按钮的Boolean值变量
         */
        private void setDel(boolean del) {
            mDelTv.setEnabled(del);
        }
    }

    /**
     * 选中文本时的两端游标View
     */
    private class CursorHandle extends View {

        private Paint mPaint;
        private PopupWindow mPopupWindow;

        private int mCircleRadius = mCursorHandleSize / 2;
        private int mWidth = mCircleRadius * 2;
        private int mHeight = mCircleRadius * 2;
        private int mPadding = 25;

        private boolean isLeft;

        private CursorHandle(boolean isLeft) {
            super(mContext);

            this.isLeft = isLeft;

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(mCursorHandleColor);
            // popWindow用于显示弹出菜单
            mPopupWindow = new PopupWindow(this);
            // 不允许PopWindow超出屏幕范围
            mPopupWindow.setClippingEnabled(false);
            mPopupWindow.setWidth(mWidth + mPadding * 2);
            mPopupWindow.setHeight(mHeight + mPadding / 2);

            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // 仿照原生游标图形 圆形+方块
            canvas.drawCircle(mPadding + mCircleRadius, mCircleRadius, mCircleRadius, mPaint);
            if (isLeft) {
                canvas.drawRect(mCircleRadius + mPadding, 0, mCircleRadius * 2 + mPadding, mCircleRadius, mPaint);
            } else {
                canvas.drawRect(mPadding, 0, mPadding + mCircleRadius, mCircleRadius, mPaint);
            }
        }

        /**
         * 记录触摸View时的坐标
         */
        private int mAdjustX;
        private int mAdjustY;

        private int mBeforeDragStart;
        private int mBeforeDragEnd;

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mBeforeDragStart = mSelectionInfo.getStart();
                    mBeforeDragEnd = mSelectionInfo.getEnd();
                    // 返回相对于当前View的相对坐标
                    mAdjustX = (int) event.getX();
                    mAdjustY = (int) event.getY();
                    break;
                // 拖拽触摸结束后显示弹出菜单
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mOperateWindow.show();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mOperateWindow.dismiss();
                    // 返回相对于屏幕的绝对坐标
                    int rawX = (int) event.getRawX();
                    int rawY = (int) event.getRawY();
                    update(rawX + mAdjustX - mWidth, rawY + mAdjustY - mHeight);
                    break;
            }
            return true;
        }

        /**
         * 随着触摸移动不断更新选中状态
         *
         * @param x:移动手指时的X坐标
         * @param y:移动手指时的Y坐标
         */
        private void update(int x, int y) {
            mTextView.getLocationInWindow(mLocation);
            int oldOffset;

            if (isLeft) {
                oldOffset = mSelectionInfo.getStart();
            } else {
                oldOffset = mSelectionInfo.getEnd();
            }

            y -= mLocation[1];

            int offset = TextLayoutUtil.getHysteresisOffset(mTextView, x, y, oldOffset);

            if (offset != oldOffset) {
                resetSelectionInfo();
                if (isLeft) {
                    // 处理如果出现用户将左边游标拖动到右边游标之后的调转情况
                    if (offset > mBeforeDragEnd) {
                        CursorHandle cursorHandle = getCursorHandle(false);
                        // 改变当前游标的方向
                        changeDirection();
                        // 改变右侧游标的方向
                        cursorHandle.changeDirection();
                        mBeforeDragStart = mBeforeDragEnd;
                        selectText(mBeforeDragEnd, offset);
                        cursorHandle.updateCursorHandle();
                    } else {
                        selectText(offset, -1);
                    }
                    updateCursorHandle();
                } else {
                    // 处理右边超过左边游标的情况
                    if (offset < mBeforeDragStart) {
                        CursorHandle cursorHandle = getCursorHandle(true);
                        cursorHandle.changeDirection();
                        changeDirection();
                        mBeforeDragEnd = mBeforeDragStart;
                        selectText(offset, mBeforeDragStart);
                        cursorHandle.updateCursorHandle();
                    } else {
                        selectText(mBeforeDragStart, offset);
                    }
                    updateCursorHandle();
                }
            }
        }

        /**
         * 更新游标选择区域的方法
         */
        private void updateCursorHandle() {
            mTextView.getLocationInWindow(mLocation);
            Layout layout = mTextView.getLayout();
            if (isLeft) {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.getStart()) - mWidth + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.getStart())) + getExtraY(), -1, -1);
            } else {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.getEnd()) + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.getEnd())) + getExtraY(), -1, -1);
            }
        }

        /**
         * 显示游标时调用的方法
         *
         * @param x:移动手指时的X坐标
         * @param y:移动手指时的Y坐标
         */
        public void show(int x, int y) {
            mTextView.getLocationInWindow(mLocation);
            int offset = isLeft ? mWidth : 0;
            int posY = y + getExtraY();
            if (posY < top || posY > bottom) {
                return;
            }
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x - offset + getExtraX(), y + getExtraY());
        }

        public int getExtraX() {
            return mLocation[0] - mPadding + mTextView.getPaddingLeft();
        }

        public int getExtraY() {
            return mLocation[1] + mTextView.getPaddingTop();
        }

        /**
         * 改变方向的方法
         */
        private void changeDirection() {
            isLeft = !isLeft;
            invalidate();
        }

        /**
         * 返回游标类型
         *
         * @param isLeft:判断是否为起始游标
         * @return 返回指定的游标对象
         */
        private CursorHandle getCursorHandle(boolean isLeft) {
            if (mStartHandle.isLeft == isLeft) {
                return mStartHandle;
            } else {
                return mEndHandle;
            }
        }

        public void dismiss() {
            mPopupWindow.dismiss();
        }
    }
}
