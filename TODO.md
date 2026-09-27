# TODO — road to 1.0

Living checklist for the rewrite (now the main codebase at the project root). Items marked **[verified]** are proven by
headless suites (`./build.sh test`, 35/35 green) or in-game runs; everything else is
measured, read from code, or reasoned — not yet proven. Update this file as items land.

```text
How to verify:  ./build.sh test      # headless suites (no window)
                ./build.sh run       # or the "Refactored" IDE config
Perf loop:      run ~30s (stand, then walk), paste [gl]/[boot]/[perf] lines.
```

---

## P-1 — Foundations for later (architecture seams, add opportunistically)

Not features — structural splits that make P2–P6 possible without rewrites. Each is
small on its own; each becomes painful once gameplay code depends on the un-split
version. Do them lazily: when a task below touches one of these areas, build the seam
first, then the feature. Partial seams already in place are marked [seam].

- [x] **Block / BlockState / BlockEntity split.** Done: `BlockState` value
      (block + data, `Blocks.stateOf`), `Block.defaultState()` + 7-arg render
      overload (stateless shapes untouched; Chunk meshes through state),
      `BlockEntity` base (pos, `tick`, NBT save/load contract) with `Level`
      store + `tickBlockEntities()` wired into `GameServer.tick()`, per-cell
      data columns (memory-only). Proven by `BlockStateTest STATE PASS`.
      Remaining: persist data columns (needs SAVE_VERSION below) before any
      stateful block ships; tick budget when entities grow past trivial.
- [ ] **Read vs write world paths.** Meshing workers only ever read; `setTile` and
      Real shape, like Minecraft: `Block` = shared stateless type (already done);
      `BlockState` = type + per-position properties (facing, open, powered, growth,
      waterlogged) so stairs/doors/slabs don't each need 16 ids; `BlockEntity` =
      per-position data + ticking for complex blocks (chest inventory, furnace
      progress, sign text, spawner). Needs: state int alongside the id in storage,
      NBT read/write per entity type, entity tick registry, save/load. Do the split
      the moment the second non-cube shape with *state* arrives (stairs force it).
- [x] **Read vs write world paths.** Done: rule documented on `Level`
      (reads worker-safe, writes main-only, `floodAdd`-as-seeding the one
      named exception); verified by auditing every `level.*` call site.
- [x] **Lighting engines as classes.** Done: `world.light` package —
      `LightWorld` interface, `SkyLightEngine` (heightmap cache), `BlockLightEngine`
      (flood add/remove, owned store); `Level` implements `LightWorld` and keeps
      identical public signatures; emitter classification moved to `Blocks`.
      Proven by `LightTest LIGHT PASS` (stub world, no `Level`) + all old suites.
      The split caught two real bugs, both fixed: (1) refactor dropped the
      source-cell write `setTile` used to do before `floodRemove` — engine clears
      its source first now; (2) pre-existing overlap hole (`>` vs `>=` in readd:
      removing one of two torches left a permanent dark scar) — previously untested.
- [ ] **Mesh pipeline stages.** `Chunk` currently meshes + uploads + draws. Split
      into Mesher (CPU arrays, worker) → Uploader (GPU, main) → draw list, connected
      by queues. Required groundwork for merged-region VBOs, and makes each stage
      independently testable/ replaceable. (16³ Section slabs tried 2026-09 and
      reverted same day: 4× draw calls + 4× per-frame frustum math tanked fps.
      Tight vertical culling wants merged VBOs first, then sub-boxes.)
- [x] **Entity base classes (move first).** Done with falling blocks: `server/Entity`
      (pos/vel/bb/grounded + axis-separated `move`, transplanted verbatim from
      `Player` — NetTest's pinned trajectory passes bit-for-bit). `Player`
      rides it directly; `ItemEntity` shares the state but keeps its own
      collide shape (local box + writeback + pickup friction). Still open for
      the first mob: `LivingEntity` (hp) split, per-chunk lists,
      spawn/despawn rules, save — do those with the mob, not before (and no
      second physics copy in the meantime).
- [x] **Inventory + ItemStack.** Done: `server/ItemStack` (id + count, cap 64)
      + `server/Inventory` (9 slots, match-stack then empty-slot add, consume,
      sync apply/compare). Hotbar is a view onto it (icons + stack bars from
      live stock, empty slots bare); placement spends it. `meta` deferred —
      counts suffice until crafting/tools need more. Proven by `ItemTest`
      (stacking/consume/sync) + full suite green.
- [x] **Scheduled block ticks.** Done: `Level` queue (packed-pos key + due
      tick, earliest-wins re-scheduling, key sign-extended on decode so
      negative coords fire at the right spot), ticked next to
      `tickBlockEntities`, not persisted (same window as drops). First
      client: player-placed saplings grow after `SAPLING_TICKS` (12000) —
      deterministic 4-6 trunk + generator canopy (cut floors, cap, crown),
      air-only writes, needs solid ground + clear trunk run. Proven by
      `GrowthTest` (growth shape, broken/floating/negative cases). Lava flow
      and crops plug into `onScheduledTick` next.
- [x] **Save versioning.** Done: `Level.SAVE_VERSION = 1` stamped as `DataVersion`
      on every column chunk (MC's tag name); read path tolerates missing (= 0,
      every save on disk today) and warns once on newer; migration switch is a
      scaffold. Proven by the `DataVersion` assertion in `LevelTest` saveload.
- [ ] **Data-driven registries.** Blocks and biomes are code-registered today ([seam]
      shaped for it). Later: JSON/codec definitions so content stops requiring
      recompiles. Don't build the loader until the second data-driven system needs it.
- [ ] **Game event bus.** Block break/place, entity death, weather change as events
      with subscribers (particles, sound, achievements, mods all hook in). Right now
      each would hardcode call sites — route the first cross-cutting feature (sound
      reacting to breaks) through a bus and keep it.
- [x] **Seeded RNG injection.** Done: `core/Rng` (world lane, `reseed` per session
      from the world seed in `GameServer`); player spawn uses it (same seed =
      same spawn). Worldgen was already seeded; particles stay `Math.random`
      (cosmetic by rule). Covered in `CoreTest`.
- [x] **Scoped profiler.** Done: `core/Profiler` (push/pop, off by default,
      prints every 600 frames via `Log`, thread-safe); wired sections: `mesh`,
      `upload`, `flood`, `sim`; `endFrame` in the client loop. Covered in `CoreTest`.
- [x] **Stupid-amount debug.** Done: `core/Debug` (slow-event tripwires with
      thread names, live worker states, 24-slot recent-event ring) + `Config`
      thresholds (mesh 150ms, upload 25ms, setTile 25ms, frame 250ms). Every
      pipeline stage reports: mesh/upload per chunk, setTile per edit, fills
      (already), carves (already, with thread). `[perf]` gained meshq/skyq
      depths + player coords; every 300 frames a `[dbg]` line prints worker
      states + chunk census; any frame past 250ms dumps the recent ring.
- [x] **Log facade.** Done: `core/Log` (tagged lines, level threshold, quiet
      mode, `raw()` for the two legacy untagged prints). All 25 call sites
      migrated; format identical. Only `Log` touches stdout/stderr now.
- [x] **Config centralization.** Done: `core/Config` (view/prebuild/budgets/
      workers/day/fog); `LevelRenderer` + `GameClient` wired to it (`DAY_LENGTH`
      kept as alias for `DayTest`). File-backed + UI with P5 settings.
- [x] **Math helpers.** Done: `core/MathHelper` (floor/clamp/lerp) + `CoreTest`;
      all `(int)Math.floor` casts migrated (Level, Noise, Raycaster, CaveCarver,
      LevelRenderer) with identical semantics. Trig/pow/min/max left alone.
- [x] **GL resource tracking.** Done: `core/GlResources` census (track/release/
      live/peak/dump, GL-free so headless tests exercise it); `Chunk` tracks its
      6 VBOs + releases on dispose. `live()` reserved for the F3 overlay (P4).
- [x] **Package dependency rule + test.** Done: `DepTest` scans compiled constant
      pools (catches real coupling, not just imports), 15 classes green. Rule:
      core is utilities (implicit everywhere); blocks → core only (`BlockEntity`
      moved to `world` to keep it so); world → blocks/gen/light/storage.nbt;
      mesh → world; server/client → down only (+server for integrated client).
      `*Test` fixtures exempt. `build.sh` cleans `out/` first (stale classes
      would false-positive).
- [x] **Coordinate precision plan.** Documented limit (Appendix B): player/boxes
      are floats, fine to ~16M blocks, then jitter. Fix when needed is origin
      rebasing, not doubles everywhere. No code — just don't let anyone
      "fix" it with doubles.
- [x] **Error policy.** Done, matches the rule already: fatal boot = dialog +
      exit(1) (`GameClient.run`); recoverable (migration fail, region fail,
      corrupt chunk, save fail, mesh worker throw) = `Log.error/warn` +
      skip-and-continue. Verified by auditing every catch site. Apply the same
      shape to new paths.

---

## P0 — Bugs & correctness (things known-broken right now)

- [x] **Pick boxes for partial blocks.** Breaking/placing aimed at full cells
      even for 2px torches and flower tufts. `Block.pickBox` (full cell by
      default; torch 0.35..0.65/0..0.6 and flower 0.3..0.7/0..0.6 per vanilla
      1.0 BlockTorch/BlockFlower bounds — generous on purpose) + a slab test
      per traversed cell in `Raycaster` (nearest box wins, face from the
      entry axis, same ids as before). Proven by `RaycastTest` (slab units,
      torch post hit/miss/crown, flower hit/miss, cube regression).

- [x] **Black destroy bursts + black drops at negative coords.** Particle and
      drop brightness sampled with `(int)` truncation, which rounds toward
      zero — every sample on the negative side of origin landed in the +x/+z
      neighbor column (usually solid): bright dig, black burst, black drops.
      Fixed with `MathHelper.floor` in `Particle` + `ItemRenderer` (the same
      bug class the helper's docs warn about). Proven by `ParticleLightTest`
      (fails 5x with 0.0 reverted) + full suite green.
- [x] **Fullbright decoupled from spectator.** Was hardwired to spectator
      fly-mode; now a client-side `\` toggle (`[FULLBRIGHT]` title tag, fog
      off while lit) independent of the server spectator flag.
- [x] **Mouse grab discipline.** The single `setGrabbed(true)` at startup
      wedged the game mouseless when the cursor started outside the window,
      and alt-tab left the cursor invisibly grabbed. The loop now holds the
      grab while active (draining deltas on re-grab, no camera whip) and
      releases when inactive so the cursor comes back.
- [x] **Clicks dead after focus-starved start (look fine).** Breaking polled      `isButtonDown` while placement read the event queue — the two disagree
      after focus loss, so clicks died while look lived. LMB state is now a
      single event-stream latch (seeded from the poll on every re-grab, stale
      edges drained), focus transitions log (`active/inactive`), and the OS
      cursor warps to window center before every grab (grabbing alone hides
      it wherever it sits — outside means clicks land behind the window).
- [x] **Resizable window.** Was fixed 1024x768. `setResizable(true)` (before
      `create()` — after half-applies on macOS and live drags SIGSEGV in
      Apple's GL bridge) plus a per-frame `wasResized()` check that only
      touches GL on an actual size change (the flag fires continuously
      mid-drag). Camera aspect and HUD follow via width/height.
- [x] **Grass side overlay smeared on +x face.** The tint-overlay remap in
      `CubeBlock.side()` took U/V bounds over the first two corners only;
      the +x face starts with two bottom corners, so every overlay vert
      pinned to one row (top-strip texel stretched down the face). Fixed by
      spanning all four corners; proven by a headless UV probe plus a
      `MeshTest` assertion (overlay V tracks height on every face).
- [x] **Torch model.** Remodeled to spec off `torch_on.png`: 2px post
      centered (7/16..9/16), 10px tall, stick strip columns 7-8 mapped 1:1,
      top cap = rows 0-1, bottom cap = rows 14-15. `BlocksTest` points at the
      new tile; `TorchFaceTest` asserts post bounds + cap UVs + lit-layer
      only. Full suite green. Eyeball in game to close (three blind reworks
      happened here before — no more blind edits). Follow-up: sub-texel
      see-through slits along the post edges — NOT geometry (corners are
      bit-shared); the mipmap chain blended the 2px strip into surrounding
      transparency at minification and alpha-test (0.5) ate the edges.
      Fixed vanilla-style: `Textures.upload` uses `glTexImage2D` (no mip
      chain), NEAREST on the base level; distant shimmer is the tradeoff.
      Slits persisted (torch-only, all distances, interior faces visible
      through them): geometry exonerated by construction (bit-shared corners)
      and by proof (headless closure probe: 48 torch verts, 12 distinct
      edges, 0 orphans) and art exonerated (crisp 2px strip, no soft edges).
      Real cause: the atlas far-edge inset (TILE_UV) poisoned sub-tile UV
      math — strip fractions of the inset rect landed at texel 6.99 / row
      5.99 (transparent neighbors), which alpha-test discarded into slits
      along the post's left/bottom edges. `TorchBlock.strip()` now computes
      rects in true texel space with 0.05-texel inward margins ([7.05,8.95]
      × [6.05,15.95], caps [6.05,7.95]/[14.05,15.95]); `TorchFaceTest` pins
      the rects + a true-texel inside-opaque-art regression.
- [ ] **Heap high-water ~470MB.** Pauses are small (3–8ms/window) so this is JVM
      ergonomics, not a leak — but confirm it plateaus over a 10+ minute session.
      If it grows without bound, suspect `heightCache` / light caches / carve masks
      (all grow with exploration, none evict — see P1).
- [x] **`blockLog`/`test.log` still written** — deleted with the P-1 split.
- [x] **Bundle an arm64 `libjinput-osx.dylib`.** Investigated with a real
      launch: the feared dead-input case does NOT happen — nothing references
      Controllers/JInput (Keyboard/Mouse live in the arm64 `liblwjgl.dylib`
      and init fine), so the old dialog was a false alarm and is removed;
      missing jinput is now a warn (gamepads off, keyboard/mouse unaffected).
      Fixed a real bug alongside: the loader retried every dir per lib and
      double-loaded `liblwjgl.dylib` (project + Extensions copies → ObjC
      duplicate-class warnings); each lib now loads once from its first dir.
      Full suite green. Remaining (controllers only): ship a universal jinput
      binary someday (`lipo -info` should list arm64).
- [x] **Trim `lib/`.** Done: deleted `lwjgl_test`, `lwjgl_util_applet`,
      `lwjgl-debug`, `asm-debug-all`, `lzma` (nothing imports them — verified in
      both trees); kept `lwjgl`, `lwjgl_util`, `jinput`, `AppleJavaExtensions`.
      `lwjgl.xml` updated to match; `test.log` (dead `blockLog` artifact) removed.
      Full suite green after the trim.
- [ ] **Move `TileArt.java` into the repo** — BLOCKED, generator is gone: it lived
      only in `/tmp`, which has been wiped (searched `/tmp`, `/var/folders`,
      `~/Documents`, `~/Desktop` — nothing). `terrain.png` is now canonical;
      mitigation committed: `res/atlas.txt` key (tile layout + free slots for the
       P4 crack tiles). Rule stands: any future generator lives in
       `tools/` from day one. (`terrain.png.bak` is archived in `orig-backup/`
       next to the old tree — rescue it from there if tile art is ever
       regenerated.)
- [x] **Old tree retired.** The pre-rewrite code (`com/`, old `region/`,
      `terrain.png*`, `level.dat.imported`) is archived in `orig-backup/` and the
      old `RubyDung` run config is deleted; the `Refactored` config (→ `Boot`,
      working dir = project root) is the only launcher.
- [x] **Torch light through walls on placement edge cases.** Proven by
      `WallLeakTest`: sealed stone room next to a torch reads 0, breaking
      the shared wall lights the interior (removal cascade + readd), rebuilding
      darkens it fully, torch removal kills the remainder — no stuck leaks
      either direction, all synchronous through `setTile` (no meshing).

## P1 — Performance (all numbers from `[perf]` output on Apple M2)

Status quo: ~110fps steady, 8.5–9ms frames (`world` ~5.2, `gui` ~2.5, `swap` ~1.0),
startup ~3s black screen. MC 1.8 does ~200 on comparable scenes; gap is Apple-legacy-GL
per-call overhead (~1,750 calls/frame), not methods — audited, we're aligned with 1.8
(VBOs, worker meshing, DDA pick, batched particles, single atlas).

- [x] **Startup: region carve prefetch.** Done differently: prebuild is async now
      (`LevelRenderer` queues spawn chunks instead of sync-meshing — frame one
      renders sky behind fog, world streams in; physics never needed meshes).
      Carves also got ~7x faster (boxed `HashMap<Long,Long>` masks ->
      `LongLongMap`, worst virgin region 2257ms -> 308ms) and globally shared.
      First frame should be ~1s; confirm in game, then delete this line.
- [x] **VBO interleave** (3 binds → 1 per chunk-layer + stride pointers). Done:
      `Chunk` holds 2 interleaved VBOs (was 6), layout x,y,z,u,v,r,g,b
      (stride 32, offsets 0/12/20), single `glBufferData` + single bind per
      layer. Proven by `InterleaveTest` (pack order + stride) + full suite
      green. Confirm +10–15fps in game with the 30s scenario.
- [ ] **Merged region meshes** (e.g. 2×2 chunks per VBO set). Kills hundreds of draws;
      est. +30–40fps. Real surgery on dirty tracking — only if interleave isn't enough.
- [x] **Cache eviction.** Done for procedural caches: `Level.evictFar`
      drops `computed` + `heightCache` beyond the keep window, delegates to
      `TerrainGenerator.evictFar` (trunkCache) → `CaveCarver.evictFar`
      (region carve maps); all pure-per-seed and regenerating lazily.
      Never evicted: edits (`columns`/`dataColumns`/`blockEntities`) and
      light stores (flood state, not regenerable). Hooked into
      `LevelRenderer.unloadFar` at `VIEW_RADIUS+1`. Proven by `EvictTest`
      (far drops, tiles regenerate, edits survive) + full suite green.
- [x] **Region file compaction.** Done: `RegionFile` tracks orphaned/slack
      sectors (append path + in-place slack), seeds initial waste from
      `file.length - live - header` on open; `maybeCompact()` repacks live
      chunks contiguously when waste exceeds 50%, called from `Level.save`
      per region with a log line. Proven by `NbtTest` compact section
      (waste tracked, triggers, repacks smaller, payloads intact).
- [ ] **Re-measure after each change** with the same 30s scenario. Stop when steady
      `frame avg` ≤ 6ms and `max` ≤ 25ms. Do not chase MC's number past that point —
      diminishing returns on a deprecated driver.

## P2 — World generation (terrain you can see)

- [x] **Trees.** Done: pure-function decorator in `TerrainGenerator` (no storage,
      seamless across borders) — hash-decided trunks (oak log, h4-6) on grass with
      headroom, 5x5-minus-corners canopy + 3x3 cap + plus crown. Proven by
      `VegTest`. No leaf decay yet (needs the P-1 tick queue). Follow-up
      (user art): fancy cutout leaves (`tinted/oak_leaves.png`, ~70% opaque)
      replace opaque — face culling alone went fancy (leaf neighbors always
      emit both sides or canopies render hollow; physics + light stay
      classic solid). Registry/tint/tests renamed to the new oak paths.
- [x] **Flowers/tall grass.** Done: `CrossBlock` + alpha-test cutout; rose +
      dandelion + tuft + sapling art (yours) scatter pure on grass. Saplings are
      decoration until the tick queue grows them. New blocks are natural-only.
      Diagonals are vanilla's center +/- 0.45 (read off 1.0
      renderCrossedSquares — corner-to-corner stretched texels 41% wide,
      unit-length looked tall/thin; 0.9 is the authentic flower); proven by
      `CrossQuadTest` (1.2728 edges).
- [x] **Multi-sided blocks.** Done: `CubeBlock(top, side, bottom)` with row-aware
      `AtlasStitcher.uv()` (32-slot, 2-row atlas); grass does top/side/dirt-bottom,
      logs do top/side. Ores/cobble/log use your art; grayscale tintables
      (oak leaves, tallgrass, overlay) were dropped — they need a biome-tint pass
      that doesn't exist. Break particles + hotbar use `particleTile()`.
- [x] **Gravel + lapis.** Done: gravel pockets + deep rare lapis veins (`GenTest`
      counts both); gravel is static like sand until the tick queue (P-1).
- [ ] **Water, for real.** Beaches currently ring a sea that doesn't exist. Needs:
      water block + tile, transparency (alpha blend pass = third mesh layer),
      buoyancy/swim movement, no fall damage into water, fog color shift underwater,
      shore generation (fill basins to sea level). Biggest single worldgen item.
- [x] **Sand/gravel physics.** Done with REAL falling entities (no teleport):
      unsupported cells become `FallingBlock` entities (item-like gravity on
      the shared `Entity` base), ticked server-side, mirrored client-side
      (`FallingSpawn` packet + `FallingRenderer` full-size static cubes —
      deliberately not drops: no bob/spin/magnet). Landing converts back to
      a tile, popping a support-needing occupant vanilla-style (torch drops);
      solid occupants pop the faller as a drop (never overwrites builds);
      out-the-bottom voids as a drop. Bottom-up spawn order keeps stacks.
      Proven by `SandTest` (stack fall, void-drop, torch-pop landing,
      gravel, spawn-anchor) + `NetTest` spawn roundtrip. No per-tick
      scheduler exists yet — lava flow still needs one. Follow-up fixed:
      `Entity.move` hardcoded the player eye height, rendering fallers 1.1
      blocks high (visible pop on swap) — now `yOffset()` (player 1.62,
      falling 0.49 center); physics was always right.
- [ ] **Biome blending for *blocks*, not just heights.** Heights already interpolate
      (`blend()`); surface block choice still switches hard at 0.5. Blend bands or
      noise-dithered borders. Also: more biomes (desert, forest, tundra?) via the
      registry + provider — heights and blocks both driven.
- [ ] **Biome tint pass (runtime).** Interim is done and proven: grass top,
      grass overlay (second side quad, same brightness, 0.002 offset), tallgrass
      and oak leaves all bake fixed plains colors at stitch time. Remaining for
      true per-biome variation: tint RGB per biome + vertex-color multiply at
      mesh time instead of the bake, and the fancy cutout leaves (needs
      non-occluding leaf blocks). Until then everything reads plains.
- [ ] **Mountains that earn the name.** Current relief 37–51 worldwide. Ridged-noise
      biome with cliffs/overhangs — note: overhangs need no engine changes (meshing
      is general) but *do* need the skylight corner-flood follow-up below, or lips
      go harsh.
- [x] **Skylight corner flood (proper).** Done twice. First as an eager box flood
      (correct light, disastrous performance: 33-48k cells recomputed per
      edit/query, async queue + workers + serials + remesh waves to cover for
      it). Then rewritten vanilla-style after reading the 1.0 sources
      (`World.updateLightByType`, `Chunk.generateSkylightMap/relightBlock`):
      reads short-circuit at the skyline (15, no fill), virgin columns seed
      one top-down shaft inline (microseconds), edits run a LOCAL relight
      (zero/fill the changed column segment like relightBlock, then update the
      range — the range updates are load-bearing: single-cell updates leave
      virgin zeros that cascades can't clear. The edited cell itself is always
      updated too (below-skyline breaks never move the skyline, so the band
      misses them — breaching a wall into a sunlit canyon stayed black
      forever without this). Static virgin lips/mouths light
      via border reconciliation (`reconcileSkyChunk`, vanilla Chunk
      func_35633_i: compare every column's skyline with neighbors at mesh
      time, update differing ranges — flat ground costs ~50us). No queues, no
      workers, no serials, no remesh waves; per-event cost is hundreds of
      cells like vanilla, all synchronous. Leaves pass light exactly like
      vanilla (opacity 0, heightmap skips). Daylight on the light curve so
      shade grades. Proven by `LightTest` (shaft/bend/edit-symmetry on a stub)
      + `LevelTest` stone-lip bend on live terrain. `MirrorTest` digs through
      the real server loop and packet-ordered client applies: TileUpdate
      precedes its BreakEffect (destroy particles sample fresh air, not the
      solid cell — the black-burst fix), and both levels agree on tiles + all
      light afterwards.
- [x] **Real day/night block light.** Done vanilla-style after reading the 1.0
      sources (`World.calculateSkylightSubtracted`): stores stay full-day RAW,
      `core/DayCycle.subFor` yields 0..11 from the day curve, and
      `Level.getBrightness` subtracts at READ (torch/lava light untouched —
      caves pop at night). Meshes bake through the same read, so each sub
      step fires a `markAllDirty` remesh wave (VBOs kept, workers stream the
      new grade over seconds, no flash). The old uniform night overlay is
      deleted (it would double-dim midnight); fog color still scales with
      day. Proven by `DayTest` (sub mapping/monotonic) + `LevelTest`
      (moonlight floor, raw untouched, torch pops, dawn restores) + full
      suite green. Confirm the dawn/dusk grade in game (torch pools at
      midnight are the tell).
- [x] **Night-grade follow-ups (flaky torches).** Two real bugs found chasing
      "only some torches update at night": (1) the mesh version stamped at
      build END, so a wave/edit landing mid-mesh was absorbed and blessed as
      current — in-flight columns went stale for a whole grade step; the
      stamp moved to build START (post-reconcile), so mid-mesh bumps retry.
      (2) Submit was frustum-gated, so unviewed columns stayed day-baked
      indefinitely (torches behind you never re-graded until looked at) —
      the gate is gone, all dirty columns offer nearest-first, DRAW stays
      culled. Also: dead `torch.png` deleted from disk while the manifest
      still listed it (AtlasTest caught it) — slot 14 now points at
      `torch_on`, nothing after it moved.
      Round 2 (same symptom, "works placed-at-night, dead next night"):
      (3) a mesh worker throwing left its column at meshState=1 FOREVER —
      offer skips it (not 0), upload skips it (not 2), setDirty never
      touches meshState, so no future wave could reach it: permanently
      wrong-grade column on screen. The catch block now resets meshState
      to 0 (dirty survives → re-offered). If the pattern persists, check
      the log for `[mesh] worker failed` — that line now means retry, and
      its stack trace names the real thrower. (4) Grade waves converge
      drawn-first: submit offers in two passes (on-screen, then virgin),
      upload drains ready the same way, and offered queue priority biases
      drawn −1000 — virgin first-builds (563ms carve-dominated, bake
      current grade whenever they run) no longer head-of-line-block visible
      re-grades. (5) Reload embers: `Level.seedEmitters()` floods every
      emitter in the edit store at boot (GameServer after loadAllRegions,
      mirror after applyBulk) — torches boot lit instead of waiting behind
      two mesh passes; proven by `EmberTest` (bulk dark pre-seed, 14+13
      falloff post-seed, reseed stable). (6) `Chunk` VBOs allocate lazily
      on first non-empty upload (ctor GL-free → mesh mechanics now
      headless-testable; empty columns cost zero GL objects).
      Round 3 (same symptom, no `[mesh] worker failed` in the log): reproduced
      headlessly — a 5-torch line through bulk → seed → night-sub → CPU-mesh
      baked pools at 0.515 vs 0.914, with mirror stores reading 4/9/14/9/5.
      Root cause: `seedEmitters` (and mesh-time discovery) only planted when
      the cell was DARK (`== 0`), so a torch inside a neighbor's glow skipped
      its own 14 and sat at glow level — order-dependent, which is why every
      couple torches worked. Day hid it (sky dominates); night exposed it.
      Placed-at-night torches worked because `setTile` floods unconditionally.
      Both sites now plant whenever below own emission (`< want`;
      `floodAdd` already upgrades + re-propagates cheaply). Post-fix probe:
      all stores 14, all pools 0.914, zero version retries. `EmberTest` grew
      a two-torch regression (both own 14 after bulk + seed).
- [x] **World seeds + slots (engine side).** Done: `world/WorldMeta`
      (`world.dat` v6: magic + version + seed + time, tolerant reads, atomic
      tmp-rename writes — stock moved OUT to `player.dat`, see below);
      `Level` carries a `worldDir` (`region/` underneath, default slot
      `saves/testing` — the old project-root save moved there, root is code
      + assets only); `GameServer(conn, worldDir, seedOverride)`
      loads stored seed/time, reseeds worldgen + RNG, persists both on
      `save()` (SaveGame packet included — clock no longer resets to dawn);
      `Boot --seed N --world DIR` (`saves/<name>` slots) with stored-seed
      default. Proven by `WorldSlotTest` (defaults/roundtrip/corrupt,
      seed-isolated terrain, time roundtrip, legacy default) + full suite
      green, DepTest clean (WorldMeta lives in `world`, no new edges).
      Plus: `Level.loadAllRegions()` at server boot — nothing ever called
      `ensureRegions` on the server level, so the snapshot synced 0 columns
      and every relaunch wiped the world (saves were on disk, never loaded).
      Plus: player data bundled per world in `player.dat` (`world/PlayerData`:
      magic + version + presence + pos/look/hp + 36+36 stock + held, atomic
      tmp-rename writes, missing/corrupt scatters) — save writes it, boot
      resumes stock, held, pos, look and hp (Rng draws already match,
      teleport consumes none). Follow-up: the mirror seeded look zero and
      clobbered the resume on tick one through gatherInput (applyState
      never touches look) — fixed by seeding mirror yRot/xRot from the
      server player once at boot. No version ladder anywhere pre-public: older
      files keep seed/time and default the rest. Proven by `PlayerDataTest`
      (roundtrip pos/look/hp/stock/held, missing-file scatter).
      Proven by a `WorldSlotTest` reboot section that fails 3x reverted.
      Remaining: seed select/new-world UI with the P4 title screen.
- [x] **Image import (`boot --import img [--world DIR]`).** Done: pastes a
      two-tone picture FLAT on the void floor (image X→world X, image rows→+z;
      black→coal, white→sand, transparent stays air, >256px downscaled
      nearest) into a FRESH void world (refuses dirs with world.dat).
      Cells go through `Level.setTileBulk` — flood-free direct writes (open
      void art skylights by short-circuit, no emitters near, and per-cell
      setTile would flood each pixel plus queue a TileUpdate storm nobody
      drains). Spawn is a stone tower south of the sheet, facing it pitched
      down (fly with `'` spectator for the top-down view). Void is a real
      world type (`TerrainGenerator` void mode + `world.dat` v4 flag —
      reboot stays void), and fixed spawns persist the same way (R-key
      respawn returns there too; Rng scatter unchanged otherwise).
      Default dir `./import/`. Proven by `ImportTest` (void air, flat 27
      dark + 36 light, brightness above art, tower, facing, reboot
      persistence) + full suite green. Reads tolerate EXIF/ICC-tagged
      downloads (APP-strip retry when stock ImageIO chokes). Grayscale-to-
      palette ramps later (threshold is the seam).
- [x] **Texture packs, first hook (`--pack NAME`).** Done: zips in `./pack/`
      whose `textures/blocks/<name>.png` entries override same-named atlas
      tiles at boot — no code per tile, ever, at ANY square size. Follow-up:
      true hi-res — tiles shelf-pack native-sized with per-tile UV rects, so
      a 512 grass shows all 512 texels in world (fractions-of-tile math in
      torch strips/crack decals/particles/icons scales off each tile's own
      footprint; inset convention kept; resolution-independent alpha masks).
      Sheet auto-grows 2048→4096→8192 until the shelves fit (a full 512 set
      lands on 4096, logged once). Follow-up: the far-edge half-texel inset
      CROPPED edge texels to half width on two sides (visible on magnified
      art) — removed; exact boundaries sample cleanly under NEAREST with no
      mipmaps (sub-tile strip margins live inside the tile, unaffected). Proven by layout-agnostic `AtlasTest`
      (rect/inset/disjoint/pixel-identity), `TorchFaceTest`, `MeshTest`,
      `VegTest`, `TexturePackTest` + full suite. Worlds reference
      the pack by FILE NAME in `world.dat` v5 (relative on purpose: absolute
      paths rot on transfer); missing file reverts to built-ins with a warn, bad
      entries warn-and-skip. Changing packs needs a restart (atlas uploads
      once). For pixel-art worlds the pack holds the real art (e.g. a black
      `coal_ore.png` + white `sand.png`) instead of hardcoded remaps (tried
      and scrapped — per-id code doesn't scale). Proven by `TexturePackTest`
      (entry filtering, stitch swap + restore, missing fallback) + full
      suite green, DepTest clean.
- [x] **Texture alternates + face rotation (anti-tiling).** Done: `<base>1..9.png`
      discovered per manifest entry (pack-aware, canonical names win ties)
      and stitched past the canonical slots; `altsFor(base)` serves them.
      Tint falls back to the base name (tinted alts keep their biome color).
      Cubes pick pool = base + alts per position-face hash (remesh- and
      replay-stable, never RNG) and rotate tops/bottoms in 90° steps — sides
      never rotate (directional art like the grass band would tip over).
      Torches/crosses untouched (directional/partial art). Proven by
      `TexturePackTest` (injected discovery, pack-state isolation, tint
      fallback), `MeshTest` (variant coverage across 64 cells, rotation
      variety, re-render stability, order-insensitive UV sets) + full suite.
- [ ] **Ore rebalance pass.** Thresholds were tuned headless for *existence*, not
      gameplay (diamond may be generous). Playtest-driven numbers.

## P3 — Survival gameplay (the actual game)

- [x] **Health, damage, death (no mobs).** Done: `Player.hp` (20 half-hearts,
      server-authoritative, mirrored via `PlayerState.hp` to a hearts row
      above the hotbar — icons.png row 0: container (16,0), full (52,0),
      dedicated half (61,0); hidden for spectators). Fall damage vanilla-shaped (1 per
      block past 3, first landing after any respawn free so sky-spawns never
      death-loop); lava burns 4 per 30 ticks on feet-IN or standing-ON
      contact (lava stays solid walkable per the pinned BlocksTest decision).
      Death auto-respawns at full health, stock kept (friendly); bare R stays
      teleport-only. Follow-up: sky spawn removed — scatter spawns spiral
      (radius ≤8) for solid ground with TWO free cells above (1.8-tall body;
      leaves are full blocks, so no canopy/wood special-casing — a canopy
      top with sky above stands, a trunk under canopy fails headroom and the
      spiral steps around); void worlds keep the sky drop, fixed
      tower/import spawns untouched. R now lands ready, not dropping. Proven by `HealthTest` (grace, scaled fall, lava clock,
      death/respawn, sync). Left out: drowning (no water), mob hits (no
      mobs yet), death screen (P4).
- [ ] **Mobs, hostile first.** Zombie-like walker: gravity physics reuse (`Player.move`
      generalizes), melee contact damage, burn in daylight, spawn in dark at night,
      despawn far. Then passive (wanderer, no AI beyond stroll+flee).
- [x] **Drops + pickup.** Done: `Blocks.dropId` table (grass→dirt,
      stone→cobble, leaves→25% sapling, else self), `server/ItemEntity`
      (shared server/client physics: magnet <3, collect <0.9, 20-tick grab
      delay, 6000-tick expiry), packets 16/17/18/19/20 (spawn/remove/
      inventory/click/give), client billboard renderer. Full stock never
      voids (item stays down). Drops render as real spinning 0.25-cubes
      (vanilla EntityItem shape: Y-spin from age, hover bob, per-face tiles
      + baked 1.0/0.6/0.8 shading; torches/flowers stay flat billboards).
      Proven by `ItemTest` end-to-end (break→spawn→teleport→collect→stock)
      + `ItemCubeTest` (outward winding/tiles/shading/spin/centroid) +
      `NetTest` roundtrips.
- [x] **Inventory (stock + finite placement).** Done with drops: placement
      spends the first matching slot (ghosts ignored), stock syncs on change
      + at connect, persists in `world.dat` v3 (36 slots + held; v1/v2 files
      load with the rest empty), 8 starter torches on fresh worlds (no torch
      recipe yet — everything else you dig up). Proven by `ItemTest`
      (paid/ghost/persist/reboot) + `WorldSlotTest` meta v2/v3.
- [x] **Inventory panel (E) + hotbar facelift.** Done: 36-slot stock (9
      hotbar + 27 main), E opens an MC-layout panel (cursor released, clicks
      route to slots; E/ESC closes), server-authoritative SlotClick (shared
      pickup/place/merge/swap rules, no prediction) with held-stack sync,
      hover ring, held icon glued to the cursor. Hotbar procedurally
      MC-flavored (opaque bar, beveled wells, white select). Proven by
      `ItemTest` click rules + `NetTest` packet pins. ART HOOK: drop the
      official `widgets.png` at `res/textures/gui/widgets.png` and Gui gets
      reskinned (layout already mirrors vanilla regions).
- [x] **Iso item icons + digit counters.** Stock bars are gone: counts render
      from `ascii.png` (8px glyphs, vanilla ASCII order, shadowed pair,
      right-aligned, >1 only) in own blended batches on hotbar, panel, and
      held stack — the old procedural 3x5 table is deleted. Metrics per
      vanilla 1.0 RenderItem/FontRenderer (read, not copied): full 8px quads
      at 6px advance (our digits meter identically), string ending well+17,
      top well+9. Blocks render with
      the byte-exact vanilla 1.0 inventory projection (read off the 1.0
      sources: Rx210/Ry45/Ry−90 + mirror, top/east/south faces with vanilla
      corner→UV orders, baked 1.0/0.6/0.8 shading; 14.1x15.7 footprint —
      hand-placed diamonds kept reading stretched, so guessing stopped).
      Icon sits 2px down in the slot (vanilla's top bleed collided with the
      slot above). Flat sprite fallback for torches/flowers/saplings. Proven
      by `IsoTest` (footprint/vertex/winding) + full suite green.
- [x] **Official GUI art wired (hotbar).** `widgets.png` bound for the
      hotbar bar + selector (stretched over the 22px grid); `Tesselator`
      gained a real alpha channel (the inventory dim-veil rendered opaque
      black without it — world invisible behind the panel). `icons.png`
      reserved for P3 hearts/hunger.
- [x] **Inventory panel skinned + camera locked.** Panel body is the official
      176x166 art (`gui/container/inventory.png`, 18px pitch: store at
      (8,84), hotbar row at (8,142), mapped 1:1). Opening the panel (E) now
      freezes look and drops all movement/dig input (yaw/pitch ride along so
      the server never snaps); E/ESC closes. `GuiAssetTest` pins all three
      sheets. Confirm the look in game (panel alignment, dim veil).
- [x] **GUI measured true + scaled.** Pixel-read both sheets instead of
      assuming vanilla coords: hotbar wells are 16px at 20px pitch from x=3
      (drawn 1:1 — the old 22px stretch drifted icons down-right), panel grid
      confirmed at (8,84)/(8,142). Plus vanilla GUI scale (auto = largest
      1..3 holding 320x240 effective, `guiScale` in options.txt) via GL
      scale with mouse conversion to match. `OptionsTest` pins the rule.
- [ ] **Crafting.** 2×2 + 3×3 grid, recipes file, furnace later. (Split out
      of inventory, which shipped above.) Oak planks (id 21) place now;
      log→planks conversion is the first recipe when this lands.
- [ ] **Debug kit (+ key).** `DebugGive` packet wipes stock (held too) and
      writes 64× dirt/stone/cobble/torch/leaves/sand/planks over hotbar slots
      0-6 — replace, never top up, so repeats are stable. Ungated
      (single-player only — gate behind cheats/OP with real multiplayer).
      Bound in options (`keyGive`, default `=`).
- [x] **Block hardness (hands).** Done: `Blocks.hardness` table in ticks
      (torch/flowers 5, leaves 10, dirt/sand/gravel 15, grass 18, wood/planks
      30, stone/cobble 60, ores 90, bedrock/lava unbreakable — the old
      25-tick universal dig also let you mine the floor out of the world).
      `tickBreaking` accrues `1/hardness` per tick and skips unbreakables
      (no crack shown). Proven by `HardnessTest` (table ordering, timed dirt
      vs stone, 150-tick bedrock survival). Tool multipliers wait for tools.
- [ ] **Tools, tiers.** Tool items + multipliers (hand < wood < stone < iron
      diamond) on top of the hardness table; needs crafting for the tools.
- [ ] **Hunger (maybe).** 1.0 has it; decide if this game wants it. If yes: exhaustion
      sources, saturation, starvation damage. If no: document the call.
- [x] **Torch rules (support).** Done: `Block.needsSupport()` (default false,
      torch true); placement onto air rejected BEFORE stock is spent; mining
      the support pops the torch (same break path: TileUpdate + burst +
      self-drop) with an upward sweep that heals legacy floating stacks.
      Proven by `TorchRuleTest` (pop-off + drop, direct mine, floating
      reject with stock intact, supported place spends one) + `NetTest`
      setup updated (its roundtrip torch now sits on dirt).
- [ ] **Torch rules (rest).** Lava: flow + damage + light already correct;
      flow needs the block scheduler from sand/gravel. Lava LOOK: animated
      (`lava_still.png` 16x256 = 16 frames, any Wx(k*W) strip splits the
      same way) — one packed rect per tile, frames retained tinted, pixels
      swapped per tick via `glTexSubImage2D` (`Textures.animateAtlas`,
      ~15fps, meshes never remesh). Proven by `AtlasTest` (16 lava frames,
      statics read 1, frame-0 identity).
- [ ] **Sleep/skip night?** Only if day/night matters to survival (it will, once mobs
      burn/spawn). Decide with mobs.
- [ ] **Armor (much later).** Damage reduction curve + equip slots. After mobs exist.

## P4 — Presentation (menus, sound, sky)

- [ ] **Title screen + pause menu + death screen.** Game boots straight into world
      today. Pause must release the mouse and stop the sim clock cleanly.
      The pause menu owns the render-distance slider at launch (the `[`/`]`
      session-only debug keys go away then, with the rest of the launch-cut
      list: fullbright `\`, give `+`, spectator `'`).
- [x] **F3-style debug overlay.** Done: `F3` toggles ascii-text lines
      top-left (fps, xyz, clock phase + sub step, chunk census + mesh queue,
      hp + radius). Text via `Gui.drawString` (8px cells, 6px advance,
      shadowed) in a self-contained blended batch; lines built only while
      visible. Proven by `FontTest` (cell mapping, widths) + `GuiAssetTest`
      (ascii.png present at 128x128). Left out: light-at-target, biome id,
      seed (one-line additions when wanted).
- [ ] **Sound engine (OpenAL dylib ships unused!).** Footsteps, break/place thuds,
      splash, hurt, ambient cave/wind loops. Biggest missing juice item.
- [ ] **Sun, moon, stars.** Day/night exists as light+overlay only; skybox bodies +
      phases. Cheap (billboard quads), high vibe payoff.
- [ ] **Weather.** Rain/snow particles + dimming + (later) wet mechanics. Particle
      engine already batches; needs sky-visibility check per column (have heightmap).
- [ ] **Held-block first-person view.** Currently invisible hands. Needs small-scale
      mesh render in front of camera.
- [ ] **Clouds.** Flat drifting quads at fixed altitude (classic). Trivial, do with weather.
- [x] **Particles for events.** Done: torch embers (bright risers off the
      flame head), lava bubbles (slow risers off lava surfaces), landing
      puffs (dirt burst on hard touchdowns) — all through testable
      `ParticleEngine` spawners (`Particle.gravity` is now a field, negative
      = rise). Client `tickAmbient` drives them: edge-detected landings every
      tick, embers off the mirror's edit store (`Level.emittersNear`, placed
      torches only) + lava box scan on a slow schedule, no packets. Proven
      by `EventParticleTest` (rise direction, counts, scan near/far).
      Follow-up: embers/bubbles moved off block-tile slices onto
      `particles.png` sprites (flame cell (0,3), bubble ring cell (0,2) —
      read off the sheet pixels), rendered in a second alpha-tested batch;
      rise retuned way down (ember 0.02 up/-0.001 over ≤12 ticks, bubble
      0.01/-0.0005 over ≤18 — the old values rocketed). Sprite rects pinned
      by headless UV assertions. Follow-up: embers hover instead of rising
      (ctor's +0.06 debris up-bias damped out, zero gravity, ~0.4s life) and
      spawn sparsely (30% per torch per pass — every-torch-every-pass was a
      blizzard). Follow-up per vanilla 1.0 EntityFlameFX (read, not copied):
      flame spawns AT the head (x+0.5/y+0.7/z+0.5, zero velocity, ±0.05 own
      jitter) living 12-44 ticks — ours now matches (0.7 height, damped
      jitter, ≤44       life, 0.004 hover). Follow-up per vanilla 1.0 EntityLavaFX (read, not copied):
      flame spawns AT the head (x+0.5/y+0.7/z+0.5, zero velocity, ±0.05 own
      jitter) living 12-44 ticks — ours now matches (0.7 height, damped
      jitter, ≤44 life, 0.004 hover). Lava corrected the same way per
      EntityLavaFX: it JUMPS (0.05-0.45 up) and falls back under gravity over
      16-80 ticks — not a slow endless rise; plus its smoke trail (young =
      certain, engine rolls pre-tick for determinism) and shrink-over-life
      (scale × (1-ageFrac²) at render). Lateral jitter kept so pops arc.
      Follow-up: two over-density bugs — vanilla's 20tps constants ran 3x too
      fast in 60tps ticks (÷3: 0.02-0.15 up, 0.01 pull, 48-160 life) and every
      pop trailed dozens of smokes (trail throttled 5x, age-0 still certain);
      rarity now vanilla-scale (~1 pop per cell per quarter minute via
      shuffle-take-2 at 5%, lake-wide rate constant). Follow-up: touchdown
      death — landed pops sat on the lava until age-out; now feet-in or
      rested-on lava kills (gated on rested contact, else the overlapping
      spawn cell stillbirths every pop tick one). Proven pos/neg headlessly
      (dies well under the 48-tick minimum natural life over lava, persists
      over stone). Follow-up: scan
      starvation fixed — first-in-order fills drew a FIXED line of emitters
      (lava loop filled from the lowest-x edge, torches lost to map order)
      while the rest stayed dark; both now gather, shuffle, and pick up to 2
      at 50% each. Follow-up: smoke joins every flame (vanilla pair),
      gray 0.35, gently accelerating up, dissipating row-0 cells 7->0 over a
      vanilla 8-39 life via a `smokeAnim` UV rewind in `Particle.tick`.
- [x] **Break-progress cracking stages.** Done, three times over:
      `renderHit` draws `destroy_stage_0..9` by break progress through each
      block's own `renderCrack` over ALL faces (digging shakes the whole
      block). Crack UVs run in world fractions (triplanar-decal style), so
      texel density is uniform everywhere — a 2px post shows its own 2px
      slice, never a squeezed full tile. Lines clip to the underlying tile's
      alpha (no floating lines in air on torches/flowers) AND the crack
      tile's own transparency, brightness bakes from the cell (no white
      flash in caves), polygon offset kills coplanar shimmer. A twisted
      bilinear (bottom edge ran C->D, mapping (1,1) onto D) turned every
      sub-quad into a bowtie — two triangles meeting at the midpoint, caught
      by a `lerp2` corner-contract test. Cube faces sample the NEIGHBOR cell
      like the faces do (solid cells read ~0 even at noon — sampling the
      target made sunlit cracks pitch black), times the directional shade.
      Proven by `CrackStageTest` (mapping) + `BlockCrackTest` (all-faces conformance,
      mask clip, baked light) + full suite green; confirm the look in game.

## P5 — Systems (engine room)

- [ ] **Real networking over the protocol.** Packets already serialize (`NetTest`
      proves roundtrips) and `LocalConnection` mirrors the future socket shape.
      Remaining: TCP framing, handshake, BulkTiles chunking (4MB+ at once will
      spike), client-side prediction for movement/placement, server reconciliation.
- [ ] **Dedicated server thread.** Single-threaded alternating ticks are correct
      *because* the sim is trivial. Split when mobs/AI make ticks heavy; the packet
      boundary was designed for exactly this cut.
- [x] **Settings file (options.txt).** Done: `client/Options` — every key
      rebindable by LWJGL name (movement, save/respawn/spectate/fullbright/
      release/quit; arrows + Backspace-quit stay fixed alts), viewRadius
      (applied over `Config.VIEW_RADIUS`, clamped 2..10), sensitivity
      (0.1..3). Missing/corrupt lines fall back with warns; saved on exit.
      Proven by `OptionsTest` (defaults/roundtrip/fallback/clamps) + full
      suite green. Remaining: options UI (with P4 menus), fullscreen toggle,
      fog density, day length.
- [x] **Autosave interval.** Done: server ticks `autosaveClock`, saves every
      `Config.AUTOSAVE_TICKS` (18000 = 5 min at 60tps); dirty-region-gated
      so idle stretches cost one world.dat rewrite (which also persists the
      advancing clock). Proven by `AutosaveTest` (stored clock equals the
      tick count after one interval, reboot resumes it).
- [ ] **Screenshots.** `screencapture`-free in-game PNG dump (F2 classic). LWJGL has
      the pixels; needs PNG encode path.
- [ ] **Skin/player model.** Third-person (F5) needs a player mesh + animation;
      skip until multiplayer makes other players visible.
- [ ] **Demo/record mode?** Timer + input packets already serializable — trivially
      recordable later. Note, don't build.

## P6 — Far future (not 1.0-blockers, do not start)

- Nether/End dimensions (dimension id through Level + portals + separate region dirs)
- Redstone (power graph + block updates scheduler + wire/dust/torch/door mechanics)
- Enchanting/brewing/villages/strongholds/minecarts — full 1.0 parity list
- Mod loader / data packs (the Block/Biome registries are already shaped for it)

---

## Appendix A — controls (also missing from the repo: no README)

Move/look: WASD + mouse · Jump/Space · Fly: `'` spectator, Space up / Shift down ·
Fullbright x-ray: `\` (debug, launch-cut) · Render distance: `[`/`]` (debug, session-only; slider in pause menu at launch) · Dig: hold LMB (drops pop out, walk over to collect) ·
Place: RMB spends the selected slot (no free blocks — dig dirt to earn dirt) ·
Hotbar: 1–9/wheel (iso icons + count digits, empty slots bare) · Inventory: E (click to move stacks) · Save: Return ·
Respawn: R · Debug kit: +/= key · Self-hurt: `/` (debug: exactly 1 half-heart, for heart/half-heart HUD checks) · F3 overlay (fps/pos/time/mesh/hp) · Time cycle: `;` (showcase: dawn → noon → dusk → midnight on the authoritative clock, transitions sweep live) · Tick rate: `,`/`.` halve/double the sim (0.125x–8x debug, title shows it when ≠1x) · Mouse release: ESC (click back in to grab) · Quit: Delete (Fn+⌫ on laptops).
You start with 8 torches (no recipe yet). Gold ore exists but isn't placeable. All keys rebindable in
options.txt (written on exit); view distance + sensitivity + gui scale live there too.

## Appendix B — decisions already taken (don't re-litigate)

- Column chunks 16×64×16, radius 6, prebuild 2; mesh workers 5, sky workers 2,
  submit/upload 8/frame. (Mesh 3→5 after headless scaling showed 14→19 virgin
  chunks/sec; sky queue is vestigial since meshes fill first.)
- Flat-lit spectator via color-array skip; fog off on shaded pass in spectator.
- Fog density 0.01 (was 0.2 — blacked hillsides); fog color near-black scaled by day.
- Light: column skylight + flooded block light; day/night subtracts 0..11 at
  read (vanilla skylightSubtracted) with remesh waves per step, no overlay.
- Fixed seed 1337; `level.dat` migrates once to `region/*.mca`, then ignored.
- Single-threaded sim; protocol ready for sockets/threads when needed.
- Float coords good to ~16M blocks; beyond that the fix is origin rebasing.
- Vanilla 1.0 sources (on disk nearby) are INSPIRATION ONLY: read for shapes,
  numbers and protocol shape, then write our own code. No byte-copying into
  this tree unless an otherwise-unsolvable bug forces it — keep this codebase
  clean-room enough to never argue about.
