import hashlib
import json
import shutil
import struct
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = 'LIy-hub/LoliPickaxe-Fabric'
TAG = 'v2026.10.05'
targets = json.loads((ROOT / 'ci-targets.json').read_text())
release = ROOT / 'ci-release'
release.mkdir(exist_ok=True)

def gh(*args):
    return subprocess.check_output(['gh', *args], text=True)

existing = json.loads(gh('api', f'repos/{REPO}/releases/403130357'))
assert existing['draft']
initial_assets = {a['name']: a for a in existing['assets']}
rows = []
for index, target in enumerate(targets, 1):
    run = json.loads(gh('api', f'repos/{REPO}/actions/runs/{target["run_id"]}'))
    assert run['head_sha'] == target['source_commit']
    assert run['head_branch'] == target['branch']
    assert run['status'] == 'completed' and run['conclusion'] == 'success'
    if target['filename'] in initial_assets:
        asset = initial_assets[target['filename']]
        assert asset['state'] == 'uploaded' and asset['size'] == target['size']
        assert asset['digest'] == 'sha256:' + target['sha256']
        row = {k: v for k, v in target.items() if k != 'local_class_count'}
        row.update(packaging_origin='Verified local native build', ci_run_url=run['html_url'], ci_conclusion=run['conclusion'])
        rows.append(row)
        print(f'[{index}/24] retained verified local upload {target["filename"]}', flush=True)
        continue
    folder = ROOT / 'downloaded-ci' / (target['loader'].lower() + '-' + target['minecraft'])
    gh('run', 'download', str(target['run_id']), '-R', REPO, '--dir', str(folder))
    jars = [p for p in folder.rglob('*.jar') if not p.name.endswith('-sources.jar') and not p.name.endswith('-dev.jar')]
    assert len(jars) == 1, [str(p) for p in jars]
    source = jars[0]
    with zipfile.ZipFile(source) as z:
        assert z.testzip() is None
        assert len(z.namelist()) == len(set(z.namelist()))
        classes = [n for n in z.namelist() if n.endswith('.class')]
        class_versions = sorted({struct.unpack('>H', z.read(n)[6:8])[0] for n in classes})
        assert classes and max(class_versions) <= target['java'] + 44
        assert len(classes) == target['local_class_count']
        if target['loader'] == 'Fabric':
            metadata = json.loads(z.read('fabric.mod.json'))
            assert metadata['id'] == 'liymod' and metadata['version'] == target['mod_version']
            assert metadata['depends'] == target['dependencies']
            assert z.read(metadata['icon'])
        else:
            assert target['mod_version'] in z.read('META-INF/mods.toml').decode()
        for lang in ('en_us', 'zh_cn'):
            assert z.read(f'assets/liymod/lang/{lang}.json')
    dest = release / target['filename']
    shutil.copyfile(source, dest)
    row = {k: v for k, v in target.items() if k != 'local_class_count'}
    row.update(sha256=hashlib.sha256(dest.read_bytes()).hexdigest(), size=dest.stat().st_size,
               ci_run_url=run['html_url'], ci_conclusion=run['conclusion'],
               original_ci_filename=source.name, class_count=len(classes), class_file_versions=class_versions,
               packaging_origin='Unmodified exact-commit successful CI build')
    rows.append(row)
    print(f'[{index}/24] downloaded and validated {dest.name}', flush=True)

manifest = dict(repository=REPO, release_tag=TAG, batch_date='2026-10-05', fabric_targets=23,
                forge_targets=1, forge_network_protocol=3, runtime_files=rows,
                packaging='Verified local uploads retained; remaining runtime JARs copied unmodified from exact-commit successful CI runs. Per-file origins recorded.',
                client_startup_evidence='Local builds of the same source implementations were started in isolated native clients.',
                in_game_visual_acceptance=False, multiplayer_acceptance=False)
(release / 'verification-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
checksums = ''.join(f'{r["sha256"]}  {r["filename"]}\n' for r in rows)
checksums += f'{hashlib.sha256((release / "verification-manifest.json").read_bytes()).hexdigest()}  verification-manifest.json\n'
(release / 'SHA256SUMS.txt').write_text(checksums, encoding='utf-8')
files = sorted(release.iterdir())
for index, p in enumerate(files, 1):
    gh('release', 'upload', TAG, str(p), '-R', REPO)
    print(f'[{index}/{len(files)}] uploaded {p.name}', flush=True)
remote = json.loads(gh('api', f'repos/{REPO}/releases/403130357'))
assets = {a['name']: a for a in remote['assets']}
assert len(assets) == 26
for r in rows:
    assert assets[r['filename']]['state'] == 'uploaded'
    assert assets[r['filename']]['size'] == r['size']
    assert assets[r['filename']]['digest'] == 'sha256:' + r['sha256']
for p in files:
    assert assets[p.name]['state'] == 'uploaded'
    assert assets[p.name]['size'] == p.stat().st_size
    assert assets[p.name]['digest'] == 'sha256:' + hashlib.sha256(p.read_bytes()).hexdigest()
(ROOT / 'ci-publication-verification.json').write_text(json.dumps(dict(draft_release_id=remote['id'],
    asset_count=len(assets), all_sha256_verified=True, assets=remote['assets']), ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('All 26 uploaded asset sizes and SHA-256 values match verified CI bytes; draft ready.', flush=True)
