"""Exercise the Gradle release tasks against a local HTTP server, never CurseForge.

Run with the JDK appropriate to the checkout, e.g.:
python scripts/testCurseForgePublishing.py --java-home /path/to/jdk
"""

import argparse
import json
import os
import subprocess
import tempfile
import threading
from email import policy
from email.parser import BytesParser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    properties = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    minecraft = properties["minecraft_version"]
    loader = {"1.20.1": "Forge", "1.21.1": "NeoForge"}[minecraft]
    requests = []
    reply = {"status": 200, "body": {"id": 123456}}

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            body = self.rfile.read(int(self.headers["Content-Length"]))
            message = BytesParser(policy=policy.default).parsebytes(
                ("Content-Type: " + self.headers["Content-Type"] + "\r\nMIME-Version: 1.0\r\n\r\n").encode() + body)
            parts = {part.get_param("name", header="Content-Disposition"): part
                     for part in message.iter_parts()}
            requests.append({"path": self.path, "token": self.headers.get("X-Api-Token"),
                             "metadata": json.loads(parts["metadata"].get_payload(decode=True)),
                             "file": parts["file"].get_payload(decode=True),
                             "filename": parts["file"].get_filename()})
            self.send_response(reply["status"])
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(reply["body"] if isinstance(reply["body"], bytes) else json.dumps(reply["body"]).encode())

        def log_message(self, *unused):
            pass

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
    token = "local-test-token-never-a-real-credential"
    try:
        with tempfile.TemporaryDirectory(prefix="fmu-curseforge-test-") as directory:
            fixture = Path(directory)
            (fixture / "settings.gradle").write_text("rootProject.name = 'release-fixture'\n")
            (fixture / "CHANGELOG.md").write_text("Release notes: Grüße 🚀\n", encoding="utf-8", newline="\n")
            (fixture / "pack-notes.md").write_text("Pack-specific release notes\n", encoding="utf-8", newline="\n")
            script = (root / "gradle/curseforge.gradle").as_posix().replace("'", "\\'")
            build = """
plugins { id 'java' }
version = '2.0'
ext.curseforgeModLoader = 'LOADER'
ext.fmuModulePublishing = [:]
if (providers.gradleProperty('fixtureModuleDefaults').orElse('false').get() == 'true') {
    fmuModulePublishing.privatePackJar = [
        projectUrl: 'https://www.curseforge.com/minecraft/mc-mods/module-default',
        projectId: 505, changelog: 'pack-notes.md', releaseType: 'beta',
        requiredDependencies: ['custom-npcs']
    ]
}
ext.fmuModuleJars = [tasks.register('officialPacksJar', Jar) {
    archiveBaseName = "[MC] Classic Packs"
    archiveVersion = '1.7'
}, tasks.register('privatePackJar', Jar) { archiveBaseName = 'Private Pack' }]
tasks.named('jar', Jar) { archiveBaseName = "[MC] Flan's Mod Ultimate" }
tasks.register('packsManagerJar', Jar) { archiveBaseName = 'Packs Manager' }
REOBF
apply from: 'SCRIPT'
tasks.matching { it.name.startsWith('publishCurseForge') && it.name != 'publishCurseForge' }.configureEach {
    apiBaseUrl.set('http://127.0.0.1:PORT')
}
"""
            reobf = """
['jar', 'officialPacksJar', 'privatePackJar'].each { name ->
    def artifact = tasks.named(name, Jar).get().archiveFile
    def reobf = tasks.register("reobf${name.capitalize()}") {
        dependsOn(name)
        doLast { artifact.get().asFile.append('REOBF') }
    }
    tasks.named(name) { finalizedBy(reobf) }
}
""" if loader == "Forge" else ""
            for key, value in {"LOADER": loader, "MC": minecraft, "REOBF": reobf,
                               "SCRIPT": script, "PORT": str(server.server_port)}.items():
                build = build.replace(key, value)
            (fixture / "build.gradle").write_text(build, encoding="utf-8")
            (fixture / "gradle.properties").write_text(
                f"minecraft_version={minecraft}\norg.gradle.configuration-cache=true\n", encoding="utf-8")
            mod = ["-PcurseforgeModProjectUrl=https://www.curseforge.com/minecraft/mc-mods/test-main",
                   "-PcurseforgeModProjectId=101"]
            pack = ["-PcurseforgeOfficialPacksProjectUrl=https://www.curseforge.com/minecraft/mc-mods/test-pack",
                    "-PcurseforgeOfficialPacksProjectId=202"]

            def run(*arguments, authenticated=False, succeeds=True):
                env = os.environ.copy()
                env["JAVA_HOME"] = args.java_home
                env.pop("CURSEFORGE_API_TOKEN", None)
                if authenticated:
                    env["CURSEFORGE_API_TOKEN"] = token
                command = [str(wrapper), "-p", str(fixture), "--console=plain", "--daemon", *arguments]
                result = subprocess.run(command, cwd=root, env=env, capture_output=True, text=True,
                                        encoding="utf-8", errors="replace", timeout=180)
                output = result.stdout + result.stderr
                assert (result.returncode == 0) == succeeds, output
                assert token not in output, "Credential appeared in Gradle output"
                return output

            output = run("publishCurseForge", "publishCurseForgePrivatePack")
            assert "publishCurseForgePrivatePack SKIPPED" in output and not requests, output
            assert not (fixture / "build/libs").exists(), "Disabled release built artifacts"
            output = run("publishCurseForgeMod", *mod, succeeds=False)
            assert "CURSEFORGE_API_TOKEN" in output and not requests, output
            assert not (fixture / "build/libs").exists(), "Missing credentials built a jar before validation"
            output = run("publishCurseForgeMod", *mod, "-PcurseforgeDryRun=true", "-Pminecraft_version=26.2", succeeds=False)
            assert "supports only" in output and not requests, output
            output = run("validateCurseForgeMod", mod[0], "-PcurseforgeDryRun=true", succeeds=False)
            assert "positive numeric" in output and not requests, output
            output = run("validateCurseForgeMod", *mod, "-PcurseforgeDryRun=true", "-PcurseforgeModProjectUrl=https://example.com/test", succeeds=False)
            assert "Use a CurseForge project URL" in output and not requests, output
            output = run("publishCurseForge", *mod, *pack, "-PcurseforgeDryRun=true", "-PcurseforgeOfficialPacksReleaseType=invalid", succeeds=False)
            assert "release, beta, or alpha" in output and not requests, output
            output = run("publishCurseForgeOfficialPacks", *mod, *pack, "-PcurseforgeDryRun=true")
            assert f'"{loader}"' in output, output
            assert not requests and not (fixture / "build/libs/Private Pack-2.0.jar").exists()
            output = run("publishCurseForgeOfficialPacks", *mod, *pack, "-PcurseforgeDryRun=true")
            assert "Reusing configuration cache" in output and not requests, output
            run("publishCurseForgeOfficialPacks", *mod, *pack, "-PcurseforgeOfficialPacksChangelog=pack-notes.md",
                "-PcurseforgeOfficialPacksReleaseType=beta", "-PcurseforgeOfficialPacksRequiredDependencies=custom-npcs,test-main",
                authenticated=True)
            assert len(requests) == 1, requests
            uploaded = requests[-1]
            assert uploaded["path"] == "/api/projects/202/upload-file", uploaded
            assert uploaded["token"] == token
            assert uploaded["filename"] == f"[{minecraft}] Classic Packs-1.7.jar", uploaded
            assert uploaded["file"].startswith(b"PK")
            assert uploaded["file"].endswith(b"REOBF") == (loader == "Forge")
            assert uploaded["metadata"] == {
                "changelog": "Pack-specific release notes\n", "changelogType": "markdown",
                "displayName": f"[{minecraft}] Classic Packs-1.7",
                "gameVersionNames": [minecraft, loader, "Client", "Server"], "releaseType": "beta",
                "relations": {"projects": [{"slug": "test-main", "projectID": "101", "type": "requiredDependency"},
                                           {"slug": "custom-npcs", "type": "requiredDependency"}]}}, uploaded
            run("publishCurseForge", *mod, *pack, authenticated=True)
            assert [request["path"] for request in requests[-2:]] == ["/api/projects/101/upload-file", "/api/projects/202/upload-file"]
            assert requests[-2]["metadata"]["changelog"] == "Release notes: Grüße 🚀\n"
            assert "relations" not in requests[-2]["metadata"]
            before = len(requests)
            run("publishCurseForgePrivatePack", "-PcurseforgePrivatePackProjectUrl=https://curseforge.com/minecraft/mc-mods/private-pack/",
                "-PcurseforgePrivatePackProjectId=303", authenticated=True)
            assert len(requests) == before + 1 and requests[-1]["path"] == "/api/projects/303/upload-file"
            assert requests[-1]["metadata"]["relations"] == {"projects": [{"slug": "flans-mod-ultimate-2", "type": "requiredDependency"}]}
            run("publishCurseForgePacksManager", "-PcurseforgePacksManagerProjectUrl=https://www.curseforge.com/minecraft/mc-mods/packs-manager",
                "-PcurseforgePacksManagerProjectId=404", authenticated=True)
            assert requests[-1]["path"] == "/api/projects/404/upload-file" and not requests[-1]["file"].endswith(b"REOBF")
            defaults = ["publishCurseForgePrivatePack", "-PfixtureModuleDefaults=true"]
            output = run(*defaults, "-PcurseforgeDryRun=true")
            assert '"releaseType": "beta"' in output and '"custom-npcs"' in output and 'ID 505' in output, output
            output = run(*defaults, "-PcurseforgeDryRun=true")
            assert "Reusing configuration cache" in output, output
            before = len(requests)
            output = run(*defaults, "-PcurseforgePrivatePackProjectUrl=")
            assert "publishCurseForgePrivatePack SKIPPED" in output and len(requests) == before, output
            run(*defaults, authenticated=True)
            assert requests[-1]["path"] == "/api/projects/505/upload-file"
            assert requests[-1]["metadata"]["changelog"] == "Pack-specific release notes\n"
            run(*defaults, "-PcurseforgePrivatePackProjectId=606", "-PcurseforgePrivatePackReleaseType=alpha", authenticated=True)
            assert requests[-1]["path"] == "/api/projects/606/upload-file" and requests[-1]["metadata"]["releaseType"] == "alpha"
            reply.update(status=403, body={"error": token})
            before = len(requests)
            output = run("publishCurseForgeMod", *mod, authenticated=True, succeeds=False)
            assert "HTTP 403" in output and len(requests) == before + 1, output
            reply.update(status=200, body={"message": "no file ID"})
            output = run("publishCurseForgeMod", *mod, authenticated=True, succeeds=False)
            assert "did not return a file ID" in output, output
            reply.update(body=token.encode())
            output = run("publishCurseForgeMod", *mod, authenticated=True, succeeds=False)
            assert "invalid upload response" in output, output
            print(f"CurseForge publishing checks passed on {minecraft}/{loader}; all requests stayed on localhost.")
    finally:
        server.shutdown()
        server.server_close()


if __name__ == "__main__":
    main()
