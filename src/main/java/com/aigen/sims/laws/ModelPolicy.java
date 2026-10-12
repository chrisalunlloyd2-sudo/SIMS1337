package com.aigen.sims.laws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GODHAND-3 (t_22bb44a7) — model admission policy, loaded from
 * AIGEN_SYS/config/model_policy.json (the same file ops_health.py consumes).
 *
 * Semantics (must stay aligned with ~/.aria/engine/ops_health.py model_decision):
 *   - unknown / banned models are DENIED (default "deny")
 *   - banned_name_patterns are substring-matched lowercased
 *   - tiers: S small (any time), M medium (needs headroom), L exclusive
 *     (only model resident + game idle), embed (embeddings only), restricted
 *     (hard token/time caps honoured by caller)
 *   - ram_floor_mb: OS free RAM that must remain after the model's footprint
 *   - max_loaded: max resident non-embed models
 *
 * Never throws: if the policy file is unreadable the class falls back to a
 * built-in copy of the same tables (graceful degradation, law 7) and warns.
 */
public final class ModelPolicy {

    public static final class ModelSpec {
        public final String name;
        public final String tier;      // S | M | L | embed | restricted
        public final int ramMb;
        public final int maxTokens;
        public final int timeoutS;
        ModelSpec(String name, String tier, int ramMb, int maxTokens, int timeoutS) {
            this.name = name; this.tier = tier; this.ramMb = ramMb;
            this.maxTokens = maxTokens; this.timeoutS = timeoutS;
        }
    }

    /** Admission verdict returned for one (model, kind) question. */
    public static final class Verdict {
        public final boolean allowed;
        public final String reason;   // human-readable why (allow or deny)
        public final String tier;     // may be null
        Verdict(boolean allowed, String reason, String tier) {
            this.allowed = allowed; this.reason = reason; this.tier = tier;
        }
        public static Verdict allow(String reason, String tier) { return new Verdict(true, reason, tier); }
        public static Verdict deny(String reason) { return new Verdict(false, reason, null); }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static volatile Map<String, ModelSpec> models = new LinkedHashMap<>();
    private static volatile List<String> bannedPatterns = new ArrayList<>();
    private static volatile int ramFloorMb = 1500;
    private static volatile int maxLoaded = 2;
    private static volatile String source = "(unloaded)";

    static {
        reload();
    }

    private ModelPolicy() { /* static-only */ }

    /** Policy file path: -Dcom.aigen.sims.modelpolicy=... overrides the default. */
    public static File policyFile() {
        String p = System.getProperty("com.aigen.sims.modelpolicy");
        if (p != null && !p.isEmpty()) return new File(p);
        String home = System.getProperty("user.home", ".");
        return new File(home + File.separator + "AIGEN_SYS" + File.separator
                + "config" + File.separator + "model_policy.json");
    }

    /** Load (or reload) the policy. Falls back to the embedded default tables. */
    public static synchronized void reload() {
        try {
            File f = policyFile();
            if (!f.isFile()) {
                loadDefaults("policy file missing: " + f.getPath());
                return;
            }
            JsonNode root = MAPPER.readTree(f);
            Map<String, ModelSpec> m = new LinkedHashMap<>();
            JsonNode arr = root.path("models");
            arr.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                m.put(e.getKey(), new ModelSpec(
                        e.getKey(),
                        v.path("tier").asText("M"),
                        v.path("ram_mb").asInt(2000),
                        v.path("max_tokens").asInt(256),
                        v.path("timeout_s").asInt(120)));
            });
            List<String> banned = new ArrayList<>();
            root.path("banned_name_patterns").forEach(n -> banned.add(n.asText()));
            models = m;
            bannedPatterns = banned;
            ramFloorMb = root.path("ram_floor_mb").asInt(1500);
            maxLoaded = root.path("max_loaded").asInt(2);
            source = f.getPath();
        } catch (Exception e) {
            loadDefaults("unreadable (" + e + ")");
        }
    }

    /** Law 7: degrade to the same tables the live ops engine encodes, never go dark. */
    private static void loadDefaults(String why) {
        Map<String, ModelSpec> m = new LinkedHashMap<>();
        m.put("qwen2.5:0.5b",           new ModelSpec("qwen2.5:0.5b", "S", 700, 256, 60));
        m.put("tinyllama:1.1b",         new ModelSpec("tinyllama:1.1b", "S", 900, 256, 60));
        m.put("llama3.2:1b",            new ModelSpec("llama3.2:1b", "S", 1500, 256, 90));
        m.put("phi:latest",             new ModelSpec("phi:latest", "S", 1900, 256, 90));
        m.put("gemma2:2b",              new ModelSpec("gemma2:2b", "M", 2200, 384, 120));
        m.put("llama3.2:3b",            new ModelSpec("llama3.2:3b", "M", 2600, 384, 150));
        m.put("qwen2.5-coder:3b",       new ModelSpec("qwen2.5-coder:3b", "M", 2600, 512, 180));
        m.put("phi3:mini",              new ModelSpec("phi3:mini", "M", 2900, 384, 150));
        m.put("deepseek-r1:1.5b",       new ModelSpec("deepseek-r1:1.5b", "restricted", 1700, 192, 45));
        m.put("nomic-embed-text:latest",new ModelSpec("nomic-embed-text:latest", "embed", 400, 0, 30));
        m.put("mistral:7b",             new ModelSpec("mistral:7b", "L", 5600, 512, 300));
        m.put("codellama:7b",           new ModelSpec("codellama:7b", "L", 5000, 512, 300));
        models = m;
        List<String> banned = new ArrayList<>();
        banned.add("cloud"); banned.add("ollama.com"); banned.add("gpt-oss:120b");
        banned.add("glm-4.6"); banned.add("kimi");
        bannedPatterns = banned;
        ramFloorMb = 1500;
        maxLoaded = 2;
        source = "embedded-defaults (" + why + ")";
    }

    public static ModelSpec spec(String model) { return models.get(model); }
    public static boolean isBanned(String model) {
        if (model == null) return true;
        String lower = model.toLowerCase();
        for (String pat : bannedPatterns) {
            if (lower.contains(pat.toLowerCase())) return true;
        }
        return false;
    }
    public static int ramFloorMb() { return ramFloorMb; }
    public static int maxLoaded() { return maxLoaded; }
    public static String source() { return source; }
    public static Map<String, ModelSpec> allModels() { return models; }

    /** Pure policy question: is this model known and clean? (RAM/CPU/residency NOT checked here.) */
    public static Verdict checkModel(String model) {
        if (model == null || model.isEmpty()) return Verdict.deny("null/empty model name");
        if (isBanned(model)) return Verdict.deny("banned by policy (" + ModelPolicy.source() + ")");
        ModelSpec s = spec(model);
        if (s == null) return Verdict.deny("unknown model — policy default is deny");
        return Verdict.allow("known model", s.tier);
    }
}