# Minecraft AI Terminal Mod — Execution Spec
> Handoff prompt for Claude Code. Execute this spec top to bottom. Do not skip sections.

---

## Project Summary

Build a Minecraft mod called **AI Terminal** that adds a placeable in-world terminal block. When a player right-clicks the block, a GUI opens with a text input and scrollable output area. The player types a question; the mod sends it to a configured OpenWebUI instance with `features.web_search: true` enabled, and displays the response in the terminal. The mod supports **Fabric**, **Forge**, and **NeoForge** from a single multi-module Gradle project with no Architectury dependency.

---

## Environment Setup (Do This First)

Before writing any mod code, set up the dev environment:

1. **Install Java 21 (Temurin recommended)**
   - macOS: `brew install --cask temurin@21`
   - Windows: Download from https://adoptium.net/temurin/releases/?version=21
   - Linux: `sudo apt install temurin-21-jdk` (or equivalent)
   - Verify: `java -version` should show `21.x`

2. **Install Gradle 8.x** (or rely on the Gradle wrapper — preferred)
   - The project will use `./gradlew` via wrapper, so no system Gradle install is strictly required

3. **Install IntelliJ IDEA Community** (recommended IDE)
   - https://www.jetbrains.com/idea/download/
   - Install the **Minecraft Development** plugin (File → Plugins → search "Minecraft Development")

4. **Git init the project**
   ```bash
   mkdir ai-terminal-mod
   cd ai-terminal-mod
   git init
   ```

---

## Project Structure

Scaffold this exact directory layout:

```
ai-terminal-mod/
├── settings.gradle
├── build.gradle                  # Root build — shared config
├── gradle.properties             # Shared version constants
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── common/                       # Pure Java — zero Minecraft imports
│   ├── build.gradle
│   └── src/main/java/com/aiterminal/common/
│       ├── config/
│       │   └── AITerminalConfig.java
│       ├── api/
│       │   ├── OpenWebUIClient.java
│       │   └── ChatResponse.java
│       └── AITerminalConstants.java
├── fabric/
│   ├── build.gradle
│   ├── src/main/resources/
│   │   ├── fabric.mod.json
│   │   └── assets/aiterminal/
│   │       ├── blockstates/terminal.json
│   │       ├── models/block/terminal.json
│   │       └── lang/en_us.json
│   └── src/main/java/com/aiterminal/fabric/
│       ├── AITerminalFabric.java
│       ├── block/
│       │   ├── TerminalBlock.java
│       │   └── TerminalBlockEntity.java
│       ├── screen/
│       │   ├── TerminalScreen.java
│       │   └── TerminalScreenHandler.java
│       └── registry/
│           └── ModRegistry.java
├── forge/
│   ├── build.gradle
│   ├── src/main/resources/
│   │   └── META-INF/
│   │       └── mods.toml
│   └── src/main/java/com/aiterminal/forge/
│       ├── AITerminalForge.java
│       ├── block/
│       │   ├── TerminalBlock.java
│       │   └── TerminalBlockEntity.java
│       ├── screen/
│       │   ├── TerminalScreen.java
│       │   └── TerminalMenu.java
│       └── registry/
│           └── ModRegistry.java
└── neoforge/
    ├── build.gradle
    ├── src/main/resources/
    │   └── META-INF/
    │       └── neoforge.mods.toml
    └── src/main/java/com/aiterminal/neoforge/
        ├── AITerminalNeoForge.java
        ├── block/
        │   ├── TerminalBlock.java
        │   └── TerminalBlockEntity.java
        ├── screen/
        │   ├── TerminalScreen.java
        │   └── TerminalMenu.java
        └── registry/
            └── ModRegistry.java
```

---

## Version Targets

| Platform   | Minecraft Version | Loader Version       | Java |
|------------|-------------------|----------------------|------|
| Fabric     | 1.21.1 – 1.21.8   | Fabric Loader 0.16+  | 21   |
| Forge      | 1.21.1            | Forge 51.0+          | 21   |
| NeoForge   | 1.21.1 – 1.21.8   | NeoForge 21.1+       | 21   |

Primary test target: **NeoForge 1.21.1** (this is what the end user runs with FTB Evolution).
Secondary: Fabric 1.21.1. Forge 1.21.1 is included for reach.

---

## gradle.properties (Shared Constants)

```properties
# Project
mod_version=1.0.0
mod_id=aiterminal
mod_name=AI Terminal
mod_group=com.aiterminal

# Java
java_version=21

# Minecraft
mc_version=1.21.1

# Fabric
fabric_loader_version=0.16.10
fabric_api_version=0.107.0+1.21.1

# Forge
forge_version=1.21.1-51.0.1

# NeoForge
neoforge_version=21.1.0
```

---

## Module: common

This module has **no Minecraft imports**. It is plain Java 21.

### AITerminalConstants.java
```java
package com.aiterminal.common;

public final class AITerminalConstants {
    public static final String MOD_ID = "aiterminal";
    public static final String MOD_NAME = "AI Terminal";
}
```

### AITerminalConfig.java

Stores all user-configurable values. Must be readable/writable as a simple `.properties` or JSON file in the game's config directory. These are the **required configurable fields**:

| Field | Default | Description |
|-------|---------|-------------|
| `openwebui_base_url` | `http://localhost:3000` | Base URL of the OpenWebUI instance |
| `openwebui_api_key` | `""` | Bearer token for OpenWebUI API auth |
| `openwebui_model` | `""` | Model ID as it appears in OpenWebUI |
| `web_search_enabled` | `true` | Whether to send `features.web_search: true` |
| `max_response_length` | `2000` | Truncate responses longer than this in the terminal display |
| `system_prompt` | (see below) | System prompt injected with every request |

**Default system prompt:**
```
You are a helpful Minecraft assistant with access to web search. 
Answer questions about Minecraft gameplay, crafting, mods, and mechanics accurately and concisely. 
When referencing mod-specific content, note which mod it belongs to.
Keep answers focused and practical for an in-game player.
```

Config is loaded once at mod init. To apply changes, the user edits the file and restarts the game. The config file must be auto-created with all defaults and inline comments on first launch if not present.

### OpenWebUIClient.java

Handles all HTTP communication. Key requirements:

- Use `java.net.http.HttpClient` (Java 11+, no external HTTP libraries)
- All requests must be **async** — use `CompletableFuture` so the game thread is never blocked
- Request format:

```json
POST {base_url}/api/chat/completions
Authorization: Bearer {api_key}
Content-Type: application/json

{
  "model": "{model}",
  "messages": [
    {"role": "system", "content": "{system_prompt}"},
    {"role": "user", "content": "{player_question}"}
  ],
  "features": {
    "web_search": true
  },
  "stream": false
}
```

- Parse the response and extract `choices[0].message.content`
- Handle error cases gracefully: connection refused, 401 unauthorized, 429 rate limit, malformed JSON — return a human-readable error string to display in the terminal rather than throwing
- Timeout: 30 seconds

### ChatResponse.java

Simple POJO / record to hold:
- `String content` — the model's response text
- `boolean success` — whether the call succeeded
- `String errorMessage` — populated on failure

Use minimal JSON parsing — `org.json` or manual string parsing is fine. Avoid pulling in Gson/Jackson unless the platform module already has it available (NeoForge ships with Gson, so you can use that in the NeoForge module but not common).

---

## Module: fabric

### AITerminalFabric.java (main entrypoint)
- Implements `ModInitializer`
- Calls `ModRegistry.register()` on init

### TerminalBlock.java
- Extends `BlockWithEntity`
- `RenderType`: cutout (for visual flexibility later)
- On right-click (`onUse`): open the terminal screen
- Has a `BlockEntityProvider` returning `TerminalBlockEntity`

### TerminalBlockEntity.java
- Extends `BlockEntity`
- Stores:
  - `List<String> outputHistory` — last N lines of conversation (cap at 50 entries)
  - `boolean isLoading` — true while waiting for API response
- Does NOT persist history to NBT (fresh on each world load is fine for v1)

### TerminalScreen.java
- Extends `HandledScreen`
- Layout:
  - Scrollable output area (top ~75% of screen) showing conversation history
  - Single-line text input at the bottom
  - "Ask" button or Enter key submits
  - "Clear" button resets history
  - While `isLoading` is true, show a blinking `...` or `Querying...` indicator in the output area
- Color scheme: dark background (`#1a1a1a`), green text (`#00ff00`) for responses, white for player input, yellow for errors — retro terminal aesthetic
- Word-wrap long lines to fit the output area width

### ModRegistry.java
- Register block, block entity, screen handler, and item (for placing the block)
- Block ID: `aiterminal:terminal`
- Item group: Technology / Redstone (or standalone creative tab)

### fabric.mod.json
```json
{
  "schemaVersion": 1,
  "id": "aiterminal",
  "version": "${version}",
  "name": "AI Terminal",
  "description": "A placeable terminal block that queries a local AI via OpenWebUI.",
  "authors": [],
  "contact": {},
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.aiterminal.fabric.AITerminalFabric"]
  },
  "depends": {
    "fabricloader": ">=0.16.0",
    "fabric-api": "*",
    "minecraft": "~1.21"
  }
}
```

---

## Module: neoforge

### AITerminalNeoForge.java
- Annotated with `@Mod(AITerminalConstants.MOD_ID)`
- Registers event listeners in constructor
- Calls `ModRegistry.register()` on mod construction

### TerminalBlock.java
- Extends `BaseEntityBlock`
- `getRenderShape()` returns `RenderShape.MODEL`
- Override `use()` to open the terminal menu/screen
- Returns `TerminalBlockEntity` from `newBlockEntity()`

### TerminalBlockEntity.java
- Extends `BlockEntity`
- Same state as Fabric version: `outputHistory`, `isLoading`
- Implements `MenuProvider` to support screen opening

### TerminalScreen.java
- Extends `AbstractContainerScreen`
- Same visual design as Fabric: retro green-on-black terminal
- Same UX: scrollable output, text input, Enter/Ask button, Clear button, loading indicator

### TerminalMenu.java
- Extends `AbstractContainerMenu`
- Used to open the screen from server-side via `NetworkHooks.openScreen` (Forge) / `player.openMenu()` (NeoForge)

### ModRegistry.java
- Use `DeferredRegister` for blocks, block entities, items, and menu types
- Register all deferred registers to the mod event bus in the main mod class

### neoforge.mods.toml
```toml
modLoader="javafml"
loaderVersion="[21,)"
license="MIT"

[[mods]]
modId="aiterminal"
version="${file.jarVersion}"
displayName="AI Terminal"
description="A placeable terminal block that queries a local AI via OpenWebUI."

[[dependencies.aiterminal]]
    modId="neoforge"
    type="required"
    versionRange="[21.1,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.aiterminal]]
    modId="minecraft"
    type="required"
    versionRange="[1.21.1,)"
    ordering="NONE"
    side="BOTH"
```

---

## Module: forge

Mirror of NeoForge with these differences:
- Main class uses `@Mod` from `net.minecraftforge.fml.common.Mod`
- `DeferredRegister` from `net.minecraftforge.registries`
- Screen opening uses `NetworkHooks.openScreen()`
- `mods.toml` targets Forge 51+ and Minecraft 1.21.1

---

## Configuration

All configuration is done **exclusively via the config file**. There is no in-game configuration UI or commands. Users edit the file in a text editor and restart the game to apply changes.

### Config File
- **Location:** `config/aiterminal.json` in the game directory (same folder as `options.txt`)
- **Auto-created** with all defaults and explanatory inline comments on first launch if not present
- **Format:** JSON
- **To apply changes:** edit the file, save, restart Minecraft

### Example aiterminal.json
```json
{
  "_comment": "AI Terminal configuration. Edit this file and restart Minecraft to apply changes.",
  "openwebui_base_url": "http://localhost:3000",
  "openwebui_api_key": "your-api-key-here",
  "openwebui_model": "your-model-name-here",
  "web_search_enabled": true,
  "max_response_length": 2000,
  "system_prompt": "You are a helpful Minecraft assistant with access to web search. Answer questions about Minecraft gameplay, crafting, mods, and mechanics accurately and concisely. When referencing mod-specific content, note which mod it belongs to. Keep answers focused and practical for an in-game player."
}
```

All fields are required to be present. If any field is missing or malformed, the mod logs a clear error to the game log and uses the default value for that field rather than crashing.

---

## Crafting Recipe

Register a shaped recipe for the terminal block:

```
[ I ] [ I ] [ I ]
[ I ] [ R ] [ I ]
[ I ] [ I ] [ I ]
```
- `I` = Iron Ingot
- `R` = Redstone Dust

This makes it obtainable in survival without being too expensive, and fits the tech aesthetic. Recipe file goes in each module's `resources/data/aiterminal/recipes/terminal.json`.

---

## Mod Compatibility Requirements

- **No hard dependencies** beyond the loader (Fabric API, NeoForge, Forge) — zero required third-party mods
- **No mixin conflicts**: avoid mixins entirely for v1. All functionality through standard block/screen/registry APIs
- **No client-only crash**: the block entity and API client must be safely skipped server-side if dedicated server (terminal GUI is client-only, but the block itself must be registerable on a dedicated server without crashing)
- **FTB Evolution compatibility**: do not register items or blocks in namespaces that conflict with FTB mods. Use `aiterminal:` namespace exclusively
- Test the NeoForge build against a basic FTB Evolution instance before considering it done

---

## Build & Output

Each module should produce a separate `.jar`:
- `fabric/build/libs/aiterminal-fabric-1.0.0.jar`
- `forge/build/libs/aiterminal-forge-1.0.0.jar`
- `neoforge/build/libs/aiterminal-neoforge-1.0.0.jar`

Root `build.gradle` should have a `buildAll` task that builds all three.

---

## Execution Order for Claude Code

Execute in this order:

1. Scaffold the full directory structure and all `build.gradle` / `gradle.properties` / `settings.gradle` files
2. Implement the `common` module completely (`Config`, `OpenWebUIClient`, `ChatResponse`)
3. Implement NeoForge module (primary target — test this first)
4. Implement Fabric module
5. Implement Forge module
6. Add crafting recipes to all three
7. Verify `./gradlew buildAll` produces three `.jar` files without errors
8. Produce a `README.md` with installation instructions, OpenWebUI setup requirements, and config field documentation

---

## Definition of Done

- [ ] `./gradlew buildAll` completes without errors
- [ ] NeoForge `.jar` loads in Minecraft 1.21.1 NeoForge without crashing
- [ ] Terminal block is craftable and placeable
- [ ] Right-clicking the block opens the terminal GUI
- [ ] Typing a question and pressing Enter sends a request to OpenWebUI with `features.web_search: true`
- [ ] Response appears in the terminal output area
- [ ] Errors (bad URL, bad key, timeout) display a readable message in the terminal rather than crashing
- [ ] `config/aiterminal.json` is auto-created with defaults and comments on first launch
- [ ] Changing the config file and restarting the game applies the new values correctly
- [ ] No crash on dedicated server (block registers, GUI simply never opens server-side)
