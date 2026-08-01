package com.aigen.sims.gate;

import java.util.*;

/**
 * AlgebraicCorrector -- 2026-07-28 Architect gist 3.3: "Algebraic Correction Engine ... intercepts
 * errors, calculates missing operators."
 *
 * Real, deterministic, symbolic (no LLM): a string/char/comment-aware bracket scanner. The one
 * repairable case is truncation -- unclosed opens at end-of-file, fixed by appending the missing
 * closers in the correct nesting order. A stray unmatched CLOSER anywhere is a genuine structural
 * error, not a truncation, and is deliberately reported as unrepairable rather than guessed at --
 * guessing there would be fabrication, not correction.
 */
public class AlgebraicCorrector {
    private static final Map<Character, Character> PAIRS = Map.of('(', ')', '{', '}', '[', ']');

    public static class ScanResult {
        public final boolean balanced;
        public final boolean repairable;
        public final String repaired;   // non-null only when repairable && !balanced
        public final List<String> issues;

        ScanResult(boolean balanced, boolean repairable, String repaired, List<String> issues) {
            this.balanced = balanced;
            this.repairable = repairable;
            this.repaired = repaired;
            this.issues = issues;
        }
    }

    public static ScanResult scan(String code) {
        Deque<Character> stack = new ArrayDeque<>();
        List<String> issues = new ArrayList<>();
        boolean inString = false, inChar = false, inLineComment = false, inBlockComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            char next = i + 1 < code.length() ? code.charAt(i + 1) : '\0';

            if (inLineComment) { if (c == '\n') inLineComment = false; continue; }
            if (inBlockComment) { if (c == '*' && next == '/') { inBlockComment = false; i++; } continue; }
            if (inString) { if (c == '\\') { i++; } else if (c == '"') inString = false; continue; }
            if (inChar) { if (c == '\\') { i++; } else if (c == '\'') inChar = false; continue; }
            if (c == '/' && next == '/') { inLineComment = true; i++; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i++; continue; }
            if (c == '"') { inString = true; continue; }
            if (c == '\'') { inChar = true; continue; }

            if (PAIRS.containsKey(c)) { stack.push(c); continue; }
            if (PAIRS.containsValue(c)) {
                if (stack.isEmpty() || PAIRS.get(stack.peek()) != c) {
                    issues.add("stray unmatched '" + c + "' at index " + i);
                    return new ScanResult(false, false, null, issues);
                }
                stack.pop();
            }
        }

        if (stack.isEmpty()) return new ScanResult(true, true, code, issues);

        List<Character> unclosed = new ArrayList<>();
        StringBuilder repaired = new StringBuilder(code);
        while (!stack.isEmpty()) {
            char open = stack.pop();
            unclosed.add(open);
            repaired.append(PAIRS.get(open));
        }
        issues.add(unclosed.size() + " unclosed bracket(s): " + unclosed);
        return new ScanResult(false, true, repaired.toString(), issues);
    }
}
