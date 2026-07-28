package com.aigen.sims.mining;

import java.util.*;

/**
 * PatternMatcher — Regex-based code pattern scanner.
 * Extracts function signatures, class declarations, imports, and test patterns.
 */
public class PatternMatcher {
    // Regex patterns for common code constructs
    private static final java.util.regex.Pattern RE_FUNC =
        java.util.regex.Pattern.compile("(?:public|private|protected|static|def)\\s+[\\w<>]+\\s+(\\w+)\\s*\\(");
    private static final java.util.regex.Pattern RE_CLASS =
        java.util.regex.Pattern.compile("(?:class|interface|enum)\\s+(\\w+)");
    private static final java.util.regex.Pattern RE_IMPORT =
        java.util.regex.Pattern.compile("(?:import|require|from)\\s+([\\w.]+)");
    private static final java.util.regex.Pattern RE_TEST =
        java.util.regex.Pattern.compile("@Test|def test_|test[A-Z]|it\\(.*should");

    /** Scan a repo and return found patterns */
    public List<PatternMatch> matchRepo(RepoWatcher.WatchedRepo repo) {
        List<PatternMatch> matches = new ArrayList<>();
        // Simulated scanning — in production this would read actual files
        String[] mockFiles = {"src/main/Main.java", "src/util/Helper.java", "test/TestMain.java"};
        String[] mockLines = {
            "public class Main { public void run() { } }",
            "import com.matrix.util.*; private static Helper factory;",
            "@Test public void testMine() { assertTrue(true); }"
        };

        for (int i = 0; i < mockFiles.length; i++) {
            String file = repo.path + "/" + mockFiles[i];
            String content = mockLines[i];

            // Match functions
            var fm = RE_FUNC.matcher(content);
            while (fm.find()) {
                matches.add(new PatternMatch(repo.name, file, "function",
                    fm.group(1) + "()", content, i + 1, 0.75, repo.hexKey));
            }

            // Match classes
            var cm = RE_CLASS.matcher(content);
            while (cm.find()) {
                matches.add(new PatternMatch(repo.name, file, "class",
                    cm.group(1), content, i + 1, 0.85, repo.hexKey));
            }

            // Match imports
            var im = RE_IMPORT.matcher(content);
            while (im.find()) {
                matches.add(new PatternMatch(repo.name, file, "import",
                    im.group(1), content, i + 1, 0.65, repo.hexKey));
            }

            // Match tests
            var tm = RE_TEST.matcher(content);
            while (tm.find()) {
                matches.add(new PatternMatch(repo.name, file, "test",
                    "test()", content, i + 1, 0.90, repo.hexKey));
            }
        }
        return matches;
    }

    /** Deduplicate patterns by signature, keeping highest confidence */
    public List<PatternMatch> deduplicate(List<PatternMatch> patterns) {
        Map<String, PatternMatch> best = new LinkedHashMap<>();
        for (PatternMatch pm : patterns) {
            PatternMatch existing = best.get(pm.signature);
            if (existing == null || pm.confidence > existing.confidence) {
                best.put(pm.signature, pm);
            }
        }
        return new ArrayList<>(best.values());
    }
}
