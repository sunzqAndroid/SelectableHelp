package cn.nuosi.andoroid.testdrawline;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.nuosi.andoroid.testdrawline.SelectableTextView.OnSelectListener;
import cn.nuosi.andoroid.testdrawline.SelectableTextView.SelectableTextHelper;
import cn.nuosi.andoroid.testdrawline.dao.Book;
import cn.nuosi.andoroid.testdrawline.greendao.gen.BookDao;

public class MainActivity extends AppCompatActivity {

    private TextView mTextView;
    /**
     * 自定义TextView的属性
     */
    private TextView mTextView2;
    private SelectableTextHelper mSelectableTextHelper;
    private SelectableTextHelper mSelectableTextHelper3;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // 解析菜单
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.selection_action_menu, menu);
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
    };

    private List<Book>mBookList = new ArrayList<>();
    private BookDao mBookDao;
    private int top;
    private int bottom;
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
        // 初始化数据库读取笔记信息
        initData();
        mTextView = (TextView) findViewById(R.id.test_view);

        setListener();

        mTextView2 = (TextView) findViewById(R.id.test_tv2);
        mTextView2.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/fzltR.TTF"));
        String html = Html.fromHtml(text).toString();
        mTextView2.setText(Html.fromHtml(text));
        mSelectableTextHelper = new SelectableTextHelper.Builder(mTextView2)
                .setSelectedColor(ContextCompat.getColor(MainActivity.this, R.color.selected_blue))
                .setCursorHandleSizeInDp(15)
                .setPopMenu(R.layout.layout_pop_menu)
                .setBookList(mBookList)
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
                mSelectableTextHelper.setTop(mLocation[1]);
                mSelectableTextHelper.setBottom(mLocation[1] + (bottom - top));
                return true;
            }
        });
        mSelectableTextHelper.setSelectListener(new OnSelectListener() {
            @Override
            public void onTextSelected(CharSequence content) {

            }
        });

    }

    private void initData() {
        mBookDao = GreenDaoManager.getInstance().getSession().getBookDao();
        mBookList = GreenDaoManager.getInstance().getSession()
                .getBookDao().queryBuilder().build().list();
    }


    private void setListener() {

        mTextView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
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
}
