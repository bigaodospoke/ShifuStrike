// Gera as texturas dos itens próprios (faca, C4, kit de desarme), os ícones brancos da tela de compra
// (tingidos na cor do time pelo cliente) e o ícone do mod, desenhados por código (sem dependências).
//   node tools/gerar_recursos.js
const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const ASSETS = path.join(__dirname, "..", "src", "main", "resources", "assets", "shifustrike");

// ---------------------------------------------------------------- PNG

const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, "ascii"), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}

/** Imagem RGBA: px(x, y) = [r, g, b, a] */
function png(w, h, px) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0;
    for (let x = 0; x < w; x++) {
      const [r, g, b, a = 255] = px(x, y);
      const o = y * (w * 4 + 1) + 1 + x * 4;
      raw[o] = r; raw[o + 1] = g; raw[o + 2] = b; raw[o + 3] = a;
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0);
  ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", ihdr),
    chunk("IDAT", zlib.deflateSync(raw)),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

function save(rel, buf) {
  const file = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, buf);
  console.log("+", path.relative(path.join(__dirname, ".."), file));
}

// ---------------------------------------------------------------- tela de desenho

/** Tela simples: formas preenchidas com uma cor; o que não foi pintado fica transparente. */
function canvas(w, h) {
  const px = Array.from({ length: h }, () => Array(w).fill(null));
  const set = (x, y, c) => {
    x = Math.round(x); y = Math.round(y);
    if (x >= 0 && y >= 0 && x < w && y < h) px[y][x] = c;
  };
  return {
    rect(x0, y0, x1, y1, c) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) set(x, y, c); return this; },
    circle(cx, cy, r, c, r0 = -1) {
      for (let y = Math.floor(cy - r); y <= cy + r; y++)
        for (let x = Math.floor(cx - r); x <= cx + r; x++) {
          const d = Math.hypot(x - cx, y - cy);
          if (d <= r && d > r0) set(x, y, c);
        }
      return this;
    },
    line(x0, y0, x1, y1, c, t = 0) {
      const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) * 2 + 1;
      for (let i = 0; i <= n; i++) {
        const x = x0 + ((x1 - x0) * i) / n, y = y0 + ((y1 - y0) * i) / n;
        for (let dy = -t; dy <= t; dy++) for (let dx = -t; dx <= t; dx++) set(x + dx, y + dy, c);
      }
      return this;
    },
    poly(points, c) {
      const ys = points.map((p) => p[1]);
      for (let y = Math.min(...ys); y <= Math.max(...ys); y++) {
        const xs = [];
        for (let i = 0; i < points.length; i++) {
          const [ax, ay] = points[i], [bx, by] = points[(i + 1) % points.length];
          if ((ay <= y && by > y) || (by <= y && ay > y)) xs.push(ax + ((y - ay) * (bx - ax)) / (by - ay));
        }
        xs.sort((a, b) => a - b);
        for (let k = 0; k + 1 < xs.length; k += 2) for (let x = Math.ceil(xs[k]); x <= Math.floor(xs[k + 1]); x++) set(x, y, c);
      }
      return this;
    },
    clear(x0, y0, x1, y1) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) if (y >= 0 && y < h && x >= 0 && x < w) px[y][x] = null; return this; },
    png() { return png(w, h, (x, y) => px[y][x] || [0, 0, 0, 0]); },
  };
}

const W = [255, 255, 255];

// ---------------------------------------------------------------- texturas de item (16x16)

const LAMINA = [196, 204, 212], LAMINA_ESC = [140, 148, 158], CABO = [40, 40, 44], CABO_CLARO = [70, 70, 76];
save("textures/item/faca.png", canvas(16, 16)
  .line(5, 10, 13, 2, LAMINA, 0).line(6, 10, 13, 3, LAMINA, 0).line(5, 9, 12, 2, LAMINA_ESC, 0)
  .line(2, 13, 5, 10, CABO, 0).line(3, 13, 6, 10, CABO_CLARO, 0).rect(4, 10, 6, 11, [90, 90, 96])
  .png());

const C4_CORPO = [150, 120, 70], C4_ESC = [110, 86, 48], FITA = [60, 60, 64], VISOR = [20, 30, 20], LED = [230, 40, 40];
save("textures/item/c4.png", canvas(16, 16)
  .rect(2, 5, 13, 12, C4_CORPO).rect(2, 11, 13, 12, C4_ESC).rect(5, 5, 6, 12, FITA).rect(10, 5, 11, 12, FITA)
  .rect(7, 6, 9, 8, VISOR).rect(8, 7, 8, 7, LED).line(3, 4, 6, 2, [200, 40, 40]).line(10, 4, 13, 2, [40, 90, 200])
  .png());

const ALICATE = [200, 60, 40], METAL = [170, 176, 184];
save("textures/item/kit_desarme.png", canvas(16, 16)
  .line(3, 13, 7, 7, ALICATE, 0).line(4, 13, 8, 7, ALICATE, 0).line(12, 13, 8, 7, ALICATE, 0).line(11, 13, 7, 7, ALICATE, 0)
  .line(7, 7, 6, 2, METAL, 0).line(8, 7, 9, 2, METAL, 0).rect(7, 6, 8, 7, [90, 90, 96])
  .png());

for (const [nome, textura, mao] of [["faca", "faca", true], ["c4", "c4", false], ["kit_desarme", "kit_desarme", false]]) {
  save(`models/item/${nome}.json`, Buffer.from(JSON.stringify({
    parent: mao ? "minecraft:item/handheld" : "minecraft:item/generated",
    textures: { layer0: `shifustrike:item/${textura}` },
  }, null, 2) + "\n"));
}

// ---------------------------------------------------------------- ícones da loja (64x64, brancos)

const colete = () => canvas(64, 64)
  .poly([[18, 12], [26, 12], [32, 18], [38, 12], [46, 12], [52, 22], [50, 54], [14, 54], [12, 22]], W)
  .clear(28, 12, 36, 16).rect(20, 30, 44, 32, W);
save("textures/gui/icone/colete.png", colete().png());
save("textures/gui/icone/colete_capacete.png", colete()
  .clear(0, 0, 63, 8)
  .circle(48, 14, 12, W).clear(36, 15, 63, 30).rect(36, 14, 60, 16, W)
  .png());
save("textures/gui/icone/zeus.png", canvas(64, 64)
  .poly([[36, 6], [18, 36], [30, 36], [24, 58], [46, 26], [34, 26], [42, 6]], W).png());
save("textures/gui/icone/flash.png", canvas(64, 64)
  .rect(26, 18, 38, 54, W).rect(28, 12, 36, 17, W).rect(37, 14, 44, 16, W)
  .line(16, 8, 22, 14, W, 1).line(48, 8, 42, 14, W, 1).line(32, 2, 32, 8, W, 1).png());
save("textures/gui/icone/fumaca.png", canvas(64, 64)
  .rect(24, 20, 40, 56, W).rect(27, 14, 37, 19, W).rect(38, 15, 46, 17, W)
  .circle(20, 10, 6, W).circle(30, 6, 5, W).circle(42, 8, 6, W).png());
save("textures/gui/icone/incendiaria.png", canvas(64, 64)
  .rect(24, 24, 40, 56, W).rect(28, 18, 36, 23, W)
  .poly([[32, 2], [40, 12], [36, 16], [28, 16], [24, 12]], W).png());
save("textures/gui/icone/molotov.png", canvas(64, 64)
  .poly([[26, 22], [38, 22], [44, 34], [44, 56], [20, 56], [20, 34]], W).rect(29, 12, 35, 21, W)
  .poly([[32, 0], [38, 8], [34, 12], [30, 12], [26, 8]], W).png());
save("textures/gui/icone/decoy.png", canvas(64, 64)
  .rect(26, 20, 38, 54, W).rect(28, 14, 36, 19, W)
  .circle(32, 37, 11, W, 8).circle(32, 37, 4, W).png());

// ---------------------------------------------------------------- ícone do mod (128x128)

const FUNDO = [21, 24, 27], OURO = [232, 182, 76], AZUL = [142, 197, 255];
const icone = canvas(128, 128).rect(0, 0, 127, 127, FUNDO);
icone.circle(64, 64, 44, OURO, 38).rect(60, 10, 67, 34, OURO).rect(60, 94, 67, 118, OURO)
  .rect(10, 60, 34, 67, OURO).rect(94, 60, 118, 67, OURO).circle(64, 64, 6, AZUL);
save("icon.png", icone.png());
