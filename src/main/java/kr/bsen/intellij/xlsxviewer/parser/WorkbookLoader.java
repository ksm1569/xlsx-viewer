package kr.bsen.intellij.xlsxviewer.parser;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import kr.bsen.intellij.xlsxviewer.XlsxViewerBundle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * .xlsx 파일을 Apache POI {@link Workbook} 으로 로딩한다.
 *
 * <p>NFR-2: 100MB 초과 파일은 사용자 확인 다이얼로그 후 진행.
 * 사용자가 거부하면 {@link UserCancelledException} 을 발생시킨다.
 *
 * <p>호출자는 반환된 워크북을 사용 후 close() 해야 한다.
 */
public final class WorkbookLoader {

    /** NFR-2 임계치: 100MB. */
    private static final long LARGE_FILE_THRESHOLD = 100L * 1024 * 1024;

    private WorkbookLoader() {
    }

    public static @NotNull Workbook load(@NotNull VirtualFile file, @Nullable Project project)
            throws IOException, UserCancelledException {
        if (file.getLength() > LARGE_FILE_THRESHOLD) {
            confirmOrThrow(file, project);
        }
        try (InputStream is = file.getInputStream()) {
            return new XSSFWorkbook(is);
        }
    }

    private static void confirmOrThrow(@NotNull VirtualFile file, @Nullable Project project)
            throws UserCancelledException {
        long sizeMb = file.getLength() / (1024 * 1024);
        int answer = Messages.showYesNoDialog(
                project,
                XlsxViewerBundle.message("warning.large.file.message", sizeMb),
                XlsxViewerBundle.message("warning.large.file.title"),
                Messages.getWarningIcon()
        );
        if (answer != Messages.YES) {
            throw new UserCancelledException();
        }
    }

    /** 사용자가 큰 파일 열기를 거부했을 때 던진다. */
    public static final class UserCancelledException extends Exception {
        public UserCancelledException() {
            super(XlsxViewerBundle.message("editor.load.cancelled"));
        }
    }
}
