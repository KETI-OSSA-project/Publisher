package com.example.publisher;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import com.example.broker.Message;
import com.example.broker.MessagingManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends ComponentActivity {
    private EditText inputTopic;
    private EditText inputMessage;
    private Button publishButton;
    private TextView statusView;
    private MessagingManager messagingManager;
    private boolean isServiceConnected = false;

    private static final String TAG = "Publisher";

    // 시험 신호(adb 셸만 보낼 수 있음): am broadcast -a test.PUBLISH --es topic sensor/1/temp [--es payload ...]
    // payload를 빼면 토픽 이름을 내용으로 보낸다(절차서 18번). --es file scene.xml 이면 앱에 든 샘플 파일 바이트 그대로(20·32번)
    private static final String ACTION_PUBLISH = "test.PUBLISH";

    private final BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String topic = intent.getStringExtra("topic");
            String payload = intent.getStringExtra("payload");
            String file = intent.getStringExtra("file");
            if (topic == null) return;
            if (file == null) {
                publishMessage(topic, (payload != null ? payload : topic).getBytes(StandardCharsets.UTF_8));
                return;
            }
            try (InputStream in = getAssets().open(file)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                publishMessage(topic, out.toByteArray());
            } catch (IOException e) {
                logStatus("❌ 샘플 파일 읽기 실패: " + file);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputTopic = findViewById(R.id.inputTopic);
        inputMessage = findViewById(R.id.inputMessage);
        publishButton = findViewById(R.id.publishButton);
        statusView = findViewById(R.id.statusView);

        initializeMessagingManager();

        publishButton.setOnClickListener(v -> {
            String topicText = inputTopic.getText().toString().trim();
            String messageText = inputMessage.getText().toString();

            if (topicText.isEmpty()) {
                logStatus("⚠️ 토픽을 입력해주세요");
                return;
            }
            if (messageText.isEmpty()) {
                logStatus("⚠️ 메시지를 입력해주세요");
                return;
            }
            if (!isServiceConnected) {
                logStatus("❌ 브로커와 연결되지 않았습니다");
                return;
            }
            publishMessage(topicText, messageText.getBytes(StandardCharsets.UTF_8));
        });

        // DUMP 권한을 가진 쪽만 보낼 수 있다: adb 셸은 갖고 있고 일반 앱은 받을 수 없는 권한
        IntentFilter filter = new IntentFilter(ACTION_PUBLISH);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(testReceiver, filter, Manifest.permission.DUMP, null, RECEIVER_EXPORTED);
        } else {
            registerReceiver(testReceiver, filter, Manifest.permission.DUMP, null);
        }
    }

    private void initializeMessagingManager() {
        try {
            messagingManager = MessagingManager.getMessagingManager(this);
            MessagingManager.MessagingServiceConnectionListener connectionListener =
                new MessagingManager.MessagingServiceConnectionListener() {
                    @Override
                    public void onConnected() {
                        isServiceConnected = true;
                        runOnUiThread(() -> logStatus("✅ 브로커 연결 완료!"));
                    }
                    @Override
                    public void onDisconnected() {
                        isServiceConnected = false;
                        runOnUiThread(() -> logStatus("❌ 브로커 연결 끊김"));
                    }
                };
            messagingManager.setMessagingServiceConnectionListener(connectionListener, null);
        } catch (Exception e) {
            logStatus("❌ MessagingManager 초기화 실패: " + e.getMessage());
        }
    }

    private void publishMessage(String topic, byte[] payload) {
        String message = new String(payload, StandardCharsets.UTF_8);
        try {
            Message msg = new Message.Builder(topic)
                    .setPayload(payload)
                    .build();
            messagingManager.publish(msg);
            // 여러 줄(XML 샘플)은 내용 대신 크기만 남기고, 송수신 대조용 sha256은 항상 남긴다
            Log.i(TAG, "publish topic=" + topic + " id=" + msg.getMessageId() + " bytes=" + payload.length
                    + " sha256=" + sha256(payload) + (message.indexOf('\n') < 0 ? " payload=" + message : ""));
            runOnUiThread(() -> {
                logStatus("🚀 메시지 발행 [" + topic + "]: " + message);
                inputMessage.getText().clear();
            });
        } catch (Exception e) {
            runOnUiThread(() -> logStatus("❌ 메시지 발행 실패: " + e.getMessage()));
        }
    }

    static String sha256(byte[] data) {
        try {
            StringBuilder sb = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void logStatus(String msg) {
        Log.d("Android", msg);
        statusView.append(msg + "\n");
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(testReceiver);
        super.onDestroy();
    }
}