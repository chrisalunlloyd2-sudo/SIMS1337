package com.aigen.sims.web;

import com.aigen.sims.commander.Strategy;
import com.aigen.sims.deploy.GateKeeper;
import com.aigen.sims.engine.TocTokTree;
import com.aigen.sims.lora.AdapterRegistry;
import com.aigen.sims.mining.SuggestionRegistry;
import com.aigen.sims.scheduler.PipelineScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WebDashboard -- 2026-07-28 Architect gist, section 4: "Replace JavaFX with a web-based UI served
 * from a local HTTP server ... Chris opens it in Chrome on his phone." JavaFX has no display
 * server on Termux/Android; this is a real, dependency-free HTTP server (the JDK's own
 * com.sun.net.httpserver -- no Spring/Netty needed) that runs anywhere Java runs.
 *
 * Real data only: every endpoint reads the SAME live in-process objects the rest of SIMS1337
 * already uses (SuggestionRegistry, GateKeeper, AdapterRegistry, AegisCommander, PipelineScheduler)
 * -- nothing here is a second, disconnected copy of that state.
 *
 * Deliberately polling-based (GET + periodic refresh), not WebSocket streaming -- claiming
 * real-time push without actually building it would be fabrication. A real, working v1 first.
 */
public class WebDashboard {
    // 2026-08-02: constructed from the scheduler itself, not separately-built copies -- WebDashboard
    // was never actually instantiated anywhere before this (verified: no caller in the whole repo),
    // so its old constructor shape had no external dependents to preserve. Sharing the SAME
    // scheduler.gateKeeper()/adapterRegistry() instances (not fresh ones) is what makes the dashboard
    // reflect real accumulating state instead of a snapshot from whenever it happened to be built.
    private final PipelineScheduler scheduler;
    private final GateKeeper gate;
    private final AdapterRegistry adapters;
    private final TocTokTree knowledge;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    public WebDashboard(PipelineScheduler scheduler, int port) {
        this.scheduler = scheduler;
        this.gate = scheduler.gateKeeper();
        this.adapters = scheduler.adapterRegistry();
        this.knowledge = new TocTokTree(scheduler.repoPath() + "/scripts/toc_tok/toc_tok.json");
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::serveIndex);
        server.createContext("/api/status", this::serveStatus);
        server.createContext("/api/suggestions", this::serveSuggestions);
        server.createContext("/api/proposals", this::serveProposals);
        server.createContext("/api/knowledge", this::serveKnowledge);
        server.setExecutor(null);
        server.start();
        System.out.println("[WebDashboard] serving on http://0.0.0.0:" + port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void write(HttpExchange ex, int code, String contentType, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private void writeJson(HttpExchange ex, Object obj) throws IOException {
        write(ex, 200, "application/json", mapper.writeValueAsBytes(obj));
    }

    private void serveIndex(HttpExchange ex) throws IOException {
        write(ex, 200, "text/html; charset=utf-8", PAGE.getBytes(StandardCharsets.UTF_8));
    }

    private void serveStatus(HttpExchange ex) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("suggestions", new SuggestionRegistry(scheduler.suggestionsPath()).getSummary());
        out.put("proposals", gate.getSummary());
        out.put("adapters", adapters.getSummary());
        try {
            Strategy s = scheduler.currentStrategy();
            Map<String, Object> strat = new LinkedHashMap<>();
            strat.put("focusRepo", s.focusRepo == null ? "" : s.focusRepo);
            strat.put("focusModel", s.focusModel == null ? "" : s.focusModel);
            strat.put("reason", s.reason);
            out.put("strategy", strat);
        } catch (Exception e) {
            out.put("strategy", Map.of("error", e.getMessage() == null ? "unknown" : e.getMessage()));
        }
        out.put("cycleLog", scheduler.cycleLog());
        writeJson(ex, out);
    }

    private void serveSuggestions(HttpExchange ex) throws IOException {
        // fresh read each poll: SuggestionRegistry reloads from disk in its own constructor, so this
        // reflects whatever the mining phase most recently wrote, not a snapshot from dashboard startup.
        List<Map<String, Object>> out = new SuggestionRegistry(scheduler.suggestionsPath()).getPendingSuggestions().stream()
            .map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.id); m.put("repoName", s.repoName); m.put("modelName", s.modelName);
                m.put("description", s.description); m.put("hexQ", s.hexQ); m.put("hexR", s.hexR);
                m.put("status", s.status);
                return m;
            }).collect(Collectors.toList());
        writeJson(ex, out);
    }

    private void serveProposals(HttpExchange ex) throws IOException {
        List<Map<String, Object>> out = gate.getProposals().stream()
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.id); m.put("repoName", p.repoName); m.put("filePath", p.filePath);
                m.put("modelName", p.modelName); m.put("status", p.status);
                m.put("hexQ", p.hexQ); m.put("hexR", p.hexR);
                return m;
            }).collect(Collectors.toList());
        writeJson(ex, out);
    }

    private void serveKnowledge(HttpExchange ex) throws IOException {
        // TocTokTree owns the Java read-side of toc_tok.json (Python owns writes); reload() here so
        // a poll always reflects the toc_tok.py process's latest state, not whatever existed when
        // this WebDashboard instance was constructed.
        knowledge.reload();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nodeCount", knowledge.size());
        out.put("subtree", knowledge.subtree("/projects/SIMS1337"));
        writeJson(ex, out);
    }

    private static final String PAGE = """
        <!doctype html>
        <html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>SIMS1337</title>
        <style>
          :root { --bg:#0a0d12; --panel:#11161f; --line:#202836; --ink:#eef2f8; --dim:#7d8ba3; --accent:#3ddbd9; }
          * { box-sizing: border-box; }
          body { margin:0; background:var(--bg); color:var(--ink); font-family:system-ui,sans-serif; }
          header { padding:1rem 1.2rem; border-bottom:1px solid var(--line); }
          h1 { font-size:1.4rem; margin:0; }
          .sub { color:var(--dim); font-size:.9rem; }
          .wrap { padding:1rem 1.2rem 2rem; display:flex; flex-direction:column; gap:1rem; }
          .card { background:var(--panel); border:1px solid var(--line); border-radius:14px; padding:1rem; }
          .card h2 { font-size:1.05rem; margin:0 0 .6rem; color:var(--accent); }
          .row { display:flex; justify-content:space-between; padding:.3rem 0; border-bottom:1px solid var(--line); }
          .row:last-child { border-bottom:none; }
          canvas { width:100%; max-width:420px; display:block; margin:0 auto; }
          .badge { display:inline-block; padding:.15rem .5rem; border-radius:999px; font-size:.75rem; }
          .PENDING { background:#3a3410; color:#f5cf42; }
          .APPROVED,.MERGED,.DEPLOYED { background:#123a1e; color:#3ddb7a; }
          .REJECTED { background:#3a1414; color:#e85a5a; }
          pre { white-space:pre-wrap; font-size:.8rem; color:var(--dim); max-height:200px; overflow:auto; }
        </style>
        </head><body>
        <header><h1>SIMS1337 Command Center</h1><div class="sub">real-time-ish (polls every 5s), no JavaFX needed</div></header>
        <div class="wrap">
          <div class="card"><h2>Strategy</h2><div id="strategy">loading...</div></div>
          <div class="card"><h2>Hex Grid (real suggestion positions)</h2><canvas id="hexCanvas" width="380" height="340"></canvas></div>
          <div class="card"><h2>Suggestions</h2><div id="suggestions">loading...</div></div>
          <div class="card"><h2>Deploy Proposals</h2><div id="proposals">loading...</div></div>
          <div class="card"><h2>Knowledge Tree (TOC-TOK)</h2><pre id="knowledge"></pre></div>
          <div class="card"><h2>Last Cycle Log</h2><pre id="cycleLog"></pre></div>
        </div>
        <script>
        async function refresh() {
          const status = await (await fetch('/api/status')).json();
          const s = status.strategy || {};
          document.getElementById('strategy').innerHTML =
            `<div class="row"><span>Focus repo</span><span>${s.focusRepo || '(none yet)'}</span></div>` +
            `<div class="row"><span>Focus model</span><span>${s.focusModel || '(none yet)'}</span></div>` +
            `<div class="row"><span>Reason</span><span>${s.reason || ''}</span></div>`;
          document.getElementById('cycleLog').textContent = status.cycleLog || '(no cycle run yet)';

          const sugg = await (await fetch('/api/suggestions')).json();
          document.getElementById('suggestions').innerHTML = sugg.length ? sugg.map(x =>
            `<div class="row"><span>${x.repoName}/${x.modelName}</span><span class="badge ${x.status}">${x.status}</span></div>`
          ).join('') : '<div class="row"><span>(none pending)</span></div>';

          const props = await (await fetch('/api/proposals')).json();
          document.getElementById('proposals').innerHTML = props.length ? props.map(x =>
            `<div class="row"><span>${x.filePath}</span><span class="badge ${x.status}">${x.status}</span></div>`
          ).join('') : '<div class="row"><span>(none yet)</span></div>';

          drawHex(sugg.concat(props));

          const kg = await (await fetch('/api/knowledge')).json();
          document.getElementById('knowledge').textContent =
            `${kg.nodeCount} nodes total\n` + (kg.subtree || []).join('\n');
        }
        function drawHex(items) {
          const cv = document.getElementById('hexCanvas'), ctx = cv.getContext('2d');
          ctx.clearRect(0,0,cv.width,cv.height);
          const R=22, CX=cv.width/2, CY=cv.height/2;
          for (const it of items) {
            const x = CX + R*1.5*it.hexQ, y = CY + R*(Math.sqrt(3)*(it.hexR + it.hexQ/2));
            ctx.beginPath();
            for (let i=0;i<6;i++){ const a=Math.PI/180*(60*i); const px=x+R*0.85*Math.cos(a), py=y+R*0.85*Math.sin(a);
              i===0?ctx.moveTo(px,py):ctx.lineTo(px,py); }
            ctx.closePath();
            ctx.fillStyle = '#1c5f5e'; ctx.fill();
            ctx.strokeStyle = '#3ddbd9'; ctx.stroke();
          }
        }
        refresh();
        setInterval(refresh, 5000);
        </script>
        </body></html>
        """;
}
