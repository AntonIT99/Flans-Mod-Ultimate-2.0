---
name: flans-master-merge-port
description: Merge master into a specified Flan's Mod Ultimate version branch, inspect the full change set, adapt Forge/NeoForge and Minecraft API differences, and document the port. Use for branch-wide master synchronization, not a single-feature port or a legacy-source comparison.
---

# Master Merge Port

Input: `TARGET_BRANCH`, the exact existing branch that should receive `master`.
`master` is always the source. Require a target branch before starting, and reject
`master` as the target. This skill does not create a new version branch.

Read the repository `AGENTS.md` and
[`flans-version-porting`](../flans-version-porting/SKILL.md) before editing. Use
that skill for the destination's API and validation details. Verify its maintained
target table against the actual target build files rather than assuming the table
is current.

## Establish the merge scope

1. Confirm the exact source and target refs, their HEAD commits and merge base.
   Check whether local `master` is behind its tracked remote; fetch and fast-forward
   it when permitted and possible. Do not silently merge a stale or diverged
   source. Do not force-update either branch.
2. Inspect `git status` in both checkouts. Use a separate target worktree when
   the current checkout is `master` or has unrelated changes. Do not switch or
   clean a dirty checkout, overwrite local work, or use a worktree with an
   unrelated merge in progress. Preserve unrelated changes throughout.
3. Run a destination baseline compile with its required Java version. Record any
   pre-existing failure so it is not mistaken for a regression.
4. Inventory `master`-only and target-only commits from the merge base. Review
   changed paths and meaningful diffs, grouped by subsystem. Trace behavior,
   registrations, callers, resources, tests, and packaging for changes that cross
   loader or Minecraft boundaries. Decide for each group whether it can merge
   directly, needs a version-specific adaptation, or must retain target behavior.
   A lack of Git conflict is not evidence that the code or data is portable.

Keep the commit inventory and port decisions available while merging. Summarize
them in the final result; do not create a report file for each merge run.

## Merge and adapt

1. In the target worktree, merge the selected `master` HEAD with `--no-commit`.
   Keep the merge parents intact so the result is a real merge. Resolve conflicts
   by intended behavior and destination architecture, not by uniformly choosing
   either side.
2. For every source change, carry its behavior into the target unless it is
   already present, intentionally superseded, or incompatible for a documented
   reason. Adapt Forge/NeoForge and Minecraft boundaries using the destination's
   established patterns: build and metadata, lifecycle and registries, events,
   networking, persistence, entities, client rendering, data formats, and resource
   paths. Keep branch-specific improvements and source-set separation.
3. Audit cleanly merged files and newly added source files as carefully as
   conflicts. Search for source-loader imports and removed APIs; follow each new
   call to its declaration, registration, serializer, and consumer. Inspect
   generated-data inputs and packaged jars rather than copying generated output.
   Keep main, bundled, and official packs as separate artifacts.
4. Resolve one subsystem at a time and run focused checks where useful. Inspect
   the full merge result for omitted changes, duplicate behavior, unregistered
   components, and client-only code reachable from a dedicated server.

## Document version differences

Maintain the single cumulative document
[`docs/minecraft-version-code-differences.md`](../../../docs/minecraft-version-code-differences.md)
in the target branch. Create it at that path if absent. Never create a separate
report for a merge run. Organize it by source and target Minecraft versions and
loaders, then classify entries by class or precise code/resource location. Update
an existing entry when a later merge changes the same seam; do not append a
duplicate entry or a new run-specific section.

Read the existing version section before resolving the merge. Treat it as a map
of known API seams, then verify each entry against the current source and target
code; remove or revise entries that the new merge makes obsolete.

For each difference caused by a Forge, NeoForge, or Minecraft API change, record
the source behavior/API, the destination adaptation, why it is necessary, and
any observable effect. Identify the affected class and path; use a path and
resource key for non-code changes. Distinguish required API adaptations from
optional refactors and record an intentional target-only implementation where
it explains a lasting version difference. Keep ordinary direct merges and
routine conflict history out of this document. Add a concise code comment only
where the API reason would be difficult to recover from the code and document.

Put source and target commits, merge base, conflict decisions, validation, and
remaining risks in the final task result and merge commit rather than in a new
report file.

Check the sibling wiki for user-facing behavior or format changes and update it
when the merge changes documented behavior. Do not put loader implementation
details in user-facing wiki pages unless they help users make a decision.

## Completion gate

Run focused tests first, then the destination's full `build` after changes to
loader setup, registries, networking, entities, resources, source sets, or
packaging. Run `packsManagerJar` and/or `officialPacksJar` when their inputs or
packaging change. Verify client and dedicated-server startup for affected loader,
rendering, mixin, or networking paths when runnable. Review the scoped diff,
check jar contents when packaging changed, and run `git diff --check`.

Commit the merge only after the required checks pass or an unavailable gate is
explicitly explained in the user-facing result. Stage explicit
paths in a mixed worktree. Report the merge commit, key adaptations, validation,
and remaining risks. Do not push or create a pull request unless requested.
