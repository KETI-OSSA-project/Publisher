package com.example.publisher;

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
    }

    private void initializeMessagingManager() {
        logStatus("🔧 MessagingManager 초기화 중...");
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
            logStatus("📡 브로커 연결 리스너 등록 완료");
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
            runOnUiThread(() -> {
                logStatus("🚀 메시지 발행 [" + topic + "]: " + message);
                inputMessage.getText().clear();
            });
        } catch (Exception e) {
            runOnUiThread(() -> logStatus("❌ 메시지 발행 실패: " + e.getMessage()));
        }
    }

    private void logStatus(String msg) {
        Log.d("MQTT", msg);
        statusView.append(msg + "\n");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 더 이상 정리할 외부 리소스 없음
    }
}