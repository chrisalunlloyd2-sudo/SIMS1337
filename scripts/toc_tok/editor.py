#!/usr/bin/env python3
"""
editor.py — TOC-TOK GUI editor (hex map + tree panel)

Serves a self-contained HTML editor over the hex-anchored knowledge tree.
Drag nodes between hex cells on the map; edit tree structure in the panel.

  python3 editor.py --port 8899 [--toc toc_tok.json]
  → open http://localhost:8899

API:
  GET  /api/tree              full tree JSON
  GET  /api/at?hex=2,1        nodes at/near hex
  POST /api/node              add/update node {path, title?, type?, hex?, tags?, content?}
  POST /api/move              move node {path, hex}
  POST /api/remove            remove node {path}
"""
import argparse, json, os, sys, time
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs
from toc_tok import load as toc_load, save as toc_save, add_node, get_path, find_by_hex

HTML = None  # loaded below from editor.html

class Handler(BaseHTTPRequestHandler):
    toc_file = "toc_tok.json"
    def log_message(self, *a): pass

    def _send(self, code, body, ctype="application/json"):
        if isinstance(body, (dict, list)): body = json.dumps(body).encode()
        elif isinstance(body, str): body = body.encode()
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        n = int(self.headers.get("Content-Length", 0))
        return json.loads(self.rfile.read(n)) if n else {}

    def do_GET(self):
        u = urlparse(self.path)
        if u.path == "/" or u.path == "/index.html":
            self._send(200, HTML or "<h1>editor.html missing</h1>", "text/html")
        elif u.path == "/api/tree":
            self._send(200, toc_load(self.toc_file))
        elif u.path == "/api/at":
            qs = parse_qs(u.query)
            hex_str = qs.get("hex", ["0,0"])[0]
            hits = [{"dist": d, "path": n.get("path"), "type": n.get("type"),
                     "hex": n.get("hex"), "title": n.get("title"),
                     "content": n.get("content", "")} for d, n in find_by_hex(toc_load(self.toc_file), hex_str)]
            self._send(200, hits)
        else:
            self._send(404, {"error": "not found"})

    def do_POST(self):
        u = urlparse(self.path)
        data = self._body()
        tree = toc_load(self.toc_file)
        if u.path == "/api/node":
            meta = {k: data.get(k) for k in ("title", "type", "hex", "tags", "content") if data.get(k) is not None}
            if data.get("tags") and isinstance(data["tags"], str):
                meta["tags"] = data["tags"].split(",")
            node, created = add_node(tree, data["path"], meta)
            toc_save(tree, self.toc_file)
            self._send(200, {"created": created, "path": node["path"], "hex": node["hex"]})
        elif u.path == "/api/move":
            node = get_path(tree, data["path"])
            if not node: self._send(404, {"error": "no node"}); return
            node["hex"] = data["hex"]
            node["updated"] = time.time()
            toc_save(tree, self.toc_file)
            self._send(200, {"moved": data["path"], "hex": data["hex"]})
        elif u.path == "/api/remove":
            parent, name = data["path"].rsplit("/", 1)
            pnode = get_path(tree, parent) if parent else tree.get("root")
            if pnode and name in pnode.get("children", {}):
                del pnode["children"][name]
                toc_save(tree, self.toc_file)
                self._send(200, {"removed": data["path"]})
            else:
                self._send(404, {"error": "no node"})
        else:
            self._send(404, {"error": "not found"})

def main():
    p = argparse.ArgumentParser(description="TOC-TOK GUI editor")
    p.add_argument("--port", type=int, default=8899)
    p.add_argument("--toc", default=os.environ.get("TOC_TOK_FILE", "toc_tok.json"))
    args = p.parse_args()
    global HTML
    here = os.path.dirname(os.path.abspath(__file__))
    html_path = os.path.join(here, "editor.html")
    if os.path.exists(html_path):
        HTML = open(html_path).read()
    Handler.toc_file = args.toc
    print(f"[editor] serving {args.toc} on http://localhost:{args.port}")
    HTTPServer(("0.0.0.0", args.port), Handler).serve_forever()

if __name__ == "__main__":
    main()
