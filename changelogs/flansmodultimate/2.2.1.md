# Flan's Mod Ultimate 2.2.1

## Guns

* Ammo held in your offhand is now loaded first, so a gun that takes several ammo types can be switched without rearranging the inventory; the HUD names the type and `preferOffhandAmmoOnReload` turns it off

## Vehicles & Aircraft

* Planes gain working cockpit gauges with simulated oil and water heat, and animated wheel covers and landing flaps; lowered flaps add configurable drag
* Aircraft with an afterburner or war emergency power can arm it with a new key (O); it lights near full throttle, adding thrust and top speed at the cost of fuel
* Vehicle models support side and turret skirts, opening body and turret doors, and alternating track links

## Teams

* Stopping Teams play now takes players off their team and class, giving them their own skin back

## Performance

* Models of nearby vehicles and carried items now load in the background, reducing the stutter when something is first seen
* ZIP and JAR content packs reprocess several times faster and with far less memory, and an invalid recipe no longer makes a pack reprocess on every start

## Content Packs & Compatibility

* Content packs tolerate more typing mistakes in their values; repaired issues go to the debug log unless `showAllParsingIssuesInErrorLog` is enabled
* Model and texture names holding stray characters, such as a colon pasted from a display name, now find their files instead of preventing the game from loading
