package com.example.smsapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private EditText phoneNumberEditText;
    private TextView tetherPriceTextView;
    private TextView bitcoinPriceTextView;
    private SmsReceiver smsReceiver; // اصلاح شده: تعریف به عنوان شی از نوع SmsReceiver

    private TextView smsCodeTextView; // اضافه کردن متغیر برای نمایش کد دریافتی

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = findViewById(R.id.phoneNumber);
        Button sendSmsButton = findViewById(R.id.sendSmsButton);
        smsCodeTextView = findViewById(R.id.smsCodeTextView); // اصلاح شده: اتصال به TextView برای نمایش کد
        bitcoinPriceTextView = findViewById(R.id.bitcoinPrice);
        tetherPriceTextView = findViewById(R.id.tetherPrice);

        sendSmsButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
            } else {
                sendSms();
            }
        });

        fetchCryptoPrices();

        // Register the SMS receiver
        smsReceiver = new SmsReceiver();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        // Register the receiver
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the receiver
        unregisterReceiver(smsReceiver);
    }

    private void sendSms() {
        String phoneNumber = phoneNumberEditText.getText().toString();
        String message = "پیامک تستی از اپلیکیشن شما.";

        if (!phoneNumber.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "پیامک ارسال شد", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "لطفاً شماره تلفن را وارد کنید", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCryptoPrices() {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=tether,bitcoin&vs_currencies=usd";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    tetherPriceTextView.setText("خطا در دریافت قیمت تتر");
                    bitcoinPriceTextView.setText("خطا در دریافت قیمت بیت‌کوین");
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        double tetherPrice = jsonObject.getJSONObject("tether").getDouble("usd");
                        double bitcoinPrice = jsonObject.getJSONObject("bitcoin").getDouble("usd");

                        runOnUiThread(() -> {
                            tetherPriceTextView.setText("قیمت لحظه‌ای تتر: " + tetherPrice + " دلار");
                            bitcoinPriceTextView.setText("قیمت لحظه‌ای بیت‌کوین: " + bitcoinPrice + " دلار");
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            tetherPriceTextView.setText("خطا در پردازش داده‌های تتر");
                            bitcoinPriceTextView.setText("خطا در پردازش داده‌های بیت‌کوین");
                        });
                    }
                }
            }
        });
    }

    // BroadcastReceiver برای دریافت پیامک‌ها
    public class SmsReceiver extends BroadcastReceiver {

        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "android.provider.Telephony.SMS_RECEIVED")) {
                // خواندن پیامک‌ها
                Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));

                        // استخراج کد از پیام
                        String code = extractCode(message);

                        if (code != null) {
                            // نمایش کد در UI اپلیکیشن
                            if (smsCodeTextView != null) {
                                smsCodeTextView.setText("کد دریافتی: " + code); // نمایش کد
                            }
                        }
                    }
                    cursor.close();
                }
            }
        }

        // متد برای استخراج کد چهار رقمی
        private String extractCode(String message) {
            if (message != null && message.matches(".*\\d{4}.*")) {
                // حذف تمام غیر از اعداد
                String code = message.replaceAll("\\D", "");
                if (code.length() == 4) {
                    return code;
                }
            }
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSms();
            } else {
                Toast.makeText(this, "مجوز ارسال پیامک رد شد", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
