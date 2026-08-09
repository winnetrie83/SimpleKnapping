# SimpleKnapping 1.0.6-dev1 — Custom Recipe Manager overlay

Overlay for the GitHub `main` source inspected on 2026-08-09 (Minecraft 26.2 / NeoForge 26.2.0.6-beta / Java 25).

## Included now

- dev1 persistent custom recipe / override / disabled layer
- corrected 26.x `SavedDataType` constructor (DataFixTypes argument included as `null`)
- `/simpleknapping recipes` command, gated by vanilla `COMMANDS_GAMEMASTER`
- bidirectional NeoForge recipe-editor payload
- server-authoritative validation on every write
- recipe catalogue snapshot server -> client
- visual admin Recipe Manager screen
- searchable recipe list
- resource/custom/override status
- 5x5 click editor
- knapping-type cycling
- searchable vanilla + modded item picker using the live item registry
- count field
- New / Duplicate / Save
- Enable / Disable
- Restore original
- Remove custom/override layer
- duplicate active-pattern conflict detection
- live server refresh after every mutation; no `/reload` required

## Install as overlay

Copy the files in this ZIP over the same paths in the current SimpleKnapping repository.
Existing files included here are replacements; new packages/files should be added.

## Test flow

1. Build with the project's normal Java 25 environment.
2. Start a world/server and OP yourself.
3. Run `/simpleknapping recipes`.
4. Select a recipe, or press New.
5. Click the 5x5 cells, pick a result item and Save.
6. Test the pattern immediately in the normal knapping UI.
7. Restart the world/server and confirm the custom recipe persisted.
8. Test Disable, Restore original and Remove custom.

## Validation limitation in this environment

The preparation container has Java 21 and no cached NeoForge 26.2 userdev artifacts, and cannot resolve Gradle/Maven hosts. A real Gradle compile therefore cannot be executed here. The code is aligned against the current GitHub source plus NeoForge 26.1/26.2-era official API documentation, but the first local `./gradlew build` may expose small mapping/signature differences that should be fixed from the compile log.
