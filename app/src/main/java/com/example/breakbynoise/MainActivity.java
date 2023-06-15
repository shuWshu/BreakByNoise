package com.example.breakbynoise;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private FilterLayer filterLayer;
    private Intent intentService;
    private TextView TextPosX, TextPosY, TextPosZ;
    private float[] AccVal = new float[3];
    private boolean BindFlag = false, TimerFlag = false;
    private long startTime, elapsedTime;
    private Button buttonS;

    // コネクション作成
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // サービス接続時に呼ばれる
            Log.i("ServiceConnection", "onServiceConnected");
            // BinderからServiceのインスタンスを取得
            filterLayer = ((FilterLayer.BindServiceBinder)service).getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // サービス切断時に呼ばれる
            Log.i("ServiceConnection", "onServiceDisconnected");
            filterLayer = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filterLayer = new FilterLayer();
        intentService = new Intent(getApplicationContext(), filterLayer.getClass());

        buttonS = findViewById(R.id.button_start);
        buttonS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BindFlag == false){
                    if (Settings.canDrawOverlays(getApplicationContext())) {
                        Log.d("main", "onclick startService");
                        //startService(intentService);
                        bindService(intentService, connection, Context.BIND_AUTO_CREATE);
                        BindFlag = true;
                        buttonS.setText(R.string.stop);
                        // API26以降
                        // startForegroundService(intentService)
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.message,
                                Toast.LENGTH_LONG).show();
                    }
                }else{
                    DisConnect();
                }
            }
        });

        // 加速度センサ関連
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor Acceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, Acceleration, SensorManager.SENSOR_DELAY_UI);

        TextPosX = findViewById(R.id.textPosx);
        TextPosY = findViewById(R.id.textPosy);
        TextPosZ = findViewById(R.id.textPosz);

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        AccVal = event.values.clone();
        TextPosZ.setText(String.valueOf(AccVal[2]));
        TextPosY.setText(String.valueOf(filterLayer.timecount));
        // 時間計測処理
        if (TimerFlag == false){
            if (AccVal[2] < 0){
                // タイマースタート
                TimerFlag = true;
                startTime = System.currentTimeMillis();
                filterLayer.SoundFlag = true;
                Log.d("timer", "start");
            }
        }else if (AccVal[2] > 0){
            // タイマーストップ
            TimerFlag = false;
            elapsedTime = System.currentTimeMillis() - startTime;
            // elapsedTime = TimeUnit.MILLISECONDS.toSeconds(elapsedTime); // 秒に直す
            TextPosX.setText(String.valueOf(elapsedTime / 1000.0f));
            filterLayer.SoundFlag = false;
            filterLayer.soundcount = 0;
            Log.d("timer", "stop");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void DisConnect(){
        unbindService(connection);
        BindFlag = false;
        buttonS.setText(R.string.start);
    }
}
