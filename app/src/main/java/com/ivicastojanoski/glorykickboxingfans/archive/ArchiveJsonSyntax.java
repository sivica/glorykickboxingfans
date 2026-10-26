package com.ivicastojanoski.glorykickboxingfans.archive;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Bounded syntax gate only; Android's adapter/model still performs all archive interpretation. */
final class ArchiveJsonSyntax {
    static final int MAX_BYTES = 1024 * 1024, MAX_DEPTH = 32;
    private final String text;
    private int at;
    private ArchiveJsonSyntax(String text) { this.text = text; }

    static void validate(String text) {
        if (text == null || text.length() > MAX_BYTES) throw invalid();
        try {
            if (StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text)).remaining() > MAX_BYTES) throw invalid();
        } catch (CharacterCodingException error) { throw invalid(); }
        ArchiveJsonSyntax scan = new ArchiveJsonSyntax(text);
        scan.space(); scan.object(1); scan.space();
        if (scan.at != text.length()) throw invalid();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid complete archive JSON"); }
    private void depth(int value) { if (value > MAX_DEPTH) throw new IllegalArgumentException("JSON nesting limit exceeded"); }
    private void space() { while (at < text.length() && " \t\r\n".indexOf(text.charAt(at)) >= 0) at++; }
    private boolean take(char expected) {
        if (at < text.length() && text.charAt(at) == expected) { at++; return true; }
        return false;
    }
    private void need(char expected) { if (!take(expected)) throw invalid(); }
    private void object(int nesting) {
        depth(nesting); need('{'); space(); if (take('}')) return;
        Set<String> keys = new HashSet<>();
        while (true) {
            if (!keys.add(string())) throw invalid();
            space(); need(':'); space(); value(nesting); space();
            if (take('}')) return;
            need(','); space();
        }
    }
    private void array(int nesting) {
        depth(nesting); need('['); space(); if (take(']')) return;
        while (true) {
            value(nesting); space(); if (take(']')) return;
            need(','); space();
        }
    }
    private void value(int nesting) {
        if (at >= text.length()) throw invalid();
        char next = text.charAt(at);
        switch (next) {
            case '{': object(nesting + 1); return;
            case '[': array(nesting + 1); return;
            case '"': string(); return;
            case 't': literal("true"); return;
            case 'f': literal("false"); return;
            case 'n': literal("null"); return;
            default: number();
        }
    }
    private void literal(String value) {
        if (!text.startsWith(value, at)) throw invalid();
        at += value.length();
    }
    private static boolean digit(char c) { return c >= '0' && c <= '9'; }
    private void digits() {
        int start = at;
        while (at < text.length() && digit(text.charAt(at))) at++;
        if (at == start) throw invalid();
    }
    private void number() {
        take('-');
        if (!take('0')) digits();
        else if (at < text.length() && digit(text.charAt(at))) throw invalid();
        if (take('.')) digits();
        if (take('e') || take('E')) { if (!take('+')) take('-'); digits(); }
    }
    private char unicode() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            if (at >= text.length()) throw invalid();
            char c = text.charAt(at++);
            int hex = c >= '0' && c <= '9' ? c - '0' : c >= 'a' && c <= 'f' ? c - 'a' + 10 : c >= 'A' && c <= 'F' ? c - 'A' + 10 : -1;
            if (hex < 0) throw invalid();
            value = value * 16 + hex;
        }
        return (char) value;
    }
    private String string() {
        need('"'); StringBuilder decoded = new StringBuilder();
        while (at < text.length()) {
            char c = text.charAt(at++);
            if (c == '"') return decoded.toString();
            if (c < 0x20) throw invalid();
            if (c == '\\') {
                if (at >= text.length()) throw invalid();
                switch (text.charAt(at++)) {
                    case '"': c = '"'; break; case '\\': c = '\\'; break; case '/': c = '/'; break;
                    case 'b': c = '\b'; break; case 'f': c = '\f'; break; case 'n': c = '\n'; break;
                    case 'r': c = '\r'; break; case 't': c = '\t'; break;
                    case 'u':
                        c = unicode();
                        if (Character.isHighSurrogate(c)) {
                            need('\\'); need('u'); char low = unicode();
                            if (!Character.isLowSurrogate(low)) throw invalid();
                            decoded.append(c).append(low); continue;
                        }
                        if (Character.isLowSurrogate(c)) throw invalid();
                        break;
                    default: throw invalid();
                }
            } else if (Character.isHighSurrogate(c)) {
                if (at >= text.length() || !Character.isLowSurrogate(text.charAt(at))) throw invalid();
                decoded.append(c).append(text.charAt(at++)); continue;
            } else if (Character.isLowSurrogate(c)) throw invalid();
            decoded.append(c);
        }
        throw invalid();
    }
}
