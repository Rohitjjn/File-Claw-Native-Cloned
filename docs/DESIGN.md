# File Claw: Design System (v1.1.0)

**Direction:** Claude's warm, editorial calm, with a restrained glass layer. Premium means *quiet*: few colours,
generous space, hairlines instead of shadows, motion that explains rather than decorates.

## Colour tokens
| Token | Light | Dark | Use |
|---|---|---|---|
| `background` | `#FAF9F5` | `#181715` | canvas |
| `surface` | `#FAF9F5` | `#252320` | sheets, dialogs |
| `primary` | `#CC785C` (terracotta) | `#CC785C` | actions, selection |
| `onBackground` | `#141413` | `#FAF9F5` | text |
| `outline` | `#E6DFD8` | `#2D2C2A` | hairlines |
| `secondary` (dark) | n/a | `#E8A55A` | accent |

## Glass tokens (`ui/theme/Glass.kt`)
| Token | Light | Dark |
|---|---|---|
| `fill` | white @ 62% | `#2B2926` @ 58% |
| `fillStrong` | white @ 80% | `#32302C` @ 80% |
| `strokeStart → strokeEnd` | white 95% → terracotta 20% | white 18% → white 4% |
| `sheen` (top highlight) | white @ 55% | white @ 7% |
| `glowA/B/C` (ambient) | terracotta 22%, amber 18%, lavender 16% | terracotta 28%, amber 14%, indigo 14% |

**How the glass is built:** translucent fill + top sheen gradient + gradient hairline border (`Modifier.glass()`),
over a backdrop of three large radial gradients (`GlassBackdrop`). Radial gradients instead of
`RenderEffect` blur: identical look on API 26+, near-zero GPU cost, and text is never blurred.

## Rules
1. **Reading surfaces are opaque.** Document, code, hex and editor viewers keep solid `background` for legibility.
2. **Glass is for chrome and cards:** app bars, cards, sheets, the permission screen.
3. One accent (terracotta). Amber only in dark mode.
4. 1 dp hairlines, 16 dp card radius, 8 dp spacing grid. Elevation via contrast, not shadow.
5. Contrast: body text ≥ 4.5:1 on its actual backdrop (check the glass fill *over* the glow, not in isolation).
6. Motion: ≤ 250 ms, ease-out; no motion that blocks input.
7. Notices (truncated, read-only, converted, view-only) use one component: `NoticeBanner`.

## Rollout status
| Area | Status |
|---|---|
| Tokens, `glass()`, `GlassBackdrop`, theme provider | ✅ done |
| `ClaudeCard`, `ClaudeAppBar` | ✅ glass |
| Permission screen | ✅ backdrop |
| Root-level backdrop behind all list screens | ⏳ needs a NavHost transition policy first: Scaffolds currently paint an opaque `background`, and making them transparent would show overlapped screens during crossfades |
| Bottom sheets / dialogs / search field | ⏳ |
| On-device contrast + 60 fps verification | ⏳ not yet measured |
