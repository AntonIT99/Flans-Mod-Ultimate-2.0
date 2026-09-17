# Encrypted optional content

Flan packaged-content modules can provide optional uncensored translations and
textures. The client setting `enableUncensoredContent` is `false` by default.
Changing it while the game is running reloads client resources. At startup, the
saved setting is read directly from `config/flansmodultimate-client.toml` if
Forge has not loaded the client config yet. The encrypted overlay therefore joins
Minecraft's normal initial resource load without an additional reload or manual
`F3+T`.

## Authoring location

Plain authoring files belong below:

```text
src/<pack>/resources/encrypted/assets/<namespace>/
```

For an Example module, use:

```text
src/examplepack/resources/encrypted/
└── assets/
    └── flansmod/
        ├── lang/
        │   ├── en_us.json
        │   ├── de_de.json
        │   └── <other_locale>.json
        └── textures/
            ├── armor/
            ├── gui/
            ├── item/
            └── skins/
```

Every valid Minecraft locale filename is supported, not only `en_us.json`. Each
translation JSON may contain only the keys that should change for that locale.
Minecraft applies the encrypted file for the language selected by the player;
provide a matching file for every locale that needs uncensored names. Texture
files must use exactly the same path and filename as the normal texture they
replace. PNG animation metadata can be placed beside a texture as the matching
`.png.mcmeta` file.

Every pack's `resources/encrypted/` directory is gitignored and explicitly
excluded from normal resource processing, so its plaintext files are never
added to a module JAR. The module's encryption task writes
`flans_content/content.fmu` into a generated resources directory, and only that
authenticated encrypted bundle is packaged. An encryption task is registered
only when the pack's `resources/encrypted/` directory exists and contains at
least one file.

For an Example module, generate or package it with:

```text
gradlew encryptExamplepackUncensoredContent
gradlew examplePack
```

Encryption prevents casual reading of the distributed file. Because the client
must contain enough information to decrypt resources when the setting is on, it
cannot prevent a determined user from extracting them at runtime.
