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

public class MainActivity extends ComponentActivity {
    private EditText inputTopic;
    private EditText inputMessage;
    private Button publishButton;
    private TextView statusView;
    private MessagingManager messagingManager;
    private boolean isServiceConnected = false;

    private static final String TAG = "Publisher";

    // 시험 신호(adb 셸만 보낼 수 있음): am broadcast -a test.PUBLISH --es topic sensor/1/temp [--es payload ...]
    // payload를 빼면 토픽 이름을 내용으로 보낸다(절차서 18번)
    private static final String ACTION_PUBLISH = "test.PUBLISH";

    private final BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String topic = intent.getStringExtra("topic");
            String payload = intent.getStringExtra("payload");
            if (topic != null) publishMessage(topic, payload != null ? payload : topic);
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
            publishMessage(topicText, messageText);
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

    private void publishMessage(String topic, String message) {
        try {
            Message msg = new Message.Builder(topic)
                    .setPayload(message.getBytes())
                    .build();
            messagingManager.publish(msg);
            Log.i(TAG, "publish topic=" + topic + " id=" + msg.getMessageId() + " payload=" + message);
            runOnUiThread(() -> {
                logStatus("🚀 메시지 발행 [" + topic + "]: " + message);
                inputMessage.getText().clear();
            });
        } catch (Exception e) {
            runOnUiThread(() -> logStatus("❌ 메시지 발행 실패: " + e.getMessage()));
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