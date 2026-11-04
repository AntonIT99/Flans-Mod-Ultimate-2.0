# Optional external mod compatibility

Flan's Mod Ultimate includes the following integrations on **Forge 1.20.1** and
**NeoForge 1.21.1**. Install external mods separately, using files for your Minecraft
version and loader. The table describes explicit compatibility code in these branches;
it does not certify every external mod release, shader pack or combination of mods.

| External mod | Forge 1.20.1 | NeoForge 1.21.1 | Compatibility feature | Project links |
| --- | --- | --- | --- | --- |
| Embeddium | Yes | Yes | Sends Flan model vertices through the renderer's bulk vertex API when supported, falling back to ordinary vertex submission when necessary. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/embeddium), [source](https://github.com/FiniteReality/embeddium) |
| Sodium | — | Yes | Uses Sodium's bulk vertex API for Flan models. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/sodium), [source](https://github.com/CaffeineMC/sodium) |
| Oculus | Yes | — | Detects active shader packs and shadow passes so Flan rendering uses compatible paths. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/oculus), [source](https://github.com/Asek3/Oculus) |
| Iris Shaders | — | Yes | Detects active shader packs and shadow passes, with compatible vertex submission and rendering fallbacks. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/irisshaders), [source](https://github.com/IrisShaders/Iris) |
| Entity Culling | Yes | Yes | Automatically exempts relevant Flan entities from tick culling to keep movement, sounds and effects updating; bullets and aircraft are also exempt from visibility culling. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/entityculling), [source](https://github.com/tr7zw/EntityCulling) |
| Distant Horizons | Yes | Yes | Adds approximate far-terrain rangefinding, distant driveable contact shapes and distant explosion cues. Requires DH API 5.1 or newer; see [requirements and server limits](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki/Distant-Horizons-Integration). | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/distant-horizons), [source](https://gitlab.com/distant-horizons-team/distant-horizons) |
| BuildCraft Community Edition | Fluid support | Fluid support | Generic fluid-container refuelling includes default registry patterns for BuildCraft oil and fuel families. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/buildcraft-community-edition), [source](https://github.com/BCCE-team/BuildCraft) |
| Just Enough Items (JEI) | Yes; 15.20+ | Yes | Shows vehicle assembly and gun-box purchase requirements, engines and crafting stations; custom crafting screen titles open the corresponding JEI category. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/jei), [source](https://github.com/mezz/JustEnoughItems) |

## Assessed as compatible without integration

These mods have no dedicated integration code in Flan's Mod Ultimate, but the
project has assessed them as safe to run together on the listed branches.

| External mod | Forge 1.20.1 | NeoForge 1.21.1 | Notes | Project links |
| --- | --- | --- | --- | --- |
| ServerCore | Yes | Yes | Default settings need no changes. If you turn on activation range, exclude Flan's entities (see below). | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/servercore), [source](https://github.com/Wesley1808/ServerCore) |
| ImmediatelyFast | Yes | Yes | No direct interaction with Flan's rendering found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/immediatelyfast), [source](https://github.com/RaphiMC/ImmediatelyFast) |
| Radium Reforged | Yes | — | Forge port of Lithium. No direct interaction found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/radium-reforged) |
| Lithium | — | Yes | No direct interaction found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/lithium), [source](https://github.com/CaffeineMC/lithium) |
| More Culling Reforged | Yes | — | Forge port of More Culling. No direct interaction found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/more-culling-reforged) |
| More Culling | — | Yes | No direct interaction found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/moreculling), [source](https://github.com/FxMorin/MoreCulling) |
| ModernFix | Yes | Yes | No direct interaction found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modernfix), [source](https://github.com/embeddedt/ModernFix) |
| FerriteCore | Yes | Yes | No direct interaction found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ferritecore), [source](https://github.com/malte0811/FerriteCore) |
| Redirected | Yes | Yes | No direct interaction found. | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/redirected) |

### ServerCore activation range

ServerCore's activation range is off by default. When it is on, entities far
from every player only tick occasionally, so bullets and grenades can stall in
flight, and unattended driveables, AA guns and AI mechas can stutter or stop. To
avoid this, add Flan's entity types to `excluded-entity-types` in
`config/servercore/optimizations.yml`:

```yaml
excluded-entity-types:
  - flansmodultimate:bullet
  - flansmodultimate:grenade
  - flansmodultimate:thrown_gun
  - flansmodultimate:deployed_gun
  - flansmodultimate:aa_gun
  - flansmodultimate:plane
  - flansmodultimate:vehicle
  - flansmodultimate:mecha
  - flansmodultimate:driveable_seat
  - flansmodultimate:driveable_wheel
  - flansmodultimate:flybyplane
  - flansmodultimate:aimecha
```

Keep ServerCore's own default entries in the list as well. If you use the Custom NPCs
module, consider excluding `customnpcs:customnpc` too. Dynamic view distance may
shorten how far players see distant vehicles and aircraft while the server is under load.

## Rendering and installation

Embeddium and Sodium are alternative renderers. The entries above indicate which
APIs Flan's Mod can use on each branch; follow the renderer and shader mod's own
dependency requirements when choosing a combination. Rendering integrations and
Entity Culling run on the client. Distant Horizons is also optional on the client;
servers do not need DH installed for Flan's integration.

With an active Oculus or Iris shader pack, Flan's experimental GPU model cache
falls back to the standard rendering path. Installing the shader mod with shaders
disabled leaves the cache available. Forge 1.20.1 also detects **OptiFine** and
disables the GPU cache as a compatibility fallback; this is more limited than the
Oculus/Iris API integration. See [Rendering Performance](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki/Rendering-Performance).

## Fluid fuels

BuildCraft compatibility uses the loader's fluid-container support rather than a
BuildCraft API dependency. Put a supported fuel container in a driveable's fuel or
cargo inventory. The common `fluidFuels` defaults are
`buildcraftenergy:oil*; 1000` and `buildcraftenergy:fuel*; 2000`, measured in Flan
fuel units per bucket. Matching registry IDs and exposed fluid containers are what
matter; other mods' fluids can be configured too. This does not imply support for
every BuildCraft machine or direct pipe connection to a driveable.

See [Mod Config](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki/Mod-Config#vehicle-fuel-settings)
for fluid fuel configuration.

## Custom NPCs and companion modules

**Custom NPCs** is optional for the main mod, but required by the separately
installed **Flan's Ultimate 2: Wolff's NPC Vehicles & Soldiers** module. That module
adds Flan models for NPCs, soldiers and vehicle crews/drivers. Use the Custom NPCs
build required by the module for your Minecraft version. See
[NPC Vehicles and Soldiers](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki/NPC-Vehicles-and-Soldiers)
for setup. The project's own Packs Manager and content packs are separate project
artifacts, rather than external compatibility mods.
