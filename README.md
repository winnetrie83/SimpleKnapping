# SimpleKnapping

> Consolidated project README containing the original repository notes and the complete development history through **1.0.6-dev1.9.2**.

## Current development status

- Latest documented development version: **1.0.6-dev1.9.2**
- Minecraft: **26.2**
- NeoForge target: **26.2.0.6-beta**
- Java: **25**
- Latest documented admin/network protocol: **`recipe_editor_5`**
- The sections below are kept in chronological order. Later sections supersede earlier behavior where explicitly stated.

## Contents

- [Original README / Development environment](#original-readme--development-environment)
- [dev1 foundation](#dev1-foundation)
- [Custom Recipe Manager](#custom-recipe-manager)
- [dev1.1 compile hotfix](#dev11-compile-hotfix)
- [Custom Knapping Type Manager](#custom-knapping-type-manager)
- [dev1.2.1 compile hotfix](#dev121-compile-hotfix)
- [dev1.3 per-recipe materials](#dev13-per-recipe-materials)
- [dev1.4 material-driven textures & compact Recipe Manager](#dev14-material-driven-textures--compact-recipe-manager)
- [dev1.5 Admin Settings GUI](#dev15-admin-settings-gui)
- [dev1.6 unified admin GUI size](#dev16-unified-admin-gui-size)
- [dev1.7 instant wooden/stone tier gating](#dev17-instant-woodenstone-tier-gating)
- [dev1.8 Recipe Guide & empty disabled crafting results](#dev18-recipe-guide--empty-disabled-crafting-results)
- [dev1.9 Flint replacement progression](#dev19-flint-replacement-progression)
- [dev1.9.1 Recipe Guide cleanup](#dev191-recipe-guide-cleanup)
- [dev1.9.2 Flint Knife & visible mob equipment](#dev192-flint-knife--visible-mob-equipment)

---

## Original README / Development environment

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/

---

## SimpleKnapping custom recipe editor — dev1 foundation

This is an **overlay patch** for the current GitHub `main` of `winnetrie83/SimpleKnapping` as inspected on 2026-08-09.

### What this milestone adds

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

### Files

Replace/add these files at the same paths in the repository:

- `src/main/java/be/winnetrie/mod/simpleknapping/SimpleKnapping.java` — replace
- `src/main/java/be/winnetrie/mod/simpleknapping/knapping/KnappingRecipeManager.java` — replace
- `src/main/java/be/winnetrie/mod/simpleknapping/knapping/StoredKnappingRecipe.java` — add
- `src/main/java/be/winnetrie/mod/simpleknapping/knapping/CustomKnappingRecipeData.java` — add

### Intentionally not in dev1 yet

The actual admin screen and network payloads are not wired yet. This milestone creates the server-side persistence/runtime layer they need, without changing normal player knapping behavior.

Next implementation layer:

1. admin permission gate / open action
2. server -> client recipe catalogue snapshot
3. recipe list screen
4. 5x5 editor + item picker
5. save/delete/disable/restore payloads with server-side validation
6. live refresh after a mutation

### Validation note

The execution container used to prepare this patch cannot resolve GitHub/Maven hosts from Gradle, and only Java 21 is locally installed while this project targets Java 25. Therefore this overlay has been statically reviewed, but a real `./gradlew build` against the project's NeoForge 26.2 dependencies still needs to be run in the normal project environment.

---

## SimpleKnapping 1.0.6-dev1 — Custom Recipe Manager overlay

Overlay for the GitHub `main` source inspected on 2026-08-09 (Minecraft 26.2 / NeoForge 26.2.0.6-beta / Java 25).

### Included now

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

### Install as overlay

Copy the files in this ZIP over the same paths in the current SimpleKnapping repository.
Existing files included here are replacements; new packages/files should be added.

### Test flow

1. Build with the project's normal Java 25 environment.
2. Start a world/server and OP yourself.
3. Run `/simpleknapping recipes`.
4. Select a recipe, or press New.
5. Click the 5x5 cells, pick a result item and Save.
6. Test the pattern immediately in the normal knapping UI.
7. Restart the world/server and confirm the custom recipe persisted.
8. Test Disable, Restore original and Remove custom.

### Validation limitation in this environment

The preparation container has Java 21 and no cached NeoForge 26.2 userdev artifacts, and cannot resolve Gradle/Maven hosts. A real Gradle compile therefore cannot be executed here. The code is aligned against the current GitHub source plus NeoForge 26.1/26.2-era official API documentation, but the first local `./gradlew build` may expose small mapping/signature differences that should be fixed from the compile log.

---

## SimpleKnapping 1.0.6-dev1.1 – compile hotfix

Fixes the six compile errors reported after the first Custom Recipe Manager overlay.

### Minecraft 26.2 API fixes

- `Minecraft.screen` -> `Minecraft.gui.screen()`
- `Minecraft#setScreen(...)` -> `Minecraft#setScreenAndShow(...)`
- `ServerPlayer#getServer()` -> `ServerPlayer#level().getServer()`

No recipe format, networking payload format, storage schema, or gameplay behavior was intentionally changed by this hotfix.

---

## SimpleKnapping 1.0.6-dev1.2 — Custom Knapping Type Manager

This overlay builds on `1.0.6-dev1.1` and extends the admin recipe manager with world-persistent knapping type management.

### New knapping type editor

Open `/simpleknapping recipes` and use **Types** to manage knapping types.

Each custom/overridden type stores:

- **Knapping tool** — any registered vanilla or modded item.
- **Knapping material** — any registered vanilla or modded item.
- **Material amount** — 1–99 items consumed when a knapping session starts.
- **Knapping texture block** — selected from the live block registry, including modded blocks.

Types can be created, duplicated, edited, enabled/disabled, restored to their resource/datapack original, and custom/server layers can be removed.

### Runtime behavior

- A knapping session now matches **tool + offhand material**, so different types can use different tools.
- Active tool/material pairs are conflict-validated server-side to avoid ambiguous type selection.
- The configured material amount is consumed once when the knapping GUI opens (creative/infinite-material players are exempt, as before).
- Non-damageable items are valid knapping tools and simply do not take durability damage.
- GUI-created types are world-persistent in a separate `custom_knapping_types` SavedData layer; resource/datapack JSON is never rewritten.
- Custom type data is restored before custom recipe data on server startup.

### Texture safety

GUI-created types select a **registered block id**, not an arbitrary texture file. The renderer derives the conventional `namespace:textures/block/<block_path>.png` texture from that block. If the resource cannot be resolved (for example, a mod uses a non-standard model/texture path), the knapping grid safely falls back to vanilla `minecraft:textures/block/clay.png`.

Existing resource types keep their legacy direct textures until an admin saves an override for them. Resource JSON may optionally add `tool` and `texture_block`; `tool` defaults to `simpleknapping:flint_knapping_tool` for backwards compatibility.

### Networking

Recipe editor protocol registration version is bumped from `recipe_editor_1` to `recipe_editor_2` because snapshots now include knapping type definitions and type edit actions.

---

## SimpleKnapping 1.0.6-dev1.2.1 – compile hotfix

Fixes the `KnappingInteraction` compile error introduced in dev1.2.

### Minecraft 26.2 API fix

- Replaced invalid `ServerPlayer#displayClientMessage(Component, boolean)` with
  `ServerPlayer#sendSystemMessage(Component)`.

No knapping type format, recipe format, persistence data, networking payload, or gameplay logic was intentionally changed.

---

## SimpleKnapping 1.0.6-dev1.3 – per-recipe materials

Built on top of `1.0.6-dev1.2.1`.

### Main change
Knapping material is now configurable **per recipe** instead of being fixed by the knapping type.

Each recipe can define:
- knapping type
- material item
- material amount
- 5x5 pattern
- result item + count

This makes families such as `wood_carving` practical: the same carving type/tool/texture can contain oak-log, dark-oak-log, spruce-log, etc. recipes, including recipes that reuse the same 5x5 pattern for different input materials.

### Runtime behavior
- The held main-hand tool plus offhand material selects the active knapping type through its active recipes.
- Material is no longer consumed when the screen opens.
- The exact material amount of the matched recipe is consumed when the result is taken.
- A recipe result can only be taken while the required offhand material and amount are still present.
- Old recipes with no recipe-level material fields remain compatible and inherit the knapping type's default material + amount.
- Type-editor material/amount are therefore now treated as **defaults for legacy/new recipe initialization**, not as a hard material lock for every recipe.

### Recipe Manager
- Added `Recipe material` and `Amount` to the recipe editor.
- The existing registry item picker can switch between `Result` and `Material`.
- Vanilla and modded registered items are supported.
- New recipes start with the selected type's default material/amount.
- Duplicate copies material/amount too.
- Recipe search also matches the material id.
- Same pattern is allowed in the same type when the recipe materials differ.

### Safety / ambiguity validation
One held `tool + material` pair must resolve to exactly one active knapping type. The admin editor rejects recipe/type edits that would make routing ambiguous across different types.

### Datapack compatibility
Resource recipes may optionally add:

```json
"material": "minecraft:dark_oak_log",
"material_cost": 1
```

If omitted, the recipe inherits the type's existing `material` and `material_cost`.

### GUI title
The play screen title no longer appends `Knapping`. A type id such as `simpleknapping:wood_carving` now displays as `Wood Carving` instead of `Wood Carving Knapping`.

### Networking
Recipe editor payload registration version bumped from `recipe_editor_2` to `recipe_editor_3` because recipe snapshots/actions now carry recipe material data.

---

## SimpleKnapping 1.0.6-dev1.4 – material-driven knapping texture + compact recipe manager

Built on top of `1.0.6-dev1.3`.

### Material-driven knapping surface

The knapping/carving surface now resolves its texture in this order:

1. If the **actual recipe material used to open the session is a block item**, SimpleKnapping derives and uses that block texture automatically.
   - Example: an oak recipe using `minecraft:oak_log` looks like oak log.
   - A dark-oak recipe using `minecraft:dark_oak_log` looks like dark oak log.
   - Registered modded block items are supported the same way.
2. If the material is not a block, or the expected block texture resource cannot be resolved, the GUI uses the **fallback texture block configured on the knapping type**.
3. If that configured fallback cannot be resolved, SimpleKnapping falls back to the hard-coded safe texture `minecraft:textures/block/clay.png`.

The Type Manager wording now calls this setting **Fallback texture block** to match the new behavior.

### Recipe Manager compact layout

The Recipe Manager panel was reduced from `720 x 390` to `580 x 320` logical GUI pixels (about 20% smaller in each dimension).

Layout changes:
- `Amount` now sits beside `Recipe ID`.
- `Result` preview and `Count` are below the 5 x 5 pattern.
- The item picker is tighter and uses smaller inventory-style cells.
- Save / Enable-Disable / Restore / Remove Custom are grouped as a compact 2 x 2 action block.
- The recipe list, paging controls and origin legend are also tightened to fit the smaller panel.

### Compatibility

- No custom recipe persistence format changes.
- No custom knapping type persistence format changes.
- No network payload layout changes.
- Existing dev1.3 worlds remain compatible.

---

## SimpleKnapping 1.0.6-dev1.5 — Admin Settings GUI

Built on the dev1.4 source overlay.

### New admin command

`/simpleknapping settings`

Uses the same vanilla GAMEMASTER / OP-level permission gate as the recipe manager.

### Dedicated settings screen

A separate server-authoritative GUI now exposes three feature toggles:

1. **Wooden tools & weapons** — default **DISABLED**
   - Disabled: vanilla wooden sword/pickaxe/axe/shovel/hoe/spear recipes are removed on recipe reload.
   - Disabled: those wooden items are filtered from generated loot.
   - Enabled: normal recipe/loot behavior is allowed.

2. **Stone tools & weapons** — default **DISABLED**
   - Disabled: vanilla stone sword/pickaxe/axe/shovel/hoe/spear recipes are removed on recipe reload.
   - Disabled: those stone items are filtered from generated loot.
   - Enabled: normal recipe/loot behavior is allowed.

3. **Tree punching** — default **DISABLED**
   - Disabled: the existing Simple Knapping rule remains active; logs require an axe.
   - Enabled: logs may be broken without an axe.

Each toggle is saved immediately through the existing NeoForge common config values. The GUI state always comes from the server rather than trusting the client.

#### Existing config compatibility

No new persistence file/schema was introduced. The GUI maps onto the mod's existing settings:

- Wooden tools enabled = inverse of `hideWoodenToolRecipes`
- Stone tools enabled = inverse of `hideStoneToolRecipes`
- Tree punching enabled = inverse of `requireAxeForLogs`

This preserves the existing defaults (`hide... = true`, `requireAxeForLogs = true`) while presenting them in user-friendly positive feature toggles.

The old combined recipe/loot master switches are kept in `Config.java` for config-file compatibility, but the new GUI-controlled independent wooden/stone toggles are the runtime source of truth for those two tiers.

### Apply timing

- Tree punching changes take effect immediately.
- Loot filtering changes take effect for newly generated loot immediately.
- Wooden/stone vanilla recipe visibility is rebuilt by `ModifyRecipeJsonsEvent`, so use `/reload` after changing either of those two toggles to update the currently loaded recipe set.

The GUI explicitly shows this note.

### Networking

A dedicated `simpleknapping:settings` payload was added and registered under the existing recipe/admin payload registrar.

Recipe/admin network version:

- `recipe_editor_3` -> `recipe_editor_4`

No knapping recipe, custom type, or saved-data schema changed.

### Files added

- `network/SettingsPayload.java`
- `network/SettingsNetwork.java`
- `client/SettingsClientPayloadHandler.java`
- `client/screen/SimpleKnappingSettingsScreen.java`
- `README-DEV6.md`

### Files updated

- `Config.java`
- `SimpleKnapping.java`
- `SimpleKnappingClient.java`
- `command/SimpleKnappingCommands.java`
- `event/RecipeHideEvents.java`
- `event/ToolBreakEvents.java`
- `loot/RemoveToolLootModifier.java`
- `network/RecipeEditorNetwork.java`
- `gradle.properties`

### Validation

- Source-overlay Java files were passed through `javac` parsing; no syntax-like errors were found.
- Full NeoForge compilation was not possible from this overlay alone because the overlay intentionally does not contain the complete Gradle project/dependency tree.

---

## SimpleKnapping 1.0.6-dev1.6

### GUI size harmonization

This update only changes the layout/size of the admin GUIs.

- `KnappingRecipeEditorScreen`: unchanged at **580 x 320**.
- `KnappingTypeEditorScreen`: reduced from **720 x 390** to **580 x 320**.
- `SimpleKnappingSettingsScreen`: enlarged from **430 x 240** to **580 x 320**.

The Type Manager layout was compacted to fit the same footprint cleanly:

- same compact left list geometry/paging as the Recipe Manager;
- Type ID + default amount remain on one row;
- tool/material/fallback texture controls are narrower;
- registry picker uses 18 px tiles;
- actions are arranged as a compact 2 x 2 block;
- all mouse hitboxes were moved together with their visual controls.

The Settings screen now uses the same 580 x 320 outer panel and wider feature rows.

No networking, recipe storage, knapping-type storage, gameplay behavior, or settings persistence changes were made.

---

## SimpleKnapping 1.0.6-dev1.7 — Instant wooden/stone tier gating

Built on top of `1.0.6-dev1.6`.

### Goal

Wooden and stone tool/weapon toggles now take effect at runtime. `/reload` is no longer required.

When a tier is disabled, the vanilla items in that tier are intended to be unavailable through normal survival gameplay. The explicit escape hatches are commands and creative-inventory insertion. If a disabled item nevertheless exists in a survival inventory (for example through `/give`, an older world, or another mod inserting it directly), it is treated as inert.

### Affected vanilla items

Wooden tier:
- `minecraft:wooden_sword`
- `minecraft:wooden_pickaxe`
- `minecraft:wooden_axe`
- `minecraft:wooden_shovel`
- `minecraft:wooden_hoe`
- `minecraft:wooden_spear`

Stone tier:
- `minecraft:stone_sword`
- `minecraft:stone_pickaxe`
- `minecraft:stone_axe`
- `minecraft:stone_shovel`
- `minecraft:stone_hoe`
- `minecraft:stone_spear`

### Runtime behavior

#### Crafting / recipes

`RecipeHideEvents` no longer removes JSON recipes during datapack reload. Instead, `DisabledEquipmentSlotMixin` gates `Slot#mayPickup` on the authoritative server.

Consequences:
- disabled wooden/stone crafting outputs cannot be taken;
- ingredients are not consumed because the result cannot be picked up;
- enabling/disabling from `/simpleknapping settings` takes effect immediately;
- no `/reload`, world reload or server restart is required;
- the same slot gate also covers normal container extraction and normal merchant result slots.

The vanilla recipes remain registered. Because of that, recipe-book/recipe-viewer entries can still be visible even while the output is blocked. Visual recipe-book filtering can be added separately later without bringing back reload-based recipe mutation.

#### Loot and drops

Defense-in-depth acquisition blocking:
- the existing global loot modifier now uses the central runtime tier check and removes disabled items from generated loot;
- `LivingDropsEvent` removes disabled equipment from mob drops;
- `ItemEntityPickupEvent.Pre` denies ground pickup while the tier is disabled;
- normal container/result-slot extraction is denied by the slot mixin.

This means items generated by an unusual/modded path can exist temporarily in the world or a container, but a survival player cannot normally take/pick them up. A mod that writes directly into a player's inventory can bypass acquisition hooks; the inert-item fallback below covers that case.

#### Inert fallback

For non-creative players, a disabled wooden/stone item already present in inventory is prevented from functioning as equipment:
- entity attacks are canceled;
- block mining is canceled;
- left-click block item action is canceled;
- right-click item use is canceled;
- right-click block use is canceled;
- entity interaction with the item is canceled;
- disabled vanilla equipment cannot be used as a custom SimpleKnapping knapping/carving tool;
- disabled wooden equipment gets furnace burn time `0` so it cannot be used as fuel.

Creative players retain normal item functionality for testing. Commands can still insert the items directly into inventories. If a command gives one to a survival player, the item remains present but is inert while its tier is disabled.

### Server authority

Player interaction and slot restrictions intentionally make their decisions on the logical server. This avoids relying on a remote client's COMMON-config copy after an admin changes a toggle at runtime.

### Settings GUI

The old `/reload` wording is removed. The GUI now states that wooden/stone changes apply immediately, and the server response confirms that no reload is required.

### Compatibility

- Mod version: `1.0.6-dev1.7`
- Minecraft: `26.2`
- NeoForge: `26.2.0.6-beta`
- Java: `25`
- Recipe editor network version remains `recipe_editor_4` (no payload/schema change).
- Knapping recipe/type persistence formats are unchanged.
- Existing config keys are retained for compatibility.

### Mixin

The existing `simpleknapping.mixins.json` is updated to register:

`be.winnetrie.mod.simpleknapping.mixin.DisabledEquipmentSlotMixin`

The public repository already references `simpleknapping.mixins.json` from `neoforge.mods.toml`.

### Validation performed here

- changed JSON files parse successfully;
- delimiter/syntax sanity checks passed for changed Java sources;
- stale `/reload` recipe-setting wording removed;
- `RecipeHideEvents` is no longer registered and no longer contains `ModifyRecipeJsonsEvent` logic;
- archive integrity tested after packaging.

A full NeoForge Gradle compile was not available in this overlay-only environment, so the build should still be compiled in the real SimpleKnapping project before runtime testing.

---

## SimpleKnapping 1.0.6-dev1.8

Built on `1.0.6-dev1.7`.

### 1. Disabled vanilla crafting results are now truly empty

Wooden and stone tools/weapons remain registered recipes so the admin settings can switch them at runtime without `/reload`, but disabled crafting results are now hidden directly at the `ResultSlot` boundary.

- disabled wooden/stone recipe -> crafting output slot is visually and logically empty;
- enabling the tier reveals the already-known vanilla result immediately;
- disabling the tier hides it immediately, even if the crafting grid was already filled;
- no recipe/resource reload is required;
- the existing survival acquisition/use restrictions from dev1.7 remain in place.

The tier state is synchronized silently to every client on login and whenever an admin changes the setting, so dedicated-server clients render the same empty/non-empty result state as the server.

### 2. Public Knapping Recipe Guide

New command, available to normal players:

`/simpleknapping guide`

This opens a read-only 580x320 recipe browser containing the currently usable SimpleKnapping recipes loaded by the server, including resource/datapack recipes, server custom recipes and overrides.

The guide includes:

- search by result, material, tool, type or recipe id;
- paged recipe list with result icons;
- result item and amount;
- friendly knapping type name;
- required knapping tool;
- per-recipe material and material amount;
- the complete 5x5 target pattern;
- material block texture when available, otherwise the knapping type fallback texture and finally the fixed clay fallback;
- clear hint that bright cells remain and dark cells are removed.

Recipes that are disabled, belong to a disabled knapping type, or currently produce a disabled wooden/stone progression item are not advertised to normal players.

### Networking

Payload registration version: `recipe_editor_5` (was `recipe_editor_4`).

The existing settings payload now also has a silent `state` form used only to synchronize wooden/stone tier state without opening the admin GUI.

### Persistence / recipe format

No persistence schema or knapping recipe/type format changes in this build.

### Validation performed

- Java source brace/syntax structure scan passed.
- NeoForge 26.2 primary source was checked for `PlayerEvent.PlayerLoggedInEvent` and `ResultSlot` availability.
- ZIP integrity checked after packaging.
- A full Gradle/NeoForge compile is still required in the real project because this development artifact is a source overlay and does not contain the complete dependency-resolved project tree.

---

## SimpleKnapping 1.0.6-dev1.9 — Flint replacement progression

Built on top of `1.0.6-dev1.8`.

### Goal

When the Wooden tools & weapons and/or Stone tools & weapons setting is disabled,
those vanilla items are no longer simply deleted or blocked when they appear in
normal survival acquisition sources. They are replaced by the matching
SimpleKnapping flint progression item.

Vanilla crafting remains intentionally different: disabled wooden/stone crafting
recipes still show an EMPTY output, so players must use the knapping progression.
The toggle remains runtime/instant and does not require `/reload`.

### Replacement map

Both wooden and stone variants use the same flint counterpart:

- Wooden Sword / Stone Sword -> `simpleknapping:flint_knife_blade`
- Wooden Pickaxe / Stone Pickaxe -> `simpleknapping:flint_pickaxe`
- Wooden Axe / Stone Axe -> `simpleknapping:flint_axe`
- Wooden Shovel / Stone Shovel -> `simpleknapping:flint_shovel`
- Wooden Hoe / Stone Hoe -> `simpleknapping:flint_hoe`
- Wooden Spear / Stone Spear -> `simpleknapping:flint_spear`

The sword replacement deliberately uses the existing flint blade component,
not the finished Flint Knife.

### Acquisition paths

#### Generated loot

The existing Global Loot Modifier now rewrites generated ItemStacks in-place
instead of deleting disabled items. This covers normal loot-table based sources
such as generated chest loot, fishing and entity loot tables.

#### Mob / entity drops

`LivingDropsEvent` now converts any disabled wooden/stone stacks to their flint
counterpart. This is an extra safety net for stacks injected by other mods after
normal loot generation.

#### Ground item pickup

`ItemEntityPickupEvent.Pre` no longer blocks pickup. If an old/mod-added disabled
vanilla tool exists as an ItemEntity, the ItemEntity is converted to the flint
counterpart before pickup.

#### Containers / old-world stored items

Non-player `Slot`s render disabled stored wooden/stone equipment as the flint
counterpart. Before extraction the server-side backing container is converted as
well. This makes old chest/container contents migrate lazily when accessed.

The player's own `Inventory` is excluded so `/give` and Creative Inventory can
still create and retain the original vanilla items as explicitly allowed.

#### Villager / wandering trader offers

`MerchantOffer#getResult()` and `MerchantOffer#assemble()` are runtime-rewritten.
The trade is therefore displayed as the flint replacement and the actual traded
output is also the flint replacement. The underlying offer is not permanently
changed, so re-enabling the tier immediately restores the vanilla result.

### Fallback / command-created originals

If a disabled wooden/stone item is deliberately introduced through commands,
Creative, an older player inventory or an unsupported direct-inventory mod path,
the existing inert-item safety remains active for non-creative players:
attacking, mining, interactions and furnace fuel use remain blocked while the
tier is disabled.

### Compatibility

- Mod version: `1.0.6-dev1.9`
- Minecraft: `26.2`
- NeoForge target remains `26.2.0.6-beta`
- Admin/network protocol unchanged from dev1.8 (`recipe_editor_5`)
- No recipe/type/settings persistence schema changes
- Existing admin GUI layout unchanged
- Existing Recipe Guide unchanged

### Validation

The changed 26.2 hooks were checked against the current NeoForge/Minecraft 26.2
API surface used by the project:

- `Slot#getItem`, `Slot#mayPickup`, `Slot#remove`, `Slot#set`, `Slot#container`
- `MerchantOffer#getResult`, `MerchantOffer#assemble`
- `ItemEntity#getItem`, `ItemEntity#setItem`
- the existing Global Loot Modifier pipeline

The delivered archive is still a source overlay, so a full NeoForge Gradle build
must be run in the complete SimpleKnapping project before runtime testing.

---

## SimpleKnapping 1.0.6-dev1.9.1 — Recipe Guide cleanup

Built on top of `1.0.6-dev1.9`.

### Change

Removed the two redundant grey instruction lines underneath the recipe details in the public Knapping Recipe Guide:

- `Bright tiles remain; dark tiles must be removed.`
- `Start with the shown tool + material, then match the pattern.`

The 5x5 pattern, tool, material and result information remain unchanged.

### Compatibility

- Mod version: `1.0.6-dev1.9.1`
- Minecraft: `26.2`
- NeoForge target: `26.2.0.6-beta`
- Network protocol unchanged (`recipe_editor_5`)
- No persistence/schema changes
- No gameplay/progression changes

---

## SimpleKnapping 1.0.6-dev1.9.2 — Flint Knife + visible mob equipment hotfix

Built on top of `1.0.6-dev1.9.1`.

### Fixes

- Corrected Wooden Sword / Stone Sword replacement:
  - before: `simpleknapping:flint_knife_blade`
  - now: `simpleknapping:flint_knife`
- `flint_knife_blade` remains a crafting component only and is no longer used as a progression replacement.
- Disabled wooden/stone equipment carried by mobs is now replaced in the mob equipment slot itself.
- A Wither Skeleton therefore spawns visibly holding a Flint Knife when Stone Tools & Weapons are disabled.
- The existing drop/loot/container/trade replacement paths continue to use the same central mapping, so sword drops now also become Flint Knives.
- Added a server-side equipment-change fallback so mobs that equip/pick up a disabled wooden/stone item after spawning are converted too.

### Compatibility

- Mod version: `1.0.6-dev1.9.2`
- No persistence changes.
- No network protocol changes.
- No recipe/type schema changes.

---

---

# Minecraft 1.21.1 Backport Development History


---

# SimpleKnapping 1.1.0 — Minecraft 1.21.1 Backport (dev1)

First compile-pass backport of the current SimpleKnapping 1.1.0 source to Minecraft 1.21.1.

## Target environment

- Minecraft: **1.21.1**
- NeoForge: **21.1.235**
- Java: **21**
- Mod version: **1.1.0**

## Backport changes in dev1

- Replaced the newer `Identifier` API with the 1.21.1 `ResourceLocation` API.
- Backported the custom GUI screens to the 1.21.1 `GuiGraphics` / mouse input APIs.
- Backported recipe/settings/guide networking to the NeoForge 1.21.1 payload API.
- Backported admin permission checks to the 1.21.1 command permission API.
- Backported block-break listeners to `BlockEvent.BreakEvent`.
- Backported custom recipe/type SavedData persistence to the 1.21.1 SavedData API.
- Backported item/tool registrations to the 1.21.1 `Tier` / `SimpleTier` item API.
- Removed the newer client item-definition resource layer that is not used by 1.21.1; classic item models remain.
- Updated NeoForge/Minecraft metadata for the 1.21.1 branch.
- Corrected the `c` item-tag folder layout for 1.21.1.

## Version-specific progression changes

### Spears removed

Minecraft 1.21.1 has no vanilla Spear. The 1.21.1 branch therefore contains no spear progression or spear-specific compatibility:

- Flint Spear and Flint Spear Head registration removed.
- Spear recipes, knapping recipe, models, textures and translation keys removed.
- Wooden/Stone Spear gating/replacement mappings removed.
- Spear item tags removed.

### Flint Pickaxe mines iron ore

The 1.21.1 progression has no vanilla copper-tool tier. The Flint tool tier therefore uses **stone-level harvesting** in this branch. The Flint Pickaxe can harvest **iron ore**, allowing Flint -> Iron progression.

The custom `simpleknapping:incorrect_for_flint_tool` block tag inherits `#minecraft:incorrect_for_stone_tool`.

## Features intentionally preserved

Existing SimpleKnapping gameplay systems remain in the backport, including Plant Fiber drops, Stick drops, tree-punching restrictions, custom knapping recipes/types, admin settings, the player Recipe Guide, and wooden/stone equipment progression replacement.

## Validation performed

- No remaining `spear` references under `src/main`.
- No copper equipment mappings or registrations were introduced.
- 47 JSON resource/data files parse successfully.
- First-pass scans for known 26.2-only API names were performed.

A full NeoForge compile cannot be completed in the preparation environment because the Gradle distribution/dependencies are not available offline. Additional 26.2 -> 1.21.1 API differences may therefore appear in the next local compile.

## Next test

Run from the 1.21.1 branch/project root:

```cmd
gradlew.bat compileJava --console=plain > compile-1.21.1-dev1.log 2>&1
```

If it fails, use that new compiler log for the next backport pass.

---

# SimpleKnapping 1.1.0 — Minecraft 1.21.1 Backport (dev2)

Second compile-pass backport, built on dev1 after the first local 1.21.1 compiler log.

## Target
- Minecraft 1.21.1
- NeoForge 21.1.235
- Java 21

## dev2 compile fixes
- Backported `SimpleJsonResourceReloadListener` use to the 1.21.1 Gson + directory constructor.
- Switched reload registration to NeoForge 1.21.1 `AddReloadListenerEvent` and its single-listener `addListener(...)` API.
- Replaced post-1.21.1 registry `getValue(ResourceLocation)` calls with the 1.21.1 registry `get(ResourceLocation)` API.
- Backported `KnappingToolItem#use` to return `InteractionResultHolder<ItemStack>`.
- Backported the Global Loot Modifier constructor/codec to the 1.21.1 conditions-only `LootModifier` API.
- Added the 1.21.1-required `data/neoforge/loot_modifiers/global_loot_modifiers.json` index so the replacement GLM is actually loaded.

## Version-specific progression retained
- All vanilla/newer spear references remain removed.
- No copper-tool assumptions are present.
- Flint Pickaxe remains stone-level for harvesting and can mine iron ore.
- Existing Plant Fiber and Stick drop systems remain included.

## Next test
```cmd
gradlew.bat compileJava --console=plain > compile-1.21.1-dev2.log 2>&1
```

---

# SimpleKnapping 1.1.0 – Minecraft 1.21.1 dev2.1

## GUI blur hotfix

Minecraft 1.21.1 uses the older screen background pipeline where `Screen#renderBackground` can invoke the vanilla accessibility blur. SimpleKnapping's custom screens already render their own backdrop, so the vanilla blurred pass is now suppressed for them.

Changed screens:
- Knapping Recipe Manager
- Knapping Type Manager
- Simple Knapping Settings
- Knapping Recipe Guide
- Knapping container (uses a simple translucent dim instead of the vanilla blur)

No gameplay, recipe, progression, networking, persistence, spear-removal, or Flint Pickaxe harvesting behavior changed.

---

# SimpleKnapping 1.1.0 - Minecraft 1.21.1 dev2.2

## Crafting recipe compatibility hotfix

- Converted all shaped crafting-recipe ingredient keys to the Minecraft 1.21.1 ingredient object format (`{"item":"namespace:id"}`).
- Fixes the Flint Knapping Tool recipe not appearing in the crafting output.
- Applies the same compatibility correction to Flint Axe, Shovel, Pickaxe, Knife, Hoe, Plant Fiber Bundle and Plant Fiber recipes.
- No gameplay balance changes.
- Spear content remains removed for Minecraft 1.21.1.
- Flint Pickaxe remains configured at stone-tier harvesting level so it can mine iron ore.

---

# SimpleKnapping 1.1.0 - Minecraft 1.21.1 dev2.3

## GUI API + IDE diagnostics cleanup

- Corrected all custom screen `renderBackground` overrides to the Minecraft 1.21.1 signature with mouse coordinates and partial tick.
- Keeps the custom SimpleKnapping backgrounds sharp without invoking the vanilla blurred background pass.
- Added explicit null guards around the nullable `Screen.minecraft` / `gameMode` references used for navigation and knapping clicks.
- Localized Eclipse/JDT `null` warning suppression to the five GUI screen classes. These warnings are annotation-interop noise at calls into Mojang/NeoForge GUI APIs (for example `Font`, `Component`, `EditBox` and payload parameters), not ignored Java compiler errors.
- No gameplay, recipe, persistence or network protocol changes.
- Spear content remains removed for Minecraft 1.21.1.
- Flint Pickaxe remains stone-tier capable so it can harvest iron ore.

---

# SimpleKnapping 1.1.0 — Minecraft 1.21.1 dev2.4

Diagnostics cleanup pass based on the exported VS Code Problems list after dev2.3.

## Fixed

- Removed two dead-code checks that were impossible on the 1.21.1 registry API.
- Removed one unused import.
- Removed three unused local variables while preserving their validation calls.
- Added an explicit null guard around `Level#getServer()` in `AdvancementHelper`.
- Scoped Eclipse/JDT `null` warning suppression to the 34 non-GUI classes that only produced Minecraft/NeoForge annotation-interoperability warnings.
  - This is intentionally local, not a project-wide compiler suppression.
  - Explicit potential-null warnings are still fixed in code rather than hidden.
- Existing GUI-local null warning suppression from dev2.3 is retained.

## Validation

- 48 JSON files parse successfully.
- No `spear` references remain under `src/main`.
- Flint Pickaxe progression remains unchanged (stone-tier harvesting / can mine iron ore).
- No gameplay, persistence, networking protocol, or recipe behavior changes in this cleanup pass.

After replacing dev2.3 with dev2.4, run `gradlew.bat compileJava --console=plain` and then `Java: Clean Java Language Server Workspace` in VS Code. The two remaining severity-2 “build file has been changed” notices in the supplied export are IDE reload notices and should disappear after the project reload.

---

# SimpleKnapping 1.1.0 — Minecraft 1.21.1 dev2.5

## Knapping GUI render hotfix

The 1.21.1 backport could show the container labels and slot contents while the actual knapping panel and 5x5 surface were missing.

Changes:
- Moved the visual knapping panel/grid rendering to the 1.21.1 `renderBackground(...)` path that is known to execute before the foreground contents in this screen.
- Kept vanilla blur disabled.
- Added a solid panel fallback behind `textures/gui/knapping.png`, so the container cannot become completely invisible if a GUI texture is unavailable/overridden.
- Kept `renderBg(...)` as an intentional no-op to avoid double-rendering the panel.
- No gameplay, recipe, networking, persistence, progression, spear, copper, or flint-pickaxe changes.

---

# SimpleKnapping 1.1.0 — Minecraft 1.21.1 dev2.6

Knapping GUI presentation hotfix.

- Removed the opaque grey fallback rectangle that dev2.5 rendered behind the knapping texture.
- The 1.21.1 render-path fix remains active, so the knapping panel and 5x5 tiles still render.
- Intentional transparency in `textures/gui/knapping.png` is respected again.
- Vanilla blur remains disabled.
- No gameplay, recipe, progression, networking, persistence, spear, or flint-pickaxe changes.
