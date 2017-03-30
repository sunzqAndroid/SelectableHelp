package cn.nuosi.andoroid.testdrawline;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by Elder on 2017/3/14.
 *
 */

public class DrawLineApplication extends Application {

    public static Typeface mTypeface;
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        GreenDaoManager.getInstance();
    }

    public static Context getContext() {
        return mContext;
    }
}
