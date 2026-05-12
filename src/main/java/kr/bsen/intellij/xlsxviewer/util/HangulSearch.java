package kr.bsen.intellij.xlsxviewer.util;

import org.jetbrains.annotations.NotNull;

/**
 * 한글 자모 분리 기반 초성 검색 유틸.
 *
 * <p>한국 사용자의 가장 일반적인 기대 동작은 "초성 검색"이다. 예:
 * <pre>
 *   "ㅇㅅㅅ"  →  "이순신", "안성수", "이상수"  모두 매칭
 *   "ㄱㅁㅈ"  →  "감리작업", "광명전자"  모두 매칭
 * </pre>
 *
 * <p>{@link #toChosung(String)} 으로 문자열을 초성 시퀀스로 변환한 뒤 양쪽을 동일 변환해
 * substring 매칭하면 된다. 영문/숫자는 lowercase 로 통일해 부수적으로 case-insensitive 매칭도 된다.
 *
 * <p>한글 자모 단독 입력(예: 사용자가 키보드로 "ㅇ"만 입력)은 초성표에 그대로 존재하므로
 * 별도 변환 없이 매치된다.
 */
public final class HangulSearch {

    /** 한글 음절(U+AC00 ~ U+D7A3) 의 초성 19자. */
    private static final char[] CHOSUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private static final int SYLLABLE_BASE = 0xAC00;
    private static final int SYLLABLE_END = 0xD7A3;
    /** 중성 21 × 종성 28 = 588. 한 초성 블록 크기. */
    private static final int CHOSUNG_BLOCK = 21 * 28;

    private HangulSearch() {
    }

    /**
     * 한글 음절은 초성으로, 그 외 문자는 lowercase 로 변환.
     * 한글 자모(ㄱ~ㅎ)는 그대로 유지된다.
     */
    public static @NotNull String toChosung(@NotNull String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= SYLLABLE_BASE && c <= SYLLABLE_END) {
                int idx = (c - SYLLABLE_BASE) / CHOSUNG_BLOCK;
                sb.append(CHOSUNG[idx]);
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
