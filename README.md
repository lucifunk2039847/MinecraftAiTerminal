# AI Terminal

A Minecraft mod that adds a placeable **terminal block**. Right-click it to open a retro
green-on-black terminal GUI, type a question, and the mod queries a configured
[OpenWebUI](https://github.com/open-webui/open-webui) instance (with web search enabled) and
shows the answer in-game.

Built from a single multi-module Gradle project for **Fabric**, **Forge**, and **NeoForge** —
no Architectury, no mixins, and no required third-party mods.

| Platform | Minecraft | Loader | Java |
|----------|-----------|--------|------|
| Fabric   | 1.21.1    | Fabric Loader 0.16+ | 21 |
| Forge    | 1.21.1    | Forge 52.x          | 21 |
| NeoForge | 1.21.1    | NeoForge 21.1.x     | 21 |

> **Primary / best-tested target:** NeoForge 1.21.1 (for FTB Evolution).

---

## Table of contents

1. [Quick start](#quick-start)
2. [OpenWebUI prerequisites](#openwebui-prerequisites)
3. [Installation](#installation)
4. [Building from source](#building-from-source)
5. [Configuration (`aiterminal.json`)](#configuration-aiterminaljson)
6. [Using the terminal in-game](#using-the-terminal-in-game)
7. [Troubleshooting](#troubleshooting)
8. [Project layout & toolchain notes](#project-layout--toolchain-notes)

---

## Quick start

1. Install and run an OpenWebUI instance, create an **API key**, and note a **model ID**
   (see [OpenWebUI prerequisites](#openwebui-prerequisites)).
2. Drop the right jar for your loader into your `mods/` folder and launch the game **once**.
3. Edit `config/aiterminal.json` (created on first launch) — set `openwebui_base_url`,
   `openwebui_api_key`, and `openwebui_model` — then **restart** Minecraft.
4. Craft the **AI Terminal** block (8 iron + 1 redstone), place it, right-click it, ask away.

---

## OpenWebUI prerequisites

The mod does **not** run any AI itself. It is a thin client for an OpenWebUI server that you
host (locally or remotely). Before the mod can answer anything you need:

1. **A running OpenWebUI instance.** For example, with Docker:
   ```bash
   docker run -d -p 3000:8080 \
     -v open-webui:/app/backend/data \
     --name open-webui ghcr.io/open-webui/open-webui:main
   ```
   It is then reachable at `http://localhost:3000`.

2. **At least one model available** in OpenWebUI (e.g. an Ollama model such as `llama3.1:8b`,
   or any model you have connected). The **model ID** is the exact string OpenWebUI shows for
   that model — this is what goes in `openwebui_model`.

3. **An API key.** In OpenWebUI: **Settings → Account → API Keys → Create new key**. Copy the
   token into `openwebui_api_key`. (If your instance allows unauthenticated API access you can
   leave the key blank, but a key is strongly recommended.)

4. **Web search enabled (recommended).** The mod sends `features.web_search: true` with every
   request so the model can use OpenWebUI's web search. For this to actually search the web you
   must configure a web-search provider in OpenWebUI: **Admin Settings → Web Search** (enable it
   and pick/configure an engine such as DuckDuckGo, SearXNG, Google PSE, etc.). If web search is
   not configured server-side, requests still work — the model simply answers without it.

5. **Network reachability.** The Minecraft client must be able to reach the OpenWebUI URL. The
   request goes out from whichever side opens the GUI (the player's client), so the URL must be
   resolvable from the player's machine.

The request the mod sends looks like:

```http
POST {openwebui_base_url}/api/chat/completions
Authorization: Bearer {openwebui_api_key}
Content-Type: application/json

{
  "model": "{openwebui_model}",
  "messages": [
    { "role": "system", "content": "{system_prompt}" },
    { "role": "user",   "content": "{your question}" }
  ],
  "features": { "web_search": true },
  "stream": false
}
```

The answer is read from `choices[0].message.content`. Requests time out after **30 seconds**.

---

## Installation

1. Make sure you have the matching loader installed for Minecraft **1.21.1**:
   - **Fabric:** install Fabric Loader **and** the [Fabric API](https://modrinth.com/mod/fabric-api) mod.
   - **Forge:** install Forge **52.x** for 1.21.1.
   - **NeoForge:** install NeoForge **21.1.x**.
2. Copy the jar for your loader into the game's `mods/` folder:
   - `aiterminal-fabric-1.0.0.jar`
   - `aiterminal-forge-1.0.0.jar`
   - `aiterminal-neoforge-1.0.0.jar`
3. Launch the game once to generate `config/aiterminal.json`, then configure it (below) and
   restart.

The mod is safe to install on a **dedicated server** — the block registers normally; the
terminal GUI is simply never opened server-side. (Only the player's client opens the GUI and
makes the HTTP request.)

There are **no required dependencies** other than the loader (and Fabric API on Fabric). The
mod uses only the `aiterminal:` namespace, so it will not clash with FTB Evolution or other packs.

---

## Building from source

Requirements: **JDK 21** on your `PATH`. The Gradle wrapper handles everything else (it pins
Gradle 8.14.3); no system Gradle install is needed. All loader SDKs are resolved at build time,
so the first build downloads a lot and can take several minutes.

```bash
# Build all three platform jars at once:
./gradlew buildAll

# Or build a single platform:
./gradlew :neoforge:build
./gradlew :fabric:build
./gradlew :forge:build
```

Output jars:

| Platform | Path |
|----------|------|
| Fabric   | `fabric/build/libs/aiterminal-fabric-1.0.0.jar`     |
| Forge    | `forge/build/libs/aiterminal-forge-1.0.0.jar`       |
| NeoForge | `neoforge/build/libs/aiterminal-neoforge-1.0.0.jar` |

---

## Configuration (`aiterminal.json`)

- **Location:** `config/aiterminal.json` in the game directory (the same folder as `options.txt`).
- **Created automatically** with all defaults and inline comments on first launch.
- **Format:** JSON. For convenience, `//` line comments and `/* */` block comments are allowed
  in the file and are ignored when read.
- **To apply changes:** edit the file, save, and **restart Minecraft**. There is no in-game
  config UI or command — configuration is exclusively via this file (by design).
- **Robust loading:** if a field is missing or has the wrong type, the mod logs a clear warning
  and uses that field's default instead of crashing.

### Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `openwebui_base_url` | string | `http://localhost:3000` | Base URL of your OpenWebUI instance. No trailing slash needed (a trailing slash is tolerated). The mod appends `/api/chat/completions`. |
| `openwebui_api_key` | string | `""` | Bearer token for the OpenWebUI API (**Settings → Account → API Keys**). If blank, no `Authorization` header is sent. |
| `openwebui_model` | string | `""` | The model ID exactly as it appears in OpenWebUI (e.g. `llama3.1:8b`). **Required** — if blank, the terminal shows a "no model configured" message instead of sending a request. |
| `web_search_enabled` | boolean | `true` | When `true`, sends `features.web_search: true` so the model can use OpenWebUI's web search. |
| `max_response_length` | number | `2000` | Responses longer than this many characters are truncated in the terminal display (the request itself is unaffected). |
| `system_prompt` | string | *(see below)* | System prompt injected as the first message of every request. |

**Default `system_prompt`:**

> You are a helpful Minecraft assistant with access to web search. Answer questions about
> Minecraft gameplay, crafting, mods, and mechanics accurately and concisely. When referencing
> mod-specific content, note which mod it belongs to. Keep answers focused and practical for an
> in-game player.

### Example

```jsonc
{
  // AI Terminal configuration. Edit this file and restart Minecraft to apply changes.
  "openwebui_base_url": "http://localhost:3000",
  "openwebui_api_key": "sk-xxxxxxxxxxxxxxxxxxxxxxxx",
  "openwebui_model": "llama3.1:8b",
  "web_search_enabled": true,
  "max_response_length": 2000,
  "system_prompt": "You are a helpful Minecraft assistant with access to web search. Answer questions about Minecraft gameplay, crafting, mods, and mechanics accurately and concisely. When referencing mod-specific content, note which mod it belongs to. Keep answers focused and practical for an in-game player."
}
```

---

## Using the terminal in-game

**Craft** the AI Terminal (shaped recipe — 8 iron ingots around 1 redstone dust):

```
[ I ] [ I ] [ I ]
[ I ] [ R ] [ I ]      I = Iron Ingot      R = Redstone Dust
[ I ] [ I ] [ I ]
```

It is also available in its own **AI Terminal** creative tab.

**Place** the block and **right-click** it to open the terminal. Then:

- Type a question in the input box at the bottom.
- Press **Enter** or click **Ask** to send it.
- While waiting, a blinking `Querying...` indicator is shown.
- The answer appears in the scrollable output area (green = AI, white = your input,
  yellow = errors). **Scroll** with the mouse wheel.
- Click **Clear** to reset the conversation history.

History holds the last 50 entries per terminal and is **not saved** — it resets when the world
reloads. Each request is independent (the AI does not see prior turns); the history is for your
on-screen reference.

---

## Troubleshooting

All failures are shown as a readable yellow message in the terminal (never a crash):

| Message | Cause / fix |
|---------|-------------|
| `No model configured...` | `openwebui_model` is blank in `aiterminal.json`. Set it and restart. |
| `Could not connect to OpenWebUI...` | The instance isn't running or `openwebui_base_url` is wrong/unreachable from the client. |
| `Unauthorized (401)...` | `openwebui_api_key` is missing or invalid. |
| `Forbidden (403)...` | The key isn't allowed to use that model. |
| `Not found (404)...` | Wrong base URL, or the model/endpoint doesn't exist. |
| `Rate limited (429)...` | Too many requests — wait and retry. |
| `Request timed out after 30 seconds...` | The model is slow/unloaded, or the server is overloaded. |
| `Malformed JSON from OpenWebUI...` | The server returned something unexpected — check the OpenWebUI logs. |

Config warnings (missing/mistyped fields) are written to the game log at startup.

---

## Project layout & toolchain notes

```
common/    Pure Java 21 — config, HTTP client, JSON, line model. Zero Minecraft imports.
           Its classes are bundled into each platform jar.
fabric/    Fabric Loom (official Mojang mappings).
forge/     ForgeGradle 6 (official Mojang mappings).
neoforge/  NeoForged ModDevGradle (official Mojang mappings).
```

A few intentional deviations from the original spec, made so the project actually builds against
real 1.21.1 SDKs:

- **Forge version:** Minecraft 1.21.1 Forge is the **52.x** line (the spec's `51.0.1` is for
  plain MC 1.21). This project targets `1.21.1-52.1.0`.
- **Forge toolchain:** uses **ForgeGradle 6** rather than ModDevGradle's legacy-Forge path,
  which could not resolve Forge's artifacts in this environment.
- **Forge screen opening:** `NetworkHooks` was removed in Forge 52.x, so the block uses
  `ServerPlayer#openMenu(provider, buf -> ...)` (equivalent behavior).
- **Fabric mappings:** uses official Mojang mappings (not Yarn) so the vanilla API matches the
  other modules; the Fabric idioms (`ModInitializer`, `ExtendedScreenHandlerType`, Fabric API
  registration) are unchanged.
- **Data folders:** recipes/loot tables use the 1.21 **singular** folder names (`recipe/`,
  `loot_table/`).
- **Gradle:** the wrapper is pinned to **8.14.3**, the version compatible with all three loader
  plugins used here.

License: MIT.
