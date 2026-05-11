package kr.bsen.intellij.xlsxviewer.ui;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import kr.bsen.intellij.xlsxviewer.XlsxViewerBundle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * 워크북 1개를 시트 탭 + 읽기 전용 테이블로 표시한다.
 * <ul>
 *   <li>FR-4: 시트 탭은 {@link JBTabbedPane}, 하단 배치.</li>
 *   <li>FR-5: 행 헤더는 1,2,3..., 컬럼 헤더는 A,B,C...</li>
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

    private static JBScrollPane buildSheetView(@NotNull Sheet sheet) {
        SheetTableModel model = new SheetTableModel(sheet);
        JBTable table = new JBTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        table.getTableHeader().setReorderingAllowed(false);

        // MVP: 기본 컬럼 폭. 자동 fit 은 Phase 2.
        for (int c = 0; c < model.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(100);
        }

        JBScrollPane scroll = new JBScrollPane(table);
        scroll.setRowHeaderView(buildRowHeader(table));
        return scroll;
    }

    /** 좌측 1,2,3... 행 번호 열. JBScrollPane.setRowHeaderView 로 부착. */
    private static JTable buildRowHeader(@NotNull JTable mainTable) {
        AbstractTableModel headerModel = new AbstractTableModel() {
            @Override public int getRowCount() { return mainTable.getRowCount(); }
            @Override public int getColumnCount() { return 1; }
            @Override public Object getValueAt(int r, int c) { return Integer.toString(r + 1); }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable header = new JTable(headerModel);
        header.setEnabled(false);
        header.setShowGrid(false);
        header.setRowHeight(mainTable.getRowHeight());

        int digits = Math.max(2, Integer.toString(Math.max(1, mainTable.getRowCount())).length());
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
