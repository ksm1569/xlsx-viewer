# XLSX Viewer Plugin — 요건정의서 (MVP)

> IntelliJ IDEA Ultimate 용 사내 xlsx 뷰어 플러그인.
> 본 문서는 늘봄학교 통합플랫폼(`klic.nForu_v3_pen`)과는 **독립된 별도 Gradle 프로젝트**의 사양을 정의한다. 본 repo 루트에는 요건정의만 두고, 플러그인 코드 자체는 별도 디렉토리(예: `D:\dev\xlsx-viewer\`)에 생성한다.

---

## 1. 배경 / 목적

- 회사 업무 중 xlsx 파일(감리 산출서, 메뉴구조, 프로그램 목록 등 `doc/감리작업/*.xlsx`)을 자주 열어야 함
- 매번 외부 Excel/LibreOffice 실행은 컨텍스트 스위칭 비용이 큼
- JetBrains Marketplace 상위 xlsx 플러그인 대부분이 중국어 README 우세(obiscr/ExcelReader 계열) → 사내 도입에 심리적 저항감 있음
- 신뢰할 수 있는 출처(본인 작성)의 한국인 친화 뷰어가 필요

## 2. MVP 범위 (Phase 1)

**포함**
- `.xlsx` 파일을 IDE 안에서 더블클릭 시 커스텀 뷰어로 열림
- 시트 탭 UI (워크북의 모든 시트를 하단/상단 탭으로 표시)
- 현재 선택된 시트의 셀 내용을 **읽기 전용** 테이블로 렌더링
- 한글 문자열이 깨지지 않고 정상 표시
- 결합셀(merged cell) 은 표시상 깨지지 않게 처리 (좌상단 셀에만 값 표시, 나머지는 빈 셀)
- ZIP 패키징 → 동료에게 전달 → "Install Plugin from Disk" 로 설치 가능

**제외 (Out of Scope, Phase 1)**
- 셀 편집 / 저장 / 행·열 추가
- 수식(formula) 평가 (값으로 캐시된 결과만 보여줌)
- 셀 서식(배경색, 폰트, 테두리) 시각 재현
- 차트, 이미지, 피벗테이블
- xls(구버전 바이너리), csv, tsv (별도 플러그인이 이미 충분)
- 검색·필터·정렬 (Phase 2)
- 한글 자모 분리 검색, 한국 공문서 패턴 튜닝 (Phase 2)
- JetBrains Marketplace 공식 등록 (Phase 3)

## 3. 사용자 시나리오 (User Story)

| ID | As a | I want to | So that |
|----|------|-----------|---------|
| US-1 | 개발자 | Project View 에서 .xlsx 파일을 더블클릭 | 외부 앱 실행 없이 내용을 본다 |
| US-2 | 개발자 | 여러 시트가 있는 워크북에서 시트 탭을 클릭 | 다른 시트의 내용으로 전환한다 |
| US-3 | 개발자 | 한글이 많은 시트(예: 감리 산출물) | 깨짐 없이 정상 표시된다 |
| US-4 | 개발자 | 결합셀이 많은 표(공문서 양식) | 레이아웃이 망가지지 않는다 |
| US-5 | 동료 | 빌드된 ZIP 파일을 전달받음 | "Install from Disk" 한 번으로 설치한다 |

## 4. 기능 요구사항 (FR)

| ID | 항목 | 상세 |
|----|------|------|
| FR-1 | 파일 타입 등록 | `.xlsx` 확장자를 신규 FileType 으로 등록 (FileType + FileTypeFactory 또는 `<fileType>` 익스텐션) |
| FR-2 | 커스텀 에디터 등록 | `FileEditorProvider` 구현, `accept()` 에서 .xlsx 만 허용 |
| FR-3 | 워크북 파싱 | Apache POI `XSSFWorkbook` (또는 streaming 필요 시 `XSSFReader`) |
| FR-4 | 시트 탭 | `JBTabbedPane` 으로 워크북의 모든 시트 노출, 시트명을 탭 라벨로 |
| FR-5 | 테이블 렌더 | `JBTable` + 커스텀 `TableModel`. 행 헤더는 1,2,3..., 열 헤더는 A,B,C... |
| FR-6 | 셀 값 표시 | `Cell.getCellType()` 별로 String/Numeric/Boolean/Formula(캐시값) 처리, Date 는 한국 로케일 `yyyy-MM-dd HH:mm:ss` |
| FR-7 | 결합셀 처리 | `Sheet.getMergedRegions()` 로 영역 조회, 좌상단 셀에만 값, 시각적으로 span 표시(MVP 는 단순히 좌상단만 표시하고 나머지는 빈칸) |
| FR-8 | 한글 인코딩 | OOXML 은 UTF-8 표준이지만 폰트 fallback 으로 한글 가독성 확보 (기본 Dialog 폰트 사용) |
| FR-9 | 읽기 전용 | `isModified()` 항상 false, `Document` 미생성 |

## 5. 비기능 요구사항 (NFR)

| ID | 항목 | 목표 |
|----|------|------|
| NFR-1 | 성능 | 100KB / 1만 행 이하 파일을 1초 내 표시 |
| NFR-2 | 메모리 | MVP 는 인메모리 로딩 허용. 100MB 초과 파일은 경고 다이얼로그 후 사용자 확인 |
| NFR-3 | 호환성 | IntelliJ Platform 2025.1 (`since-build="251"`) ~ 최신 (`until-build="261.*"`) |
| NFR-4 | JDK | Java 21 |
| NFR-5 | 라이선스 | 사내 사용은 MIT 또는 Apache-2.0 가정. POI 가 Apache-2.0 이므로 호환 |
| NFR-6 | 한국어 UI | 모든 메뉴/메시지/에러 다이얼로그를 한국어로 (Resource Bundle 분리해서 추후 영어 추가 대비) |

## 6. 기술 스택

| 영역 | 선택 |
|------|------|
| 언어 | **Java 21** |
| 빌드 | Gradle (Kotlin DSL `build.gradle.kts`) + IntelliJ Platform Gradle Plugin 2.x |
| 베이스 템플릿 | `JetBrains/intellij-platform-plugin-template` |
| xlsx 파싱 | Apache POI 5.x (`poi-ooxml`, `poi-ooxml-lite`) |
| UI | Swing — `JBTable`, `JBTabbedPane`, `JBScrollPane` (IntelliJ 룩앤필 자동 적용) |
| 테스트 | JUnit 5 + IntelliJ Platform Test Fixtures (`intellijPlatform { testFramework(...) }`) |
| 형상관리 | Git (별도 repo, `nForu_v3_pen` 과 분리) |

## 7. 확장성 고려 (C → A 마이그레이션 전제)

> "MVP는 ZIP 직접 배포(C 안), 안정화 후 JetBrains Marketplace 공개(A 안) 로 전환" 이라는 결정에 따라, 초기부터 아래 항목을 충족시킨다.

| 항목 | MVP 단계에서도 미리 준비 | 이유 |
|------|------------------------|------|
| `plugin.xml` ID | `kr.bsen.intellij.xlsxviewer` 등 회사·역도메인 기반 unique ID | Marketplace 는 ID 변경 불가 (변경 시 새 플러그인으로 인식) |
| 버전 체계 | SemVer `MAJOR.MINOR.PATCH` (예: `0.1.0`) | Marketplace 가 동일 버전 재업로드 거부 |
| `since-build` / `until-build` | 명시적 지정 | 마켓 자동 호환성 검사 통과 |
| Vendor 정보 | `<vendor email="..." url="...">BSEN</vendor>` 정확히 기재 | 마켓 등록 시 그대로 노출 |
| Change Notes | `CHANGELOG.md` 따로 두고 `<change-notes>` 에 패치 시 주입 | Marketplace 가 릴리스 노트로 사용 |
| 라이선스 파일 | `LICENSE` 루트에 동봉 | Marketplace 필수 항목 |
| 서명 키 | 첫 빌드부터 self-signed 인증서로 `signPlugin` 사용 (사내 배포 단계에서도) | 마켓 전환 시 동일 키 재사용 가능 |
| Resource Bundle | `messages.XlsxViewerBundle` 분리, 한국어 default + 영어 i18n | 마켓 전환 시 글로벌 사용자 대응 용이 |
| 아이콘 | 16x16, 13x13 PNG/SVG 미리 준비 (`META-INF/pluginIcon.svg`) | Marketplace 노출 카드에 사용 |
| GitHub Actions CI | 빌드/테스트 자동화 + Release 시 ZIP artifact | 마켓 자동 publish 로 자연스럽게 확장 |
| README.md | 사용법 영어/한국어 병기 | 마켓 description 으로 그대로 사용 |

## 8. 향후 로드맵

| Phase | 목표 | 추가 기능 |
|-------|------|----------|
| **1 (MVP)** | 사내 ZIP 배포, 본인+팀 | 시트 탭, 읽기 전용 테이블 |
| 2 | 사내 안정화 | 검색·정렬·필터, 셀 서식(배경/폰트), Freeze Panes, 한국어 자모 분리 검색 |
| 3 | Marketplace 공개 | xls 지원, csv 통합, 차트 미리보기, 셀 편집(읽기→쓰기), 다국어 UI |

## 9. 별도 프로젝트 디렉토리 구조 (제안)

```
D:\dev\xlsx-viewer\                       # 본 프로젝트와 분리된 신규 폴더
├── .github\workflows\build.yml           # CI (Phase 3 부터 활성)
├── gradle\wrapper\
├── src\
│   ├── main\
│   │   ├── java\kr\bsen\intellij\xlsxviewer\
│   │   │   ├── XlsxFileType.java
│   │   │   ├── XlsxFileEditorProvider.java
│   │   │   ├── XlsxFileEditor.java
│   │   │   ├── ui\
│   │   │   │   ├── XlsxViewerPanel.java
│   │   │   │   └── SheetTableModel.java
│   │   │   └── parser\
│   │   │       └── WorkbookLoader.java
│   │   └── resources\
│   │       ├── META-INF\
│   │       │   ├── plugin.xml
│   │       │   └── pluginIcon.svg
│   │       └── messages\
│   │           ├── XlsxViewerBundle.properties        # 한국어 (default)
│   │           └── XlsxViewerBundle_en.properties     # 영어
│   └── test\java\...
├── build.gradle.kts
├── gradle.properties                     # platformVersion, pluginVersion 등
├── settings.gradle.kts
├── CHANGELOG.md
├── LICENSE
└── README.md
```

## 10. 개발환경 초기 셋팅

> 본 섹션은 **템플릿 clone 직후부터 MVP 코드 작성 직전까지** 의 단계를 self-contained 하게 정리한다. 이 문서 하나만 보고 셋업을 끝낼 수 있도록 한다.

### 10.1 사전 조건

- JDK 21 설치 완료 (IntelliJ Project Structure → SDKs 에 등록되어 있어야 함)
- 프로젝트 폴더 (`D:\dev\xlsx-viewer` 권장) 가 `JetBrains/intellij-platform-plugin-template` 기반으로 clone 되어 있어야 함
- 템플릿의 `.git` 은 제거하고 본인 `git init` 으로 새로 시작한 상태
- 이 문서 (`XLSX_VIEWER_REQUIREMENTS.md`) 는 프로젝트 폴더 루트로 이동해 둔 상태

### 10.2 IntelliJ 에서 프로젝트 열기

1. **File → Open** → 프로젝트 폴더 선택 → "Trust and Open"
2. 첫 Gradle Sync 는 IntelliJ Platform SDK 다운로드로 5~15분 소요. 끝까지 대기

### 10.3 JDK 21 매핑 (반드시 2곳 모두)

- `Ctrl+Alt+Shift+S` → **Project Settings → Project**
  - **SDK** = 21
  - **Language level** = 21
- `Ctrl+Alt+S` → **Build, Execution, Deployment → Build Tools → Gradle**
  - **Gradle JVM** = 21

### 10.4 `gradle.properties` (전체 교체)

```properties
pluginGroup = kr.bsen.intellij
pluginName = xlsx-viewer
pluginRepositoryUrl = https://github.com/ksm1569/xlsx-viewer

pluginVersion = 0.1.0
pluginSinceBuild = 251
pluginUntilBuild = 261.*

platformType = IC
platformVersion = 2025.1
platformPlugins =
platformBundledPlugins =

gradleVersion = 8.10
```

### 10.5 `build.gradle.kts` (추가/수정 부분)

JDK 21 toolchain 확인 (템플릿에 이미 `jvmToolchain` 이 있으면 값만 21로):

```kotlin
kotlin {
    jvmToolchain(21)
}
```

`dependencies { ... }` 블록 안에 Apache POI 추가:

```kotlin
dependencies {
    intellijPlatform {
        create(providers.gradleProperty("platformType"),
               providers.gradleProperty("platformVersion"))
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    implementation("org.apache.poi:poi-ooxml:5.3.0") {
        exclude(group = "org.apache.logging.log4j")
    }
}
```

### 10.6 `src/main/resources/META-INF/plugin.xml` (전체 교체)

```xml
<idea-plugin>
    <id>kr.bsen.intellij.xlsxviewer</id>
    <name>XLSX Viewer (Korean)</name>
    <vendor email="tnals1569@gmail.com" url="https://github.com/ksm1569">BSEN</vendor>

    <description><![CDATA[
        한국인 친화 xlsx 뷰어. 시트 탭과 읽기 전용 테이블로 표시.
    ]]></description>

    <depends>com.intellij.modules.platform</depends>

    <extensions defaultExtensionNs="com.intellij">
        <fileType name="XLSX"
                  implementationClass="kr.bsen.intellij.xlsxviewer.XlsxFileType"
                  fieldName="INSTANCE"
                  language=""
                  extensions="xlsx"/>
        <fileEditorProvider implementation="kr.bsen.intellij.xlsxviewer.XlsxFileEditorProvider"/>
    </extensions>
</idea-plugin>
```

> ⚠ 10.7 단계에서 `XlsxFileType`/`XlsxFileEditorProvider` Java 클래스가 아직 없어 `runIde` 시 "클래스 못 찾음" 경고가 뜰 수 있다. 그 경우 `<extensions>` 블록 전체를 일단 주석 처리하고 인프라 확인 후, MVP 코드 작성 시 주석 해제한다.

### 10.7 인프라 동작 확인

IntelliJ 우측 **Gradle** 탭 → `Tasks → intellij platform → runIde` 더블클릭
→ 샌드박스 IDE 가 뜨고, 그 안 **Settings → Plugins → Installed** 에 "XLSX Viewer (Korean)" 가 보이면 셋업 완료.

### 10.8 (옵션) 첫 ZIP 빌드 확인

PowerShell:
```powershell
.\gradlew.bat buildPlugin
```

`build/distributions/xlsx-viewer-0.1.0.zip` 생성 확인. 본인 IntelliJ 에 **Install Plugin from Disk** 로 설치해서 가벼운 검증 가능.

### 10.9 Git 초기 커밋

```powershell
git add .
git commit -m "초기 셋업: IntelliJ Platform Plugin Template + JDK 21 + POI 5.3.0"
```

GitHub repo 연결 시:
```powershell
git remote add origin https://github.com/ksm1569/xlsx-viewer.git
git branch -M main
git push -u origin main
```

### 10.10 자주 막히는 지점

- **첫 Gradle sync 타임아웃** — `repo.maven.apache.org`, `cache-redirector.jetbrains.com`, `download.jetbrains.com` 접속 가능한지 확인. 사내 프록시면 `gradle.properties` 에 `systemProp.https.proxyHost=...` 추가
- **JDK 21 인식 안됨** — 10.3 의 2곳을 다시 확인. 둘 다 21이어야 함
- **`platformVersion` 과 IDE 버전 불일치 경고** — 본인 IntelliJ Ultimate 가 2025.1 미만이면 IDE 업데이트 또는 `platformVersion` 을 사용 중인 IDE 빌드에 맞춤
- **POI 의존성 충돌** — `log4j` 외에 `commons-logging` 도 충돌하면 `exclude(group = "commons-logging")` 추가

---

## 11. 검증 기준 (Definition of Done — MVP)

- [ ] `./gradlew runIde` 로 샌드박스 IDE 가 실행되고, 그 안에서 임의의 .xlsx 파일을 더블클릭하면 커스텀 뷰어가 열린다
- [ ] `doc/감리작업/BAS_DE11_프로그램 목록_v1.8_20260508.xlsx` 가 정상 표시되며 한글이 깨지지 않는다
- [ ] 시트 탭으로 시트 전환 가능
- [ ] 결합셀이 많은 메뉴구조 xlsx 가 레이아웃 깨짐 없이 표시
- [ ] `./gradlew buildPlugin` 으로 `build/distributions/*.zip` 생성
- [ ] 팀원 PC IntelliJ 에 "Install Plugin from Disk" 로 설치 성공
- [ ] 설치된 IDE 에서 동일 동작 확인

## 12. 참고

- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Editors | IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/editors.html)
- [FileEditor.java (community source)](https://github.com/JetBrains/intellij-community/blob/master/platform/editor-ui-api/src/com/intellij/openapi/fileEditor/FileEditor.java)
- [Apache POI XSSF](https://poi.apache.org/components/spreadsheet/)
- [SeeSharpSoft/intellij-csv-validator](https://github.com/SeeSharpSoft/intellij-csv-validator) — 가장 좋은 오픈소스 레퍼런스
- [obiscr/ExcelReader](https://github.com/obiscr/ExcelReader) — 기능 벤치마크
- [Custom Plugin Repository](https://plugins.jetbrains.com/docs/intellij/custom-plugin-repository.html)
- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
