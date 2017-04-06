package cn.nuosi.andoroid.testdrawline;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import cn.nuosi.andoroid.testdrawline.SelectableTextView.SelectableTextHelper;
import cn.nuosi.andoroid.testdrawline.SelectableTextView.TextLayoutUtil;
import cn.nuosi.andoroid.testdrawline.dao.Book;
import cn.nuosi.andoroid.testdrawline.greendao.gen.BookDao;
import cn.nuosi.andoroid.testdrawline.greendao.gen.BookDao.Properties;
import cn.nuosi.andoroid.testdrawline.info.BookInfo;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView mTextView1;
    private TextView mTextView2;
    private TextView tv_font_small;
    private TextView tv_font_middle;
    private TextView tv_font_large;
    private SelectableTextHelper mSelectableTextHelper1;
    private SelectableTextHelper mSelectableTextHelper2;

    private BookDao mBookDao;
    private int top;
    private int bottom;
    private String html;
    private String text = "<p>其支持的标签\n\n有:<br>" +
            "<strong>br</strong>:换行<br>" +
            "<strong>a:</strong>链接<br>" +
            "<strong>p</strong>:段落<br>" +
            "<strong>div</strong><br>" +
            "<strong>strong</strong>:粗体<br>" +
            "<strong>b</strong>:粗体<br>" +
            "<strong>em</strong><br>" +
            "<strong>cite</strong><br>" +
            "<strong>dfn</strong><br>" +
            "<strong>i</strong>:斜体<br>" +
            "<strong>big</strong>:大字体 相对于当前字体的<strong>1.25</strong>倍<br>" +
            "<strong>small</strong>:小字体，相对于当前字体的<strong>0.8</strong>倍<br>" +
            "<strong>font</strong>:字体，支持<strong>color</strong>和<strong>face</strong>属性<br>" +
            "<strong>blockquote</strong><br>" +
            "<strong>tt</strong>:<br>" +
            "<strong>u</strong>:下划线<br>" +
            "<strong>sup</strong>:上标<br>" +
            "<strong>sub</strong>:下标<br>" +
            "<strong>h1-6</strong>:<br>" +
            "<strong>img</strong>:支持图片，但是必须有一个<strong>Html.ImageGetter对象</strong>可以获取图片对象</p>" +
            "" +
            "" +
            "" +
            "<h2 id=\"html源码解析\">Html源码解析</h2>" +
            "" +
            "<br><br>Html类是处于包android.text下的一个text处理工具类";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        addListener();
        // 初始化数据库读取笔记信息
        initData();
        BookInfo bookInfo1 = new BookInfo();
        bookInfo1.startX = 0;
        bookInfo1.endX = bookInfo1.startX + html.length();
        loadBookData(bookInfo1);

        mSelectableTextHelper1 = new SelectableTextHelper.Builder(mTextView1)
                .setSelectedColor(ContextCompat.getColor(MainActivity.this, R.color.selected_red))
                .setCursorHandleSizeInDp(15)
                .setPopMenu(R.layout.layout_pop_menu)
                .setBookInfo(bookInfo1)
                .setCursorHandleColor(ContextCompat.getColor(MainActivity.this, R.color.cursor_handle_color))
                .build();

        BookInfo bookInfo2 = new BookInfo();
        bookInfo2.startX = html.length();
        bookInfo2.endX = bookInfo2.startX + html.length();
        loadBookData(bookInfo2);

        mSelectableTextHelper2 = new SelectableTextHelper.Builder(mTextView2)
                .setSelectedColor(ContextCompat.getColor(MainActivity.this, R.color.selected_red))
                .setCursorHandleSizeInDp(15)
                .setPopMenu(R.layout.layout_pop_menu)
                .setBookInfo(bookInfo2)
                .setCursorHandleColor(ContextCompat.getColor(MainActivity.this, R.color.cursor_handle_color))
                .build();
        final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                top = scrollView.getTop();
                bottom = scrollView.getBottom();
                int[] mLocation = new int[2];
                scrollView.getLocationOnScreen(mLocation);
                mSelectableTextHelper1.setTop(mLocation[1]);
                mSelectableTextHelper1.setBottom(mLocation[1] + (bottom - top));
                mSelectableTextHelper2.setTop(mLocation[1]);
                mSelectableTextHelper2.setBottom(mLocation[1] + (bottom - top));
                return true;
            }
        });
    }

    private void initView() {
        mTextView1 = (TextView) findViewById(R.id.test_tv1);
        mTextView2 = (TextView) findViewById(R.id.test_tv2);
        tv_font_small = (TextView) findViewById(R.id.tv_font_small);
        tv_font_middle = (TextView) findViewById(R.id.tv_font_middle);
        tv_font_large = (TextView) findViewById(R.id.tv_font_large);
        DrawLineApplication.mTypeface = Typeface.createFromAsset(getAssets(), "fonts/fzltR.TTF");
        mTextView1.setTypeface(DrawLineApplication.mTypeface);
        mTextView2.setTypeface(DrawLineApplication.mTypeface);
        html = Html.fromHtml(text).toString();
        mTextView1.setText(Html.fromHtml(text));
        mTextView2.setText(Html.fromHtml(text));
    }

    private void addListener() {
        tv_font_small.setOnClickListener(this);
        tv_font_middle.setOnClickListener(this);
        tv_font_large.setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        BookInfo bookInfo1 = new BookInfo();
        bookInfo1.startX = 0;
        bookInfo1.endX = bookInfo1.startX + html.length();
        loadBookData(bookInfo1);
        mSelectableTextHelper1.updateBookInfo(bookInfo1);

        BookInfo bookInfo2 = new BookInfo();
        bookInfo2.startX = html.length();
        bookInfo2.endX = bookInfo2.startX + html.length();
        loadBookData(bookInfo2);
        mSelectableTextHelper2.updateBookInfo(bookInfo2);
    }

    private void initData() {
        mBookDao = GreenDaoManager.getInstance().getSession().getBookDao();
    }

    private void loadBookData(BookInfo bookInfo) {
        QueryBuilder qb = mBookDao.queryBuilder();
        qb.where(Properties.Start.ge(bookInfo.startX), Properties.Start.le(bookInfo.endX));
        List<Book> list = qb.build().list();
        if (list != null) {
            for (Book book : list) {
                //重新拷贝标注信息(GreedDao框架读取出来的集合，修改集合对象后就相当于直接修改了数据库内容。如果此时不是copy一份，直接修改读取出来的对象，相当于直接修改了数据库内容）
                Book newBook = book.copy();
                newBook.setStart(newBook.getStart() - bookInfo.startX);
                newBook.setEnd(newBook.getEnd() - bookInfo.startX);
                bookInfo.mBookList.add(newBook);
            }
        }
    }

    private void setListener() {
        mTextView1.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // 解析菜单
                MenuInflater menuInflater = mode.getMenuInflater();
                if (menuInflater != null) {
                    menuInflater.inflate(R.menu.selection_action_menu, menu);
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                //根据item的ID处理点击事件
                switch (item.getItemId()) {
                    case R.id.Informal22:
                        Toast.makeText(MainActivity.this, "点击的是22", Toast.LENGTH_SHORT).show();
                        mode.finish();//收起操作菜单
                        break;
                    case R.id.Informal33:
                        Toast.makeText(MainActivity.this, "点击的是33", Toast.LENGTH_SHORT).show();
                        mode.finish();
                        break;
                }
                return false;//返回true则系统的"复制"、"搜索"之类的item将无效，只有自定义item有响应
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }

        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_font_small:
                mTextView1.setTextSize(TextLayoutUtil.dp2px(this, 6));
                mTextView2.setTextSize(TextLayoutUtil.dp2px(this, 6));
                break;
            case R.id.tv_font_middle:
                mTextView1.setTextSize(TextLayoutUtil.dp2px(this, 8));
                mTextView2.setTextSize(TextLayoutUtil.dp2px(this, 8));
                break;
            case R.id.tv_font_large:
                mTextView1.setTextSize(TextLayoutUtil.dp2px(this, 10));
                mTextView2.setTextSize(TextLayoutUtil.dp2px(this, 10));
                break;
        }
        mSelectableTextHelper1.changeTextSize();
        mSelectableTextHelper2.changeTextSize();
    }
}
