"""UNIFY-1 (t_0c47b2b6) tests: model policy + gateway queue/priority semantics.

Mirrors the ModelPolicy cases the architect cited from aria's
tests/test_ops_health.py (branch ops-health-20261010): cloud name ban, banned
patterns, unknown model default-deny, embed misuse, size-aware RAM floor,
tier L exclusive window (residency + game idle + size+3000), tier M game
headroom, and the caps contract. Plus gateway behaviour tests: priority drain
order (game never starves), per-class queue caps (429), identity-deny split
(403 vs 507), num_ctx hard cap, num_predict clamp, num_thread passthrough,
and worker crash containment.

Offline + deterministic: ollama_ps / free_ram_gb / log_event are stubbed;
no network, no real telemetry writes.
"""
import importlib.util
import json
import queue
import threading
import unittest

ORCH = r"C:\Users\viper\AIGEN_SYS\repos\SIMS1337\scripts\ollama_orchestrator.py"


def load_orch():
    spec = importlib.util.spec_from_file_location("orch", ORCH)
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    # keep test log noise out of the real telemetry ledger
    m.log_event = lambda *a, **k: None
    m.job_seq = 0
    return m


ORCH_M = load_orch()


def base_policy():
    """Copy of the embedded policy tables (same tiers as model_policy.json)."""
    return json.loads(json.dumps(ORCH_M._EMBEDDED_POLICY))


class PolicyTests(unittest.TestCase):
    """model_decision mirror of ops_health.py semantics."""

    def test_01_cloud_name_always_denied(self):
        ok, why, caps = ORCH_M.model_decision(base_policy(), "mistral-7b-cloud",
                                              1_000_000, resident=())
        self.assertFalse(ok)
        self.assertIn("PRIMARY DIRECTIVE", why)
        self.assertEqual(caps, {})

    def test_02_ollama_com_pattern_denied(self):
        ok, why, _ = ORCH_M.model_decision(base_policy(), "http://ollama.com/foo",
                                           1_000_000, resident=())
        self.assertFalse(ok)
        self.assertIn("PRIMARY DIRECTIVE", why)

    def test_03_banned_pattern_denied(self):
        for name in ("kimi:latest", "glm-4.6-quant", "gpt-oss:120b"):
            ok, why, _ = ORCH_M.model_decision(base_policy(), name, 1_000_000)
            self.assertFalse(ok, name)
            self.assertIn("banned pattern", why)

    def test_04_unknown_model_default_deny(self):
        ok, why, _ = ORCH_M.model_decision(base_policy(), "not-in-policy:1b",
                                           1_000_000)
        self.assertFalse(ok)
        self.assertIn("default deny", why)

    def test_05_policy_none_denies_after_cloud_check(self):
        # a cloud name is denied even when no policy could be loaded
        ok, why, _ = ORCH_M.model_decision(None, "cloudy:1b", 1_000_000)
        self.assertFalse(ok)
        self.assertIn("PRIMARY DIRECTIVE", why)
        # a clean name with no policy is a default deny (403 identity class)
        ok, why, _ = ORCH_M.model_decision(None, "llama3.2:1b", 1_000_000)
        self.assertFalse(ok)
        self.assertTrue(ORCH_M.identity_deny(why))

    def test_06_small_model_fits(self):
        ok, why, caps = ORCH_M.model_decision(base_policy(), "qwen2.5:0.5b",
                                              4000, resident=())
        self.assertTrue(ok, why)
        self.assertEqual(caps, {"max_tokens": 256, "timeout_s": 60})

    def test_07_size_aware_ram_floor(self):
        # 4700 MB free would pass the old flat floor; size-aware denies
        # because 4700 - 5600 (mistral:7b) < floor 1500
        ok, why, _ = ORCH_M.model_decision(base_policy(), "mistral:7b", 4700)
        self.assertFalse(ok)
        self.assertIn("RAM:", why)
        # 4700 - 700 = 4000 >= 1500 -> small model still fits
        ok, why, _ = ORCH_M.model_decision(base_policy(), "qwen2.5:0.5b", 2300)
        self.assertTrue(ok, why)

    def test_08_tier_L_residency(self):
        ok, why, _ = ORCH_M.model_decision(base_policy(), "mistral:7b", 20_000,
                                           resident=("mistral:7b",),
                                           game_running=False)
        self.assertTrue(ok, why)  # already resident -> exclusive of itself
        ok, why, _ = ORCH_M.model_decision(base_policy(), "mistral:7b", 20_000,
                                           resident=("qwen2.5:0.5b",),
                                           game_running=False)
        self.assertFalse(ok)
        self.assertIn("exclusive window", why)

    def test_09_tier_L_game_idle(self):
        ok, why, _ = ORCH_M.model_decision(base_policy(), "mistral:7b", 20_000,
                                           resident=(), game_running=True)
        self.assertFalse(ok)
        self.assertIn("game to be idle", why)

    def test_10_tier_L_needs_size_plus_3000(self):
        # 7300 - 5600 = 1700 < 3000 -> deny; 8700 - 5600 = 3100 -> allow
        ok, why, _ = ORCH_M.model_decision(base_policy(), "mistral:7b", 7300,
                                           resident=(), game_running=False)
        self.assertFalse(ok)
        self.assertIn("size + 3000", why)
        ok, why, _ = ORCH_M.model_decision(base_policy(), "mistral:7b", 8700,
                                           resident=(), game_running=False)
        self.assertTrue(ok, why)

    def test_11_embed_misuse_and_ok(self):
        # embed-tier model refused for generation
        ok, why, _ = ORCH_M.model_decision(base_policy(),
                                           "nomic-embed-text:latest", 10_000,
                                           resident=(), kind="infer")
        self.assertFalse(ok)
        self.assertIn("embedding-only", why)
        # generation model refused for embeddings
        ok, why, _ = ORCH_M.model_decision(base_policy(), "llama3.2:1b", 10_000,
                                           resident=(), kind="embed")
        self.assertFalse(ok)
        self.assertIn("embed-tier", why)
        # embed-kind on an embed model is fine, with the embed timeout
        ok, why, caps = ORCH_M.model_decision(base_policy(),
                                              "nomic-embed-text:latest", 10_000,
                                              resident=(), kind="embed")
        self.assertTrue(ok, why)
        self.assertEqual(caps["timeout_s"], 30)

    def test_12_max_loaded_ceiling(self):
        pol = base_policy()
        pol["max_loaded"] = 2
        ok, why, _ = ORCH_M.model_decision(pol, "qwen2.5:0.5b", 30_000,
                                           resident=("llama3.2:1b", "gemma2:2b"))
        self.assertFalse(ok)
        self.assertIn("resident (max 2)", why)

    def test_13_tier_m_game_headroom(self):
        # gemma2:2b (2200 MB): game running requires free - need >= floor+1000
        # 4600 - 2200 = 2400 < 2500 -> deny; 4800 - 2200 = 2600 -> allow
        ok, why, _ = ORCH_M.model_decision(base_policy(), "gemma2:2b", 4600,
                                           resident=(), game_running=True)
        self.assertFalse(ok)
        self.assertIn("+1000 MB headroom", why)
        ok, why, caps = ORCH_M.model_decision(base_policy(), "gemma2:2b", 4800,
                                              resident=(), game_running=True)
        self.assertTrue(ok, why)

    def test_14_identity_vs_resource_deny_split(self):
        identity = ("model not in policy (default deny)",
                    "banned pattern 'kimi'",
                    "PRIMARY DIRECTIVE: cloud model names are never allowed",
                    "no model policy loaded: default deny",
                    "embedding-only model",
                    "kind=embed requires an embed-tier model")
        for reason in identity:
            self.assertTrue(ORCH_M.identity_deny(reason), reason)
        resource = ("RAM: 2000 MB free - 2600 model < floor 1500",
                    "already 2 models resident (max 2)",
                    "tier L waits for the game to be idle",
                    "tier L needs size + 3000 MB free",
                    "tier L needs the exclusive window: other models resident: qwen2.5:0.5b",
                    "tier M with the game running needs +1000 MB headroom")
        for reason in resource:
            self.assertFalse(ORCH_M.identity_deny(reason), reason)

    def test_15_restricted_tier_caps(self):
        ok, why, caps = ORCH_M.model_decision(base_policy(), "deepseek-r1:1.5b",
                                              10_000)
        self.assertTrue(ok, why)
        self.assertEqual(caps, {"max_tokens": 192, "timeout_s": 45})

    def test_16_load_policy_file(self):
        pol, p, src = ORCH_M.load_policy()
        self.assertIn("models", pol)
        self.assertTrue(src.startswith("file:"))

    def test_17_policy_fallback_never_blank(self):
        pol, p, src = ORCH_M.load_policy(r"Z:\no\such\path\model_policy.json")
        self.assertIn("models", pol)
        self.assertIn("embedded-defaults", src)


def make_job(**kw):
    """Worker-callable job dict bypassing the HTTP layer."""
    ORCH_M.job_seq += 1
    return {"id": str(ORCH_M.job_seq),
            "model": kw.get("model", "qwen2.5:0.5b"),
            "prompt": kw.get("prompt", "hi"),
            "num_predict": kw.get("num_predict", 64),
            "num_thread": kw.get("num_thread"),
            "caller": kw.get("caller", "batch"),
            "done_ev": threading.Event(), "out": None}


class SingleFlightBase(unittest.TestCase):
    """Harness: SingleFlight with stubbed ollama/RAM/log; sync _process_job."""

    def setUp(self):
        self.m = ORCH_M
        self.sf = self.m.SingleFlight.__new__(self.m.SingleFlight)
        sf, m = self.sf, self.m
        sf.policy = base_policy()
        sf.policy_source = "test"
        sf.queues = {c: queue.Queue(maxsize=m.QUEUE_CAPS[c]) for c in m.PRIORITY}
        sf.active_model = None
        sf.hot_model = None
        sf.last_infer_end = 0.0
        sf.started = 0.0
        sf.recent = []
        sf.stats = {"done": 0, "reject_queue": 0, "reject_ram": 0, "timeout": 0,
                    "errors": 0, "policy_deny_identity": 0,
                    "policy_deny_resource": 0, "queued_wait": 0}
        for c in m.PRIORITY:
            sf.stats["done_" + c] = 0
            sf.stats["reject_queue_" + c] = 0
        sf.lock = threading.Lock()
        sf.game_flag = False
        sf._game_flag = lambda: sf.game_flag
        m.free_ram_gb = lambda: self.free_gb
        m.ollama_ps = lambda: (list(self.hot_from_ps), None)
        self.free_gb = 12.0
        self.hot_from_ps = ()
        self.generate_calls = []

        def fake_generate(model, prompt, num_predict, timeout_s, num_thread=None):
            self.generate_calls.append({"model": model, "prompt": prompt,
                                        "num_predict": num_predict,
                                        "timeout_s": timeout_s,
                                        "num_thread": num_thread})
            return 200, {"response": "ok"}

        sf._generate = fake_generate

    def run_job(self, job):
        self.sf._process_job(job["caller"], job)
        self.assertTrue(job["done_ev"].is_set())

    def run_through_worker(self, jobs):
        """Push jobs through submit() + a transient worker thread."""
        for job in jobs:
            self.sf.submit(job["caller"], job)
        t = threading.Thread(target=lambda: [self.sf._process_job(c, j)
                                             for c, j in self._drain_all()])
        t.start()
        for job in jobs:
            self.assertTrue(job["done_ev"].wait(timeout=10),
                            "job %s never finished" % job["id"])
        t.join(timeout=5)

    def _drain_all(self):
        out = []
        for c in self.m.PRIORITY:
            while True:
                try:
                    out.append((c, self.sf.queues[c].get_nowait()))
                except queue.Empty:
                    break
        return out


class QueuePriorityTests(SingleFlightBase):
    def test_18_priority_drain_order(self):
        """THE no-starvation property: with all three queues loaded, the
        worker takes game -> crew -> batch regardless of arrival order."""
        sf = self.sf
        seq = []

        def gen(model, prompt, np, ts, nt=None):
            seq.append((model, prompt))
            return 200, {"response": prompt}

        sf._generate = gen
        g = make_job(caller="game", prompt="G")
        c_ = make_job(caller="crew", prompt="C")
        b = make_job(caller="batch", prompt="B")
        # enqueue in reverse: batch first, then crew, then game -> first out
        sf.submit("batch", b)
        sf.submit("crew", c_)
        sf.submit("game", g)
        first, second, third = (sf._next_job_once(), sf._next_job_once(),
                                sf._next_job_once())
        self.assertIs(first[1], g)
        self.assertIs(second[1], c_)
        self.assertIs(third[1], b)

    def test_19_game_unfair_share(self):
        """8 batch jobs queued + game job behind them -> game first."""
        sf = self.sf
        game = make_job(caller="game")
        for _ in range(8):
            sf.submit("batch", make_job(caller="batch"))
        sf.submit("game", game)
        first = sf._next_job_once()
        self.assertIs(first[1], game)

    def test_20_per_class_queue_caps_429(self):
        sf = self.sf
        cap = self.m.QUEUE_CAPS["batch"]
        with self.assertRaises(queue.Full):
            for _ in range(cap + 1):
                sf.submit("batch", make_job(caller="batch"))
        self.assertEqual(sf.queues["batch"].qsize(), cap)

    def test_21_unknown_caller_defaults_batch(self):
        sf = self.sf
        sf.submit("hooligan", make_job())
        self.assertEqual(sf.queues["batch"].qsize(), 1)


class WorkerPolicyTests(SingleFlightBase):
    def test_22_num_predict_clamped_to_policy(self):
        self.hot_from_ps = ("qwen2.5:0.5b",)  # already hot -> skip RAM gate
        job = make_job(model="qwen2.5:0.5b", num_predict=999, caller="game")
        job["caller"] = "game"
        self.run_job(job)
        # policy S cap 256 beats the requested 999
        self.assertEqual(self.generate_calls[0]["num_predict"], 256)
        self.assertEqual(job["out"][0], 200)

    def test_23_num_ctx_hard_cap(self):
        job = make_job(model="qwen2.5:0.5b")
        self.run_job(job)
        # _generate itself stamps num_ctx: verify via the outbound request body
        sent = self.capture_generate_body("qwen2.5:0.5b", 32)
        self.assertEqual(sent["body"]["options"]["num_ctx"], self.m.NUM_CTX)
        self.assertEqual(sent["url"], self.m.OLLAMA + "/api/generate")

    def capture_generate_body(self, model, num_predict, num_thread=None):
        sent = {}
        sf = self.m.SingleFlight.__new__(self.m.SingleFlight)  # bare: no _generate shadow

        class FakeResp:
            def __enter__(self):
                return self

            def __exit__(self, *a):
                return False

            def read(self):
                return json.dumps({"response": "ok"}).encode()

        def fake_urlopen(req, timeout=None):
            sent["url"] = req.full_url
            sent["body"] = json.loads(req.data.decode())
            sent["timeout"] = timeout
            return FakeResp()

        real = self.m.urllib.request.urlopen
        self.m.urllib.request.urlopen = fake_urlopen
        try:
            code, body = sf._generate(model, "p", num_predict, 60,
                                      num_thread)
        finally:
            self.m.urllib.request.urlopen = real
        return sent

    def test_24_identity_deny_is_403(self):
        self.hot_from_ps = ("llama3.2:1b",)
        job = make_job(model="cloud-sauce:8b", caller="game")
        self.run_job(job)
        code, body = job["out"]
        self.assertEqual(code, 403)
        self.assertEqual(body["error"], "policy_deny")
        self.assertEqual(self.sf.stats["policy_deny_identity"], 1)
        self.assertEqual(self.generate_calls, [])

    def test_25_resource_deny_is_507(self):
        """2 GB free, llama3.2:1b needs 1500 -> 2000-1500 < floor 1500 -> 507."""
        self.free_gb = 2.0
        job = make_job(model="llama3.2:1b")
        self.run_job(job)
        code, body = job["out"]
        self.assertEqual(code, 507)
        self.assertEqual(body["error"], "resource_denied")
        self.assertEqual(self.sf.stats["reject_ram"], 1)

    def test_26_resident_model_skips_ram_gate(self):
        """Model already hot per /api/ps -> `already` relaxes the RAM floor."""
        self.free_gb = 1.0
        self.hot_from_ps = ("qwen2.5:0.5b",)
        job = make_job(model="qwen2.5:0.5b", caller="game")
        self.run_job(job)
        self.assertEqual(job["out"][0], 200)
        self.assertEqual(self.generate_calls, [{
            "model": "qwen2.5:0.5b", "prompt": "hi", "num_predict": 64,
            "timeout_s": 60, "num_thread": None}])

    def test_27_worker_survives_generate_crash(self):
        def boom(*a, **k):
            raise RuntimeError("simulated upstream failure")
        self.hot_from_ps = ("qwen2.5:0.5b",)
        self.sf._generate = boom
        job = make_job(model="qwen2.5:0.5b", caller="game")
        self.run_job(job)
        code, body = job["out"]
        self.assertEqual(code, 502)
        self.assertEqual(body["error"], "worker_error")
        self.assertEqual(self.sf.stats["errors"], 1)

    def test_28_generate_outbound_contract(self):
        sent = self.capture_generate_body("qwen2.5:0.5b", 999)
        self.assertEqual(sent["url"], self.m.OLLAMA + "/api/generate")
        self.assertEqual(sent["body"]["options"]["num_ctx"], self.m.NUM_CTX)
        self.assertEqual(sent["body"]["options"]["num_predict"], 999)
        self.assertEqual(sent["body"]["stream"], False)
        self.assertIn("keep_alive", sent["body"])

    def test_29_generate_num_thread_passthrough(self):
        sent = self.capture_generate_body("qwen2.5:0.5b", 10, num_thread=4)
        self.assertEqual(sent["body"]["options"]["num_thread"], 4)


class EndToEndQueueTests(SingleFlightBase):
    def test_30_submit_through_worker_keeps_priority(self):
        """Full path: submit() reverse-ordered -> one worker pass drains
        game first, then crew, then batch."""
        order = []

        def gen(model, prompt, np, ts, nt=None):
            order.append(prompt)
            return 200, {"response": prompt}

        self.sf._generate = gen
        self.hot_from_ps = ("qwen2.5:0.5b",)
        g = make_job(caller="game", prompt="G")
        c_ = make_job(caller="crew", prompt="C")
        b = make_job(caller="batch", prompt="B")
        self.run_through_worker([b, c_, g])
        self.assertEqual(order, ["G", "C", "B"])
        self.assertEqual(self.sf.stats["done_game"], 1)
        self.assertEqual(self.sf.stats["done_crew"], 1)
        self.assertEqual(self.sf.stats["done_batch"], 1)


class GameSamplerTests(unittest.TestCase):
    def test_31_sampler_offline_default(self):
        """Library import (no main()) keeps GAME_SAMPLER None and never
        lets _game_flag crash."""
        self.assertIsNone(ORCH_M.GAME_SAMPLER)
        sf = ORCH_M.SingleFlight.__new__(ORCH_M.SingleFlight)
        self.assertFalse(sf._game_flag())


# ======================================================================
# Compat-layer tests (UNIFY-1 migration support): the gateway must speak
# enough ollama AND OpenAI that migrated callers keep their shapes.
# ======================================================================

def make_chat_job(**kw):
    """Worker-callable chat job (native /api/chat passthrough shape)."""
    ORCH_M.job_seq += 1
    chat_body = {
        "model": kw.get("model", "qwen2.5:0.5b"),
        "messages": kw.get("messages", [{"role": "user", "content": "hi"}]),
        "stream": False,
        "keep_alive": 0,
        "options": {},
    }
    if kw.get("tools"):
        chat_body["tools"] = kw["tools"]
    return {"id": str(ORCH_M.job_seq),
            "model": kw.get("model", "qwen2.5:0.5b"),
            "chat_body": chat_body,
            "num_predict": kw.get("num_predict", 64),
            "num_thread": kw.get("num_thread"),
            "caller": kw.get("caller", "batch"),
            "done_ev": threading.Event(), "out": None}


class ChatNativeWorkerTests(SingleFlightBase):
    """/api/chat rides the same single flight; tool_calls must survive."""

    def setUp(self):
        super().setUp()
        self.sf._generate = lambda *a, **k: self.fail(
            "chat job must not hit the generate lane")
        self.chat_calls = []

        def fake_gen(chat_body, num_predict, timeout_s, num_thread=None):
            self.chat_calls.append({"body": chat_body,
                                    "num_predict": num_predict,
                                    "timeout_s": timeout_s,
                                    "num_thread": num_thread})
            return 200, {"message": {"role": "assistant", "content": "ok",
                                     "tool_calls": [{"function": {
                                         "name": "read_file",
                                         "arguments": {"path": "x"}}}],
                                     "done": True}}

        self.sf._generate_chat = fake_gen

    def test_compat_01_chat_goes_to_chat_lane(self):
        self.hot_from_ps = ("qwen2.5:0.5b",)
        job = make_chat_job(caller="game")
        self.run_job(job)
        self.assertEqual(job["out"][0], 200)
        self.assertEqual(len(self.chat_calls), 1)
        self.assertEqual(job["out"][1]["message"]["tool_calls"][0]["function"]["name"],
                         "read_file")

    def test_compat_02_chat_options_capped_at_dispatch(self):
        self.hot_from_ps = ("qwen2.5:0.5b",)
        job = make_chat_job(caller="game", num_predict=512)
        self.run_job(job)
        sent = self.chat_calls[0]
        # 512 requested; tier S caps qwen2.5:0.5b at 256 -> clamp wins in
        # _process_job; the options-dict stamping itself is compat_04's
        # job (real _generate_chat).
        self.assertEqual(sent["num_predict"], 256)

    def test_compat_03_chat_through_queue_keeps_priority(self):
        """Chat jobs queue/prioritise exactly like generate jobs."""
        order = []
        self.hot_from_ps = ("qwen2.5:0.5b",)
        self.sf._generate_chat = lambda b, n, t, nt=None: (
            order.append("C:" + b["model"]) or (200, {"message": {}, "done": True}))
        self.sf._generate = lambda m, p, n, t, nt=None: (
            order.append("G:" + m) or (200, {"response": p}))
        b = make_chat_job(caller="batch", model="gemma2:2b")
        c_ = make_chat_job(caller="crew")
        g = make_job(caller="game")
        self.run_through_worker([b, c_, g])
        self.assertEqual(order, ["G:qwen2.5:0.5b", "C:qwen2.5:0.5b",
                                 "C:gemma2:2b"])

    def test_compat_04_chat_outbound_contract(self):
        """Outbound /api/chat: native URL, tools preserved, stream off,
        num_ctx stamped by _generate_chat itself."""
        m = self.m
        sent = {}
        sf = m.SingleFlight.__new__(m.SingleFlight)

        class FakeResp:
            def __enter__(self): return self
            def __exit__(self, *a): return False
            def read(self):
                return json.dumps({"message": {"role": "assistant",
                                               "content": "ok",
                                               "tool_calls": [1, 2, 3]},
                                   "done": True}).encode()

        def fake_urlopen(req, timeout=None):
            sent["url"] = req.full_url
            sent["body"] = json.loads(req.data.decode())
            sent["timeout"] = timeout
            return FakeResp()

        real = m.urllib.request.urlopen
        m.urllib.request.urlopen = fake_urlopen
        try:
            code, body = sf._generate_chat(
                {"model": "qwen2.5:0.5b",
                 "messages": [{"role": "user", "content": "hi"}],
                 "tools": [{"type": "function",
                            "function": {"name": "read_file"}}],
                 "stream": True, "options": {}},
                64, 60)
        finally:
            m.urllib.request.urlopen = real
        self.assertEqual(code, 200)
        self.assertEqual(sent["url"], m.OLLAMA + "/api/chat")
        self.assertEqual(sent["body"]["stream"], False)
        self.assertEqual(sent["body"]["tools"][0]["function"]["name"], "read_file")
        self.assertEqual(sent["body"]["options"]["num_ctx"], m.NUM_CTX)
        self.assertEqual(sent["body"]["options"]["num_predict"], 64)
        self.assertEqual(body["message"]["tool_calls"], [1, 2, 3])
        self.assertTrue(body["done"])


class CompatPolicyTests(unittest.TestCase):
    """qwen2.5-coder:0.5b is the default model of the OpenAI-shape viper
    scripts; it must pass the identity gate (no default-deny 403)."""

    def test_compat_05_viper_default_model_in_policy(self):
        pol = base_policy()
        ok, why, caps = ORCH_M.model_decision(pol, "qwen2.5-coder:0.5b",
                                              4000, resident=())
        self.assertTrue(ok, why)
        self.assertEqual(caps["max_tokens"], 256)
        self.assertEqual(caps["timeout_s"], 60)

    def test_compat_06_policy_file_carries_viper_default(self):
        pol, p, src = ORCH_M.load_policy()
        entry = (pol.get("models") or {}).get("qwen2.5-coder:0.5b")
        self.assertIsNotNone(entry,
                             "model_policy.json lost qwen2.5-coder:0.5b")
        self.assertEqual(entry["tier"], "S")


if __name__ == "__main__":
    unittest.main(verbosity=2)