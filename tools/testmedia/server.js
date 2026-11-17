// Servidor para la fase 0 y pruebas de desarrollo: sirve el set de medios por HTTP con soporte de Range,
// que es lo que necesita el Chromecast para hacer seek. Loguea cada request para ver qué pide
// el dispositivo y con qué headers.
const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');

const ROOT = __dirname;
const PORT = Number(process.env.PORT || 8099);

const MIME = {
  '.mp4': 'video/mp4',
  '.webm': 'video/webm',
  '.mkv': 'video/x-matroska',
  '.m3u8': 'application/vnd.apple.mpegurl',
  '.ts': 'video/mp2t',
  '.key': 'application/octet-stream',
  '.txt': 'text/plain; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json',
};

const lanIps = Object.values(os.networkInterfaces())
  .flat()
  .filter(
    (i) =>
      i.family === 'IPv4' &&
      !i.internal &&
      // fuera las virtuales: VirtualBox host-only y WSL/Hyper-V
      !/^192\.168\.56\./.test(i.address) &&
      !/^172\.(1[6-9]|2\d|3[01])\./.test(i.address)
  )
  .map((i) => i.address);

const MEDIA_BASE = process.env.MEDIA_BASE || `http://${lanIps[0] || '127.0.0.1'}:${PORT}`;

const log = (o) => console.log(JSON.stringify({ t: new Date().toISOString(), ...o }));

function safeJoin(root, urlPath) {
  const p = path.normalize(path.join(root, decodeURIComponent(urlPath)));
  return p.startsWith(root) ? p : null;
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://x');
  log({
    method: req.method,
    path: url.pathname,
    range: req.headers.range || null,
    referer: req.headers.referer || null,
    ua: req.headers['user-agent'] || null,
  });

  if (url.pathname === '/config.json') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ lanIps, port: PORT }));
  }

  let filePath = safeJoin(ROOT, url.pathname === '/' ? '/sender.html' : url.pathname);
  if (!filePath || !fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
    const mediaPath = safeJoin(path.join(ROOT, 'media'), url.pathname);
    if (mediaPath && fs.existsSync(mediaPath) && fs.statSync(mediaPath).isFile()) {
      filePath = mediaPath;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('not found');
    }
  }

  if (filePath.endsWith('sender.html')) {
    const base = MEDIA_BASE;
    const html = fs.readFileSync(filePath, 'utf8').replaceAll('__MEDIA_BASE__', base);
    res.writeHead(200, { 'Content-Type': MIME['.html'] });
    return res.end(html);
  }

  const stat = fs.statSync(filePath);
  const type = MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream';
  const range = req.headers.range;

  if (range) {
    const m = /^bytes=(\d*)-(\d*)$/.exec(range);
    if (!m) {
      res.writeHead(416, { 'Content-Range': `bytes */${stat.size}` });
      return res.end();
    }
    let start = m[1] === '' ? stat.size - Number(m[2]) : Number(m[1]);
    let end = m[1] !== '' && m[2] !== '' ? Number(m[2]) : stat.size - 1;
    if (Number.isNaN(start) || start < 0) start = 0;
    if (end >= stat.size) end = stat.size - 1;
    if (start > end) {
      res.writeHead(416, { 'Content-Range': `bytes */${stat.size}` });
      return res.end();
    }
    res.writeHead(206, {
      'Content-Type': type,
      'Content-Length': end - start + 1,
      'Content-Range': `bytes ${start}-${end}/${stat.size}`,
      'Accept-Ranges': 'bytes',
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'no-store',
    });
    if (req.method === 'HEAD') return res.end();
    return fs.createReadStream(filePath, { start, end }).pipe(res);
  }

  res.writeHead(200, {
    'Content-Type': type,
    'Content-Length': stat.size,
    'Accept-Ranges': 'bytes',
    'Access-Control-Allow-Origin': '*',
    'Cache-Control': 'no-store',
  });
  if (req.method === 'HEAD') return res.end();
  fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`escuchando en ${lanIps.map((ip) => `http://${ip}:${PORT}`).join('  ')}`);
  console.log(`sender: http://localhost:${PORT}/sender.html`);
});
