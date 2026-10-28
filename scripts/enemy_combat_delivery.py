"""Collect real combat captures and verify the tested production/installed artifact."""
import argparse
import hashlib
import html
import json
import re
import shutil
import xml.etree.ElementTree as ET
import zipfile
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GALLERY = ROOT.parent / '概念图/2026-10-28_敌人战斗优化'
NAMES = {
    'scrap_skitter': ('废铁掠行蛛', '追击移动目标，蓄势后锁向扑击，收招后侧向重新站位。'),
    'coil_sentry': ('线圈巡逻机', '绕障寻找射线，贴身后撤并保持炮身朝向；侧移和有限提前瞄准，双发间隔0.6秒。'),
    'breach_guard': ('重装破拆卫', '贴身盾击推开目标，再预警钻刺；钻刺推进，出手与恢复时暴露正面。'),
    'quarry_warden': ('矿站监工 MK-I', '按距离选择横扫、砸地和冲撞；冲撞接砸地，半血可横扫接砸地，连招最多两段。'),
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(installed=None, backup=None):
    source = ROOT / 'run-enemy-combat/screenshots/enemy-combat'
    assert (source / 'complete.txt').is_file(), 'Client suite incomplete'
    shots = sorted(source.glob('*.png'))
    assert len(shots) == 33, 'Expected 22 pose scenes and 11 world views'
    active = sorted(source.glob('*-active.txt'))
    records = []
    for path in active:
        value = path.read_text(encoding='utf-8')
        phase = int(re.search(r'phase=(\d+)', value)[1])
        action = int(re.search(r'action=(\d+)', value)[1])
        elapsed = float(re.search(r'elapsed=([\d.]+)', value)[1])
        assert phase == 2 and 3 <= elapsed <= 7, value
        records.append({'scene': path.stem, 'phase': phase, 'action': action, 'elapsed_ticks': elapsed})
    assert sorted(r['action'] for r in records) == list(range(7)), 'Missing live attack capture'
    totals = {key: 0 for key in ('tests', 'failures', 'errors', 'skipped')}
    for path in (ROOT / 'build/test-results/test').glob('TEST-*.xml'):
        node = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(node.attrib.get(key, 0))
    assert totals['tests'] > 0 and totals['failures'] == totals['errors'] == 0, totals
    log_paths = ['logs/enemy-combat-gametests-final.log', 'logs/enemy-combat-client.log', 'logs/enemy-combat-release.log']
    logs = [(ROOT / p).read_text(encoding='utf-8', errors='replace') for p in log_paths]
    assert 'All 21 required tests passed' in logs[0]
    assert all('BUILD SUCCESSFUL' in value for value in logs)
    jar = ROOT / 'build/libs/starboundmc-0.1-alpha.jar'
    exports = json.loads((ROOT / 'dev-assets/enemies/export-validation.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(jar) as package:
        names = package.namelist()
        assert not any(token in name for name in names for token in ('EnemyGameTests', 'EnemyCombatGameTests', 'EnemyRenderSmoke', 'enemy_test_empty'))
        for name in ('MachineEnemy', 'EnemyCombatMovement', 'EnemyFlightControl'):
            assert 'com/starboundmc/enemy/' + name + '.class' in names
        for row in exports:
            ident = row['id']
            assert sha(ROOT / f'dev-assets/enemies/models/{ident}.bbmodel') == row['source_sha256']
            for name, key in [(f'enemies/{ident}.json', 'runtime_sha256'), (f'textures/entity/enemies/{ident}.png', 'texture_sha256')]:
                assert hashlib.sha256(package.read('assets/starboundmc/' + name)).hexdigest() == row[key]
    GALLERY.mkdir(parents=True, exist_ok=True)
    game = GALLERY / 'game'
    game.mkdir(exist_ok=True)
    for path in source.iterdir():
        if path.suffix in ('.png', '.txt'):
            shutil.copy2(path, game / path.name)
    body = ['<h1>四种机械敌人 · 战斗优化</h1><p>2026-10-28 · 仍仅使用刷怪蛋生成。</p>',
            '<p>攻击预警前半段跟踪，末段锁定，保留躲避与恢复空档。下列图片来自真实客户端：'
            'active为未暂停AI的实际攻击，windup为进入预警后暂停的姿态检查。伤害与绕障由服务端真实世界tick测试验证。</p>',
            '<p><a href="../../模组位置/process.md">过程记录</a> · <a href="../../模组位置/docs/reference/enemies.md">当前战斗规则</a></p>']
    for ident, (name, description) in NAMES.items():
        body += [f'<section><h2>{name}</h2><p>{description}</p><div class="grid">']
        for path in shots:
            if path.name.startswith(ident + '-') and '-active' in path.stem:
                label = html.escape(path.stem)
                body += [f'<figure><a href="game/{path.name}"><img src="game/{path.name}" alt="{label}"></a><figcaption>{label}</figcaption></figure>']
        body += ['</div><details><summary>待机、移动和预警截图</summary><div class="grid">']
        for path in shots:
            if path.name.startswith(ident + '-') and '-active' not in path.stem:
                body += [f'<figure><a href="game/{path.name}"><img src="game/{path.name}" alt="{path.stem}"></a><figcaption>{path.stem}</figcaption></figure>']
        body += ['</div></details></section>']
    page = '<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width">' \
           '<title>敌人战斗优化</title><style>body{max-width:1500px;margin:32px auto;padding:0 24px;background:#17212c;color:#dce9ee;font:16px/1.7 system-ui}' \
           'a{color:#91d0de}section{margin:40px 0;border-top:1px solid #43525d}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(360px,1fr));gap:18px}' \
           'img{display:block;width:100%;height:auto}figure{margin:0}figcaption{font-size:13px;color:#a3b5bf}summary{cursor:pointer;padding:18px 0}</style><body>' \
           + '\n'.join(body) + '</body></html>'
    (GALLERY / 'index.html').write_text(page, encoding='utf-8')
    receipt = {'created_at': datetime.now(timezone(timedelta(hours=8))).isoformat(),
               'branch': 'feat/enemy-combat-ai', 'baseline_commit': '1792b3d',
               'scope': 'Combat AI and attack animation; spawn eggs only', 'unit_tests': totals,
               'server_game_tests_passed': 21, 'live_attack_captures': records,
               'client_pose_scenes': 22, 'client_world_views': 11,
               'native_geometry_and_textures_unchanged': True, 'development_fixtures_excluded': True,
               'jar': {'path': str(jar.relative_to(ROOT)), 'sha256': sha(jar), 'bytes': jar.stat().st_size},
               'gallery': str(GALLERY / 'index.html'),
               'screenshots': [{'path': 'game/' + p.name, 'sha256': sha(p)} for p in shots],
               'logs': [{'path': p, 'sha256': sha(ROOT / p)} for p in log_paths],
               'remaining_manual_review': ['combat feel and balance', 'two real networked clients', 'large groups and long sessions']}
    if installed:
        assert sha(installed) == sha(jar), 'PCL JAR differs from build'
        receipt['installed'] = {'path': str(installed), 'sha256': sha(installed), 'matches_build': True}
    if backup:
        receipt['previous_jar_backup'] = {'path': str(backup), 'sha256': sha(backup)}
    (ROOT / 'dev-assets/enemies/combat-validation.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'jar_sha256': sha(jar), 'server_tests': 21, 'unit_tests': totals, 'screenshots': len(shots), 'installed': bool(installed)}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--installed', type=Path)
    parser.add_argument('--backup', type=Path)
    args = parser.parse_args()
    run(args.installed, args.backup)
