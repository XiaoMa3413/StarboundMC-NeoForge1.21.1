"""Assemble review evidence from native Blockbench captures and a completed client run."""
import hashlib
import html
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GALLERY = ROOT.parent / '概念图/2026-10-27_敌人设计与建模'
NAMES = {
    'scrap_skitter': ('废铁掠行蛛', '前期四足近战：抬前足预警后扑击。'),
    'coil_sentry': ('线圈巡逻机', '前中期悬浮远程：双线圈蓄能后发射可躲避弹体。'),
    'breach_guard': ('重装破拆卫', '中期精英：左盾右钻，正面防御，恢复时露出空档。'),
    'quarry_warden': ('矿站监工 MK-I', '小Boss：夹钳横扫、冲击锤砸地，范围攻击不破坏方块。'),
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run():
    source = ROOT / 'run-enemy-smoke/screenshots/enemies-v01'
    assert (source / 'complete.txt').is_file(), 'Client smoke is not complete'
    shots = sorted(source.glob('*.png'))
    assert len(shots) == 17, 'Expected 13 pose scenes and four world views'
    game = GALLERY / 'game'
    game.mkdir(exist_ok=True)
    for path in source.iterdir():
        if path.suffix in ('.png', '.txt'):
            shutil.copy2(path, game / path.name)
    concepts = json.loads((ROOT / 'dev-assets/enemies/concepts/manifest.json').read_text(encoding='utf-8'))
    records = []
    body = ['<h1>机械敌人 · 概念与模型审查</h1>',
            '<p>2026-10-27 · 四种敌人仅通过刷怪蛋生成，未添加自然刷新。</p>',
            '<p>概念图由内置 imagegen 生成。模型视图来自真实 Blockbench 原生工程保存、重开后的截图；'
            '游戏视图来自真实开发客户端。对照图统一高度并保留比例，不自动宣称95%匹配或替用户确认美术通过。</p>',
            '<p><a href="../../模组位置/dev-assets/enemies/design.md">设计说明</a> · '
            '<a href="../../模组位置/dev-assets/enemies/concept-prompts.json">完整概念提示词</a> · '
            '<a href="../../模组位置/process.md">可回溯 process</a></p>']
    for concept in concepts:
        ident = concept['id']
        name, description = NAMES[ident]
        shutil.copy2(ROOT / concept['path'], GALLERY / (ident + '-concept.png'))
        body += [f'<section><h2>{name} <small>{ident}</small></h2><p>{description}</p>',
                 f'<p><a href="../../模组位置/dev-assets/enemies/models/{ident}.bbmodel">打开原生 bbmodel 工程</a> · '
                 f'<a href="{ident}/receipt.json">原生截图回执</a></p>',
                 f'<a href="{ident}/concept.png"><img class="wide" src="{ident}/concept.png" alt="{name}概念与三视图"></a>',
                 '<h3>实际 Blockbench 三视图及透视图</h3><div class="grid">']
        for view, title in [('front', '正面'), ('side', '左侧'), ('back', '背面'), ('perspective', '透视')]:
            body += [f'<figure><a href="{ident}/{view}.png"><img src="{ident}/{view}.png" alt="{title}"></a>'
                     f'<figcaption>{title} · <a href="{ident}/{view}-editor.png">完整建模界面截图</a></figcaption></figure>']
        body += ['</div><h3>概念与实际几何对照</h3><div class="grid">']
        for view in ('front', 'side', 'back'):
            body += [f'<a href="{ident}/comparison-{view}.png"><img src="{ident}/comparison-{view}.png" alt="{view}对照"></a>']
        body += ['</div><h3>游戏中：刷怪蛋、待机、移动与预警</h3><div class="grid">']
        for shot in shots:
            if shot.name.startswith(ident + '-'):
                label = html.escape(shot.stem)
                body += [f'<figure><a href="game/{shot.name}"><img src="game/{shot.name}" alt="{label}"></a><figcaption>{label}</figcaption></figure>']
        body += ['</div></section>']
        folder = GALLERY / ident
        artifacts = sorted(folder.glob('*.png')) + [ROOT / 'dev-assets/enemies/models' / (ident + '.bbmodel')]
        records.append({'id': ident, 'name': name, 'art_review': 'pending_user_review',
                        'artifacts': [{'path': str(p.relative_to(ROOT.parent)), 'sha256': sha(p)} for p in artifacts]})
    body += ['<p>预警截图在真实AI进入预警阶段后暂停AI，用于检查骨骼姿态。伤害、视线与攻击时序另有服务端行为测试。'
             '多人、长时间战斗及最终造型评价仍需人工试玩。</p>']
    page = '<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width">' \
           '<title>机械敌人概念与模型审查</title><style>body{max-width:1500px;margin:32px auto;padding:0 24px;' \
           'background:#17212c;color:#dce9ee;font:16px/1.7 system-ui}a{color:#91d0de}small{font-size:14px;color:#a3b5bf}' \
           'section{margin:48px 0;border-top:1px solid #43525d}img{width:100%;height:auto;display:block}' \
           '.wide{max-height:680px;object-fit:contain}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:18px}' \
           'figure{margin:0}figcaption{font-size:14px;color:#a3b5bf}</style><body>' + '\n'.join(body) + '</body></html>'
    (GALLERY / 'index.html').write_text(page, encoding='utf-8')
    receipt = {'gallery': str(GALLERY), 'art_acceptance': 'pending_user_review', 'models': records,
               'client_screenshots': [{'path': 'game/' + p.name, 'sha256': sha(p)} for p in shots],
               'client_complete': (source / 'complete.txt').read_text(encoding='utf-8')}
    (ROOT / 'dev-assets/enemies/gallery-validation.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'gallery': str(GALLERY / 'index.html'), 'models': len(records), 'game_screenshots': len(shots)}, ensure_ascii=False))


if __name__ == '__main__':
    run()
