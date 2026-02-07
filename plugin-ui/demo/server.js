import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = fileURLToPath(new URL(".", import.meta.url));
const publicDir = join(__dirname, "public");

const MIME_TYPES = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon"
};

const server = createServer(async (req, res) => {
  try {
    const url = new URL(req.url ?? "/", "http://localhost");
    const pathname = url.pathname === "/" ? "/index.html" : url.pathname;
    const safePath = normalize(pathname).replace(/^\.(?=\/)/, "");
    const filePath = join(publicDir, safePath);
    const data = await readFile(filePath);
    const ext = extname(filePath);

    res.writeHead(200, {
      "Content-Type": MIME_TYPES[ext] ?? "application/octet-stream"
    });
    res.end(data);
  } catch (error) {
    res.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
    res.end("Not found");
  }
});

const port = process.env.PORT ? Number.parseInt(process.env.PORT, 10) : 3000;
server.listen(port, "0.0.0.0", () => {
  // eslint-disable-next-line no-console
  console.log(`Parser demo running at http://localhost:${port}`);
});
