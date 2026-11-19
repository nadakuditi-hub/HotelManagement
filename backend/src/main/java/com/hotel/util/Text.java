package com.hotel.util;

import com.hotel.exception.ApiException;

/** Small input-cleaning helpers shared by controllers. */
public final class Text {
    private Text() {}

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Accepts only web links or our own uploaded files, so odd values like "javascript:..." are never stored. */
    public static String safeImageUrl(String s) {
        String v = blankToNull(s);
        if (v == null) return null;
        boolean ok = (v.startsWith("https://") || v.startsWith("http://") || v.startsWith("/uploads/"))
                && v.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c) || c == '"' || c == '<' || c == '>');
        if (!ok) throw ApiException.badRequest("Photo link must start with http:// or https:// and contain no spaces");
        return v;
    }
}
