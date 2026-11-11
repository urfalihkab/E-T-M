#!/usr/bin/env node
// EĞİTİM SEBLAZİZ - katalog üretici
// Tonguç Akademi'nin herkese açık YouTube sınıf kanallarındaki oynatma listelerini ve
// videolarını okur, derslere göre raflara ayırır ve catalog/ klasörüne yazar.
// Kullanım: node tools/build-catalog.js   (Node 18+, ek paket gerekmez)

const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'catalog');
const CLIENT = { clientName: 'WEB', clientVersion: '2.20251101.00.00', hl: 'tr', gl: 'TR' };

// Kademe -> sınıf -> kanal. TV uygulamasındaki düzenle aynı.
const LEVELS = [
  { id: 'ilkokul', title: 'İlkokul', grades: [
    { id: '1', title: '1. Sınıf', channel: 'UCyRSGDRdOroDN6vXtobLTfw', handle: '@tonguc1' },
    { id: '2', title: '2. Sınıf', channel: 'UCkI5pnECWXuniFCQpe6nWQw', handle: '@tonguc2' },
    { id: '3', title: '3. Sınıf', channel: 'UCWKFn2p0uodV8JCdsYtGLIw', handle: '@tonguc3' },
    { id: '4', title: '4. Sınıf', channel: 'UCK4MoKPUA5lJjiO18yc_YMA', handle: '@tonguc4' },
  ]},
  { id: 'ortaokul', title: 'Ortaokul', grades: [
    { id: '5', title: '5. Sınıf', channel: 'UCb-0JQ4tI03T5rHdFe2GVoA', handle: '@tonguc5' },
    { id: '6', title: '6. Sınıf', channel: 'UCntJ4oa66gOoonxnJ5MWduQ', handle: '@tonguc6' },
    { id: '7', title: '7. Sınıf', channel: 'UCI5Ir6-br-HM54InF7HUlzg', handle: '@tonguc7' },
    { id: '8', title: '8. Sınıf (LGS)', channel: 'UCPdN4Vx2DogwmjDJCkyVcXQ', handle: '@tonguc8' },
  ]},
  { id: 'lise', title: 'Lise', grades: [
    { id: '9', title: '9. Sınıf', channel: 'UCnMkrwpqeeUdULISjn-_NGA', handle: '@tonguc9' },
    { id: '10', title: '10. Sınıf', channel: 'UC-5ku9Z61IGy12z8xVeFA6g', handle: '@tonguc10' },
    { id: '11', title: '11. Sınıf', channel: 'UCiuFKj-SmFf8M4m1ybbftCw', handle: '@tonguc11' },
  ]},
  { id: 'diger', title: 'Diğer', grades: [
    { id: 'kampus', title: 'YKS (tonguç KAMPÜS)', channel: 'UCyKKD_T683_M2-cmnVwtFfg', handle: '@kampus' },
    { id: 'akademi', title: 'tonguç AKADEMİ', channel: 'UCm3vDH7Uvz_qwql5Qih4yGw', handle: '@tongucakademi' },
  ]},
];

// Raf sırası; ilk eşleşen kural kazanır.
const SHELVES = [
  // Başlıklar Türkçe kurala göre küçültülüp test edilir (İ -> i). Ders kuralları kamplardan önce gelir,
  // böylece "TYT Coğrafya Kampı" Coğrafya rafına, "5. Sınıf Yazılı Kampı" Yazılı Kampları rafına düşer.
  ['inkilap', 'İnkılap Tarihi 🇹🇷', /inkılap|inkilap|atatürk/],
  ['matematik', 'Matematik 📘', /matemat|geometri|aritmet/],
  ['edebiyat', 'Edebiyat 📜', /edebiyat/],
  ['turkce', 'Türkçe 📙', /türkçe|turkce|harf|hece|okuma|çizgi|kitabımı/],
  ['fizik', 'Fizik ⚛️', /fizik/],
  ['kimya', 'Kimya 🧪', /kimya/],
  ['biyoloji', 'Biyoloji 🧬', /biyoloji/],
  ['fen', 'Fen Bilimleri 🌎', /\bfen\b|fen bil|fen ve|deney/],
  ['hayat', 'Hayat Bilgisi 🎈', /hayat bil/],
  ['sosyal', 'Sosyal Bilgiler 📕', /sosyal/],
  ['tarih', 'Tarih 🏛️', /tarih/],
  ['cografya', 'Coğrafya 🗺️', /coğrafya|cografya/],
  ['felsefe', 'Felsefe 💭', /felsefe/],
  ['ingilizce', 'Yabancı Dil 🇬🇧', /ingilizce|english|yabancı dil|almanca/],
  ['din', 'Din Kültürü 🤲', /\bdin\b|din kült/],
  ['yazili', 'Yazılı Kampları ✍️', /yazılı|yazili/],
  ['tekrar', 'Tekrar ve Tatil Kampları 🔁', /tekrar|kamp|sömestir|somestir|tatil|hazırlık|hazirlik|bursluluk|bilsem|atak|yaz okulu/],
  ['rehberlik', 'Rehberlik ve Motivasyon 🎯', /rehberlik|motivasyon|taktik|verimli|pomodoro|program|günlüğü|ipuç/],
  ['diger', 'Diğer ✨', /./],
];
const categorize = (title) => SHELVES.find(([, , re]) => re.test(title.toLocaleLowerCase('tr-TR')))[0];
// Ekranda gösterim sırası: önce dersler, sonra kamplar ve rehberlik.
const LAST = ['yazili', 'tekrar', 'rehberlik', 'diger'];
const displayRank = (id) => (LAST.includes(id) ? 100 + LAST.indexOf(id) : SHELVES.findIndex((s) => s[0] === id));
const SKIP = /#?shorts|kutu açılımı|kamera arkası|tüyap|ürün inceleme/i;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function browse(body, tries = 3) {
  for (let i = 0; i < tries; i++) {
    try {
      const r = await fetch('https://www.youtube.com/youtubei/v1/browse?prettyPrint=false', {
        method: 'POST',
        headers: { 'content-type': 'application/json', 'accept-language': 'tr-TR,tr;q=0.9' },
        body: JSON.stringify({ context: { client: CLIENT }, ...body }),
      });
      if (r.ok) return await r.json();
      throw new Error('HTTP ' + r.status);
    } catch (e) {
      if (i === tries - 1) throw e;
      await sleep(1500 * (i + 1));
    }
  }
}

function walk(o, p, f) {
  if (!o || typeof o !== 'object') return;
  f(o, p);
  for (const k in o) walk(o[k], p + '.' + k, f);
}

function nextToken(d) {
  let tok = null;
  walk(d, '', (o, p) => {
    if (!tok && o.continuationItemRenderer && !p.includes('engagementPanel')) {
      tok = o.continuationItemRenderer.continuationEndpoint?.continuationCommand?.token || null;
    }
  });
  return tok;
}

async function channelPlaylists(channelId) {
  const all = new Map();
  const grab = (d) => walk(d, '', (o) => {
    const l = o.lockupViewModel;
    if (l && l.contentType === 'LOCKUP_CONTENT_TYPE_PLAYLIST' && !all.has(l.contentId)) {
      all.set(l.contentId, { id: l.contentId, title: l.metadata?.lockupMetadataViewModel?.title?.content || '' });
    }
  });
  let d = await browse({ browseId: channelId, params: 'EglwbGF5bGlzdHPyBgQKAkIA' });
  grab(d);
  for (let tok = nextToken(d), n = 0; tok && n < 40; n++) {
    d = await browse({ continuation: tok });
    grab(d);
    tok = nextToken(d);
  }
  return [...all.values()];
}

async function playlistVideos(listId) {
  const vids = new Map();
  const grab = (d) => walk(d, '', (o) => {
    const v = o.playlistVideoRenderer;
    if (v && v.videoId && v.isPlayable !== false && !vids.has(v.videoId)) {
      vids.set(v.videoId, {
        id: v.videoId,
        title: v.title?.runs?.[0]?.text || v.title?.simpleText || '',
        duration: v.lengthText?.simpleText || '',
      });
    }
    // Yeni YouTube biçimi
    const l = o.lockupViewModel;
    if (l && l.contentType === 'LOCKUP_CONTENT_TYPE_VIDEO' && l.contentId && !vids.has(l.contentId)) {
      let duration = '';
      walk(l.contentImage, '', (x) => {
        const t = x.thumbnailBadgeViewModel?.text;
        if (!duration && t && /^\d+(:\d+)+$/.test(t)) duration = t;
      });
      vids.set(l.contentId, {
        id: l.contentId,
        title: l.metadata?.lockupMetadataViewModel?.title?.content || '',
        duration,
      });
    }
  });
  let d = await browse({ browseId: 'VL' + listId });
  grab(d);
  for (let tok = nextToken(d), n = 0; tok && n < 20; n++) {
    d = await browse({ continuation: tok });
    grab(d);
    tok = nextToken(d);
  }
  return [...vids.values()].filter((v) => v.title && !/^\[(Özel|Silinmiş|Private|Deleted)/i.test(v.title));
}

function cleanTitle(t) {
  return t.replace(/\s+/g, ' ').trim();
}

async function buildGrade(grade) {
  const lists = (await channelPlaylists(grade.channel)).filter((p) => !SKIP.test(p.title));
  const shelves = new Map(SHELVES.map(([id, title]) => [id, { id, title, playlists: [] }]));
  for (const pl of lists) {
    let videos = [];
    try { videos = await playlistVideos(pl.id); } catch (e) { console.warn('  !', pl.id, e.message); }
    if (!videos.length) continue;
    shelves.get(categorize(pl.title)).playlists.push({ id: pl.id, title: cleanTitle(pl.title), videos });
    await sleep(120);
  }
  const out = [...shelves.values()].filter((s) => s.playlists.length).sort((a, b) => displayRank(a.id) - displayRank(b.id));
  const count = out.reduce((a, s) => a + s.playlists.reduce((b, p) => b + p.videos.length, 0), 0);
  console.log(`  ${grade.title}: ${lists.length} liste, ${count} video`);
  return { id: grade.id, title: grade.title, handle: grade.handle, shelves: out };
}

// node tools/build-catalog.js --regroup : indirmeden, mevcut catalog/ dosyalarını yeni raf kurallarıyla yeniden dizer.
function regroup() {
  const idxFile = path.join(OUT, 'index.json');
  const index = JSON.parse(fs.readFileSync(idxFile, 'utf8'));
  for (const lv of index.levels) for (const g of lv.grades) {
    const f = path.join(OUT, `grade-${g.id}.json`);
    const data = JSON.parse(fs.readFileSync(f, 'utf8'));
    const all = data.shelves.flatMap((s) => s.playlists);
    const shelves = new Map(SHELVES.map(([id, title]) => [id, { id, title, playlists: [] }]));
    for (const p of all) shelves.get(categorize(p.title)).playlists.push(p);
    data.shelves = [...shelves.values()].filter((s) => s.playlists.length).sort((a, b) => displayRank(a.id) - displayRank(b.id));
    fs.writeFileSync(f, JSON.stringify(data));
    g.cover = data.shelves[0]?.playlists[0]?.videos[0]?.id || '';
    g.playlists = all.length;
    console.log(g.id.padEnd(8), data.shelves.map((s) => `${s.id}:${s.playlists.length}`).join(' '));
  }
  fs.writeFileSync(idxFile, JSON.stringify(index, null, 1));
}

if (process.argv.includes('--regroup')) { regroup(); return; }

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const index = { version: 1, updatedAt: new Date().toISOString(), provider: {
    id: 'tonguc', name: 'tonguç AKADEMİ', slogan: 'tonguç anladıysa herkes anlar!',
    about: "Türkiye'nin alanında en iyi hocalarıyla hazırladığı konu anlatımları; öğrencilerin en hızlı öğreneceği taktikler ve gülerek, sıkılmadan öğrenmelerini sağlayacak bir tarzda anlatım.",
    website: 'https://www.tongucakademi.com', social: 'tongucakademi',
  }, levels: [] };
  for (const level of LEVELS) {
    const lv = { id: level.id, title: level.title, grades: [] };
    for (const g of level.grades) {
      console.log('>', g.title);
      const data = await buildGrade(g);
      fs.writeFileSync(path.join(OUT, `grade-${g.id}.json`), JSON.stringify(data));
      const first = data.shelves[0]?.playlists[0]?.videos[0]?.id || '';
      lv.grades.push({ id: g.id, title: g.title, cover: first,
        playlists: data.shelves.reduce((a, s) => a + s.playlists.length, 0) });
    }
    index.levels.push(lv);
  }
  fs.writeFileSync(path.join(OUT, 'index.json'), JSON.stringify(index, null, 1));
  console.log('Bitti ->', OUT);
})().catch((e) => { console.error(e); process.exit(1); });
