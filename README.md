# XLSX Viewer (Korean)

> IntelliJ IDEA용 한국인 친화 xlsx 뷰어 플러그인.
> 외부 Excel / LibreOffice 실행 없이 IDE 안에서 `.xlsx` 파일을 시트 탭과 읽기 전용 테이블로 표시합니다.

## 소개

업무 중 자주 열어보는 `.xlsx` 문서(감리 산출서, 메뉴구조, 프로그램 목록 등)를 매번 외부 앱으로 띄우는 컨텍스트 스위칭 비용을 줄이기 위해 만든 사내용 도구입니다. 기존 JetBrains Marketplace 의 xlsx 뷰어 플러그인 대부분이 중국어 README 위주라 사내 도입에 심리적 저항감이 있어, 한국어를 기본으로 신뢰할 수 있는 출처에서 직접 만들었습니다.

## 주요 기능 (Phase 1 / MVP)

- `.xlsx` 파일 더블클릭 시 커스텀 뷰어로 열림
- 워크북의 **모든 시트를 하단 탭** 으로 노출, 시트 전환 가능
- 현재 선택된 시트의 셀 내용을 **읽기 전용 테이블** 로 렌더링
  - 행 헤더: `1, 2, 3, ...`
  - 컬럼 헤더: `A, B, C, ... Z, AA, AB, ...` (Excel 스타일)
- **한글 문자열 정상 표시** (UTF-8 + IntelliJ 기본 폰트)
- **결합셀(merged cell) 처리**: 좌상단에만 값 표시, 나머지는 빈 칸 (레이아웃 보존)
- **셀 타입별 표시**: 문자열 / 숫자 / Boolean / Date(한국 로케일 `yyyy-MM-dd HH:mm:ss`) / Formula 캐시값
- **100MB 초과 파일 경고 다이얼로그** (사용자 확인 후 로딩)
- 한국어 기본 메시지 + 영어 i18n stub (마켓 등록 대비)

## 향후 로드맵

| Phase | 목표 | 추가 기능 |
|---|---|---|
| 1 (현재) | 사내 ZIP 배포 | 시트 탭 + 읽기 전용 테이블 |
| 2 | 사내 안정화 | 검색·정렬·필터, 셀 서식(배경/폰트), Freeze Panes, 한국어 자모 분리 검색 |
| 3 | Marketplace 공개 | `.xls` 지원, csv 통합, 차트 미리보기, 셀 편집(읽기→쓰기), 다국어 UI |

다음 세션에서 이어 개발할 때 필요한 상세 인수인계 정보는 [`XLSX_VIEWER_REQUIREMENTS.md`](./XLSX_VIEWER_REQUIREMENTS.md) §13 참고.

## 설치 (사용자)

빌드된 ZIP 파일을 전달받았다면:

1. IntelliJ IDEA 실행
2. **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
3. `xlsx-viewer-0.1.0.zip` 선택
4. IDE 재시작
5. Project View 에서 `.xlsx` 파일을 더블클릭하면 본 뷰어가 자동으로 열립니다.

## 빌드 (개발자)

### 사전 조건

- JDK 21
- IntelliJ IDEA 2025.1 이상

### Gradle 빌드 (CLI)

```powershell
# 환경변수 JAVA_HOME 이 JDK 21 을 가리키도록 설정한 뒤
.\gradlew.bat buildPlugin
```

산출물: `build/distributions/xlsx-viewer-0.1.0.zip`

### 개발 중 동작 확인

IntelliJ 우측 **Gradle** 탭 → `Tasks → intellij platform → runIde` 더블클릭.
샌드박스 IDE 가 띄워지고 거기서 `.xlsx` 파일을 더블클릭하면 본 뷰어가 동작합니다.

## 호환성

| 항목 | 값 |
|---|---|
| IntelliJ Platform | 2025.1 이상 (since-build `251`, until-build `261.*`) |
| Java | 21 |
| 지원 파일 형식 | `.xlsx` (OOXML). `.xls` / `.csv` / `.tsv` 는 별도 플러그인 사용 |

## 기술 스택

- Java 21, Swing (`JBTable`, `JBTabbedPane`, `JBScrollPane`)
- Gradle Kotlin DSL + IntelliJ Platform Gradle Plugin 2.x
- Apache POI 5.3.0 (`poi-ooxml`)

## 알려진 사항

- POI 가 `log4j-api` 를 직접 호출하지만 본 플러그인은 출력 구현(`log4j-core`)을 동봉하지 않습니다. 따라서 첫 POI 사용 시 IDE 로그에 `Log4j2 could not find a logging implementation ... Using SimpleLogger` 한 줄이 표시될 수 있습니다. **무해한 정보 메시지** 이며 동작에 영향 없습니다.
- 셀 서식(배경/폰트/테두리), 차트, 이미지, 수식 평가는 Phase 1 범위 외입니다 (Phase 2~3 예정).

## 라이선스

Apache License 2.0 — [LICENSE](./LICENSE) 참고.
본 플러그인은 Apache POI (Apache-2.0) 를 사용합니다.

## Vendor

**BSEN** — 사내 자체 개발.
이슈 / 제안: [tnals1569@gmail.com](mailto:tnals1569@gmail.com)

---

## English (Brief)

**XLSX Viewer (Korean)** is an IntelliJ IDEA plugin that opens `.xlsx` files inside the IDE as a sheet-tabbed, read-only table viewer. Primarily designed for Korean office documents (handles merged cells, Hangul text, and Korean locale date formatting). Korean is the default UI language; English is provided as an i18n stub.

- Open `.xlsx` by double-click → sheet tabs + read-only table
- Merged cells, Korean date locale, formula-cached values
- 100 MB warning dialog
- Apache POI 5.3.0 under the hood
- Requires IntelliJ IDEA 2025.1+ and JDK 21

Install via **Settings → Plugins → ⚙️ → Install Plugin from Disk...** with the built ZIP.

License: Apache-2.0.
