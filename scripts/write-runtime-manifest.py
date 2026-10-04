"""Record runtime JAR entry hashes so downloads can be matched to tested binaries."""
import hashlib
import json
import os
from pathlib import Path
import zipfile


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def manifest_attributes(raw):
    # Unfold the standard JAR manifest continuation lines before comparison.
    lines = raw.decode("utf-8").replace("\r\n", "\n").replace("\n ", "").splitlines()
    attributes = dict(line.split(": ", 1) for line in lines if ": " in line)
    # Loom lists the same client-only files in filesystem iteration order.
    client_entries = "Fabric-Loom-Client-Only-Entries"
    if client_entries in attributes:
        attributes[client_entries] = sorted(attributes[client_entries].split(";"))
    return attributes


def main():
    jars = []
    for path in sorted(Path("build/libs").glob("*.jar")):
        if path.name.endswith("-sources.jar"):
            continue
        data = path.read_bytes()
        with zipfile.ZipFile(path) as jar:
            if "fabric.mod.json" not in jar.namelist():
                continue
            entries = {
                entry.filename: {"bytes": entry.file_size, "sha256": sha256(jar.read(entry))}
                for entry in sorted(jar.infolist(), key=lambda item: item.filename)
                if not entry.is_dir()
            }
            jars.append({"file": path.name, "bytes": len(data), "sha256": sha256(data),
                         "entries": entries,
                         "manifestAttributes": manifest_attributes(jar.read("META-INF/MANIFEST.MF"))})
        print("RUNTIME_JAR_SHA256", sha256(data), path.name)
    if len(jars) != 1:
        raise SystemExit(f"Expected one runtime JAR, found {len(jars)}")
    output = Path("build/runtime-manifest.json")
    output.write_text(json.dumps({"schema": "liymod.runtime-identity/1",
                                 "sourceCommit": os.environ.get("GITHUB_SHA"),
                                 "jars": jars}, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("Runtime identity:", output, output.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
