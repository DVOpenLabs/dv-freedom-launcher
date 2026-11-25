package com.dvopenlabs.freedom.panchang;

final class Language {
    private Language() { }
    static String supported(String code) { return "hi".equals(code) ? "hi" : "en"; }
}
