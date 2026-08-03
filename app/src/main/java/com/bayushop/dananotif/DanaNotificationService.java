package com.bayushop.dananotif;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DanaNotificationService extends NotificationListenerService {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:Rp\\s*)?([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> recentKeys = new HashSet<>();
    private static DanaNotificationService activeService;

    @Override
    public void onCreate() {
        super.onCreate();
        activeService = this;
    }

    @Override
    public void onDestroy() {
        if (activeService == this) {
            activeService = null;
        }
        super.onDestroy();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        activeService = this;
        scanActiveNotifications();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        processNotification(sbn);
    }

    static void scanActiveNotificationsFromService() {
        DanaNotificationService service = activeService;
        if (service != null) {
            service.scanActiveNotifications();
        }
    }

    private void scanActiveNotifications() {
        StatusBarNotification[] notifications;

        try {
            notifications = getActiveNotifications();
        } catch (Exception ignored) {
            return;
        }

        if (notifications == null) {
            return;
        }

        for (StatusBarNotification sbn : notifications) {
            processNotification(sbn);
        }
    }

    private void processNotification(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }

        Bundle extras = notification.extras;
        String title = safeText(extras.getCharSequence(Notification.EXTRA_TITLE));
        String text = safeText(extras.getCharSequence(Notification.EXTRA_TEXT));
        String bigText = safeText(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String packageName = safeText(sbn.getPackageName());
        String content = (packageName + " " + title + " " + text + " " + bigText).trim();

        if (!looksLikeIncomingPayment(content)) {
            return;
        }

        long amount = extractAmount(content);
        if (amount <= 0) {
            return;
        }

        String dedupeKey = packageName + "|" + sbn.getPostTime() + "|" + amount + "|" + content.hashCode();

        synchronized (recentKeys) {
            if (recentKeys.contains(dedupeKey)) {
                return;
            }

            recentKeys.add(dedupeKey);

            if (recentKeys.size() > 100) {
                recentKeys.clear();
            }
        }

        sendWebhook(this, amount, title, text, bigText, packageName, sbn.getPostTime(), false);
    }

    static void sendTestWebhook(Context context) {
        sendWebhook(
                context,
                10047,
                "DANA",
                "Uang masuk Rp10.047",
                "Test webhook dari DANA Notif Gateway",
                "test",
                System.currentTimeMillis(),
                true
        );
    }

    private static void sendWebhook(
            Context context,
            long amount,
            String title,
            String text,
            String bigText,
            String packageName,
            long postTime,
            boolean test
    ) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        String webhook = prefs.getString(MainActivity.KEY_WEBHOOK, "").trim();
        String secret = prefs.getString(MainActivity.KEY_SECRET, "").trim();

        if (webhook.isEmpty()) {
            if (test) {
                Toast.makeText(context, "Webhook URL kosong", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String payload = "{"
                + "\"source\":\"dana_notification\","
                + "\"test\":" + test + ","
                + "\"amount\":" + amount + ","
                + "\"title\":" + quote(title) + ","
                + "\"text\":" + quote(text) + ","
                + "\"big_text\":" + quote(bigText) + ","
                + "\"package_name\":" + quote(packageName) + ","
                + "\"post_time\":" + postTime
                + "}";

        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(webhook).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "DanaNotifGateway/1.0");

                if (!secret.isEmpty()) {
                    conn.setRequestProperty("X-Webhook-Secret", secret);
                }

                byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }

                int code = conn.getResponseCode();

                if (test) {
                    showToast(context, "Webhook test status: " + code);
                }

                conn.disconnect();
            } catch (Exception e) {
                if (test) {
                    showToast(context, "Webhook gagal: " + e.getMessage());
                }
            }
        }).start();
    }

    private static boolean looksLikeIncomingPayment(String content) {
        String lower = content.toLowerCase(Locale.ROOT);

        boolean danaNotif = lower.contains("dana")
                || lower.contains("id.dana")
                || lower.contains("dana bisnis");

        boolean mentionsMoney = lower.contains("rp")
                || lower.contains("uang")
                || lower.contains("saldo")
                || lower.contains("pembayaran");

        boolean incoming = lower.contains("masuk")
                || lower.contains("diterima")
                || lower.contains("menerima")
                || lower.contains("pembayaran masuk")
                || lower.contains("dana bisnis")
                || lower.contains("dana masuk")
                || lower.contains("top up");

        boolean outgoing = lower.contains("transfer ke")
                || lower.contains("kirim uang")
                || lower.contains("uang keluar");

        return danaNotif && mentionsMoney && incoming && !outgoing;
    }

    private static long extractAmount(String content) {
        Matcher matcher = AMOUNT_PATTERN.matcher(content);
        long best = 0;

        while (matcher.find()) {
            String raw = matcher.group(1).replace(".", "").replace(",", "");

            try {
                long value = Long.parseLong(raw);

                if (value > best) {
                    best = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return best;
    }

    private static String safeText(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private static String quote(String value) {
        if (value == null) {
            value = "";
        }

        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    private static void showToast(Context context, String message) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}