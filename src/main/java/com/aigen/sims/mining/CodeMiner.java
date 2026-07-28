package com.aigen.sims.mining;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
public class CodeMiner {
    private final String basePath;
    public CodeMiner(String basePath) { this.basePath = basePath; }
    public List<RepoContext> scanAllRepos() {
        List<RepoContext> repos = new ArrayList<>();
        File baseDir = new File(basePath);
        if (!baseDir.isDirectory()) return repos;
        File[] dirs = baseDir.listFiles(File::isDirectory);
        if (dirs == null) return repos;
        for (File dir : dirs) {
            if (new File(dir, ".git").isDirectory()) {
                RepoContext ctx = scanRepo(dir.getName(), dir.getAbsolutePath());
                if (ctx != null) repos.add(ctx);
            }
        }
        return repos;
    }
    public RepoContext scanRepo(String name, String path) {
        try {
            String diff = execGit(path, "diff", "HEAD", "--stat");
            List<String> commits = execGitLines(path, "log", "--oneline", "-5");
            Map<String, String> contents = new HashMap<>();
            Files.walk(Paths.get(path))
                .filter(p -> p.toString().endsWith(".java"))
                .limit(10)
                .forEach(p -> {
                    try { String rel = Paths.get(path).relativize(p).toString();
                           contents.put(rel, Files.readString(p)); }
                    catch (IOException e) {}
                });
            List<String> patterns = extractPatterns(contents);
            int[] hex = hashToHex(name);
            return new RepoContext(name, path, diff, commits, contents, patterns, hex[0], hex[1]);
        } catch (Exception e) { return null; }
    }
    public List<String> extractPatterns(Map<String, String> fileContents) {
        Set<String> patterns = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            String content = entry.getValue();
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:public|private|protected)?\\s*(?:class|interface|enum)\\s+(\\w+)")
                .matcher(content);
            while (m.find()) patterns.add("class:" + m.group(1));
            m = java.util.regex.Pattern
                .compile("(?:public|private|protected)?\\s*\\w+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{")
                .matcher(content);
            while (m.find()) patterns.add("method:" + m.group(1));
            m = java.util.regex.Pattern.compile("import\\s+([\\w.]+);").matcher(content);
            while (m.find()) patterns.add("import:" + m.group(1));
        }
        return new ArrayList<>(patterns);
    }
    private String execGit(String repoPath, String... args) {
        try {
            List<String> cmd = new ArrayList<>(); cmd.add("git"); cmd.addAll(Arrays.asList(args));
            ProcessBuilder pb = new ProcessBuilder(cmd); pb.directory(new File(repoPath));
            pb.redirectErrorStream(true); Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); return out.trim();
        } catch (Exception e) { return ""; }
    }
    private List<String> execGitLines(String repoPath, String... args) {
        String out = execGit(repoPath, args);
        return out.isEmpty() ? new ArrayList<>() : Arrays.asList(out.split("\n"));
    }
    int[] hashToHex(String name) {
        int hash = Math.abs(name.hashCode());
        return new int[]{(hash % 7) - 3, ((hash / 7) % 7) - 3};
    }
}
