# MiniEngine2D

A v1 prototype of a visual 2D game engine that runs **on** Android — live
preview on top, code editor + resource buttons on the bottom, same idea as
GDevelop but built by you, for your phone.

## Try it in 10 seconds

Open `preview.html` (or `app/src/main/assets/index.html` — they're the same
file) in any browser. No build, no install. You'll see two Tree blocks, a
Hero, and a Slime already placed, because the script box starts with:

```js
engine.setBlock(2, 2, "Tree");
engine.setBlock(3, 2, "Tree");
engine.addCharacter("Hero", 4, 4);
engine.addMob("Slime", 6, 4);
```

Edit that code and the canvas above updates about half a second after you
stop typing. Tap **+ Block**, name something "Rock", pick a colour, save —
then type `engine.setBlock(5,1,"Rock")` and watch it appear.

Drag the joystick (bottom-left of the preview) to actually walk the Hero
around and bump into the Slime to fight it.

## How it's built (and why)

The whole engine is one HTML file — plain JS + Canvas, no frameworks. That's
what makes "type code, see it instantly" easy: your script re-runs against
a fresh empty scene on every keystroke, using an `engine` object as the only
API surface. No native interpreter to write, no bridge between a scripting
language and a renderer.

The Android app (`MainActivity.kt`) is a **single-Activity WebView shell**
that loads that same HTML file from local assets — nothing native to build
except the wrapper itself. This is a deliberate architecture choice, not a
shortcut: it's the same approach Construct 3 and several other visual game
makers use, and it means the identical file works as your in-browser
prototype, your Android app, and (later) something installable from a
website via a Trusted Web Activity if you ever want that.

## Scripting API (v1)

| Call | What it does |
|---|---|
| `engine.setBlock(x, y, name, terrain?)` | Places a registered Block at (x,y) in `terrain` (default `"Terrain 1"`) |
| `engine.clearBlock(x, y, terrain?)` | Removes whatever block is at (x,y) in that terrain |
| `engine.addCharacter(name, x, y, terrain?)` | Places a registered Character at (x,y) in that terrain |
| `engine.addNPC(name, x, y, terrain?)` | Places a registered NPC |
| `engine.addMob(name, x, y, terrain?)` | Places a registered Mob |
| `engine.addItem(name, x, y, terrain?)` | Places a registered Item |
| `engine.addCustom(name, x, y, terrain?)` | Places a registered Custom resource |
| `engine.setActiveTerrain(terrain)` | Makes `terrain` the one shown/played on start (default `"Terrain 1"`) |
| `engine.addConnector(x, y, toTerrain, toX, toY, terrain?)` | Places a terrain-transition point at (x,y) leading to (toX,toY) in `toTerrain` |
| `engine.clearAll()` | Empties every terrain |
| `engine.log(...)` | Prints to the status bar instead of the canvas |

The grid is 9×6 cells per terrain, (0,0) at top-left. `name` always refers
to a resource you created with the `+` buttons — that's the link between
the visual side and the code side. Leave off `terrain` and everything goes
into `"Terrain 1"`, so any script written before terrains existed still
works unchanged.

## Controls & combat

The joystick (drag) and arrow keys / WASD (keyboard, for browser testing)
move the **first Character** placed in the scene, one grid cell per step.

Two ways to fight or talk:
- **Bump into it** — walking into a Mob attacks it immediately; walking
  into an NPC shows its dialogue. No extra step.
- **The Attack / Talk button** (bottom-right of the preview) — appears
  whenever the Hero is standing *next to* a Mob or NPC, even approached
  from the side. Same button, it relabels itself: red "⚔ Attack" next to
  a Mob, gold "💬 Talk" next to an NPC.

| Bump into... | Result |
|---|---|
| A **Block** with "Solid" checked in its editor | Blocked — you don't move |
| A **Mob** | Attack — 25 damage to it, 10 back to you, each hit |
| An **NPC** | Shows its Dialogue text in the status bar |
| An **Item** | Picked up and removed from the scene |

Hero HP is shown top-center of the preview. At 0 HP the Hero stops
responding to input until you tap **⟲ Reset**, which re-runs your script
from scratch.

### Play mode

Tap **▶ Run** and the preview takes over the whole screen and locks to
landscape — an actual play-test view instead of the cramped top-half
editor preview. Tap the **✕** at the bottom-center to come back to the
editor (bottom, not top - see "Why the bottom" below).

Landscape lock goes through a tiny native bridge (`MainActivity.kt` exposes
`window.Android.lockLandscape()`), because the web-only Screen Orientation
API is unreliable inside a plain WebView. That means it's solid in the
installed app; testing `preview.html` in a desktop browser just shows the
fullscreen layout without rotating anything (nothing to rotate).

## Save / Load

Two layers, on purpose:

- **Autosave** — every time your script runs (typing, saving a resource in
  an editor, tapping a palette chip), your resources + script save to this
  device automatically, using `localStorage`. Close the app, reopen it,
  everything's exactly where you left it. No button to remember.
- **Export / Import** — for backups and moving to another device. Export
  turns your project into JSON text in a copyable box; Import pastes it
  back in. This is the one that matters before you rebuild the APK:
  autosave lives in the *old* app's storage, and installing a freshly built
  APK can wipe that, so export first if you want to keep what you built,
  then import it back once the new build is installed.

## Terrain painter — and connecting multiple terrains

Typing `engine.setBlock(x, y, "Grass")` fifty times to fill a map gets old
fast. **+ Terrain** (and **Edit Terrain** — same tool) opens a full-screen
painter: the grid is up top, and the tabs, block palette, and Cancel/Done
buttons all live in a panel anchored to the **bottom** of the screen.

**Why the bottom:** the first version pinned those controls near the top,
which turned out to sit right under the status bar / camera cutout on a
lot of phones — visible, but not reliably tappable. Two things fixed it:
`targetSdk` was quietly forcing edge-to-edge display (Android does this by
default from API 35), which is why content was landing under the system
bars in the first place - it's back to 34. And on top of that, none of
this app's controls depend on the very top edge being tappable anymore,
on any screen, so the same class of bug can't quietly reappear even if a
future device turns out to be picky about insets in some other way.

Also fixed in the same pass: Cancel/Done (and the eraser swatch) were
rendering with no set colour, which meant near-white text on the browser's
default near-white button background — readable nowhere. Every button in
every modal now has an explicit colour by default, not just the ones that
happened to get a hand-written style, so this shouldn't come back either.

**Naming a new terrain no longer uses a popup.** It used to call
JavaScript's `prompt()`, which needs the native app to explicitly hand it
off to a real dialog - this app's `WebView` never did that, so there's a
real chance it was silently failing rather than asking you anything. **+
New terrain** is now a plain text box + Add button built into the painter
itself, pre-filled with a suggested name when you tap **+ Terrain**. Every
other popup (`alert()`) in the editor got replaced the same way, with a
message that appears in the modal instead of a dialog that may not have
been showing at all.

The tab bar at the top of the painter is what makes this multiple *places*
instead of one big room: each tab is a separate 9×6 terrain with its own
layout. Tap **+ New** to create another one (e.g. paint "Terrain 1" as a
village, **+ New** → name it "Cave" → paint that separately) — switching
tabs keeps whatever you painted on the one you're leaving, and **Done**
writes *all* of them into your script at once, each in its own
`// --- terrain:<name>:start ---` / `:end` block.

## Connecting terrains: the Terrain Connector

Connecting terrains used to be a property on a *block* — check "leads to
another terrain" on the Road block, pick a destination, done. That fell
apart the moment you had more than one connection: the destination lived
on the block *type*, so every Road everywhere led to the same place. Three
terrains in and you'd need a new block resource per connection just to
give each one a different destination — exactly the wall you hit.

**+ Terrain Connector** (and **Edit Terrain Connector** — same tool) fixes
this by making a connection belong to the *specific spot* you place it,
not to a block type. Any block can sit underneath a connector for looks —
same "Road" everywhere is fine now — the destination is stored separately
per placement. Open the tool: pick a **From** terrain and tap a cell on
its grid, pick a **To** terrain and tap a cell there, leave **"Also create
a path back"** checked if you want both directions in one go, **Save**.
Walk onto that cell in play mode (look for the gold ring with an arrow)
and you're switched straight into the other terrain, Hero HP and all —
nothing resets. **Edit Terrain Connector** lists every connector you've
made so far so you can tap one to change or delete it.

The script stays the source of truth throughout; the painter and the link
checkbox are just faster ways to write parts of it. Hand-editing inside
(or outside) the terrain markers works exactly like any other code.

## Sprite images

Every resource type except Block can carry an uploaded image instead of a
flat colour circle — open **Edit Character** (or NPC/Mob/Item/Custom) →
"Sprite image" → pick a photo/PNG from your phone. It's stored as the
image data itself inside the resource, so it travels with autosave and
Export/Import automatically — no separate asset files to manage.

*(If you tried this before and the picker never opened: that was a real
bug, not something you did wrong — a plain Android WebView doesn't let
`<input type="file">` open anything unless the app explicitly wires it up
to a native picker. `MainActivity.kt` now does that via `WebChromeClient`,
so this needs the rebuilt APK to actually work.)*

**Characters** additionally get **Sprite columns / rows**: if your image
is a sheet of several poses, say how many frames across and down, and the
Hero steps through them in order, one frame per grid move, for a simple
walk animation. It cycles frame 0 → 1 → 2 → ... in reading order. If your
sheet is actually laid out as *facing directions* (down/up/left/right)
rather than a walk cycle, tell me the layout in chat and I'll change the
logic to pick a row by direction instead of just cycling — I didn't want
to guess wrong from the image alone and lock in the wrong behaviour.

## Project layout

```
MiniEngine2D/
├── preview.html                 ← the engine, standalone (for browser testing)
├── app/
│   ├── build.gradle              ← app module config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/.../MainActivity.kt   ← WebView shell + orientation/file-picker bridges
│       └── assets/index.html     ← same engine, bundled into the APK
├── build.gradle                  ← root: plugin versions
├── settings.gradle
├── gradle.properties
└── .github/workflows/build-apk.yml   ← builds the APK on every push
```

## Build the APK — Termux + GitHub Actions

You don't need the Android SDK in Termux at all; GitHub's servers do the
build. Termux's only job is getting the code onto GitHub.

**1. In Termux, one-time setup:**
```bash
pkg install git -y
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

**2. Create an empty repo on GitHub** (github.com → New repository →
don't initialize with a README, you already have one).

**3. Push this project:**
```bash
cd MiniEngine2D
git init
git add .
git commit -m "Initial MiniEngine2D prototype"
git branch -M main
git remote add origin https://github.com/<your-username>/<repo-name>.git
git push -u origin main
```
Use a GitHub [personal access token](https://github.com/settings/tokens) as
the password when prompted (GitHub no longer accepts account passwords over
HTTPS git).

**4. Watch the build:** on GitHub, open your repo → **Actions** tab. The
"Build APK" workflow starts automatically on push and takes a few minutes.

**5. Download the APK:** when the run finishes (green check), open it →
scroll to **Artifacts** → tap `MiniEngine2D-debug-apk` → it downloads as a
`.zip` straight to your phone. Unzip it (any file manager can do this) to
get `app-debug.apk`.

**6. Install it:** tap the APK. Android will ask you to allow installs from
this source the first time — that's expected for a debug build you built
yourself.

## Turning an exported game into an APK

**Export** in the toolbar exports this *project* (resources + script) as
JSON, for backing up or handing off to edit later. **Export Game (HTML)** is
different: it produces a `game.html` that plays your game immediately with
no editor at all - same engine, same rendering and controls, just with the
editor chrome stripped out and your project baked in as data instead of
loaded from storage.

A WebView can't invoke a compiler, so `game.html` on its own is not an APK -
turning it into one reuses the exact build you already have working:

1. Tap **Export Game (HTML)**. It downloads `game.html`.
2. Duplicate this whole project folder under a new name (e.g. `my-game-app`).
   Give it its own GitHub repo too - keep the editor project and each
   exported game as separate repos rather than overwriting one with the other.
3. In the copy, replace **both** `app/src/main/assets/index.html` and
   `preview.html` with the exported `game.html` (same filename, `index.html`
   and `preview.html`, so nothing else needs to change).
4. Optional: `app/src/main/AndroidManifest.xml` has
   `android:label="MiniEngine2D"` — change that string to your game's actual
   name so it doesn't show up on the home screen labeled "MiniEngine2D".
5. Push it and let the same `.github/workflows/build-apk.yml` build it -
   nothing about the workflow itself needs to change, it just builds
   whatever's in `assets/index.html` now, which is your game.

The result is a real, standalone, installable APK of just your game - no
toolbar, no code editor, launches straight into it.

## Why these versions

`build.gradle` pins **AGP 8.7.0 / Kotlin 2.0.21 / compileSdk 35**, and the
CI workflow installs **Gradle 8.9 directly** rather than committing a
`gradlew` wrapper (that wrapper needs a binary `.jar` file, which isn't
something to hand-write). If you ever set this up in Android Studio
instead, Studio will offer to generate that wrapper for you automatically —
totally fine to accept.

AGP 9.x is out and current as of mid-2026 with a new "built-in Kotlin"
model, but it changes enough about how Kotlin plugins are declared that
Google ships a dedicated upgrade assistant for it. 8.7.0 is the last
release before that shift and is extremely well documented, so this scaffold
uses it to maximize the odds your very first CI run succeeds. Upgrading
later is a good phase-2 task once the basics are working.

## Known limitations (v1)

- **Blocks are still colour-only** — sprite images work for Character/NPC/
  Mob/Item/Custom, but terrain blocks are flat coloured squares for now.
- **Sprite animation is a simple frame-cycle**, not direction-aware — see
  "Sprite images" above.
- **Each terrain is a fixed 9×6 grid** — multiple terrains linked by doors
  is how you build something bigger, rather than one giant scrolling map.
- **Each connector is still one direction under the hood** — "Also create
  a path back" makes both at once by default, but if you turn that off
  (or delete just one side later) the other direction won't exist until
  you add it.
- **No syntax highlighting** — the code box is a plain `<textarea>`.
- **One controllable Character** — the joystick/action button always
  target whichever Character is in the active terrain.
- **Mobs don't move** — they stand still until bumped; no patrol/chase AI.
- **Not sandboxed.** Your script runs with full JS access (like any code
  editor's live preview, e.g. CodePen). Fine for your own single-player
  tool; don't paste in code you don't trust.

## Roadmap

1. **Real code editor** — swap the `<textarea>` for CodeMirror (line
   numbers, syntax highlighting, bracket matching).
2. **Block textures** — image support for terrain blocks, not just entities.
3. **Two-way terrain links** — auto-create (or at least suggest) a return
   door when you link one terrain to another.
4. **Mob AI** — patrol/chase behaviour instead of standing still. Would
   want its own sprite animation too, once mobs actually move.
5. **Named, multi-project saves** — right now there's one autosave slot;
   a proper "Save As" using Android's file picker
   (`Intent.ACTION_CREATE_DOCUMENT`) would allow several named projects.
6. **Export finished games separately** — MiniEngine2D packages *your* game
   as its own APK, GDevelop-style. This is the long-term goal and a
   substantial project on its own — worth tackling once the basics feel solid.
