package kr.bsen.intellij.xlsxviewer.ui;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 한 개의 시트를 표 형태로 노출하는 읽기 전용 모델.
 *
 * <p>처리 규칙:
 * <ul>
 *   <li>FR-5: 컬럼 헤더는 Excel 스타일 (A, B, ..., Z, AA, AB, ...).</li>
 *   <li>FR-6: STRING/NUMERIC/BOOLEAN/Formula(캐시) 처리, Date 는 한국 로케일 yyyy-MM-dd HH:mm:ss.</li>
 *   <li>FR-7: 결합셀의 좌상단만 값 표시, 나머지 셀은 빈 문자열.</li>
 *   <li>Phase 2-2: 결합셀 비-원점 좌표도 좌상단 셀의 스타일을 노출하도록 {@link #getStyleSourceCell} 제공.</li>
 * </ul>
 */
public final class SheetTableModel extends AbstractTableModel {

    private static final ThreadLocal<SimpleDateFormat> DATE_FMT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
    );

    private final Sheet sheet;
    private final int rowCount;
    private final int columnCount;
    /** (row,col) → [originRow, originCol]. 진입돼 있으면 결합 영역 내 비-원점 셀. */
    private final Map<Long, int[]> mergedOriginMap;

    public SheetTableModel(@NotNull Sheet sheet) {
        this.sheet = sheet;
        this.rowCount = computeRowCount(sheet);
        this.columnCount = computeColumnCount(sheet);
        this.mergedOriginMap = buildMergeIndex(sheet);
    }

    private static int computeRowCount(Sheet sheet) {
        int last = sheet.getLastRowNum();
        return last < 0 ? 0 : last + 1;
    }

    private static int computeColumnCount(Sheet sheet) {
        int max = 0;
        for (Row row : sheet) {
            if (row != null) {
                max = Math.max(max, row.getLastCellNum());
            }
        }
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            max = Math.max(max, region.getLastColumn() + 1);
        }
        return Math.max(max, 0);
    }

    private static Map<Long, int[]> buildMergeIndex(Sheet sheet) {
        Map<Long, int[]> map = new HashMap<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            int firstRow = region.getFirstRow();
            int firstCol = region.getFirstColumn();
            int[] origin = {firstRow, firstCol};
            for (int r = firstRow; r <= region.getLastRow(); r++) {
                for (int c = firstCol; c <= region.getLastColumn(); c++) {
                    if (r == firstRow && c == firstCol) continue;
                    map.put(key(r, c), origin);
                }
            }
        }
        return map;
    }

    private static long key(int row, int col) {
        // col 은 Excel 최대 16384(2^14) 이내. 20-bit shift 로 충분히 안전.
        return ((long) row << 20) | (col & 0xFFFFFL);
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public String getColumnName(int column) {
        return excelColumnName(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public @NotNull Object getValueAt(int rowIndex, int columnIndex) {
        if (mergedOriginMap.containsKey(key(rowIndex, columnIndex))) {
            return "";
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null) return "";
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatCell(cell);
    }

    /**
     * 셀 서식 추출을 위한 source cell.
     * 결합 영역 내부 셀이면 좌상단 셀, 일반 셀이면 그 셀 자체.
     */
    public @Nullable Cell getStyleSourceCell(int row, int col) {
        int[] origin = mergedOriginMap.get(key(row, col));
        int sr = origin == null ? row : origin[0];
        int sc = origin == null ? col : origin[1];
        Row poiRow = sheet.getRow(sr);
        if (poiRow == null) return null;
        return poiRow.getCell(sc, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }

    private static String formatCell(@NotNull Cell cell) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatNumeric(cell);
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case ERROR -> "#ERR";
            case BLANK, FORMULA, _NONE -> "";
        };
    }

    private static String formatNumeric(@NotNull Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d == null ? "" : DATE_FMT.get().format(d);
        }
        double v = cell.getNumericCellValue();
        if (!Double.isInfinite(v) && !Double.isNaN(v) && v == Math.floor(v)
                && Math.abs(v) < 1e15) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }

    /** 0→A, 25→Z, 26→AA, 27→AB, ... */
    static @NotNull String excelColumnName(int columnIndex) {
        StringBuilder sb = new StringBuilder();
        int n = columnIndex;
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }
}
