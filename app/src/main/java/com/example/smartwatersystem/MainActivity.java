package com.example.smartwatersystem;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;

//import android.support.v7.app.AppCompatActivity;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    //private static final String TAG = "MainActivity";
    private EditText editTextMinute;
    private Button btnSetTime;
    private TextView countdownTime;
    private TextView soil;
    private TextView pump;
    private TextView isAuto;
    private Switch switchAuto;
    private Switch switchPump;

    private long timeCountInMilliSeconds = 10 * 60 * 1000;
    private CountDownTimer countDownTimer;

    MQTTService mqttService;
    boolean fAuto = true;
    boolean fPump = false;
    boolean initFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();
        startMqtt();
    }

    private void initViews() {
        editTextMinute = (EditText) findViewById(R.id.editTextMinute);
        btnSetTime = (Button) findViewById(R.id.btn_set_time);
        countdownTime = (TextView) findViewById(R.id.countdownTime);
        soil = (TextView) findViewById(R.id.soil);
        pump = (TextView) findViewById(R.id.pump);
        isAuto = (TextView) findViewById(R.id.isAuto);
        switchAuto = (Switch) findViewById(R.id.switch_auto);
        switchPump = (Switch) findViewById(R.id.switch_pump);
    }

    private void initListeners() {
        btnSetTime.setOnClickListener(this);
        switchAuto.setOnClickListener(this);
        switchPump.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_set_time:
                setTimeToCountDown();
                break;
            case R.id.switch_auto:
                actionSwitchAuto();
                break;
            case R.id.switch_pump:
                try {
                    actionSwitchPump();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void startMqtt() {
        mqttService = new MQTTService(getApplicationContext());
        mqttService.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws JSONException {
                Log.d("MQTT", mqttMessage.toString());
                JSONObject soilMoisture = new JSONObject(mqttMessage.toString());
                Integer moisture = Integer.valueOf(soilMoisture.getString("data"));
                soil.setText(soilMoisture.getString("data") + "&#x0025;");

                if(fAuto){
                    if (moisture <= 65 && !switchPump.isChecked()){
                        //65% -- 75%
                        changeStateOfPump(true);
                        switchPump.setChecked(true);
                    }
                    else if (moisture > 75 && switchPump.isChecked()) {
                        changeStateOfPump(false);
                        switchPump.setChecked(false);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    public String getStringJson(String id, String value) throws  JSONException {
        JSONObject obj = new JSONObject();
        try {
            if (id == "1") {
                // 0: OFF, 1: RED, 2: GREEN
                obj.put("id","1");
                obj.put("name","LED");
                obj.put("data", value);
                obj.put("unit","");
            }
            else if (id == "11") {
                obj.put("id","11");
                obj.put("name","RELAY");
                obj.put("data", value);
                obj.put("unit","");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }

    private void actionSwitchAuto() {
        if (switchAuto.isChecked()) {
            isAuto.setText("Bật");
            switchPump.setEnabled(false);
            fAuto = true;
            Toast.makeText(getApplicationContext(), "Khoá máy bơm", Toast.LENGTH_LONG).show();
        }
        else {
            isAuto.setText("Tắt");
            fAuto = false;
            switchPump.setEnabled(true);
        }
    }

    private void actionSwitchPump() throws JSONException {
        if (switchPump.isChecked()) {
            changeStateOfPump(true);
        }
        else {
            changeStateOfPump(false);
        }
    }

    private void setTimeToCountDown() {
        setTimerValues();
        countdownTime.setText(hmsTimeFormatter(timeCountInMilliSeconds));
    }

    private void setTimerValues() {
        int time = 10;
        if (!editTextMinute.getText().toString().isEmpty()) {
            // fetching value from edit text and type cast to integer
            time = Integer.parseInt(editTextMinute.getText().toString().trim());
        } else {
            // toast message to fill edit text
            Toast.makeText(getApplicationContext(), "Please Enter Minutes...", Toast.LENGTH_LONG).show();
            return;
        }
        // assigning values after converting to milliseconds
        timeCountInMilliSeconds = time * 1 * 1000;
        editTextMinute.getText().clear();
    }

    private void changeStateOfPump(boolean turnOn) throws JSONException {
        if(turnOn) {
            pump.setText("Bật");
            editTextMinute.setEnabled(false);
            btnSetTime.setEnabled(false);
            startCountDownTimer();
            sendDataMQTT("lykienminh/feeds/pump", getStringJson("11", "1"));
            sendDataMQTT("lykienminh/feeds/led", getStringJson("1", "2"));
        }
        else {
            pump.setText("Tắt");
            editTextMinute.setEnabled(true);
            btnSetTime.setEnabled(true);
            countDownTimer.cancel();
            countdownTime.setText(hmsTimeFormatter(timeCountInMilliSeconds));
            sendDataMQTT("lykienminh/feeds/pump", getStringJson("11", "0"));
            sendDataMQTT("lykienminh/feeds/led", getStringJson("1", "0"));
        }
    }

    private void startCountDownTimer() {
        countDownTimer = new CountDownTimer(timeCountInMilliSeconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownTime.setText(hmsTimeFormatter(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                Toast.makeText(getApplicationContext(), "Hết thời gian tưới", Toast.LENGTH_LONG).show();
                switchPump.setChecked(false);
                try {
                    changeStateOfPump(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }.start();
        countDownTimer.start();
    }

    private String hmsTimeFormatter(long milliSeconds) {
        if(milliSeconds < 60*60*1000){
            @SuppressLint("DefaultLocale") String hms = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
                    TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)));
            return hms;
        }

        @SuppressLint("DefaultLocale")String hms = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliSeconds),
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)));

        return hms;
    }

    private void sendDataMQTT(String topic, String data){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);

        byte[] b = data.getBytes(Charset.forName("UTF−8"));
        msg.setPayload(b);

        Log.d("ABC","Publish:"+ msg);
        try {
            mqttService.mqttAndroidClient.publish(topic, msg);
        } catch (MqttException e){
        }
    }
}