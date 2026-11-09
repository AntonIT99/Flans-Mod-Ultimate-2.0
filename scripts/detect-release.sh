#!/usr/bin/env bash
# Finds the modules of this checkout whose current version has not been released yet.
# A version counts as released when its tag exists or a GitHub release already holds its jar.
# Requires GITHUB_OUTPUT, RELEASE_NOTES_FILE and GITHUB_REPOSITORY, an authenticated gh, and fetched tags.
# See docs/github-releases.md.
set -euo pipefail

output="${GITHUB_OUTPUT:?}"
notes="${RELEASE_NOTES_FILE:?}"
repository="${GITHUB_REPOSITORY:?}"

property() { sed -n "s/^$1=//p" gradle.properties | tr -d '\r ' | head -n 1; }
field() { sed -n "s/.*$2: *'\([^']*\)'.*/\1/p" "$1" | tr -d '\r' | head -n 1; }

minecraft="$(property minecraft_version)"
assets="$(gh api --paginate "repos/$repository/releases" --jq '.[].assets[].name')"

is_released() {
    git rev-parse -q --verify "refs/tags/$1-$minecraft-$2" > /dev/null || grep -Fxq "$1-$minecraft-$2.jar" <<< "$assets"
}

require_changelog() {
    local heading
    heading="$(grep -m 1 . "$1" | tr -d '\r')"
    if [[ "$heading" != *" $2" ]]; then
        echo "::error file=$1::The changelog heading '$heading' does not end in version $2."
        exit 1
    fi
}

# Upload task suffix, as derived in gradle/curseforge.gradle.
upload_task() {
    local name="${1%Jar}"
    echo "publishCurseForge${name^}"
}

tags=()
tasks=()
jars=()
pack_jars=()

select_module() { # id, version, upload task
    tasks+=("$3")
    jars+=("build/libs/$1-$minecraft-$2.jar")
}

: > "$notes"

main_id="$(property mod_id)"
main_version="$(property mod_version)"
if ! is_released "$main_id" "$main_version"; then
    require_changelog CHANGELOG.md "$main_version"
    tags+=("$main_id-$minecraft-$main_version")
    select_module "$main_id" "$main_version" publishCurseForgeMod
    cat CHANGELOG.md >> "$notes"
    echo >> "$notes"
    echo "Main mod: $main_version"
fi
has_main="${#tags[@]}"

npc_descriptor=src/npcs/fmu-module.gradle
npc_id="$(field "$npc_descriptor" modId)"
npc_version="$(field "$npc_descriptor" archiveVersion)"
if ! is_released "$npc_id" "$npc_version"; then
    require_changelog src/npcs/CHANGELOG.md "$npc_version"
    tags+=("$npc_id-$minecraft-$npc_version")
    select_module "$npc_id" "$npc_version" "$(upload_task "$(field "$npc_descriptor" jarTask)")"
    cat src/npcs/CHANGELOG.md >> "$notes"
    echo >> "$notes"
    echo "NPC mod: $npc_version"
fi

for descriptor in src/*/fmu-module.gradle; do
    [[ "$descriptor" == "$npc_descriptor" ]] && continue
    [[ -n "$(field "$descriptor" projectUrl)" ]] || continue
    id="$(field "$descriptor" modId)"
    version="$(field "$descriptor" archiveVersion)"
    is_released "$id" "$version" && continue
    select_module "$id" "$version" "$(upload_task "$(field "$descriptor" jarTask)")"
    pack_jars+=("$id-$minecraft-$version.jar")
    # A release needs a tag, so a pack-only release is named after its first pack.
    [[ "${#tags[@]}" -gt 0 ]] || tags+=("$id-$minecraft-$version")
    echo "Content pack: $id $version"
done

if [[ "${#pack_jars[@]}" -gt 0 ]]; then
    echo "## Content packs" >> "$notes"
    echo >> "$notes"
    printf -- '- `%s`\n' "${pack_jars[@]}" >> "$notes"
fi

if [[ "${#tags[@]}" -eq 0 ]]; then
    echo "Nothing to release: every current version is already released."
    echo "release=false" >> "$output"
    exit 0
fi

{
    echo "release=true"
    echo "title=${tags[0]}"
    echo "latest=$([[ "$has_main" -gt 0 ]] && echo true || echo false)"
    echo "tags=${tags[*]}"
    echo "tasks=${tasks[*]}"
    echo "jars=${jars[*]}"
} >> "$output"
