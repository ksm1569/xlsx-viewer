package kr.bsen.intellij.xlsxviewer.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import kr.bsen.intellij.xlsxviewer.XlsxViewerBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;

/**
 * 시트별 검색 도구바.
 *
 * <ul>
 *   <li>{@link SearchTextField}: 입력 즉시 컨트롤러에 반영 (히스토리는 IntelliJ 기본 동작).</li>
 *   <li>대소문자 토글: {@link AllIcons.Actions#MatchCase}.</li>
 *   <li>필터 토글: 매칭된 행만 표시 ({@link AllIcons.General#Filter}).</li>
 *   <li>자모 토글: Phase 2-4 에서 추가.</li>
 * </ul>
 */
public final class SheetSearchToolbar extends JPanel {

    private final SheetSearchController controller;
    private final SearchTextField searchField = new SearchTextField();
    private final JBLabel statusLabel = new JBLabel("");

    public SheetSearchToolbar(@NotNull SheetSearchController controller) {
        super(new BorderLayout(8, 0));
        this.controller = controller;
        setBorder(JBUI.Borders.empty(2, 4));

        searchField.getTextEditor().getEmptyText().setText(
                XlsxViewerBundle.message("search.placeholder"));
        searchField.addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { sync(); }
            @Override public void removeUpdate(DocumentEvent e) { sync(); }
            @Override public void changedUpdate(DocumentEvent e) { sync(); }
            private void sync() { controller.setQuery(searchField.getText()); }
        });

        JToggleButton caseBtn = makeIconToggle(
                AllIcons.Actions.MatchCase, "search.case.tooltip", controller::setCaseSensitive);
        JToggleButton filterBtn = makeIconToggle(
                AllIcons.General.Filter, "search.filter.tooltip", controller::setFilterMode);
        // 자모(초성) 검색 토글. 마땅한 표준 아이콘이 없어 한글 텍스트로 표시.
        JToggleButton jamoBtn = makeTextToggle(
                "한", "search.jamo.tooltip", controller::setJamoMode);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        right.setOpaque(false);
        right.add(caseBtn);
        right.add(jamoBtn);
        right.add(filterBtn);
        right.add(statusLabel);

        add(searchField, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        controller.addListener(this::updateStatus);
    }

    private @NotNull JToggleButton makeIconToggle(@NotNull Icon icon, @NotNull String tooltipKey,
                                                  @NotNull Consumer<Boolean> setter) {
        JToggleButton b = new JToggleButton(icon);
        return configureToggle(b, tooltipKey, setter, JBUI.insets(2));
    }

    private @NotNull JToggleButton makeTextToggle(@NotNull String text, @NotNull String tooltipKey,
                                                  @NotNull Consumer<Boolean> setter) {
        JToggleButton b = new JToggleButton(text);
        return configureToggle(b, tooltipKey, setter, JBUI.insets(2, 6));
    }

    private @NotNull JToggleButton configureToggle(@NotNull JToggleButton b, @NotNull String tooltipKey,
                                                   @NotNull Consumer<Boolean> setter,
                                                   @NotNull java.awt.Insets margin) {
        b.setToolTipText(XlsxViewerBundle.message(tooltipKey));
        b.setFocusable(false);
        b.setMargin(margin);
        b.addActionListener(e -> setter.accept(b.isSelected()));
        return b;
    }

    private void updateStatus() {
        if (controller.isEmpty()) {
            statusLabel.setText("");
        } else if (controller.getMatchCount() == 0) {
            statusLabel.setText(XlsxViewerBundle.message("search.no.match"));
        } else {
            statusLabel.setText(XlsxViewerBundle.message("search.matches", controller.getMatchCount()));
        }
    }
}
