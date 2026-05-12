package kr.bsen.intellij.xlsxviewer.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.hover.TableHoverListener;
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

        // 행 호버 반투명 오버레이: 베이스 색에 알파 합성으로 살짝 얹는다.
        // 검색 하이라이트/POI 배경/흰 배경 위 어디든 자연스럽게 동작.
        if (TableHoverListener.getHoveredRow(table) == row) {
            bg = applyHoverOverlay(bg);
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

    /**
     * 행 호버 반투명 오버레이.
     * 라이트 테마는 검정 ~14%, 다크 테마는 흰색 ~14% 알파로 베이스 위에 합성한다.
     * 단색 회색 띠로 셀 서식을 가리는 대신, 베이스 색을 살리면서 모던하게 톤만 입힌다.
     * (Swing 의 setBackground 는 알파 채널을 그리기 단계에서 보장하지 않아
     *  cell 영역이 transparent 일 때 알파 색이 그대로 어둡게 보일 수 있다.
     *  여기서는 사전에 직접 알파 합성해 단색 RGB 로 변환한다.)
     */
    private static @NotNull Color applyHoverOverlay(@NotNull Color base) {
        int or, og, ob, oa;
        if (JBColor.isBright()) {
            // 검정 14% — 흰/연한 배경 위에 또렷하면서 너무 진하지 않은 톤.
            or = 0; og = 0; ob = 0; oa = 36;
        } else {
            // 다크 위 검정은 안 보이니 흰색 14% 로 살짝 떠오르게.
            or = 255; og = 255; ob = 255; oa = 36;
        }
        float a = oa / 255f;
        int r = Math.round(base.getRed() * (1 - a) + or * a);
        int g = Math.round(base.getGreen() * (1 - a) + og * a);
        int b = Math.round(base.getBlue() * (1 - a) + ob * a);
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** fg/bg 명도 차가 임계치 이상이면 가독성 확보됐다고 판단. */
    private static boolean hasContrast(@NotNull Color fg, @NotNull Color bg) {
        return Math.abs(luminance(fg) - luminance(bg)) > 0.25;
    }

    private static double luminance(@NotNull Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
    }
}
