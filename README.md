# Publisher_java

---

## Azena Messaging Publisher
<p align="center">
  <img src="https://github.com/user-attachments/assets/d15b5f1f-2052-4b2e-b914-270107ee7431" alt="<프로젝트명> Banner" width="70%" />
</p>

<p align="center">
  <b>Validate and integrate MQTT messaging services and brokers with a unified tool.</b>
</p>

<div align="center">

  <!-- 윗줄: Android 관련 2개 -->
  <img src="https://img.shields.io/badge/Android_Studio-Koala-3DDC84?logo=androidstudio&logoColor=white" />
  <img src="https://img.shields.io/badge/Android-12_(API_32)-3DDC84?logo=android&logoColor=white" />
  <!-- 아랫줄: 나머지 3개 -->
  <img src="https://img.shields.io/badge/Java-11-007396?logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Mosquitto-2.0.22-3C5280?logo=eclipseide&logoColor=white" />

</div>

## 개요

Publisher 모듈은 내부 Broker(메시징 허브) 및 Gateway를 통해 **MQTT 메시지를 발행(Publish)** 하는 역할을 담당합니다.
토픽(Topic)과 메시지(Payload)를 지정해 내부 생태계로 전송하거나, Gateway 설정(Uplink 규칙)에 따라 **외부 MQTT Broker(Mosquitto)** 로 라우팅할 수 있습니다.

---
## Demo
### 내부 ↔ 내부 통신
<p align="center">
  <img src="https://github.com/user-attachments/assets/5fda5904-9d37-4324-8dae-87ddd36bc08a" width="40%" />
</p>

### 내부 ↔ 외부 통신
<table>
<tr>
    <td><img src="https://github.com/user-attachments/assets/3372b3b6-22df-4a4c-b832-d55950039a4c" alt="uplink-broker"></td>
    <td><img src="https://github.com/user-attachments/assets/e029e60a-968f-4e0d-9743-4cc5d612e071" alt="downlink-subscriber"></td>
</tr>
</table>
<p align="center">
  <a href="https://drive.google.com/file/d/1_TukQ0CwD708QUjqD8ZCfM2t8XZdoEBp/view?usp=drive_link">
    <b>📽️ MQTT Demo</b>
  </a>
</p>

## 디렉토리 구조

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

## 주요 기능

* **토픽 기반 메시지 발행**: 내부 Broker로 메시지 전송
* **Gateway 연동(Uplink)**: 규칙에 부합하는 토픽은 외부 MQTT로 라우팅
* **토픽 패턴 필터링**: `Filter.java`를 통해 허용/차단 규칙 적용 가능
* **메시지 구조화**: `Message.java`로 일관된 데이터 모델 사용
* **연결 수명주기 관리**: `MessagingManager.java`가 연결/발행/해제 책임

---

## 실행 환경

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

## 빠른 점검 (Troubleshooting)

* **외부로 안 나감**: Publisher 토픽이 Gateway **Uplink 패턴**과 일치하는지 확인
* **수신이 없음**: Subscriber가 **발행 전**에 구독했는지, 토픽이 정확한지 확인
* **연결 실패**: `MessagingManager`의 브로커 URL/포트, 네트워크 연결, 권한(네트워크) 확인
* **인코딩 문제**: Payload 직렬화 방식(String/JSON/bytes) 일관성 체크

---

## Acknowledgement
**개발 및 유지 관리 기관** : 한국전자기술연구원(KETI)

※ 본 연구는 2025년도 정부(과학기술정보통신부)의 재원으로 정보통신기획평가원의 지원을 받아 수행된 연구임(No. RS-2024-00394190, 온-디바이스 자율보호가 내재화된 개방형 영상보안플랫폼 기술 개발).
