# Test web server for the remote-screen VM test: serves one origin's pages and noVNC, and records the
# results the pages POST to /result/<name> as files in RESULTS. With --credentials it also serves that
# file at /credentials (only the legitimate viewer's origin gets this). GET /signal/<name> answers 200
# once RESULTS/<name> exists, so the test script can tell a page when to go on.
import http.server, json, os, sys

host, port, root, results = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
credentials = sys.argv[6] if len(sys.argv) > 6 and sys.argv[5] == "--credentials" else None

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=root, **kw)

    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def do_GET(self):
        if self.path == "/credentials" and credentials:
            body = open(credentials, "rb").read()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(body)
        elif self.path.startswith("/signal/"):
            name = os.path.basename(self.path)
            self.send_response(200 if os.path.exists(os.path.join(results, name)) else 404)
            self.end_headers()
        else:
            super().do_GET()

    def do_POST(self):
        name = os.path.basename(self.path)
        body = self.rfile.read(int(self.headers["Content-Length"]))
        json.loads(body)
        with open(os.path.join(results, name + ".json"), "wb") as f:
            f.write(body)
        self.send_response(204)
        self.end_headers()

http.server.ThreadingHTTPServer((host, port), Handler).serve_forever()
