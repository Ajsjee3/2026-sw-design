# EasyTrade Android

EasyTrade의 Android Native Kotlin 앱 초안입니다.

## 현재 구현된 화면

- 로그인 화면
- 회원가입 화면
- 홈 화면
- 주식 검색 화면
- 주식 상세 화면
- 매수 화면
- 매도 화면
- 포트폴리오 화면

## 기술

- Android Native Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose

## 실행 방법

1. Android Studio를 설치한다.
2. Android Studio에서 이 `android/` 디렉터리를 연다.
3. Gradle Sync를 실행한다.
4. 에뮬레이터 또는 실제 Android 기기에서 `app`을 실행한다.

## 현재 한계

현재 앱 화면은 백엔드 API 연동 전 단계이다. 따라서 회원가입, 로그인, 매수,
매도, 포트폴리오는 앱 내부 임시 상태 데이터로 동작한다.

백엔드 실행과 API 검증이 끝나면 다음 작업으로 앱에 API 통신 계층을 추가한다.

```text
Android 앱
  -> Spring Boot 백엔드 API
  -> MySQL
  -> 한국투자증권 Open API
```
