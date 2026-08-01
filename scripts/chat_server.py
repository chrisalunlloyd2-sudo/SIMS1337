#!/usr/bin/env python3
"""
chat_server.py — minimal HTTP chat server for the fleet.
Exposes local models (Ollama :11434 or GGUF :5000) as an OpenAI-style
/v1/chat/completions endpoint so the desktop/web UI can talk to SLMs.

Usage:
  python3 chat_server.py --port 8899 --backend ollama
"""
import argparse, json, sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen

class ChatHandler(BaseHTTPRequestHandler):
    backend_url = "http://localhost:11434"
    backend = "ollama"
    def log_message(self, *a): pass

    def _send(self, code, obj):
        body = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/v1/models":
            try:
                with urlopen(self.backend_url + "/api/tags", timeout=3) as r:
                    data = json.load(r)
                    models = [{"id": m["name"]} for m in data.get("models", [])]
                self._send(200, {"object": "list", "data": models})
            except Exception as e:
                self._send(500, {"error": str(e)})
        elif self.path == "/healthz":
            self._send(200, {"status": "ok"})
        else:
            self._send(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/v1/chat/completions":
            self._send(404, {"error": "not found"}); return
        n = int(self.headers.get("Content-Length", 0))
        try:
            req = json.loads(self.rfile.read(n))
        except Exception:
            self._send(400, {"error": "bad json"}); return
        model = req.get("model", "qwen2.5:0.5b")
        messages = req.get("messages", [])
        prompt = "\n".join(f"{m.get('role','user')}: {m.get('content','')}" for m in messages)
        prompt += "\nassistant:"
        try:
            if self.backend == "ollama":
                payload = json.dumps({"model": model, "prompt": prompt,
                                      "stream": False, "options": {"num_predict": req.get("max_tokens", 256)}}).encode()
                with urlopen(Request(self.backend_url + "/api/generate", data=payload,
                                     headers={"Content-Type": "application/json"}), timeout=180) as r:
                    out = json.load(r).get("response", "")
            else:  # gguf server v2
                payload = json.dumps({"prompt": prompt, "max_tokens": req.get("max_tokens", 256)}).encode()
                with urlopen(Request(self.backend_url + "/api/generate", data=payload,
                                     headers={"Content-Type": "application/json"}), timeout=180) as r:
                    out = json.load(r).get("response", json.load(r).get("content", ""))
            self._send(200, {"choices": [{"message": {"role": "assistant", "content": out}}]})
        except Exception as e:
            self._send(500, {"error": str(e)})

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--port", type=int, default=8899)
    p.add_argument("--backend", choices=["ollama", "gguf"], default="ollama")
    p.add_argument("--backend-url", default=None)
    args = p.parse_args()
    ChatHandler.backend = args.backend
    ChatHandler.backend_url = args.backend_url or ("http://localhost:11434" if args.backend == "ollama" else "http://localhost:5000")
    print(f"[chat_server] {args.backend} → {ChatHandler.backend_url} | :{args.port}")
    HTTPServer(("0.0.0.0", args.port), ChatHandler).serve_forever()

if __name__ == "__main__":
    main()
