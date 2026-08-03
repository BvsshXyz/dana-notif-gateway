package com.bayushop.dananotif;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String PREFS = "dana_notif_gateway";
    static final String KEY_WEBHOOK = "webhook_url";
    static final String KEY_SECRET = "webhook_secret";

    private EditText webhookInput;
    private EditText secretInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startGatewayService();
        requestNotificationPermission();

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(42, 54, 42, 42);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(0xFFF6F7F3);

        TextView title = new TextView(this);
        title.setText("DANA Notif Gateway");
        title.setTextSize(26);
        title.setTextColor(0xFF17211B);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, matchWrap());

        TextView desc = new TextView(this);
        desc.setText("Baca notifikasi DANA masuk lalu kirim data nominal ke webhook bot/VPS kamu.");
        desc.setTextSize(15);
        desc.setTextColor(0xFF5F6B63);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 12, 0, 28);
        root.addView(desc, matchWrap());

        webhookInput = new EditText(this);
        webhookInput.setHint("Webhook URL, contoh: https://domain.com/dana-webhook");
        webhookInput.setSingleLine(true);
        webhookInput.setText(prefs.getString(KEY_WEBHOOK, ""));
        root.addView(webhookInput, matchWrap());

        secretInput = new EditText(this);
        secretInput.setHint("Secret opsional");
        secretInput.setSingleLine(true);
        secretInput.setText(prefs.getString(KEY_SECRET, ""));
        root.addView(secretInput, matchWrap());

        Button save = button("Simpan Setting");
        save.setOnClickListener(v -> {
            saveSettings();
            startGatewayService();
        });
        root.addView(save, matchWrap());

        Button access = button("Buka Akses Notifikasi");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access, matchWrap());

        Button test = button("Test Webhook");
        test.setOnClickListener(v -> {
            saveSettings();
            startGatewayService();
            DanaNotificationService.sendTestWebhook(this);
        });
        root.addView(test, matchWrap());

        TextView note = new TextView(this);
        note.setText("Aktifkan akses notifikasi untuk app ini. App memproses notifikasi DANA/DANA Bisnis yang terlihat seperti pembayaran masuk.");
        note.setTextSize(13);
        note.setTextColor(0xFF5F6B63);
        note.setPadding(0, 28, 0, 0);
        root.addView(note, matchWrap());

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startGatewayService();
        DanaNotificationService.scanActiveNotificationsFromService();
    }

    private void saveSettings() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_WEBHOOK, webhookInput.getText().toString().trim())
                .putString(KEY_SECRET, secretInput.getText().toString().trim())
                .apply();
        Toast.makeText(this, "Setting tersimpan", Toast.LENGTH_SHORT).show();
    }

    private void startGatewayService() {
        Intent intent = new Intent(this, GatewayForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7002);
        }
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        return params;
    }
}