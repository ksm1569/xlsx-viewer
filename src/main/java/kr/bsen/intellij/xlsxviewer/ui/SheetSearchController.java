package kr.bsen.intellij.xlsxviewer.ui;

import kr.bsen.intellij.xlsxviewer.util.HangulSearch;
import org.jetbrains.annotations.NotNull;

import javax.swing.RowFilter;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableModel;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 한 시트의 검색 상태와 매칭 결과를 보관한다.
 *
 * <ul>
 *   <li>{@code query} 변경 시 모델 전체를 스캔해 매칭 좌표를 재계산.</li>
 *   <li>대소문자 토글 / 필터 모드(매칭된 행만 표시) 토글 지원.</li>
 *   <li>자모 분리 검색은 Phase 2-4 에서 {@link #normalize(String)} 분기로 추가.</li>
 * </ul>
 *
 * <p>매칭 좌표는 (row,col)→long 키 집합으로, 매칭 행은 별도 정수 집합으로 유지한다.
 * RowFilter.include() 가 행 단위로 빈번하게 호출되므로 행 집합을 분리해 O(1) 검사한다.
 */
public final class SheetSearchController {

    @FunctionalInterface
    public interface Listener extends EventListener {
        void onStateChanged();
    }

    private final SheetTableModel model;
    private final EventListenerList listenerList = new EventListenerList();

    private String query = "";
    private boolean caseSensitive = false;
    private boolean filterMode = false;
    private boolean jamoMode = false;

    private final Set<Long> matchKeys = new HashSet<>();
    private final Set<Integer> matchRows = new HashSet<>();
    private int matchCount = 0;

    public SheetSearchController(@NotNull SheetTableModel model) {
        this.model = model;
    }

    public void addListener(@NotNull Listener listener) {
        listenerList.add(Listener.class, listener);
    }

    public void setQuery(@NotNull String query) {
        if (this.query.equals(query)) return;
        this.query = query;
        recompute();
    }

    public void setCaseSensitive(boolean value) {
        if (this.caseSensitive == value) return;
        this.caseSensitive = value;
        recompute();
    }

    public void setFilterMode(boolean value) {
        if (this.filterMode == value) return;
        this.filterMode = value;
        fireStateChanged();
    }

    public void setJamoMode(boolean value) {
        if (this.jamoMode == value) return;
        this.jamoMode = value;
        recompute();
    }

    public boolean isMatch(int row, int col) {
        return !matchKeys.isEmpty() && matchKeys.contains(key(row, col));
    }

    public boolean isRowMatch(int row) {
        return matchRows.contains(row);
    }

    public int getMatchCount() {
        return matchCount;
    }

    public boolean isEmpty() {
        return query.isEmpty();
    }

    /**
     * 필터 모드 ON + 검색어 있음 → 매칭된 행만 통과시키는 RowFilter.
     * 그 외에는 모든 행 통과 (정렬은 호출부에서 비활성화한다).
     */
    public @NotNull RowFilter<TableModel, Integer> rowFilter() {
        return new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                if (!filterMode || query.isEmpty()) return true;
                return isRowMatch(entry.getIdentifier());
            }
        };
    }

    private void recompute() {
        matchKeys.clear();
        matchRows.clear();
        matchCount = 0;

        if (!query.isEmpty()) {
            String needle = normalize(query);
            if (!needle.isEmpty()) {
                int rows = model.getRowCount();
                int cols = model.getColumnCount();
                for (int r = 0; r < rows; r++) {
                    boolean rowMatched = false;
                    for (int c = 0; c < cols; c++) {
                        Object v = model.getValueAt(r, c);
                        if (v == null) continue;
                        String s = v.toString();
                        if (s.isEmpty()) continue;
                        if (normalize(s).contains(needle)) {
                            matchKeys.add(key(r, c));
                            matchCount++;
                            rowMatched = true;
                        }
                    }
                    if (rowMatched) matchRows.add(r);
                }
            }
        }
        fireStateChanged();
    }

    /**
     * 비교 정규화.
     * <ul>
     *   <li>{@code jamoMode}: 한글 음절 → 초성, 그 외 lowercase (caseSensitive 무시).</li>
     *   <li>그 외: {@code caseSensitive=false} 면 영문 lowercase.</li>
     * </ul>
     */
    private @NotNull String normalize(@NotNull String s) {
        if (jamoMode) return HangulSearch.toChosung(s);
        return caseSensitive ? s : s.toLowerCase(Locale.ROOT);
    }

    /** SheetTableModel.key() 와 동일한 인코딩 (row << 20 | col). */
    private static long key(int row, int col) {
        return ((long) row << 20) | (col & 0xFFFFFL);
    }

    private void fireStateChanged() {
        for (Listener l : listenerList.getListeners(Listener.class)) {
            l.onStateChanged();
        }
    }
}
