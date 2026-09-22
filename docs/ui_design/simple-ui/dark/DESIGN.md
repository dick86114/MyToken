---
name: Obsidian Translucent
colors:
  surface: '#10131a'
  surface-dim: '#10131a'
  surface-bright: '#363941'
  surface-container-lowest: '#0b0e15'
  surface-container-low: '#191b23'
  surface-container: '#1d1f27'
  surface-container-high: '#272a32'
  surface-container-highest: '#32353d'
  on-surface: '#e1e2ec'
  on-surface-variant: '#b9cbbd'
  inverse-surface: '#e1e2ec'
  inverse-on-surface: '#2d3038'
  outline: '#849588'
  outline-variant: '#3b4a40'
  surface-tint: '#00e390'
  primary: '#c9ffdb'
  on-primary: '#003920'
  primary-container: '#10f49c'
  on-primary-container: '#006b41'
  inverse-primary: '#006d42'
  secondary: '#bdf4ff'
  on-secondary: '#00363d'
  secondary-container: '#00e3fd'
  on-secondary-container: '#00616d'
  tertiary: '#fff0de'
  on-tertiary: '#432c00'
  tertiary-container: '#ffce7f'
  on-tertiary-container: '#7c5500'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#52ffac'
  primary-fixed-dim: '#00e390'
  on-primary-fixed: '#002111'
  on-primary-fixed-variant: '#005231'
  secondary-fixed: '#9cf0ff'
  secondary-fixed-dim: '#00daf3'
  on-secondary-fixed: '#001f24'
  on-secondary-fixed-variant: '#004f58'
  tertiary-fixed: '#ffdeac'
  tertiary-fixed-dim: '#ffba38'
  on-tertiary-fixed: '#281900'
  on-tertiary-fixed-variant: '#604100'
  background: '#10131a'
  on-background: '#e1e2ec'
  surface-variant: '#32353d'
typography:
  display-lg:
    fontFamily: Space Grotesk
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.02em
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system channels the refined, precision-engineered aesthetic of desktop macOS and translates it into a native mobile dashboard experience. It blends deep dark-mode obsidian surfaces with multi-layered optical glassmorphism, giving telemetry and account usage metrics an authoritative, mission-control feel. 

The aesthetic is built on three core pillars:
- **Depth through Translucency:** Interfaces are composed of stacked translucent layers over rich midnight backdrops, using frosted blur treatments to maintain structural hierarchy without relying on heavy solid fills.
- **Instrument Precision:** High-contrast, vivid neon accents punctuate the UI to highlight quotas, states, and critical thresholds, reminiscent of professional hardware monitors and Unix terminal aesthetics refined for consumer touchscreens.
- **Crisp Structural Integrity:** Micro-borders with low white opacity define boundaries cleanly, ensuring high legibility and contrast against dark grounds without visual clutter.

## Colors

The palette establishes an ultra-dark canvas illuminated by targeted, high-energy spectral accents:

- **Canvases & Surfaces:**
  - Base Obsidian: `#0A0D14` (root viewport background)
  - Midnight Glass Tier 1: `#121722` (primary card backings with 80% opacity)
  - Elevated Slate Tier 2: `#182030` (nested modules, segments, and active list rows)
- **Vivid Telemetry Accents:**
  - **Electric Emerald (`#10F49C`):** Primary action color, healthy usage thresholds (<70%), and positive deltas.
  - **Cyan Neon (`#00E5FF`):** Secondary data stream, bandwidth/network gauges, and informational metrics.
  - **Amber Glow (`#FFB300`):** Warning states, quota approaching capacity (70%–90%), and billing notices.
  - **Coral Red (`#FF4757`):** Critical exhaustion, error states, and threshold breaches (>90%).
- **Text & Foreground:**
  - High-Emphasis: `#F8FAFC` (pure readability on deep dark surfaces)
  - Mid-Emphasis: `#94A3B8` (labels, metric metadata, system breadcrumbs)
  - Low-Emphasis/Muted: `#475569` (disabled items, trailing hashes, timestamps)
- **Border Inlines:**
  - Subtle Glass Border: `rgba(255, 255, 255, 0.08)` to `rgba(255, 255, 255, 0.14)`

## Typography

The typographic hierarchy relies on a technical tripartite structure:
- **Headings & Key Value Outputs:** Set in `Space Grotesk` to introduce structural weight, mathematical precision, and modernist curves that feel native to high-end operating system utilities.
- **Interface & General Body:** Set in `Geist` for crisp geometric rendering, neutral character forms, and exceptional legibility across dense dashboard layouts.
- **Metrics, Counters, Code, & Status Chips:** Set in `JetBrains Mono` with tabular numerals enabled by default. This ensures usage metrics, IP addresses, bytes, and percentage values do not shift layout boundaries during live telemetry streaming.

## Layout & Spacing

The mobile layout implements a vertical, fluid single-column structure designed for touch ergonomic convenience and vertical stream-scrolling:
- **Canvas Boundaries:** Margins default to `1rem` (16px) on mobile portrait viewports, expanding to `1.5rem` (24px) on devices wider than 480px.
- **Modular Rhythm:** Dashboard modules adhere to an 8pt base grid. Component cards sit separated by `space-md` (16px) to let deep obsidian gutters clearly demarcate data domains.
- **Internal Grouping:** Micro-elements within cards (e.g., metric label to progress bar) use `space-xs` (4px) and `space-sm` (8px) for compact coherence, keeping information glanceable above the fold.
- **Sticky Island Navigation:** Bottom sheet controls and segmented tab switchers float with a constant `space-md` padding above the device safe area.

## Elevation & Depth

Rather than relying on diffuse drop shadows that wash out dark interfaces, this design system establishes depth through **optical layering, backdrop blurs, and luminous light inlines**:

- **Layer 0 (Canvas Base):** Solid `#0A0D14`.
- **Layer 1 (Frosted Dashboard Panels):** `rgba(18, 23, 34, 0.75)` combined with `backdrop-filter: blur(24px) saturate(180%)`. Border: 1px solid `rgba(255, 255, 255, 0.08)`.
- **Layer 2 (Interactive Floating Surfaces / Overlays):** `rgba(24, 32, 48, 0.85)` with `backdrop-filter: blur(32px)`. Border: 1px solid `rgba(255, 255, 255, 0.15)`. Top inset highlight: `inset 0 1px 0 rgba(255, 255, 255, 0.1)`.
- **Neon Atmospheric Glows:** Critical statuses and high-priority cards emit a localized radial shadow tint:
  - Electric Emerald: `0 8px 32px -8px rgba(16, 244, 156, 0.25)`
  - Coral Breach: `0 8px 32px -8px rgba(255, 71, 87, 0.3)`

## Shapes

The design uses continuous curvature (`squircle`-style corner geometry) typical of modern desktop OS hardware and software integration:
- Base card containers, sheets, and interactive dialogs use standard large curvature (`1rem` / 16px).
- Secondary controls, segmented sliders, and input fields use medium curvature (`0.5rem` / 8px).
- Badges, status pills, and toggle thumb switches utilize fully rounded stadium caps (`9999px`) to visually separate continuous data tags from rectangular card containers.

## Components

### Buttons
- **Primary Action:** Solid Electric Emerald (`#10F49C`) fill with `#0A0D14` high-contrast bold typography. Active state adds an internal drop shadow and scales down slightly (`0.98`).
- **Glass/Ghost Utility:** Background `rgba(255, 255, 255, 0.04)`, border 1px solid `rgba(255, 255, 255, 0.12)`, text `#F8FAFC`. On hover/touch, background transitions to `rgba(255, 255, 255, 0.08)`.

### Cards & Telemetry Containers
- Built with frosted glass backing (`rgba(18, 23, 34, 0.8)` with blur).
- Each card incorporates a crisp top highlight (`border-t: rgba(255, 255, 255, 0.15)`) simulating light cast from the top of the display glass.
- Usage cards house linear meter gauges with neon track fills over deeply muted track channels (`rgba(255, 255, 255, 0.06)`).

### Metric Gauges & Progress Trackers
- Track backgrounds: `rgba(255, 255, 255, 0.06)` with a 4px height and rounded pill ends.
- Dynamic fills: Electric Emerald for regular throughput, auto-shifting to Amber at 75% capacity and Coral Red at 90%. Gauge heads incorporate a soft 4px neon glow dot.

### Chips & Telemetry Badges
- Pill-shaped (`rounded-full`) with `label-sm` monospaced type.
- Composed of tinted translucent backdrops with color-matched 1px outlines (e.g., active cluster chip: `bg-[#10F49C]/10`, `border-[#10F49C]/30`, `text-[#10F49C]`).
- Includes a 6px glowing indicator dot at the leading edge.

### Segmented Controls & Switches
- macOS-style enclosed track: Container uses `#0A0D14` recessed fill with `space-xs` padding.
- Selected segment: Elevated with `rgba(255, 255, 255, 0.1)` glass, crisp 1px border `rgba(255, 255, 255, 0.18)`, and white high-contrast text.

### Inputs & Search Bars
- Recessed slate background (`rgba(10, 13, 20, 0.6)`) with subtle inset shadow.
- Border: `rgba(255, 255, 255, 0.1)`. Focus state creates a sharp `rgba(0, 229, 255, 0.4)` glow outline with cyan text caret.