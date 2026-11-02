# Extending a content pack team

A player-class definition can join teams supplied by another installed pack:

```text
AddToTeam ww2:germanTeam
```

Repeat `AddToTeam` to join several teams. The target is the logical pack ID
(the folder below `flans_content`, such as `ww2`), a colon, and the team's
original `ShortName`. Matching ignores case. For legacy directory/ZIP packs,
use the pack's full display name, including its extension when present,
URL-encoded as UTF-8 (spaces become `+`). References must be pack-qualified;
registry collision aliases such as `_2` are not used.

References resolve after all packs load. Missing or ambiguous targets produce
a warning and are skipped. The target's existing classes come first, followed
by contributed classes in loading order, without duplicates. Equipment and
skins still belong to the class's source pack. Declare any required pack
dependency in the supplying module's metadata as usual.

Official Packs need no reference to the extending pack. Removing that pack
restores the original roster. An explicit Teams editor class list overrides
the entire roster; otherwise the editor inherits contributions. Disabled and
edited classes are respected.
