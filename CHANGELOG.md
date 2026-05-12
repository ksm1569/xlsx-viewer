# XLSX Viewer Changelog

본 파일은 [Keep a Changelog](https://keepachangelog.com) 형식을 따릅니다.

## [Unreleased]

### 예정 (Phase 3)
- 좌측 열 Freeze Panes (현 V1 미지원)
- 셀 indexed/theme color 변환
- xls(구버전) 지원, csv 통합, 차트 미리보기, 셀 편집
- JetBrains Marketplace 공식 등록 (pluginIcon.svg, signPlugin, CI)

## [0.2.0] - 2026-05-12

### Added (Phase 2)
- 시트별 검색 도구바 + 매칭 셀 하이라이트 + 필터 모드(매칭된 행만 표시)
- 한글 자모(초성) 검색 토글 — `ㅇㅅㅅ` → "이순신" 매칭 (`util.HangulSearch.toChosung`)
- POI `CellStyle` 기반 셀 배경/글자색/굵기/기울임 시각 재현 (배경 명도에 따라 글자색 가독성 자동 보정)
- 상단 N행 Freeze Panes — `JBScrollPane.setColumnHeaderView` 에 freeze 영역 테이블을 끼워 가로 스크롤 자동 동기화

### Technical
- POI 5.3.0 API 시그니처 차이 흡수 — `CellStyle.getFillPattern()` 가 `FillPatternType` 을 직접 반환, `PaneInformation` 은 `ss.util` 패키지로 이동
- `SheetTableModel` 결합셀 매핑을 좌상단 좌표 형태로 변경 → 결합 영역 전체에 좌상단 셀 스타일 적용
- 결합셀이 있는 시트는 정렬 의미가 모호하므로 `TableRowSorter` 의 모든 컬럼 `setSortable(false)` (Sorter 는 RowFilter 호스트 역할만)
- 셀 색상 변환 규칙 (다크 테마 IntelliJ 위에서 Excel 원본 가독성 확보):
    - `isAuto()` / `isThemed()` / `IndexedColors.AUTOMATIC` 색은 변환하지 않고 IntelliJ 테마 기본색에 위임. POI 가 theme/auto 를 RGB(0,0,0) = 검정으로 fallback 하는 케이스 방어
    - 색 추출 순서를 `getRGB()` (tint 미적용) 우선 + `getRGBWithTint()` fallback 으로 변경. POI 의 한국어 워크북 tint 변환이 어두운 회색으로 빗나가는 케이스 방어
    - 명시적 fg/bg 의 명도 차가 0.25 미만이면 가독성 우선해 fg 자동 보정
    - `NO_FILL` 셀(명시적 배경 없음)은 다크 IntelliJ 배경 대신 흰 배경을 강제. Excel 원본이 흰 배경을 전제로 디자인한 의도(셀 데이터 영역 = 라이트, 헤더/탭/검색바 = IntelliJ 다크 테마) 재현

## [0.1.0] - 2026-05-11

### Added
- `.xlsx` 파일 더블클릭 시 커스텀 FileEditor 로 열림 (FR-1, FR-2)
- 워크북 시트 탭 (`JBTabbedPane`, 하단 배치) 으로 모든 시트 노출 (FR-4)
- 읽기 전용 테이블 렌더링 (`JBTable` + `SheetTableModel`) — 행 헤더 `1, 2, 3...`, 컬럼 헤더 `A, B, C...` (FR-5)
- 셀 타입별 값 표시 — 문자열 / 숫자 / Boolean / Formula(캐시값) / Date(한국 로케일 `yyyy-MM-dd HH:mm:ss`) (FR-6)
- 결합셀 처리 — `Sheet.getMergedRegions()` 기반, 좌상단만 값 표시 (FR-7)
- 100 MB 초과 파일 경고 다이얼로그 (NFR-2)
- 한국어 기본 + 영어 i18n stub Resource Bundle (NFR-6)
- 사용자/개발자 가이드 README, 인수인계 섹션이 포함된 요건정의서

### Technical
- Apache POI 5.3.0 통합 — `log4j-core` 만 exclude, `log4j-api` 는 유지하여 `NoClassDefFoundError` 회피
- IntelliJ Platform 2025.1 호환 (since-build `251`, until-build `261.*`)
- `XlsxFileEditorProvider` 에 `DumbAware` 마커 추가 (`HIDE_DEFAULT_EDITOR` 정책 필수 조건)
- `instrumentCode` 비활성화 — Microsoft 빌드 OpenJDK 21 (`ms-21.0.11`) 호환 회피
- 템플릿 Kotlin 스캐폴드 (`org.jetbrains.plugins.template`) 전체 제거

[Unreleased]: https://github.com/ksm1569/xlsx-viewer/compare/0.2.0...HEAD
[0.2.0]: https://github.com/ksm1569/xlsx-viewer/releases/tag/0.2.0
[0.1.0]: https://github.com/ksm1569/xlsx-viewer/releases/tag/0.1.0
