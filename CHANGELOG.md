# XLSX Viewer Changelog

본 파일은 [Keep a Changelog](https://keepachangelog.com) 형식을 따릅니다.

## [Unreleased]

### 예정
- 검색·정렬·필터 (Phase 2)
- 셀 서식(배경/폰트) 시각 재현 (Phase 2)
- Freeze Panes (Phase 2)
- 한국어 자모 분리 검색 (Phase 2)

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

[Unreleased]: https://github.com/ksm1569/xlsx-viewer/compare/0.1.0...HEAD
[0.1.0]: https://github.com/ksm1569/xlsx-viewer/releases/tag/0.1.0
