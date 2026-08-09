# SimpleKnapping 1.0.6-dev1.1 – compile hotfix

Fixes the six compile errors reported after the first Custom Recipe Manager overlay.

## Minecraft 26.2 API fixes

- `Minecraft.screen` -> `Minecraft.gui.screen()`
- `Minecraft#setScreen(...)` -> `Minecraft#setScreenAndShow(...)`
- `ServerPlayer#getServer()` -> `ServerPlayer#level().getServer()`

No recipe format, networking payload format, storage schema, or gameplay behavior was intentionally changed by this hotfix.
