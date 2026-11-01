# Flan's Ultimate 2: NPC Vehicles & Soldiers 1.0

First release. Requires Flan's Mod Ultimate 2.2 or newer and Custom NPCs.

* Teams NPC vehicles respect each vehicle spawner's allowed categories and exact definitions; excluded vehicles wait for an eligible spawner instead of appearing at infantry spawn points

## Soldiers

* Custom NPCs use Flan's guns, grenades, throwables, melee weapons, shields and armour, with reloads, ammunition from their inventory and Flan's sounds and muzzle flashes; the sounds an NPC makes carry as far, and as loudly, as the same weapon in a player's hands
* Soldier class loadouts from Flan's teams and classes, with skins, themed name lists and naming by class and gender; NPCs whose random Custom NPCs preset skin is missing wear Steve instead of the missing texture
* Flan NPC tactics for soldiers, vehicles and aircraft, including cover, peeking and Custom NPCs' own tactics with Flan's weapons
* NPCs throw what they carry by per-kind strategies, and open carried parachutes when they fall
* NPCs with Custom NPCs' crawling animation fire with their gun's prone accuracy and hold their gun forward along the body
* NPCs drawn with the player model are hit limb by limb by Flan rounds like players: headshots and the other hit zones use the same damage modifiers, worn armour protects the part it covers, and rounds passing between the limbs miss. `Limb hitboxes` (Protection > General) turns this off for a single NPC
* NPCs always have their full-size hitbox: Custom NPCs left NPCs created on the server, such as Teams bots, a box one block high there, and NPCs that got off a vehicle a box 77% as high, so rounds, arrows and melee aimed at the chest or head passed over them
* Presets in the Wolff's Mod tab, such as Mob-like and Player-like, with undo; presets preserve the existing `Return to start` setting
* NPCs look for and keep targets out to their aggro range instead of only Custom NPCs' `NpcNavRange`, so planes and helicopters flying high above their targets spot and hold them; aircraft models fight no faster than lets them come round within their aggro range, so fast planes no longer fly off out of the simulated area; planes fly straight on to gain room when their target is too close beside them to turn onto, rather than circling it, and keep their target while they come round; planes and helicopters that see their goal head straight for it instead of searching a path through the air, and `Efficient path searches` covers flying NPCs too, so a single bomber no longer stalls the server
* Wandering range `0` allows unlimited roaming without triggering `Return to start` for distance; returning after combat and on respawn is preserved
* `Teleport home when inactive` (Advanced > Performance & debug > Activity): NPCs with `Return to start` left away from home outside the simulation distance, or resting beyond their dormant range, are sent straight back once no player is near enough to see them go
* NPCs that fight back can no longer be picked off from beyond their aggro range: an enemy that hits them or fires close past them stays their target, however far off, for `Remember attackers` seconds after its last shot (15 by default), and one with nobody else to fight turns on a shooter firing past it. `Call out attackers` lets allies of the same faction within that radius take the enemy on too; one that cannot reach its own target turns on it when it can (Tactics > Return fire)
* NPCs on foot shot at from beyond their reach take cover from the shooter and move up on it from cover to cover instead of walking at it in the open (`Take cover when outranged`, Tactics > Return fire)
* `Firing range from weapon` (Detection & ranges): an NPC fires, and notices enemies, as far as the spread of the weapon it holds still lets it hit, within a limit for its kind of weapon, so a scoped rifle outranges a sub-machine gun; its own worse accuracy shortens the range
* Human limits for any NPC (Aiming > Human limits): reaction time, aim settling time, first-shot aim error, lead share, damage to players and `Never kill players`, filled in from the Teams bot difficulty levels by `Skill level` or set by hand; they put no limit on an NPC by default, and a Teams bot fights with its own again once it leaves its battle
* `Suppressed by near misses` (Tactics > Return fire): enemy rounds passing close to an NPC on foot make it aim worse, and once pinned down it moves no closer to its target until the fire lets up
* Duties for any NPC on foot (Tactics > Duties): `Duty` makes it a medic, engineer, support or anti-tank soldier for the players and NPCs its faction is friendly to, by hand or from its equipment, `Fall back below health` sends it to a medic or home when badly hurt, and `Prefer suited targets` makes anti-tank soldiers look at armour first and everyone else at infantry, as Teams bots do
* `Name tag for allies only` (Effects): only players the NPC's faction is friendly to see its name tag

## Vehicles, Aircraft & Mechas

* Ship bots use the nearest clear water surface within 64 blocks of their selected spawn point, and wait to spawn when no water is available in loaded chunks

* NPCs drawn as Flan's vehicles, planes, helicopters, ships, mechas and AA guns move, fight, sound and take damage like their driveables, with part hitboxes, armour and HEAT overrides; destroyed planes and helicopters fall under gravity; vehicle muzzle flashes replace the fallback flame particle while preserving authored shooting particles
* NPCs board, drive and fly the Flan's driveables players place, lead their targets, land planes and watch their fuel
* Vehicle NPCs carry crews in their seats; seat guns use normal magazine reload delays and sounds, crewed seat guns pick their own targets, and crew can get out to fight or jump by parachute
* Seated crew keep their body aligned with their seat through rendering and turns across 180 degrees; gunners turn with their gun as players do and, on vehicle NPCs, move round the gun's pivot; NPCs riding normal aircraft inherit pitch and roll like players
* Friendly players can right-click any empty seat at its position to board vehicle NPCs, including seats outside the central hitbox, occupy empty driver seats and man gunner seats, and change seats with Flan's change-seat key; the NPC still drives when a player occupies its driver seat
* NPC vehicles manoeuvre for a shot, back away from targets under their barrel and can be repaired
* Vehicles, aircraft and ships keep clear of their own blast: they hold their fire at a target close enough for their explosive rounds to catch them and pull back out of reach first (`Keep clear of own blast`, `Blast safety margin`)
* Ships run in bow on to fire torpedoes and anything else fixed to their hull, turn away after each run, and circle their target so the turrets along their side bear once they have none left (`Ships turn to bear`)
* Vehicle, plane and mecha NPCs fire both weapon groups of their model by default (`Fire both weapon groups`): the group beside the main attack fires its guns, shells, rockets and bombs at its own cadence, and its bombs fall only on vehicles and other large targets on the ground, which planes approach in a level bomb run; a model whose chosen group is empty, such as a bomber that only carries bombs, attacks with its other group
* Players can stand and walk on the solid hull and turret of an NPC vehicle, which carries them smoothly without running them over or pushing them off; fixed sudden turns and drops from unstable carry transforms, including on large ships, and replayed movement when server time is synchronized
* The Guard job's target lists leave out Flan's driveables and NPC models, and chosen targets leave Available Targets

## Teams Bots, Factions & Battles

* NPCs attack players on Peaceful difficulty too, which Custom NPCs otherwise forbids: Teams bots always, other NPCs while `Fight players on Peaceful` (Server > Rules) is on, as it is by default
* Bot difficulty, cycled from trainee up to ace, set for a round, skirmish or war and independent of the world's difficulty: `ace` (no human limits), `elite` (nearly full use of the equipment with a human's reaction), `veteran`, `regular` (the default), `recruit` and `trainee`. Lower levels react later to a newly seen target, need longer for their aim to settle, lead moving targets less, aim worse and notice enemies less far out, while keeping the health and rate of fire their equipment gives them; `recruit` bots deal half their damage to players and `trainee` bots a quarter. `custom` fights with values of its own for everything a level sets, chosen with `Edit` beside the difficulty in the bot editor and on the skirmish and war pages, starting from any level, or given as `difficulty custom <accuracy> <aggro> <reaction> <settle> <error> <lead> <player_damage> <non_lethal>`. A changed level or custom value reaches the bots already in the field within a second of saving the settings
* `Cinematic mode` (Server > Ranges & rules), for filming battles: players cannot die, whatever hits them, except by falling out of the world or `/kill`; NPCs still kill each other
* Teams, skirmish and war bots follow the global `botHealth` maximum-HP override from the command centre, including bots already in the field; 0 keeps their own health
* Teams soldiers follow the round regeneration rule and heal half a heart every five seconds by default; medical healing and vehicle repairs remain independent
* Teams bots made from a class fire as far as their weapon and level let them hit, about 128 blocks with an accurate rifle and up to 256 with a scoped one, and call out whoever shoots at them to the bots within 48 blocks, so a player sniping them from afar draws their fire and their marksmen and vehicles

* Teams bots: NPC soldiers, squads and vehicles that fight Teams rounds, with player orders (`Squad orders` key, O), a commander seat, transports, class roles (a medic heals and an engineer repairs once every five seconds, and the same ally or vehicle at most every ten seconds), every class of the team fielded in proportion to its weight, grenades thrown at any enemy in safe reach, suppression and squad leading; bots approach CTF flags at ground level even on taller poles
* Manage Custom NPCs factions for battles: branches of Flan's teams, alliances, wars and peace between factions
* Players can join and leave factions; factions are invite-only by default, and operators choose who may join, the leave penalty and the rejoin cooldown
* `Attack Mobs` in Global > Factions: every NPC of the faction hunts hostile mobs on sight, slimes and ghasts included, without needing the Guard job
* Skirmishes between factions outside Teams rounds, and endless wars over chunk territory
* Teams bot editor (`/teams ai editor`): a team's clone, class weights, squads, vehicles, class roles and vehicle crew classes are chosen in pickers with search, pages and previews of the clone, class loadout or squad leader; vehicles are browsed by category with an optional content-pack filter, and their previews fit the full model within their panel; vehicle allocations have quantity controls, named spawn and carried-squad settings, and a choice of all paintjobs or a selected subset; vehicle lists fall back to definition names or shortnames and previews use their proper model textures; the count mode is explained beside each team's count, with the number of soldiers it fields, and every setting explains itself in its tooltip; the Rules tab fits above the round selector
* Factions, Skirmishes, Wars and Bots pages in the Teams command centre
* Configurable friendly fire between NPCs, players and allies (`wolffsmodnpcs-common.toml`, `/flansnpcfriendlyfire`, Server > Rules > Friendly fire in the Wolff's Mod tab)

## Performance

* NPCs far from every player work out their decisions every second or every fourth tick instead of every tick, which is what a large battle spends its tick time on. Their movement, physics, damage and shots keep running every tick, and an NPC in a seat or drawn as a vehicle, aircraft, mecha or AA gun always decides every tick. `aiTickLod`, with its distances and periods, turns this off or retunes it in `wolffsmodnpcs-common.toml`
* NPC path searches run on worker threads, so a crowd asking for paths no longer lengthens the server tick: an NPC keeps the path it has and takes the new one a tick or two later (`asyncPathfinding`)
* `budgetTargetMillisPerTick` works out the target and path search limits from how long the server's ticks are taking, so a battle gives up search cadence rather than tick rate and operators need not guess numbers for their hardware; 0 keeps the `SearchBudget` and `PathBudget` settings as they are set
* Target searches, call-outs to allies and duties read one list of the level's living entities, taken once a tick, instead of every NPC asking the level for the entities near itself over and over; a target search also picks out the nearest candidate that matters instead of putting a whole band in order

## Tools

* Custom NPCs can run on the headless `GameTestServer` without trying to load client classes when opening its save directory
* The Wolff's Mod tab in the Custom NPCs editor, with combat, crew, movement, naming, model and server pages
* Flan model picker with previews and tooltips
* NPC diagnosis, including a flying plane's speed and circle radius, server limits, rules, census and benchmarks
* Flan's debug mode marks the seats and seat-gun muzzles of vehicle NPCs, as it does for driveables
* Killed NPCs drop no items unless Server > Rules in the Wolff's Mod tab turns item drops on; Teams bots then drop what the Teams `weaponDrops` and `armourDrops` rules let a dying player drop, including grenades and bandages. Their experience still drops either way
* Scripting controls for Flan's features
