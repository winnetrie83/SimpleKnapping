# SimpleKnapping custom recipe editor — dev1 foundation

This is an **overlay patch** for the current GitHub `main` of `winnetrie83/SimpleKnapping` as inspected on 2026-08-09.

## What this milestone adds

- Separates recipe state into:
  - resource/datapack recipes
  - world-persistent server recipes/overrides
  - disabled recipe ids
  - effective runtime recipes
- Adds `StoredKnappingRecipe`, a disk-safe form of a knapping recipe.
- Adds `CustomKnappingRecipeData` using Minecraft/NeoForge `SavedData`.
- Server recipes can override a built-in/datapack recipe simply by using the same recipe id.
- Built-in/datapack recipes can be disabled without rewriting their JSON.
- Saving/removing/disabling a server recipe rebuilds the effective runtime map immediately; no `/reload` is required.
- Loads the world-specific custom state when the server has started.

## Files

Replace/add these files at the same paths in the repository:

- `src/main/java/be/winnetrie/mod/simpleknapping/SimpleKnapping.java` — replace
- `src/main/java/be/winnetrie/mod/simpleknapping/knapping/KnappingRecipeManager.java` — replace
- `src/main/java/be/winnetrie/mod/simpleknapping/knapping/StoredKnappingRecipe.java` — add
- `src/main/java/be/winnetrie/mod/simpleknapping/knapping/CustomKnappingRecipeData.java` — add

## Intentionally not in dev1 yet

The actual admin screen and network payloads are not wired yet. This milestone creates the server-side persistence/runtime layer they need, without changing normal player knapping behavior.

Next implementation layer:

1. admin permission gate / open action
2. server -> client recipe catalogue snapshot
3. recipe list screen
4. 5x5 editor + item picker
5. save/delete/disable/restore payloads with server-side validation
6. live refresh after a mutation

## Validation note

The execution container used to prepare this patch cannot resolve GitHub/Maven hosts from Gradle, and only Java 21 is locally installed while this project targets Java 25. Therefore this overlay has been statically reviewed, but a real `./gradlew build` against the project's NeoForge 26.2 dependencies still needs to be run in the normal project environment.
