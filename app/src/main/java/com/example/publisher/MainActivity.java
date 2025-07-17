package com.example.publisher;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.example.messagebroker.Message;
import com.example.messagebroker.MessagingManager;
// import com.example.messagebroker.MessagingManager.MessagingServiceConnectionListener;

public class MainActivity extends ComponentActivity {

    private final String brokerHost = "192.168.0.8";
    private final int brokerPort = 1883;
    private final String topic = "hello";

    private EditText inputTopic;
    private EditText inputMessage;
    private Button publishButton;
    private TextView statusView;

    private Mqtt3AsyncClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputTopic = findViewById(R.id.inputTopic);
        inputMessage = findViewById(R.id.inputMessage);
        publishButton = findViewById(R.id.publishButton);
        statusView = findViewById(R.id.statusView);

        setupMqttClient();

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
            publishMessage(topicText, messageText);
        });
    }

    private void setupMqttClient() {
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("android-publisher-001")
                .serverHost(brokerHost)
                .serverPort(brokerPort)
                .buildAsync();

        mqttClient.connectWith().send().whenComplete((result, throwable) -> {
            runOnUiThread(() -> {
                if (throwable == null) {
                    logStatus("✅ MQTT 브로커 연결 성공");
                } else {
                    logStatus("❌ MQTT 연결 실패: " + throwable.getMessage());
                }
            });
        });
    }

    // api 표준화 대상
//    private void publishMessage(String topic, String message) {
//        if (!mqttClient.getState().isConnected()) {
//            logStatus("⚠️ MQTT 클라이언트가 아직 연결되지 않았습니다.");
//            return;
//        }
//
//        mqttClient.publishWith()
//                .topic(topic)
//                .payload(message.getBytes())
//                .qos(MqttQos.AT_LEAST_ONCE)
//                .send()
//                .whenComplete((result, throwable) -> {
//                    runOnUiThread(() -> {
//                        if (throwable == null) {
//                            logStatus("🚀 메시지 발행 [" + topic + "]: " + message);
//                            inputMessage.getText().clear();
//                        } else {
//                            logStatus("❌ 메시지 발행 실패: " + throwable.getMessage());
//                        }
//                    });
//                });
//    }

    private void publishMessage(String topic, String message) {
        if (!mqttClient.getState().isConnected()) {
            logStatus("⚠️ MQTT 클라이언트가 아직 연결되지 않았습니다.");
            return;
        }

        Message msg = new Message.Builder(topic)
                // .setTopic(topic)
                .setPayload(message.getBytes())
                .build();

        // 선언 방식 확인
        MessagingManager messagingManager = MessagingManager.getMessagingManager(this);
        try {
            messagingManager.publish(msg);
            runOnUiThread(() -> {
                logStatus("🚀 메시지 발행 [" + msg.getTopic() + "]: " + new String(msg.getPayload()));
                inputMessage.getText().clear();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                logStatus("❌ 메시지 발행 실패: " + e.getMessage());
            });
        }
    }

    private void logStatus(String msg) {
        Log.d("MQTT", msg);
        statusView.append(msg + "\n");
    }
}