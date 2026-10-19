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
