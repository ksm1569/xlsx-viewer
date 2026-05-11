package kr.bsen.intellij.xlsxviewer;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * .xlsx 파일이 열릴 때 {@link XlsxFileEditor} 를 생성한다.
 *
 * <p>xlsx 는 바이너리이므로 기본 텍스트 에디터와 경쟁시키지 않고
 * {@link FileEditorPolicy#HIDE_DEFAULT_EDITOR} 로 단독 표시한다.
 * 해당 정책은 {@link DumbAware} 구현이 필수.
 */
public final class XlsxFileEditorProvider implements FileEditorProvider, DumbAware {

    private static final String EDITOR_TYPE_ID = "xlsx-viewer";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getFileType() instanceof XlsxFileType;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new XlsxFileEditor(project, file);
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
