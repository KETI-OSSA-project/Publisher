package com.example.publisher;

import android.content.Context;
import android.content.Intent;
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

public class MainActivity extends ComponentActivity {

    private final String brokerHost = "192.168.0.8";
    private final int brokerPort = 1883;
    private final String topic = "hello";

    private EditText inputTopic;
    private EditText inputMessage;
    private Button publishButton;
    private TextView statusView;

    private Mqtt3AsyncClient mqttClient;
    private MessagingManager messagingManager;
    private boolean isServiceConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            inputTopic = findViewById(R.id.inputTopic);
            inputMessage = findViewById(R.id.inputMessage);
            publishButton = findViewById(R.id.publishButton);
            statusView = findViewById(R.id.statusView);

            Log.d("MQTT", "MainActivity onCreate 시작");

            // 서비스 시작 - 디버깅 체크 용도
            try {
                Intent serviceIntent = new Intent(this, com.example.messagebroker.MessagingService.class);

                // 일반 서비스로 시작
                android.content.ComponentName result = startService(serviceIntent);
                if (result != null) {
                    Log.d("MQTT", "서비스 시작 성공: " + result.toString());
                } else {
                    Log.e("MQTT", "서비스 시작 실패: startService returned null");
                }

                // Android 8.0+ 에서는 Foreground 서비스로 시작
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        startForegroundService(serviceIntent);
                        Log.d("MQTT", "Foreground 서비스로도 시작 시도함");
                    } catch (Exception foregroundError) {
                        Log.w("MQTT", "Foreground 서비스 시작 실패, 일반 서비스 사용: " + foregroundError.getMessage());
                    }
                }

                // 서비스가 실제로 시작되었는지 확인
                new android.os.Handler().postDelayed(() -> {
                    if (isServiceRunning()) {
                        Log.d("MQTT", "✅ 서비스 실행 확인됨");
                    } else {
                        Log.e("MQTT", "❌ 서비스가 여전히 실행되지 않음");
                        logStatus("❌ 서비스 시작 실패 - 수동으로 다시 시도합니다");

                        // 한 번 더 시도
                        try {
                            startService(serviceIntent);
                            Log.d("MQTT", "서비스 재시작 시도");
                        } catch (Exception retryError) {
                            Log.e("MQTT", "서비스 재시작도 실패", retryError);
                        }
                    }
                }, 500); // 0.5초 후 확인

            } catch (Exception e) {
                Log.e("MQTT", "서비스 시작 실패", e);
                logStatus("❌ 서비스 시작 실패: " + e.getMessage());
            }

            // MessagingManager 초기화 및 연결 리스너 설정
            try {
                setupMessagingManager();
                Log.d("MQTT", "setupMessagingManager 호출 성공");
            } catch (Exception e) {
                Log.e("MQTT", "setupMessagingManager 실패", e);
                logStatus("❌ MessagingManager 설정 실패: " + e.getMessage());
            }

            publishButton.setOnClickListener(v -> {
                try {
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
                        logStatus("⚠️ 서비스가 아직 연결되지 않았습니다. 잠시 후 다시 시도해주세요.");
                        return;
                    }

                    publishMessage(topicText, messageText);
                } catch (Exception e) {
                    Log.e("MQTT", "버튼 클릭 처리 중 에러", e);
                    logStatus("❌ 버튼 처리 에러: " + e.getMessage());
                }
            });

            Log.d("MQTT", "MainActivity onCreate 완료");

        } catch (Exception e) {
            Log.e("MQTT", "MainActivity onCreate 전체 실패", e);
            // 안전한 fallback UI
            try {
                if (statusView != null) {
                    runOnUiThread(() -> logStatus("❌ 앱 초기화 실패: " + e.getMessage()));
                }
            } catch (Exception fallbackError) {
                Log.e("MQTT", "Fallback 처리도 실패", fallbackError);
            }
        }
    }

    private void setupMessagingManager() {
        Log.d("MQTT", "setupMessagingManager() 시작");

        // 먼저 서비스가 실제로 실행 중인지 확인
        if (!isServiceRunning()) {
            Log.w("MQTT", "MessagingService가 실행되지 않음. 강제로 시작합니다.");
            Intent serviceIntent = new Intent(this, com.example.messagebroker.MessagingService.class);
            startService(serviceIntent);

            // 서비스 시작 후 잠시 대기
            new android.os.Handler().postDelayed(() -> {
                setupMessagingManagerInternal();
            }, 1000); // 1초 대기
        } else {
            Log.d("MQTT", "MessagingService가 이미 실행 중입니다.");
            setupMessagingManagerInternal();
        }
    }

    private void setupMessagingManagerInternal() {
        try {
            messagingManager = MessagingManager.getMessagingManager(this);
            Log.d("MQTT", "MessagingManager 인스턴스 생성 완료");

            // 연결 상태 리스너 설정
            MessagingManager.MessagingServiceConnectionListener connectionListener =
                new MessagingManager.MessagingServiceConnectionListener() {
                    @Override
                    public void onConnected() {
                        Log.d("MQTT", "ConnectionListener.onConnected() 호출됨");
                        isServiceConnected = true;
                        runOnUiThread(() -> {
                            logStatus("✅ MessagingService 연결 성공");
                            publishButton.setEnabled(true);
                        });
                        Log.d("MQTT", "MessagingService 연결됨 - UI 업데이트 완료");
                    }

                    @Override
                    public void onDisconnected() {
                        Log.d("MQTT", "ConnectionListener.onDisconnected() 호출됨");
                        isServiceConnected = false;
                        runOnUiThread(() -> {
                            logStatus("❌ MessagingService 연결 끊어짐");
                            publishButton.setEnabled(false);
                        });
                        Log.d("MQTT", "MessagingService 연결 끊어짐 - UI 업데이트 완료");
                    }
                };

            Log.d("MQTT", "ConnectionListener 설정 중...");
            messagingManager.setMessagingServiceConnectionListener(connectionListener, null);
            Log.d("MQTT", "ConnectionListener 설정 완료");

            // 초기에는 버튼 비활성화
            publishButton.setEnabled(false);
            logStatus("🔄 MessagingService 연결 중...");

            // 연결 타임아웃 처리 (5초로 단축)
            publishButton.postDelayed(() -> {
                if (!isServiceConnected) {
                    Log.w("MQTT", "연결 타임아웃! 재시도 중...");
                    logStatus("⚠️ 연결 타임아웃. 재시도 중...");

                    // 기존 연결 정리하고 재시도
                    try {
                        if (messagingManager != null) {
                            messagingManager.close();
                        }
                    } catch (Exception e) {
                        Log.e("MQTT", "기존 연결 정리 중 에러", e);
                    }

                    // 재시도
                    setupMessagingManager();
                }
            }, 5000); // 5초로 단축

        } catch (Exception e) {
            logStatus("❌ MessagingManager 초기화 실패: " + e.getMessage());
            Log.e("MQTT", "MessagingManager 초기화 실패", e);
        }
    }

    private boolean isServiceRunning() {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (com.example.messagebroker.MessagingService.class.getName().equals(service.service.getClassName())) {
                Log.d("MQTT", "MessagingService 실행 중 확인됨");
                return true;
            }
        }
        Log.d("MQTT", "MessagingService 실행되지 않음");
        return false;
    }

    // api 표준화 대상
    private void publishMessage(String topic, String message) {
        if (messagingManager == null) {
            logStatus("❌ MessagingManager가 초기화되지 않았습니다.");
            return;
        }

        if (!isServiceConnected) {
            logStatus("❌ 서비스가 연결되지 않았습니다.");
            return;
        }

        // 메시지 객체 생성
        Message msg = new Message.Builder(topic)
                .setPayload(message.getBytes())
                .build();

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

    // 임시 테스트용 - MessagingManager 대신 직접 바인딩
    private void testDirectBinding() {
        Log.d("MQTT", "직접 바인딩 테스트 시작");
        try {
            Intent intent = new Intent(this, com.example.messagebroker.MessagingService.class);
            boolean result = bindService(intent, new android.content.ServiceConnection() {
                @Override
                public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) {
                    Log.d("MQTT", "✅ 직접 바인딩 성공!");
                    runOnUiThread(() -> {
                        logStatus("✅ 직접 바인딩 성공!");
                        publishButton.setEnabled(true);
                    });
                }

                @Override
                public void onServiceDisconnected(android.content.ComponentName name) {
                    Log.d("MQTT", "❌ 직접 바인딩 끊어짐");
                    runOnUiThread(() -> {
                        logStatus("❌ 직접 바인딩 끊어짐");
                        publishButton.setEnabled(false);
                    });
                }
            }, Context.BIND_AUTO_CREATE);

            Log.d("MQTT", "바인딩 시도 결과: " + result);
        } catch (Exception e) {
            Log.e("MQTT", "직접 바인딩 실패", e);
            logStatus("❌ 직접 바인딩 실패: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagingManager != null) {
            try {
                messagingManager.close();
            } catch (Exception e) {
                Log.e("MQTT", "MessagingManager 종료 중 에러", e);
            }
        }
    }
}