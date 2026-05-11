package kr.bsen.intellij.xlsxviewer;

import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * .xlsx 파일을 위한 커스텀 FileType.
 * plugin.xml 의 fileType extension 에서 fieldName="INSTANCE" 로 참조된다.
 */
public final class XlsxFileType implements FileType {

    public static final XlsxFileType INSTANCE = new XlsxFileType();

    private XlsxFileType() {
    }

    @Override
    public @NotNull String getName() {
        return "XLSX";
    }

    @Override
    public @NotNull String getDescription() {
        return "Microsoft Excel Workbook (.xlsx)";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "xlsx";
    }

    @Override
    public @Nullable Icon getIcon() {
        // MVP: 기본 파일 아이콘 사용. 추후 pluginIcon 작업 시 커스텀 아이콘으로 교체.
        return null;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
