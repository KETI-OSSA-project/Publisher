# Publisher_java

---

## Azena Messaging Publisher

**개요**

Publisher 모듈은 내부 Broker(메시징 허브) 및 Gateway를 통해 **MQTT 메시지를 발행(Publish)** 하는 역할을 담당합니다.
토픽(Topic)과 메시지(Payload)를 지정해 내부 생태계로 전송하거나, Gateway 설정(Uplink 규칙)에 따라 **외부 MQTT Broker(Mosquitto)** 로 라우팅할 수 있습니다.

---

## 📂 디렉토리 구조

```
publisher_java/
└─ app/
   └─ java/com/example/
      ├─ broker/
      │  ├─ Filter.java             # 토픽 패턴 필터링 (Uplink/Downlink 규칙 적용 시 사용)
      │  ├─ Message.java            # 메시지 데이터 구조 (topic, payload 등)
      │  └─ MessagingManager.java   # MQTT 연결/발행/해제 관리 핵심 로직
      │
      └─ publisher/
         └─ MainActivity.java       # UI 진입점 (토픽/메시지 입력, Publish 버튼)
```

---

## ⚙️ 주요 기능

* **토픽 기반 메시지 발행**: 내부 Broker로 메시지 전송
* **Gateway 연동(Uplink)**: 규칙에 부합하는 토픽은 외부 MQTT로 라우팅
* **토픽 패턴 필터링**: `Filter.java`를 통해 허용/차단 규칙 적용 가능
* **메시지 구조화**: `Message.java`로 일관된 데이터 모델 사용
* **연결 수명주기 관리**: `MessagingManager.java`가 연결/발행/해제 책임

---

## 🖥실행 환경

* **Android Studio Koala (2024.1.1 Patch 1)**
* **Android 12 이하 (SDK/API Level ≤ 32)**
* 내부 Broker 및 (외부 발행 시) **Gateway + 외부 MQTT Broker(Mosquitto)** 실행 권장

---

## 빌드 & 실행

1. Android Studio로 `publisher_java` 열기
2. 필요 시 `MessagingManager.java` 내 브로커 엔드포인트 확인/수정
3. `MainActivity` 실행 → 단말에서 토픽/메시지 입력 후 **Publish** 클릭

> 참고: 외부 발행이 필요하면 Gateway 앱에서 **Uplink 토픽 패턴**을 설정하고, Publisher 토픽이 해당 패턴을 만족하도록 입력하세요.

---

## 사용 흐름 (내부 → 외부 포함)

1. **내부 발행 (기본)**

* Topic: `azena/test`
* Payload: `Hello Azena!`
* 결과: 내부 Broker로 발행 → 내부 Subscriber가 수신

2. **외부 발행 (Gateway Uplink 경유)**

* Gateway Uplink 패턴: `uplink/#` (예시)
* Publisher Topic: `uplink/device1/status`
* 결과: 내부 발행 → Gateway 필터 통과 → 외부 MQTT Broker로 전달 → 외부 Subscriber 수신

> Uplink 규칙을 만족하지 않는 토픽은 외부로 전달되지 않습니다.

---

## 동작 규칙(핵심 정리)

* 구독 이전에 발행된 메시지는 해당 Subscriber에서 수신되지 않음
* 구독하지 않은(또는 해제된) 토픽의 메시지는 수신되지 않음
* 동일 토픽을 중복 구독해도 메시지는 1회만 수신됨
* 발행 시점 이전부터 구독 중인 토픽은 자동 수신
* Uplink/Downlink 패턴은 Gateway에서 자유롭게 변경 가능(단, 서로 동일하게 설정하면 안 됨)

---

## 빠른 점검 (Troubleshooting)

* **외부로 안 나감**: Publisher 토픽이 Gateway **Uplink 패턴**과 일치하는지 확인
* **수신이 없음**: Subscriber가 **발행 전**에 구독했는지, 토픽이 정확한지 확인
* **연결 실패**: `MessagingManager`의 브로커 URL/포트, 네트워크 연결, 권한(네트워크) 확인
* **인코딩 문제**: Payload 직렬화 방식(String/JSON/bytes) 일관성 체크

---
