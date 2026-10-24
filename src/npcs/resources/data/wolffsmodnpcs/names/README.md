# Soldier name lists

These are manually curated pools of real given names and surnames. Four lists
are labeled `(21st century)` for contemporary use, and 32 lists are labeled
`(20th century)` with a strong world-war-era focus. The century denotes the
intended setting, not the person's birth century. They are not exhaustive
representations of national populations or measured historical frequency models.
See [HISTORICAL.md](HISTORICAL.md) for historical coverage, references and assumptions.
Names can be shared across national lists. No real person's complete identity is
used as a generation template.

The JSON files are loaded from the NPC module's classpath as UTF-8. They
are bundled inputs, not reloadable datapack registries. Rebuilding `npcsJar` is
required after changing them. Malformed or missing bundled data fails visibly
rather than silently substituting a different country's list.

Each file has an `id` and a `groups` array. Each group contains:

- `weight`: positive integer controlling group selection.
- `male` and `female`: nonempty arrays of complete given names.
- `surnames`: nonempty array of complete surnames.
- `femaleSurnames`: optional nonempty array; defaults to `surnames`.
- `surnameFirst`: optional boolean; defaults to false. Hungarian, Chinese and
  Japanese historical pools use true.

Historical files also include `setting` and `sources` metadata for maintainers.
This metadata does not change generation probabilities.

Within each pool every entry is equally likely. Duplicates are rejected.
Gender uses Custom NPCs' existing values: 0 either, 1 male, 2 female. Either
chooses each gender with equal probability. First select a group, then gender,
then given name and surname from that group; components never cross groups.
The selected group's `surnameFirst` flag determines component order.

British combines English/general British, Scottish and Welsh pools. Its 8:1:1
group weights are an illustrative mix, not measured demographic frequencies.
American includes surnames from several naming traditions but is not a complete
model of American naming diversity. Russian uses full given names, Latin
transliteration and explicit feminine surnames; it omits patronymics.

The append-only Custom NPCs numeric slots are 10 German, 11 British, 12 American,
13 Russian (the four modern lists), followed by historical slots 14–45 as listed
in [HISTORICAL.md](HISTORICAL.md). Preserve these assignments and the original slots 0–9. NPC display
NBT also stores `WolffsModNameList` as `wolffsmodnpcs:<id>`; that stable identity
takes precedence when loading. Unknown stable identities fall back to the
original Roman generator without changing the saved NPC name. Returning to an
original generator removes the extra key on the next save. Script calls to
Custom NPCs' existing random-name API can also use slots 10–45.

Useful references for future expansion and frequency research (the starter data
is not a transcription of these datasets):

- [US Social Security Administration given names](https://www.ssa.gov/oact/babynames/)
- [ONS historical given names for England and Wales](https://www.ons.gov.uk/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalestop100babynameshistoricaldata)

Name generation and the Custom NPCs adapters remain internal to the NPC module:
they do not require any main-mod API access or introduce a Custom NPCs dependency
into the main mod.

## Class-picker associations

On Forge 1.20.1, the server creates `config/wolffsmodnpcs/soldier-names.json`
from the bundled `data/wolffsmodnpcs/soldier-names.json` on first startup. Edit
that config and restart the server (or reopen the single-player world) to change
the associations. Existing files replace the defaults and are never overwritten.

`teams` and `classes` map exact, case-sensitive Flan `ShortName` IDs to stable
name-list IDs, such as `wolffsmodnpcs:german_20th`. For example:

```json
{
  "teams": { "myTeam": "wolffsmodnpcs:german_20th" },
  "classes": { "myClass": "wolffsmodnpcs:british_20th", "keepName": "" }
}
```

`customnpcs:roman` selects Custom NPCs' original Latin/Roman generator (slot 0).
Both Wolff Rome teams use it by default. Names are generated through Custom NPCs
with the association gender (male by default); the original numeric generator and save format are
retained. This reuses its built-in Markov name generation rather than a curated
historical Roman name pool. Unknown `customnpcs:` IDs are rejected.

A class association takes precedence over its selected team. An empty string
explicitly disables automatic naming, including team inheritance for that class.
Unmapped selections keep their current name and generator. Unknown list IDs or
malformed JSON disable automatic naming for that server session and log an error;
the file remains intact. Associations for packs that are not installed are harmless.

The model picker's **Apply class name** toggle defaults to Yes, independently of
loadout and skin. Selecting a class generates a name on the server using the association gender (male by default) and saves the associated generator for later use.
Turning the toggle on also names the currently selected class. Turning it off
preserves the current name on subsequent selections. The toggle lasts for the
builder's client session. Merely loading an existing NPC does not rename it.

Defaults cover national WWII teams in the official, Warfare 44 and Wolff Germany
packs, and modern KSK, Marines, SAS, Spetsnaz and SWAT teams from the official
packs. Other classes without a suitable existing list stay unmapped.

## Clone designations

The appended generator slots 46–55 are `clone_ct`, `clone_cc`, `clone_arc`,
`clone_rc`, `clone_cl`, `clone_cs`, `clone_alpha`, `clone_null`, `clone_rex` and `clone_cody`. Existing national generator IDs stay unchanged.
They appear in both naming menus and use the same stable-ID persistence and scripts.

Designation JSON replaces `groups` with `designations`, an array of patterns such
as `CT-####` or `CT-##-####`. Each `#` independently generates a decimal digit,
including leading zeros; a pattern is selected uniformly. Gender has no effect.
A file supplies either national name groups or designation/fixed-name options, never both.
These are display-name generators, not a globally unique serial registry.

The shipped class associations cover the 80 Republic clone classes in the Wolff
Star Wars pack. Ordinary troops, lieutenants and sergeants use CT; commanders,
ordinary captains use CC; ARC classes use the mixed ARC generator. RC, CL and CS remain optional
Legends-style choices. These are editable conventions based on the supplied GAR
research, not mandatory rank rules: promotions do not automatically rename NPCs,
and neither legion nor armor phase is encoded in the number. Rex and Cody classes
use fixed Captain Rex and Commander Cody generators by default; change their
associations to clone_ct or clone_cc for generic clones.

Existing user association configs are preserved. To use these new shipped defaults
on an existing installation, merge the desired entries from the bundled
`data/wolffsmodnpcs/soldier-names.json` into the server config and restart.

The mixed `clone_arc` pool includes CT infantry-retained and ARC designations,
plus Legends Alpha (`A-`/`Alpha-`, 1–100) and Null (`N-`/`Null-`, 1–12) ranges.
`clone_alpha` and `clone_null` select only those series. The Null range represents
the experimental series, not a claim that all twelve survived. Choices are uniform
across patterns/ranges, not a simulation of the relative sizes of these formations.

JSON can additionally specify `serials`, e.g.
`[{"prefix":"Alpha-","min":1,"max":100}]`, with inclusive bounds, or
`fixedNames`, e.g. `["Captain Rex"]`. Each pattern, range, or fixed-name entry is
one equally likely option. Serial ranges use unpadded numbers. Fixed-name pools
can be used for other special characters without changing the generator code.

## Imperial designations

Slots 56–62 append `imperial_tk`, `imperial_td`, `imperial_ts`, `imperial_tb`,
`imperial_rs`, `imperial_tx` and `imperial_tk_legends`. Ordinary lists generate
three- or four-digit numbers. The Legends TK variant chooses lengths 1–10.
Leading zeros are retained; no rank, unit, branch-transfer or recycling simulator
is implied. Serial uniqueness is not guaranteed.

The Imperial team defaults to TK, including its shadow trooper class. TD, TS, TB
and RS are optional branch conventions from the supplied Legends research.
TX is explicitly labeled a fan-costuming convention, not confirmed Imperial doctrine.
For a deliberate TX shadow variant, map `WolffStarWars_ImperialShadowtrooper` to
`wolffsmodnpcs:imperial_tx` in the server config; the class overrides the team's TK.

## Battle droid designations

Slots 63–69 append `droid_b1`, `droid_oom`, `droid_b2`, `droid_bx`,
`droid_tactical`, `droid_magnaguard` and `droid_droideka`. The two Separatist
teams default to B1 names. Commander classes override this with a B1/OOM mixture;
OOM is an available convention, not a universal command-rank prefix. Droideka
classes override it with an explicitly invented Droideka serial convention.

B1 options are `B1-###`, `B1-####`, numeric `####`, and `3B3-##` / `3B3-####`.
The commander pool also permits `OOM-#`, `OOM-##` and `OOM-###`; the longer OOM
formats are illustrative extensions of an attested short designation.
Tactical droids use `TX-##` / `TA-###`. B2, BX, IG-100 and Droideka numeric
schemes are labeled invented: model classifications alone do not establish an
individual serial format. These are display conventions based on the supplied
research, not an army-wide production or rank encoding system. Numerical-only
names remain strings, preserving leading zeros. All pools ignore NPC gender.

`#` patterns permit fixed alphanumeric model prefixes, multiple numeric blocks,
or no prefix at all. Neither this generator nor the NPC association system adds
an internal globally unique droid ID or changes model, rank or unit state.

Association generation defaults to **male**, independently of the NPC Name menu's
current gender. Optional `teamGenders` and `classGenders` maps accept `male`,
`female` and `mixed` (equal male/female probability). A class gender overrides its
team's gender; otherwise the team value applies, then male. Gender and list
inheritance are independent, so a female-only class can reuse its team's list:

```json
{
  "teams": { "gign": "wolffsmodnpcs:french" },
  "classes": {},
  "teamGenders": { "gign": "male" },
  "classGenders": { "myFemaleClass": "female", "myMixedClass": "mixed" }
}
```

These options control names generated by class selection. They do not alter the
NPC Name menu's own gender selection. Serial and fixed-name generators ignore
gender. GIGN now inherits the bundled modern `french` pool; existing server
configs need its association merged manually, followed by a restart.
