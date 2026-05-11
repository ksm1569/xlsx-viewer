package kr.bsen.intellij.xlsxviewer;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * messages.XlsxViewerBundle 리소스 번들 접근자.
 * 한국어가 default, 영어가 i18n stub.
 */
public final class XlsxViewerBundle extends DynamicBundle {

    @NonNls
    private static final String BUNDLE_FQN = "messages.XlsxViewerBundle";

    private static final XlsxViewerBundle INSTANCE = new XlsxViewerBundle();

    private XlsxViewerBundle() {
        super(BUNDLE_FQN);
    }

    public static @NotNull @Nls String message(
            @NotNull @PropertyKey(resourceBundle = BUNDLE_FQN) String key,
            Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }
}
