package kr.bsen.intellij.xlsxviewer.ui;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * POI 셀 스타일을 AWT {@link Color}/폰트 스타일로 변환한다.
 *
 * <p>같은 시트에서 다수의 셀이 동일한 CellStyle 인덱스를 공유하므로,
 * 인덱스 단위로 결과를 캐싱한다. 결합셀의 경우 호출자(SheetTableModel)가
 * 좌상단 셀을 source cell 로 넘겨주는 책임을 진다.
 *
 * <p>색상 추출 전략:
 * <ul>
 *   <li>{@link XSSFColor#getRGBWithTint()} 우선 — Excel 의 tint 가 반영된 실제 표시 색.</li>
 *   <li>실패 시 {@link XSSFColor#getRGB()} fallback.</li>
 *   <li>indexed/theme 만 지정된 셀은 변환 실패 → 테마 기본색 사용.</li>
 * </ul>
 */
public final class CellStyleResolver {

    /** 배경/글자색 없음, plain 폰트. */
    public static final Spec EMPTY = new Spec(null, null, false, false);

    private final Workbook workbook;
    private final Map<Short, Spec> cache = new HashMap<>();

    public CellStyleResolver(@NotNull Workbook workbook) {
        this.workbook = workbook;
    }

    public @NotNull Spec resolve(@Nullable Cell cell) {
        if (cell == null) return EMPTY;
        CellStyle style = cell.getCellStyle();
        if (style == null) return EMPTY;
        return cache.computeIfAbsent(style.getIndex(), k -> compute(style));
    }

    private @NotNull Spec compute(@NotNull CellStyle style) {
        Color bg = extractFill(style);
        Font poiFont = workbook.getFontAt(style.getFontIndex());
        Color fg = extractFontColor(poiFont);
        boolean bold = poiFont.getBold();
        boolean italic = poiFont.getItalic();
        return new Spec(bg, fg, bold, italic);
    }

    private static @Nullable Color extractFill(@NotNull CellStyle style) {
        // POI 5.3.0 의 CellStyle.getFillPattern() 은 FillPatternType 을 직접 반환한다.
        if (style.getFillPattern() == FillPatternType.NO_FILL) return null;
        if (style instanceof XSSFCellStyle xs) {
            return toAwt(xs.getFillForegroundXSSFColor());
        }
        return null;
    }

    private static @Nullable Color extractFontColor(@NotNull Font font) {
        if (font instanceof XSSFFont xf) {
            return toAwt(xf.getXSSFColor());
        }
        return null;
    }

    private static @Nullable Color toAwt(@Nullable XSSFColor color) {
        if (color == null) return null;

        // "Automatic" 색은 시스템 기본색이므로 IntelliJ 테마에 위임.
        // (POI 가 이걸 RGB(0,0,0) = 검정으로 변환해 다크 배경에 검은 글자가 되는 케이스 방어)
        if (color.isAuto()) return null;

        // POI 5.x 의 isAuto() 가 일부 케이스에서 false 반환하는데, 실제로는 indexed 64 (Automatic).
        // 명시적으로 추가 차단.
        if (color.isIndexed() && color.getIndex() == IndexedColors.AUTOMATIC.getIndex()) {
            return null;
        }

        // Theme 색(예: theme 0/Dark 1, theme 1/Light 1)은 워크북 theme.xml 를 참조해야 한다.
        // POI 가 theme 정보를 못 풀면 RGB(0,0,0) = 검정으로 fallback 되는 경우가 잦아
        // 한국어 Excel 의 헤더 셀이 일괄 검정 배경으로 깨진다.
        // V1 에서는 theme 색을 일관되게 무시하고 IntelliJ 테마 기본색에 위임.
        if (color.isThemed()) return null;

        // getRGB() (tint 미적용) 를 우선 시도. POI 의 tint 변환이 한국어 워크북에서 종종
        // 어두운 회색으로 빗나가는 케이스 방어. 실패 시 getRGBWithTint() fallback.
        byte[] rgb = color.getRGB();
        if (rgb == null) rgb = color.getRGBWithTint();
        if (rgb == null) return null;
        if (rgb.length == 3) {
            return new Color(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
        }
        if (rgb.length == 4) {
            // ARGB → alpha 무시하고 RGB 만 사용 (테이블 셀은 반투명 의미가 약함)
            return new Color(rgb[1] & 0xFF, rgb[2] & 0xFF, rgb[3] & 0xFF);
        }
        return null;
    }

    /** 셀 스타일 추출 결과. 모든 필드는 null/false 일 수 있음. */
    public static final class Spec {
        public final @Nullable Color background;
        public final @Nullable Color foreground;
        public final boolean bold;
        public final boolean italic;

        Spec(@Nullable Color bg, @Nullable Color fg, boolean bold, boolean italic) {
            this.background = bg;
            this.foreground = fg;
            this.bold = bold;
            this.italic = italic;
        }

        public boolean isEmpty() {
            return background == null && foreground == null && !bold && !italic;
        }
    }
}
