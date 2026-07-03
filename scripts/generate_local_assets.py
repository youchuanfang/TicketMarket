from pathlib import Path
from html import escape
import shutil


ROOT = Path(__file__).resolve().parents[1]
UPLOADS = ROOT / "uploads"
PUBLIC = ROOT / "frontend" / "public"


PALETTES = [
    ("#1d2530", "#e7374a", "#f6c85f", "#177e89"),
    ("#24243e", "#ff4f7b", "#65d6ce", "#f7f0d4"),
    ("#143642", "#d95d39", "#f5b700", "#6fffe9"),
    ("#2b2d42", "#ef476f", "#ffd166", "#06d6a0"),
    ("#202a44", "#ff8a00", "#a5ffd6", "#f7f7ff"),
    ("#27213c", "#e84855", "#b8f2e6", "#f9dc5c"),
]


BANNERS = [
    ("夏日城市舞台", "演出 电影 展览 一站式发现", "上海", "07-20"),
    ("周末剧场计划", "精选场次与舒适票档", "杭州", "08-02"),
    ("星河音乐现场", "灯光与旋律同步开场", "深圳", "08-18"),
    ("亲子幻想日", "全家共享的轻松观演", "南京", "08-30"),
    ("热血运动夜", "看台视角与现场欢呼", "上海", "09-05"),
    ("光影艺术季", "沉浸展览与城市漫游", "深圳", "09-18"),
]

PERFORMANCES = [
    ("星河回声巡回演唱会", "演唱会", "上海", "08-18"),
    ("城市剧场 夜航西窗", "话剧", "杭州", "08-21"),
    ("仲夏室内乐精选", "音乐会", "南京", "08-09"),
    ("次元夏日嘉年华", "二次元", "南京", "08-30"),
    ("海风音乐节双日通票", "音乐节", "深圳", "09-12"),
    ("未来城市互动展", "展览", "深圳", "07-25"),
    ("亲子幻想剧场", "亲子", "杭州", "08-02"),
    ("热血篮球挑战赛", "体育", "上海", "09-05"),
    ("光影沉浸艺术展", "艺术展", "深圳", "09-18"),
    ("周末脱口秀专场", "曲艺", "上海", "08-16"),
    ("古典芭蕾精选夜", "舞蹈", "杭州", "09-02"),
    ("国风民乐新声音乐会", "民乐", "南京", "09-09"),
]

MOVIES = [
    ("星港来信", "剧情", "118分钟", "07-18"),
    ("云端列车", "冒险", "126分钟", "07-26"),
    ("第七号观测站", "科幻", "132分钟", "08-01"),
    ("夏日猫咪事务所", "喜剧", "102分钟", "08-09"),
    ("深海信号", "悬疑", "124分钟", "08-16"),
    ("城市微光", "剧情", "116分钟", "08-23"),
]

DETAILS = [
    ("舞台声浪", "灯光、座席与入场动线完整呈现"),
    ("剧场幕间", "以层次化图文组织项目详情"),
    ("城市看台", "清晰展示分区与服务信息"),
    ("音乐长廊", "适合演出详情页横幅展示"),
    ("亲子梦境", "温暖轻快的家庭观演氛围"),
    ("运动现场", "动感线条和看台节奏"),
    ("光影空间", "沉浸展览视觉延展"),
    ("电影之夜", "影院场次与影像质感"),
]


def ensure_dirs():
    for path in [
        UPLOADS / "banners",
        UPLOADS / "posters" / "performance",
        UPLOADS / "posters" / "movie",
        UPLOADS / "detail",
        PUBLIC / "uploads",
        PUBLIC / "posters" / "performance",
        PUBLIC / "posters" / "movie",
    ]:
        path.mkdir(parents=True, exist_ok=True)


def svg_text(x, y, text, size, weight="700", color="#ffffff", anchor="start"):
    return (
        f'<text x="{x}" y="{y}" text-anchor="{anchor}" '
        f'font-family="Microsoft YaHei, PingFang SC, Arial" font-size="{size}" '
        f'font-weight="{weight}" fill="{color}">{escape(text)}</text>'
    )


def decorative(width, height, palette, seed):
    base, accent, gold, teal = palette
    circles = []
    for i in range(6):
        cx = (seed * 73 + i * 151) % width
        cy = (seed * 47 + i * 83) % height
        r = 28 + (i * 13) % 70
        color = [accent, gold, teal][i % 3]
        circles.append(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{color}" opacity="0.18"/>')
    beams = [
        f'<polygon points="{width/2},0 {width*0.08},{height} {width*0.24},{height}" fill="{gold}" opacity="0.13"/>',
        f'<polygon points="{width/2},0 {width*0.76},{height} {width*0.92},{height}" fill="{teal}" opacity="0.13"/>',
    ]
    skyline = []
    for i in range(12):
        x = i * width / 12
        h = 34 + ((i + seed) % 5) * 16
        skyline.append(f'<rect x="{x}" y="{height - h}" width="{width / 16}" height="{h}" fill="#ffffff" opacity="0.12"/>')
    return "\n".join(beams + circles + skyline)


def render_banner(item, index):
    title, subtitle, city, date = item
    palette = PALETTES[index % len(PALETTES)]
    base, accent, gold, teal = palette
    width, height = 1440, 420
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
<defs>
  <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
    <stop offset="0" stop-color="{base}"/>
    <stop offset="0.55" stop-color="{accent}"/>
    <stop offset="1" stop-color="{teal}"/>
  </linearGradient>
</defs>
<rect width="{width}" height="{height}" fill="url(#bg)"/>
{decorative(width, height, palette, index + 1)}
<rect x="86" y="72" width="620" height="230" rx="20" fill="#000000" opacity="0.18"/>
{svg_text(118, 148, title, 54)}
{svg_text(120, 204, subtitle, 24, "500", "#f7f0f2")}
{svg_text(120, 260, f"TicketMarket · {city} · {date}", 26, "700", gold)}
<path d="M1030 112 C1110 70 1248 78 1320 146 C1256 142 1198 170 1156 216 C1110 178 1062 150 1030 112Z" fill="#ffffff" opacity="0.20"/>
<circle cx="1130" cy="260" r="62" fill="{gold}" opacity="0.86"/>
<circle cx="1260" cy="238" r="92" fill="#ffffff" opacity="0.16"/>
</svg>
'''


def render_poster(item, index, movie=False):
    title, kind, city_or_len, date = item
    palette = PALETTES[(index + (2 if movie else 0)) % len(PALETTES)]
    base, accent, gold, teal = palette
    width, height = 600, 800
    meta = f"{kind} · {city_or_len} · {date}"
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
<defs>
  <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
    <stop offset="0" stop-color="{base}"/>
    <stop offset="0.62" stop-color="{accent}"/>
    <stop offset="1" stop-color="#111827"/>
  </linearGradient>
</defs>
<rect width="{width}" height="{height}" fill="url(#bg)"/>
{decorative(width, height, palette, index + 11)}
<rect x="44" y="54" width="512" height="692" rx="28" fill="#000" opacity="0.17"/>
<circle cx="300" cy="284" r="128" fill="{gold}" opacity="0.18"/>
<path d="M118 515 C205 458 309 455 388 515 C440 554 477 612 506 681 L94 681 C118 616 82 560 118 515Z" fill="#ffffff" opacity="0.16"/>
<path d="M98 184 L502 126 L470 174 L132 226Z" fill="#ffffff" opacity="0.20"/>
{svg_text(76, 126, "TicketMarket", 24, "800", gold)}
{svg_text(76, 610, title, 40)}
{svg_text(78, 662, meta, 22, "500", "#f4f6fb")}
<rect x="76" y="696" width="186" height="38" rx="19" fill="{gold}" opacity="0.94"/>
{svg_text(169, 722, "本地原创素材", 18, "800", base, "middle")}
</svg>
'''


def render_detail(item, index):
    title, subtitle = item
    palette = PALETTES[(index + 3) % len(PALETTES)]
    base, accent, gold, teal = palette
    width, height = 1200, 520
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
<defs>
  <linearGradient id="bg" x1="0" x2="1">
    <stop offset="0" stop-color="{base}"/>
    <stop offset="1" stop-color="{accent}"/>
  </linearGradient>
</defs>
<rect width="{width}" height="{height}" fill="url(#bg)"/>
{decorative(width, height, palette, index + 31)}
<rect x="70" y="80" width="640" height="280" rx="24" fill="#000" opacity="0.18"/>
{svg_text(108, 168, title, 56)}
{svg_text(110, 230, subtitle, 25, "500", "#f7f0f2")}
{svg_text(110, 304, "TicketMarket 原创视觉资产", 24, "800", gold)}
<rect x="790" y="118" width="280" height="260" rx="30" fill="#fff" opacity="0.15"/>
<circle cx="930" cy="248" r="92" fill="{gold}" opacity="0.52"/>
<path d="M820 340 C900 300 980 300 1050 342" stroke="#fff" stroke-width="12" opacity="0.35" fill="none"/>
</svg>
'''


def write_asset(relative, content):
    target = UPLOADS / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")
    public_target = PUBLIC / "uploads" / relative
    public_target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(target, public_target)
    return target


def main():
    ensure_dirs()
    written = []
    for index, item in enumerate(BANNERS, start=1):
        written.append(write_asset(Path("banners") / f"banner-{index:02d}.svg", render_banner(item, index)))
    for index, item in enumerate(PERFORMANCES, start=1):
        name = f"poster-{100 + index}.svg"
        source = write_asset(Path("posters") / "performance" / name, render_poster(item, index))
        shutil.copyfile(source, PUBLIC / "posters" / "performance" / name)
    for index, item in enumerate(MOVIES, start=1):
        name = f"movie-{200 + index}.svg"
        source = write_asset(Path("posters") / "movie" / name, render_poster(item, index, movie=True))
        shutil.copyfile(source, PUBLIC / "posters" / "movie" / name)
    for index, item in enumerate(DETAILS, start=1):
        written.append(write_asset(Path("detail") / f"detail-{index:02d}.svg", render_detail(item, index)))
    print("generated banners=6 performance_posters=12 movie_posters=6 detail_images=8")


if __name__ == "__main__":
    main()
