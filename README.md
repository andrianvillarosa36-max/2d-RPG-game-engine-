# MiniEngine2D

A v1 prototype of a visual 2D game engine that runs **on** Android — live
preview on top, code editor + resource buttons on the bottom, same idea as
GDevelop but built by you, for your phone.

## Try it in 10 seconds

Open `preview.html` (or `app/src/main/assets/index.html` — they're the same
file) in any browser. No build, no install. You'll see two tree blocks and a
Hero character already placed, because the script box starts with:

```js
engine.setBlock(2, 2, "Tree");
engine.setBlock(3, 2, "Tree");
engine.addCharacter("Hero", 4, 4);
```

Edit that code and the canvas above updates about half a second after you
stop typing. Tap **+ Block**, name something "Rock", pick a colour, save —
then type `engine.setBlock(5,1,"Rock")` and watch it appear.

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
| `engine.setBlock(x, y, name)` | Places a registered Block resource at grid cell (x,y) |
| `engine.clearBlock(x, y)` | Removes whatever block is at (x,y) |
| `engine.addCharacter(name, x, y)` | Places a registered Character at (x,y) |
| `engine.addNPC(name, x, y)` | Places a registered NPC |
| `engine.addMob(name, x, y)` | Places a registered Mob |
| `engine.addItem(name, x, y)` | Places a registered Item |
| `engine.addCustom(name, x, y)` | Places a registered Custom resource |
| `engine.clearAll()` | Empties the scene |
| `engine.log(...)` | Prints to the status bar instead of the canvas |

The grid is 9×6 cells, (0,0) at top-left. `name` always refers to a resource
you created with the `+` buttons — that's the link between the visual side
and the code side.

## Project layout

```
MiniEngine2D/
├── preview.html                 ← the engine, standalone (for browser testing)
├── app/
│   ├── build.gradle              ← app module config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/.../MainActivity.kt   ← WebView shell (the only native code)
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

- **Nothing persists.** Resources and the canvas reset on reload. Save/load
  is the top item in the roadmap below.
- **Colours only, no image sprites** — every block/character is a flat
  coloured shape.
- **No syntax highlighting** — the code box is a plain `<textarea>`.
- **Fixed 9×6 grid**, not resizable from the UI yet.
- **Not sandboxed.** Your script runs with full JS access (like any code
  editor's live preview, e.g. CodePen). Fine for your own single-player
  tool; don't paste in code you don't trust.

## Roadmap

1. **Save/load** — `localStorage` inside the WebView (works fine on-device,
   just don't reuse this exact HTML file as a Claude.ai artifact if you add
   it, since Claude.ai's artifact preview blocks browser storage).
2. **Real code editor** — swap the `<textarea>` for CodeMirror (line
   numbers, syntax highlighting, bracket matching).
3. **Image sprites** — let `+ Block` / `+ Character` accept an uploaded
   image instead of only a colour.
4. **Tilemap + collision** — bigger, scrollable maps and basic AABB
   collision so characters can't walk through blocks.
5. **Animation & simple AI** — walk cycles, and patrol/chase behaviour for
   mobs.
6. **Export finished games separately** — MiniEngine2D packages *your* game
   as its own APK, GDevelop-style. This is the long-term goal and a
   substantial project on its own — worth tackling once 1–5 feel solid.
