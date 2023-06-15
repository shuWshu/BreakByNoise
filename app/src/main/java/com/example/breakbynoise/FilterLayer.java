package com.example.breakbynoise;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.PixelFormat;
import android.media.MediaParser;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;

public class FilterLayer extends Service {

    private View newView;
    private WindowManager windowManager;
    private Timer timer;
    public boolean SoundFlag = false;
    public int soundcount, timecount;
    private MediaPlayer mediaPlayer0, mediaPlayer1, mediaPlayer2;
    private Handler handler;
    private WindowManager.LayoutParams params;

    // 最大秒数、回復量、開始までの秒数
    private int TIME_MAX = 60, TIME_RECOV = 6, TIME_START = 10;

    public class BindServiceBinder extends Binder {
        // TestBindService自身を返す
        FilterLayer getService(){
            return FilterLayer.this;
        }
    }

    private final IBinder mBinder = new BindServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("filter", "create");
        windowManager = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);

        // LayoutInflaterの生成
        LayoutInflater layoutInflater = LayoutInflater.from(this);

        // レイアウトファイルからInflateするViewを作成
        ViewGroup nullParent = null;
        newView = layoutInflater.inflate(R.layout.service_layer, nullParent);

        // 音楽系処理
        mediaPlayer0 = MediaPlayer.create(getBaseContext(), R.raw.recovery);
        mediaPlayer1 = MediaPlayer.create(getBaseContext(), R.raw.recovery);
        mediaPlayer2 = MediaPlayer.create(getBaseContext(), R.raw.recoveryend);
        soundcount = 0;

        timecount = 0;

        // Timer
        timer = new Timer();

        // メインスレッドのLooperに基づいたHandlerを作成
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 破壊時処理
        Log.d("filter", "onDestroy");
        // Viewを削除
        windowManager.removeView(newView);
        mediaPlayer0.release();
        mediaPlayer1.release();
        mediaPlayer2.release();
        timer.cancel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("filter", "bind");
        int typeLayer = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY; // 表示層

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, // 幅を画像に合わせる
                WindowManager.LayoutParams.WRAP_CONTENT, // 高さを画像に合わせる
                typeLayer,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE // オーバーレイに触れない
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // オーバーレイを貫通する
                PixelFormat.TRANSLUCENT // 透明or半透明
        );

        params.alpha = 0.0f;

        // Viewを画面上に追加
        windowManager.addView(newView, params);

        TimerTask timerTask = new TimerTask() { // 実行されるタスクを作成
            @Override
            public void run() {
                if (SoundFlag == true){
                    Log.d("filter", "music start");
                    if (timecount > 0){
                        timecount -= TIME_RECOV; // カウント減少
                        if (timecount <= TIME_START){
                            timecount = 0;
                            mediaPlayer2.start();
                        }else if (soundcount % 2 == 0){
                            mediaPlayer0.start();
                            soundcount += 1;
                        }else{
                            mediaPlayer1.start();
                            soundcount += 1;
                        }
                    }
                }else{
                    if(timecount < TIME_MAX + TIME_START){ // 時間カウント上限
                        timecount += 1;
                    }
                }


                if (newView.isAttachedToWindow()) {
                    float alpha;
                    float timerange = TIME_MAX; // floatに変換
                    alpha = Math.max(Math.min((timecount - TIME_START) / timerange, 1.0f), 0.0f);
                    Log.d("alpha", String.valueOf(alpha));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            params.alpha = alpha;
                            windowManager.updateViewLayout(newView, params);
                        }
                    });
                } else {
                    // Viewがアタッチされていない場合の処理
                    Log.d("filter", "エラー：Viewがアタッチされていない");
                }

            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000); // 1000ミリ秒（1秒）ごとにタスクを実行
        return mBinder;
    }
}