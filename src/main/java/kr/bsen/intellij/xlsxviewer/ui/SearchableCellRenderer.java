package kr.bsen.intellij.xlsxviewer.ui;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * 검색 매칭 셀 하이라이트 + POI 셀 서식(배경/글자색/굵기/기울임) 적용 렌더러.
 *
 * <p>우선순위(높음→낮음):
 * <ol>
 *   <li>셀이 선택되어 있으면 IntelliJ 테마 선택 색상 그대로 (super 호출 결과 유지).</li>
 *   <li>검색 매칭이면 노란색 하이라이트 배경 + 글자색은 가독성 자동.</li>
 *   <li>POI 셀 서식이 있으면 그 배경/글자색/굵기/기울임 적용.</li>
 *   <li>둘 다 없으면 IntelliJ 테마 기본색.</li>
 * </ol>
 */
public final class SearchableCellRenderer extends DefaultTableCellRenderer {

    private static final Color HIGHLIGHT_BG = new JBColor(
            new Color(0xFFF59D),   // 라이트 테마: 연한 노랑
            new Color(0x73613B)    // 다크 테마: 어두운 카멜
    );

    private final SheetSearchController controller;
    private final SheetTableModel model;
    private final CellStyleResolver styleResolver;

    public SearchableCellRenderer(@NotNull SheetSearchController controller,
                                  @NotNull SheetTableModel model,
                                  @NotNull CellStyleResolver styleResolver) {
        this.controller = controller;
        this.model = model;
        this.styleResolver = styleResolver;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (isSelected) {
            return c;
        }

        // RowSorter / 컬럼 재배열이 있어도 모델 기준 좌표로 매칭/스타일 판정.
        int modelRow = table.convertRowIndexToModel(row);
        int modelCol = table.convertColumnIndexToModel(column);

        CellStyleResolver.Spec spec = styleResolver.resolve(model.getStyleSourceCell(modelRow, modelCol));
        boolean matched = controller.isMatch(modelRow, modelCol);

        Color bg;
        Color fg;
        if (matched) {
            bg = HIGHLIGHT_BG;
            fg = pickReadableForeground(HIGHLIGHT_BG);
        } else if (spec.background != null) {
            bg = spec.background;
            // 명시적 fg 가 bg 와 명도 차이가 너무 작으면 가독성을 위해 자동 보정.
            // theme 색이 RGB(0,0,0) 으로 잘못 풀려 fg/bg 가 모두 검정이 되는 케이스의 안전망.
            if (spec.foreground != null && hasContrast(spec.foreground, bg)) {
                fg = spec.foreground;
            } else {
                fg = pickReadableForeground(bg);
            }
        } else {
            // 명시적 배경 없는 셀(NO_FILL): xlsx 원본은 Excel 의 흰 배경을 전제로 디자인된다.
            // IntelliJ 다크 테마 위에 그대로 두면 검정 글자가 다크 회색 배경에 묻혀 안 보인다.
            // 시트 데이터 영역만 흰 배경을 강제해 Excel 원본 가독성을 살린다.
            // (라이트 테마라도 IntelliJ 기본이 거의 흰색이라 결과 동일)
            bg = Color.WHITE;
            fg = spec.foreground != null ? spec.foreground : Color.BLACK;
        }
        c.setBackground(bg);
        c.setForeground(fg);

        Font baseFont = table.getFont();
        int style = Font.PLAIN;
        if (spec.bold) style |= Font.BOLD;
        if (spec.italic) style |= Font.ITALIC;
        c.setFont(style == Font.PLAIN ? baseFont : baseFont.deriveFont(style));
        return c;
    }

    /** 배경 명도 기반 가독성 글자색 (라이트 배경 → 검정, 어두운 배경 → 흰색). */
    private static @NotNull Color pickReadableForeground(@NotNull Color bg) {
        return luminance(bg) > 0.6 ? Color.BLACK : Color.WHITE;
    }

    /** fg/bg 명도 차가 임계치 이상이면 가독성 확보됐다고 판단. */
    private static boolean hasContrast(@NotNull Color fg, @NotNull Color bg) {
        return Math.abs(luminance(fg) - luminance(bg)) > 0.25;
    }

    private static double luminance(@NotNull Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
    }
}
