"""Verify the production JAR, evidence and installed copy; write a reproducible receipt."""
import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET
import zipfile
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify(installed=None, backup=None):
    jar = ROOT / 'build/libs/starboundmc-0.1-alpha.jar'
    models = json.loads((ROOT / 'dev-assets/enemies/export-validation.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(jar) as package:
        names = package.namelist()
        forbidden = ('EnemyRenderSmoke', 'EnemyGameTests', 'enemy_test_empty', 'dev-assets/')
        assert not any(token in name for name in names for token in forbidden), 'Development fixture in production JAR'
        for model in models:
            ident = model['id']
            required = [f'assets/starboundmc/enemies/{ident}.json',
                        f'assets/starboundmc/textures/entity/enemies/{ident}.png',
                        f'assets/starboundmc/models/item/{ident}_spawn_egg.json',
                        f'data/starboundmc/loot_table/entities/{ident}.json']
            for name in required:
                assert package.read(name) == (ROOT / 'src/main/resources' / name).read_bytes(), name
        for name in ('MachineEnemy', 'ModEnemies', 'EnemyProfile', 'EnemyBolt'):
            assert f'com/starboundmc/enemy/{name}.class' in names
        assert 'com/starboundmc/client/enemy/EnemyModel.class' in names
        assert 'assets/starboundmc/textures/entity/enemies/plasma_bolt.png' in names
    totals = {key: 0 for key in ('tests', 'failures', 'errors', 'skipped')}
    for path in (ROOT / 'build/test-results/test').glob('TEST-*.xml'):
        node = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(node.attrib.get(key, 0))
    assert totals['tests'] > 0 and totals['failures'] == totals['errors'] == 0, totals
    log_paths = ['logs/enemy-gametests-final.log', 'logs/enemy-client-smoke-release.log', 'logs/enemy-release-build.log']
    logs = [(ROOT / path).read_text(encoding='utf-8', errors='replace') for path in log_paths]
    assert re.search(r'All 9 required tests passed', logs[0])
    assert all('BUILD SUCCESSFUL' in log for log in logs)
    gallery = json.loads((ROOT / 'dev-assets/enemies/gallery-validation.json').read_text(encoding='utf-8'))
    assert len(gallery['client_screenshots']) == 17
    gallery_root = Path(gallery['gallery'])
    for model in gallery['models']:
        for artifact in model['artifacts']:
            assert sha(ROOT.parent / artifact['path']) == artifact['sha256'], artifact['path']
    for screenshot in gallery['client_screenshots']:
        assert sha(gallery_root / screenshot['path']) == screenshot['sha256']
    receipt = {'created_at': datetime.now(timezone(timedelta(hours=8))).isoformat(),
               'branch': 'feat/mechanical-enemy-roster', 'baseline_commit': 'e0c7313',
               'scope': 'Four mechanical enemies; spawn eggs only; no natural spawn entries',
               'jar': {'path': str(jar.relative_to(ROOT)), 'sha256': sha(jar), 'bytes': jar.stat().st_size,
                       'production_assets_match_sources': True, 'development_fixtures_excluded': True},
               'models': models, 'unit_tests': totals, 'server_game_tests_passed': 9,
               'client_pose_scenes': 13, 'client_world_views': 4,
               'art_acceptance': 'pending_user_review', 'gallery': str(gallery_root / 'index.html'),
               'evidence_logs': [{'path': path, 'sha256': sha(ROOT / path)} for path in log_paths]}
    if installed:
        installed = Path(installed)
        assert sha(installed) == sha(jar), 'Installed JAR differs from production build'
        receipt['installed'] = {'path': str(installed), 'sha256': sha(installed), 'matches_build': True}
    if backup:
        backup = Path(backup)
        receipt['previous_jar_backup'] = {'path': str(backup), 'sha256': sha(backup)}
    (ROOT / 'dev-assets/enemies/delivery.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'sha256': sha(jar), 'unit_tests': totals, 'server_tests': 9, 'game_screenshots': 17,
                      'installed': bool(installed)}, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--installed', type=Path)
    parser.add_argument('--backup', type=Path)
    args = parser.parse_args()
    verify(args.installed, args.backup)
