package kr.bsen.intellij.xlsxviewer.ui;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.table.JBTable;
import kr.bsen.intellij.xlsxviewer.XlsxViewerBundle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.PaneInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * 워크북 1개를 시트 탭 + 읽기 전용 테이블 + 검색 도구바로 표시한다.
 *
 * <ul>
 *   <li>FR-4: 시트 탭은 {@link JBTabbedPane}, 하단 배치.</li>
 *   <li>FR-5: 행 헤더는 1,2,3..., 컬럼 헤더는 A,B,C...</li>
 *   <li>Phase 2-1: 시트마다 상단 검색 도구바 + 매칭 셀 하이라이트 + 필터 모드.</li>
 *   <li>Phase 2-2: POI {@code CellStyle} 기반 셀 배경/글자색/굵기/기울임 시각 재현.</li>
 *   <li>Phase 2-3: 상단 N행 Freeze Panes 지원. {@link JBScrollPane#setColumnHeaderView}
 *       에 freeze 영역 테이블을 끼워 자동 가로 스크롤 동기화. 좌측 열 freeze 는 Phase 3.</li>
 * </ul>
 */
public final class XlsxViewerPanel extends JPanel {

    public XlsxViewerPanel(@NotNull Workbook workbook) {
        super(new BorderLayout());

        int sheetCount = workbook.getNumberOfSheets();
        if (sheetCount == 0) {
            add(new JLabel(XlsxViewerBundle.message("sheet.empty"), SwingConstants.CENTER),
                    BorderLayout.CENTER);
            return;
        }

        JBTabbedPane tabbedPane = new JBTabbedPane(SwingConstants.BOTTOM);
        for (int i = 0; i < sheetCount; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            tabbedPane.addTab(sheet.getSheetName(), buildSheetView(sheet));
        }
        add(tabbedPane, BorderLayout.CENTER);
    }

    private static JComponent buildSheetView(@NotNull Sheet sheet) {
        SheetTableModel model = new SheetTableModel(sheet);
        SheetSearchController search = new SheetSearchController(model);
        CellStyleResolver styleResolver = new CellStyleResolver(sheet.getWorkbook());

        int freezeRows = resolveFreezeRows(sheet, model);

        JBTable mainTable = createTable(model, search, styleResolver);
        TableRowSorter<SheetTableModel> mainSorter = createSorter(model);
        mainSorter.setRowFilter(mainFilter(search, freezeRows));
        mainTable.setRowSorter(mainSorter);

        @Nullable JBTable topTable = null;
        @Nullable TableRowSorter<SheetTableModel> topSorter = null;
        if (freezeRows > 0) {
            topTable = createTable(model, search, styleResolver);
            // 컬럼 모델 공유 → 가로 스크롤 / 컬럼 폭 자동 동기화
            topTable.setColumnModel(mainTable.getColumnModel());
            topSorter = createSorter(model);
            topSorter.setRowFilter(topFilter(freezeRows));
            topTable.setRowSorter(topSorter);
            topTable.setTableHeader(null);   // 컬럼 헤더는 mainTable 의 것만 사용
        }

        for (int c = 0; c < model.getColumnCount(); c++) {
            mainTable.getColumnModel().getColumn(c).setPreferredWidth(100);
        }

        JBScrollPane scroll = new JBScrollPane(mainTable);
        JTable rowHeader = buildRowHeader(mainTable, model);
        scroll.setRowHeaderView(rowHeader);

        if (topTable != null) {
            JPanel headerContainer = new JPanel(new BorderLayout());
            headerContainer.add(mainTable.getTableHeader(), BorderLayout.NORTH);
            headerContainer.add(topTable, BorderLayout.CENTER);
            scroll.setColumnHeaderView(headerContainer);
        }

        final JBTable finalTopTable = topTable;
        final TableRowSorter<SheetTableModel> finalTopSorter = topSorter;
        search.addListener(() -> {
            mainSorter.allRowsChanged();
            if (finalTopSorter != null) finalTopSorter.allRowsChanged();
            ((AbstractTableModel) rowHeader.getModel()).fireTableDataChanged();
            mainTable.repaint();
            if (finalTopTable != null) finalTopTable.repaint();
        });

        JPanel container = new JPanel(new BorderLayout());
        container.add(new SheetSearchToolbar(search), BorderLayout.NORTH);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private static JBTable createTable(@NotNull SheetTableModel model,
                                       @NotNull SheetSearchController search,
                                       @NotNull CellStyleResolver styleResolver) {
        JBTable table = new JBTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        table.getTableHeader().setReorderingAllowed(false);
        // 플랫폼 기본 행 hover 는 단색 회색 띠로 행을 덮어 셀 서식/검색 하이라이트가 사라진다.
        // JBTable.prepareRenderer 가 hover 행에 hover bg 를 강제 setBackground 하는 동작을 끄고,
        // Renderer 에서 직접 베이스 색에 반투명 오버레이를 합성해 모던한 효과로 교체한다.
        RenderingUtil.setHoverPaintingDisabled(table, true);
        table.setDefaultRenderer(Object.class, new SearchableCellRenderer(search, model, styleResolver));
        return table;
    }

    /** 결합셀이 있는 모델은 정렬이 모호하므로 모든 컬럼 정렬 비활성화한 RowSorter. */
    private static TableRowSorter<SheetTableModel> createSorter(@NotNull SheetTableModel model) {
        TableRowSorter<SheetTableModel> sorter = new TableRowSorter<>(model);
        for (int c = 0; c < model.getColumnCount(); c++) {
            sorter.setSortable(c, false);
        }
        return sorter;
    }

    /** mainTable 영역 필터: freeze 행 제외 + 검색 필터 모드 위임. */
    private static RowFilter<TableModel, Integer> mainFilter(
            @NotNull SheetSearchController search, int freezeRows) {
        RowFilter<TableModel, Integer> searchFilter = search.rowFilter();
        return new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                int row = entry.getIdentifier();
                if (row < freezeRows) return false;
                return searchFilter.include(entry);
            }
        };
    }

    /** topTable 영역 필터: freeze 행만 통과. 검색 매칭과 무관하게 항상 표시. */
    private static RowFilter<TableModel, Integer> topFilter(int freezeRows) {
        return new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                return entry.getIdentifier() < freezeRows;
            }
        };
    }

    /**
     * POI Sheet 의 freeze pane 정보에서 상단 freeze 행 수를 추출한다.
     * - freeze 가 아닌 split 만 있으면 0 반환 (split 은 Phase 3 에서 별도 처리).
     * - freeze 행 ≥ 전체 행이면 전체가 freeze 되므로 의미 없음 → 0.
     * - 좌측 열 freeze 는 V1 미지원 (Phase 3 인수인계).
     */
    private static int resolveFreezeRows(@NotNull Sheet sheet, @NotNull SheetTableModel model) {
        PaneInformation pane = sheet.getPaneInformation();
        if (pane == null || !pane.isFreezePane()) return 0;
        int vertical = pane.getVerticalSplitPosition();
        if (vertical <= 0) return 0;
        if (vertical >= model.getRowCount()) return 0;
        return vertical;
    }

    /**
     * 좌측 1,2,3... 행 번호 열. JBScrollPane.setRowHeaderView 로 부착.
     * <p>폭은 모델 전체 행 수 기준 자릿수로 계산해 freeze 적용 후에도 흔들리지 않게 한다.
     * 표시 값은 view→model 변환 후 +1 이라 필터/freeze 와 무관하게 원본 행 번호가 유지된다.
     */
    private static JTable buildRowHeader(@NotNull JTable mainTable, @NotNull SheetTableModel model) {
        AbstractTableModel headerModel = new AbstractTableModel() {
            @Override public int getRowCount() { return mainTable.getRowCount(); }
            @Override public int getColumnCount() { return 1; }
            @Override public Object getValueAt(int r, int c) {
                int modelRow = mainTable.convertRowIndexToModel(r);
                return Integer.toString(modelRow + 1);
            }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable header = new JTable(headerModel);
        header.setEnabled(false);
        header.setShowGrid(false);
        header.setRowHeight(mainTable.getRowHeight());

        int digits = Math.max(2, Integer.toString(Math.max(1, model.getRowCount())).length());
        int width = 16 + digits * 8;
        TableColumn col = header.getColumnModel().getColumn(0);
        col.setPreferredWidth(width);
        col.setMinWidth(width);
        col.setMaxWidth(width);
        header.setPreferredScrollableViewportSize(new Dimension(width, 0));

        JTableHeader th = mainTable.getTableHeader();
        header.setBackground(th.getBackground());
        header.setForeground(th.getForeground());
        header.setFont(th.getFont().deriveFont(Font.PLAIN));

        return header;
    }
}
