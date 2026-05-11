package kr.bsen.intellij.xlsxviewer;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import kr.bsen.intellij.xlsxviewer.parser.WorkbookLoader;
import kr.bsen.intellij.xlsxviewer.ui.XlsxViewerPanel;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;

/**
 * .xlsx 파일을 표시하는 읽기 전용 FileEditor.
 *
 * <p>생성 시 {@link WorkbookLoader} 로 워크북을 로딩하고 {@link XlsxViewerPanel} 로 렌더링한다.
 * 사용자 취소 / 로딩 실패 시에는 중앙 안내 라벨을 표시한다.
 */
public final class XlsxFileEditor extends UserDataHolderBase implements FileEditor {

    private static final Logger LOG = Logger.getInstance(XlsxFileEditor.class);

    private final VirtualFile file;
    private final JPanel panel;
    private final @Nullable Workbook workbook;

    public XlsxFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.file = file;
        this.panel = new JPanel(new BorderLayout());

        Workbook loaded = null;
        try {
            loaded = WorkbookLoader.load(file, project);
            panel.add(new XlsxViewerPanel(loaded), BorderLayout.CENTER);
        } catch (WorkbookLoader.UserCancelledException cancelled) {
            showCenterLabel(XlsxViewerBundle.message("editor.load.cancelled"));
        } catch (Exception e) {
            LOG.warn("XLSX 로딩 실패: " + file.getPath(), e);
            showCenterLabel(XlsxViewerBundle.message(
                    "editor.load.error.message",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
            ));
        }
        this.workbook = loaded;
    }

    private void showCenterLabel(@NotNull String text) {
        panel.removeAll();
        panel.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.CENTER);
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return panel;
    }

    @Override
    public @NotNull String getName() {
        return "XLSX Viewer";
    }

    @Override
    public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // 읽기 전용 - 복원할 상태 없음
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return file.isValid();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // 변경 이벤트 없음 - no-op
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // no-op
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public void dispose() {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (Exception e) {
                LOG.warn("워크북 close 실패", e);
            }
        }
    }
}
