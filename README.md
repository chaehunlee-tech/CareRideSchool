# CareRide School

통학차량 동승보호자가 하원 출발 전에 학생의 탑승을 확인하고, 하차할 학생이 없는 정류장을 경로에서 제외하는 차량용 앱 시연입니다.

## 개발 환경

| 구성 | 버전 |
| --- | --- |
| JDK / Gradle Wrapper / Android Gradle Plugin / Kotlin | 17 / 8.13 / 8.13.2 / 2.2.21 |
| Android | 컴파일·대상 API 34, 최소 API 28 |
| Pleos 시스템 이미지 | Pleos Connect 2.1.0, API 34, Windows x86_64 |
| Pleos Maven | Vehicle 2.3.2, NaviHelper 3.0.5, FusedLocation 1.1.3 |

Vehicle과 FusedLocation은 Pleos 공식 샘플의 버전입니다. 시작 가이드 및 초기 릴리스 노트에 표시된 Vehicle 2.3.1과 FusedLocation 1.1.2는 공개 Maven 저장소에서 404를 반환했습니다.

## 빌드와 실행

Android Studio로 이 폴더를 열고 Gradle JDK에 JDK 17을 선택합니다. SDK Manager에서 Android 14(API 34), Platform-Tools, Android Emulator를 설치합니다. 터미널 빌드에는 `JAVA_HOME`과 `ANDROID_HOME`을 설정하거나 Android Studio에서 SDK 경로를 설정합니다.

```powershell
.\gradlew.bat :app:assembleDemoDebug :app:assemblePleosDebug
```

- **demoDebug**는 일반 Android API 28+에서 실행할 수 있습니다. 도어 조회도 모의 응답입니다.
- **pleosDebug**는 Pleos 에뮬레이터용입니다. 운전석 도어 위치 조회가 Vehicle SDK를 사용합니다. 명단, 안전벨트와 내비 경로는 시연 데이터입니다.
- 빌드 결과는 `app/build/outputs/apk/` 아래에 생성됩니다. 이 프로젝트의 시연 APK는 [일반 Android용](apk/CareRideSchool-demo-debug.apk)과 [Pleos 에뮬레이터용](apk/CareRideSchool-pleos-debug.apk)입니다.

### Pleos 에뮬레이터

1. SDK Manager → SDK Update Sites에 [Windows용 Pleos 이미지 저장소](https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml)를 추가합니다.
2. SDK Platforms에서 Show Package Details를 켜고 API 34의 Pleos Connect v2.1.0 이미지를 설치한 뒤 Android Studio를 다시 시작합니다.
3. Device Manager → Automotive → Pleos Connect v2.1.0을 선택합니다. API 34 Pleos 이미지를 지정하고 Cold boot, RAM 4GB로 만든 뒤 시작합니다.
4. 부팅 후 `pleosDebug`를 설치하고 앱에서 **운전석 도어 위치 조회**를 누릅니다. 아래 명령의 응답 로그와 앱 표시를 확인합니다. 에뮬레이터가 여러 개 연결돼 있으면 `adb -s <serial>`로 대상 기기를 선택합니다.

```powershell
adb shell getprop sys.boot_completed
adb forward tcp:8088 tcp:8088
adb logcat -s CareRideDoor
```

PC 브라우저에서 `http://127.0.0.1:8088/`을 열고 Virtual Console의 Convenience → Door에서 운전석 도어 값을 바꾼 뒤 다시 조회합니다. Manifest의 `pleos.car.permission.CAR_DOORS`가 이 호출에 필요합니다.

## 시연 흐름

1. 하원 출발 화면에서 예정 4명과 가상 벨트 체결 3석을 대조합니다. 벨트 개수만으로 누가 빠졌는지 결정할 수 없으므로 담당자가 이름별로 확인합니다.
2. A·B·D를 **탑승**, C를 **미탑승**으로 표시합니다. 확인을 마치면 C만 내리는 중앙공원이 제외 후보로 표시됩니다.
3. **변경 승인 · 모의 내비 갱신**을 누릅니다. 경로가 도서관 → 한빛아파트로 바뀝니다.
4. 초기화한 뒤 A만 미탑승으로 처리하면 B가 내리는 도서관은 유지됩니다. 전원이 미탑승이면 경로 갱신 없이 운행 취소 여부를 확인하도록 표시합니다.

## SDK 연동 범위

- Vehicle은 `getCurrentDoorPosition()`과 성공·실패 콜백을 화면 및 로그에 연결합니다.
- NaviHelper와 FusedLocation은 Pleos 빌드에 의존성을 연결했습니다. 시연 경로는 앱 내부에서 갱신합니다. 실제 내비 연동 시 정차 여부를 확인하고, 남은 정류장 중 마지막을 목적지로 지정합니다. 경유지가 있을 때 `requestRouteWithWaypoints()`, 목적지만 있을 때 `requestRoute()`를 사용합니다. `NAVI_ROUTE_SEARCH`를 선언하고 내비 반영을 확인한 뒤 경로를 확정하는 것이 다음 연동 지점입니다.
- FusedLocation으로 위치를 읽는 단계에는 `FUSED_LOCATION` 권한과 기기 지원 확인이 필요합니다. 좌석·벨트는 대상 좌석의 가용성과 앱의 접근 권한을 확인할 때까지 모의 값으로 표시합니다.

## 검증 결과

- Windows 11에서 Pleos Connect 2.1.0(API 34)을 부팅하고 Virtual Console HTTP 응답을 확인했습니다.
- Pleos에 설치한 앱에서 Vehicle SDK의 운전석 도어 위치 응답 `0`을 확인했습니다.
- Pleos 앱 화면 테스트에서 H03·H08의 중앙공원 제외와 H07의 공동 정류장 유지가 통과했습니다.
- 두 APK의 서명 검증과 정적 검사를 완료했습니다.

[Pleos 시작 가이드](https://document.pleos.ai/en/docs/pleos-only/getting-started) · [에뮬레이터](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) · [Virtual Console](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/use-vehicle-emulator) · [공모전 제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission)
