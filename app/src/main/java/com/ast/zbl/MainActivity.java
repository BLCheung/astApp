package com.ast.zbl;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.ast.zbl.bean.Token;
import com.ast.zbl.util.ToastUtil;
import com.ezvizuikit.open.EZUIError;
import com.ezvizuikit.open.EZUIKit;
import com.ezvizuikit.open.EZUIKitUilts;
import com.ezvizuikit.open.EZUIPlayer;
import com.google.gson.Gson;
import com.videogo.openapi.EZOpenSDK;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Created by BLCheung
 * @email 925306022@qq.com
 * @date Created on 2018/9/11 11:04
 */
public class MainActivity extends Activity implements EZUIPlayer.EZUIPlayerCallBack, WindowSizeChangeNotifier.OnWindowSizeChangedListener {
    private final String TAG = this.getClass().getSimpleName();
    private final String PREFS_TOKEN = "token";
    private EZUIPlayer mEZUIPlayer;
    private MyOrientationEventListener listener;
    private String appKey = "de3f16dc9a13450d92e0f65ab3d165c4";
    private String appSecret = "f97a30ce8b9599da463e7392770517ab";
    private String mUrl = "ezopen://open.ys7.com/822205090/1.hd.live";
    private String getTokenUrl = "https://open.ys7.com/api/lapp/token/get";
    private boolean isResumePlay = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listener = new MyOrientationEventListener(this);
        new WindowSizeChangeNotifier(this, this);

        initView();
        preparePlay();
        setSurfaceSize();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mEZUIPlayer = findViewById(R.id.main_ezplayer);
//        mEZUIPlayer.setRatio(3 * 1.0f / 4);       万恶的setRatio 坑！
        mEZUIPlayer.setLoadingView(getProgressBar());
    }

    /**
     * 设置播放资源参数
     */
    private void preparePlay() {
        try {
            Log.d(TAG, "preparePlay: ");
            EZUIKit.setDebug(true);
            EZUIKit.initWithAppKey(getApplication(), appKey);
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String prefsAccessToken = prefs.getString(PREFS_TOKEN, null);

            if (prefsAccessToken != null) {
                Log.d(TAG, "初始化: Prefs_token: " + prefsAccessToken);
                EZUIKit.setAccessToken(prefsAccessToken);
            } else {
                Log.d(TAG, "token为空: 获取token");
                getToken();
            }
            mEZUIPlayer.setCallBack(this);
            mEZUIPlayer.setUrl(mUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 调整播放区域的宽高
     */
    private void setSurfaceSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        boolean isWideScreen = listener.isWideScreen();
        // 竖屏
        if (!isWideScreen) {
            // 调整竖屏播放区域的大小，宽设为全屏,高根据分辨率自适应
            mEZUIPlayer.setSurfaceSize(dm.widthPixels, 0);
        } else {
            // 调整横屏播放区域的大小，宽高均为全屏
            mEZUIPlayer.setSurfaceSize(dm.widthPixels, dm.heightPixels);
        }
    }

    /**
     * 初始化Loading界面
     *
     * @return
     */
    @NonNull
    private View getProgressBar() {
        //创建loadingview
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setBackgroundColor(Color.parseColor("#000000"));
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relativeLayout.setLayoutParams(lp);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        //addRule参数对应RelativeLayout XML布局的属性
        rlp.addRule(RelativeLayout.CENTER_IN_PARENT);
        ProgressBar mProgressBar = new ProgressBar(this);
        relativeLayout.addView(mProgressBar, rlp);
        return relativeLayout;
    }

    /**
     * 获取token
     */
    private void getToken() {
        try {
            OkHttpClient okHttpClient = new OkHttpClient();

            // post请求 提交表单数据
            FormBody formBody = new FormBody.Builder()
                    .add("appKey", appKey)
                    .add("appSecret", appSecret)
                    .build();

            final Request request = new Request.Builder()
                    .post(formBody)
                    .url(getTokenUrl)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {

                private String accessToken;

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d(TAG, "onFailure: 获取token失败");
                }

                @Override
                public void onResponse(Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        Log.d(TAG, "onResponse: " + responseData);
                        Gson gson = new Gson();
                        final Token token = gson.fromJson(responseData, Token.class);
                        accessToken = token.getData().getAccessToken();
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                        editor.putString(PREFS_TOKEN, accessToken);
                        editor.apply();
                        Log.d(TAG, "token: " + accessToken);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastUtil.showToast(MainActivity.this, "获取Token成功!");
                                EZUIKit.setAccessToken(accessToken);
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: isResumePlay " + isResumePlay);
        listener.enable();
        // 界面resume时，如果还在暂停状态，isResumePlay为true则播放，最后恢复isResumePlay状态为false
        if (isResumePlay) {
            mEZUIPlayer.startPlay();
            isResumePlay = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        listener.disable();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: " + mEZUIPlayer.getStatus());
        if (mEZUIPlayer.getStatus() != EZUIPlayer.STATUS_STOP) {
            // 界面stop时，如果还在播放状态，则把isResumePlay设为true，以便resume时恢复播放
            isResumePlay = true;
        }
        mEZUIPlayer.stopPlay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        // 释放资源
        mEZUIPlayer.releasePlayer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSurfaceSize();
    }

    @Override
    public void onPlaySuccess() {
        Log.d(TAG, "onPlaySuccess: 播放成功!");
    }

    @Override
    public void onPlayFail(EZUIError ezuiError) {
        Log.d(TAG, "onPlayFail: 播放失败!");
        ToastUtil.showToast(this, "播放失败!");
        switch (ezuiError.getErrorString()) {
            case EZUIError.UE_ERROR_ACCESSTOKEN_ERROR_OR_EXPIRE:
                ToastUtil.showToast(this, "accesstoken异常或失效，需要重新获取accesstoken，并传入到sdk");
                mEZUIPlayer.setLoadingView(getProgressBar());
                getToken();
                mEZUIPlayer.setCallBack(this);
                mEZUIPlayer.setUrl(mUrl);
                break;
            case EZUIError.UE_ERROR_APPKEY_ERROR:
                ToastUtil.showToast(this, "appkey和AccessToken不匹配,建议更换appkey或者AccessToken");
                break;
            case EZUIError.UE_ERROR_CAMERA_NOT_EXIST:
                ToastUtil.showToast(this, "通道不存在，设备参数错误，建议重新获取播放地址");
                break;
            case EZUIError.UE_ERROR_CAS_MSG_PU_NO_RESOURCE:
                ToastUtil.showToast(this, "设备连接数过大，停止其他连接后再试试吧");
                break;
            case EZUIError.UE_ERROR_DEVICE_NOT_EXIST:
                ToastUtil.showToast(this, "设备不存在，设备参数错误，建议重新获取播放地址");
                break;
            case EZUIError.UE_ERROR_INNER_DEVICE_NULLINFO:
                ToastUtil.showToast(this, "设备信息异常为空，建议重新获取播放地址");
                break;
            case EZUIError.UE_ERROR_INNER_STREAM_TIMEOUT:
                ToastUtil.showToast(this, "播放失败，请求连接设备超时，检测设备网路连接是否正常");
                break;
            case EZUIError.UE_ERROR_INNER_VERIFYCODE_ERROR:
                ToastUtil.showToast(this, "视频验证码错误，建议重新获取url地址增加验证码");
                break;
            case EZUIError.UE_ERROR_NOT_FOUND_RECORD_FILES:
                ToastUtil.showToast(this, "未查找到录像文件");
                break;
            case EZUIError.UE_ERROR_PARAM_ERROR:
                ToastUtil.showToast(this, "url参数，建议重新获取播放地址");
                break;
            case EZUIError.UE_ERROR_PLAY_FAIL:
                ToastUtil.showToast(this, "视频播放失败");
                break;
            case EZUIError.UE_ERROR_PROTOCAL_ERROR:
                ToastUtil.showToast(this, "播放地址错误,建议重新获取播放地址");
                break;
            case EZUIError.UE_ERROR_STREAM_CLIENT_LIMIT:
                ToastUtil.showToast(this, "取流并发路数限制");
                break;
            case EZUIError.UE_ERROR_TRANSF_DEVICE_OFFLINE:
                ToastUtil.showToast(this, "设备不在线，确认设备上线之后重试");
                break;
            case EZUIError.UE_ERROR_TRANSF_TERMINAL_BINDING:
                ToastUtil.showToast(this, "当前账号开启了终端绑定，只允许指定设备登录操作");
                break;
            default:
                break;
        }
    }

    @Override
    public void onVideoSizeChange(int i, int i1) {
//        Log.d(TAG, "onVideoSizeChange播放分辨率回调: " + "width= " + i + " height= " + i1);
        setSurfaceSize();
    }

    @Override
    public void onPrepared() {
        Log.d(TAG, "onPrepared: 回放数据准备完毕，可以调用");
        mEZUIPlayer.startPlay();
    }

    @Override
    public void onPlayTime(Calendar calendar) {
//        Log.d(TAG, "onPlayTime: " + calendar.getTime().toString());
    }

    @Override
    public void onPlayFinish() {
        Log.d(TAG, "onPlayFinish: 播放结束");
    }

    @Override
    public void onWindowSizeChanged(int w, int h, int oldW, int oldH) {
        if (mEZUIPlayer != null) {
            setSurfaceSize();
        }
    }

    public class MyOrientationEventListener extends OrientationEventListener {
        private WindowManager manager;
        private int mLastOrientation = 0;

        public MyOrientationEventListener(Context context) {
            super(context);
            manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }

        /**
         * 是否横屏
         *
         * @return
         */
        public boolean isWideScreen() {
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            return point.x > point.y;
        }

        @Override
        public void onOrientationChanged(int orientation) {
            int value = getCurentOrientationEx(orientation);
            if (value != mLastOrientation) {
                mLastOrientation = value;
                int current = getRequestedOrientation();
                if (current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        || current == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                }
            }
        }

        private int getCurentOrientationEx(int orientation) {
            int value = 0;
            if (orientation >= 315 || orientation < 45) {
                // 0度
                value = 0;
                return value;
            }
            if (orientation >= 45 && orientation < 135) {
                // 90度
                value = 90;
                return value;
            }
            if (orientation >= 135 && orientation < 225) {
                // 180度
                value = 180;
                return value;
            }
            if (orientation >= 225 && orientation < 315) {
                // 270度
                value = 270;
                return value;
            }
            return value;
        }
    }
}
