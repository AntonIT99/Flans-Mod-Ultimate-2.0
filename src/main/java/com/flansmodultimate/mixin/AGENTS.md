# Mixins

Mixins live in `src/main/java/com/flansmodultimate/mixin` (config
`src/main/resources/flansmodultimate.mixins.json`) and `src/npcs/java/com/wolffsmod/npcs/mixin`
(config `src/npcs/resources/wolffsmodnpcs.mixins.json`).

- At most one mixin class per target class, across every source set and mixin config. Add new
  injections, shadows, and accessors to the existing class for that target instead of creating
  another. A class with a multi-target `@Mixin({A.class, B.class})` owns all of those targets.
- Name a new mixin after its target (`EntityMixin`, `EntityRendererMixin`); `*Accessor` is for a
  class that only declares `@Accessor`/`@Invoker` methods. Older feature-named mixins
  (for example `NpcArmorControlsMixin`) keep their names.
- Keep each class's Javadoc listing what it does for its target, so merged hooks stay findable,
  and group the hooks by feature inside the class.
- Register every mixin in its config: `mixins` for targets present on both sides, `client` for
  client-only targets. A mixin whose target is common but that also hooks a client-only member
  (for example `@OnlyIn(Dist.CLIENT)` methods) stays in `mixins` and marks that injector
  `require = 0`, with a comment saying why.
- Prefix handler methods, `@Unique` members, accessor/invoker methods, and methods of
  interfaces a mixin adds to its target with the mod id, without `$`, so they cannot collide
  with the target or other mods: `flansmodultimateCollideWithDriveableHulls`,
  `wolffsmodnpcsShieldCooldown`, and `WOLFFSMODNPCS_DISTANCE_DATA` for `@Unique` constants.
- IntelliJ and SonarLint misread mixin code. Suppress such false positives with
  `@SuppressWarnings` on the class or member plus a short reason, for example `DataFlowIssue`
  on `(Target) (Object) this` casts, `java:S1905` on a required `(Object) this instanceof`
  cast, or `FieldCanBeLocal` on `@Shadow` fields.

- Keep each Mixin's source, JSON entry, target, and signature synchronized.
- Avoid early static registry access.
