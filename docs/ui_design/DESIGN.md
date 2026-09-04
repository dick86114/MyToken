---
name: Obsidian Precision macOS
colors:
  surface: '#0f131c'
  surface-dim: '#0f131c'
  surface-bright: '#353942'
  surface-container-lowest: '#0a0e16'
  surface-container-low: '#181c24'
  surface-container: '#1c2028'
  surface-container-high: '#262a33'
  surface-container-highest: '#31353e'
  on-surface: '#dfe2ee'
  on-surface-variant: '#c1c6d7'
  inverse-surface: '#dfe2ee'
  inverse-on-surface: '#2c3039'
  outline: '#8b91a0'
  outline-variant: '#414754'
  surface-tint: '#abc7ff'
  primary: '#abc7ff'
  on-primary: '#002f65'
  primary-container: '#438fff'
  on-primary-container: '#002959'
  inverse-primary: '#005cbb'
  secondary: '#7bd0ff'
  on-secondary: '#00354a'
  secondary-container: '#00a6e0'
  on-secondary-container: '#00374d'
  tertiary: '#c0c1ff'
  on-tertiary: '#1000a9'
  tertiary-container: '#8083ff'
  on-tertiary-container: '#0d0096'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d7e3ff'
  primary-fixed-dim: '#abc7ff'
  on-primary-fixed: '#001b3f'
  on-primary-fixed-variant: '#00458f'
  secondary-fixed: '#c4e7ff'
  secondary-fixed-dim: '#7bd0ff'
  on-secondary-fixed: '#001e2c'
  on-secondary-fixed-variant: '#004c69'
  tertiary-fixed: '#e1e0ff'
  tertiary-fixed-dim: '#c0c1ff'
  on-tertiary-fixed: '#07006c'
  on-tertiary-fixed-variant: '#2f2ebe'
  background: '#0f131c'
  on-background: '#dfe2ee'
  surface-variant: '#31353e'
  surface-card: rgba(18, 26, 43, 0.75)
  surface-popover: rgba(15, 23, 42, 0.88)
  surface-hover: rgba(255, 255, 255, 0.05)
  border-hairline: rgba(255, 255, 255, 0.08)
  border-highlight: rgba(56, 189, 248, 0.3)
  text-primary: '#F8FAFC'
  text-muted: '#94A3B8'
  text-subtle: '#64748B'
  status-success: '#10B981'
  status-warning: '#F59E0B'
  status-danger: '#EF4444'
  tag-rou: '#38BDF8'
  tag-ds: '#60A5FA'
  tag-glm: '#A78BFA'
  tag-vol: '#FB923C'
  tag-new: '#34D399'
typography:
  headline-hero:
    fontFamily: inter
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.025em
  headline-hero-mobile:
    fontFamily: inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-section:
    fontFamily: inter
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-section-mobile:
    fontFamily: inter
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.015em
  title-card:
    fontFamily: inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-large:
    fontFamily: inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-base:
    fontFamily: inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
  body-small:
    fontFamily: inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-code:
    fontFamily: jetbrainsMono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-badge:
    fontFamily: jetbrainsMono
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  label-metric:
    fontFamily: jetbrainsMono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
  space-3xl: 4rem
  space-4xl: 6rem
  popover-width: 440px
  container-max: 1200px
---

## Brand & Style

The design system embodies the high-craft elegance of modern macOS Sequoia and Sonoma operating systems combined with developer-grade technical transparency. Built for engineers, AI researchers, and technical power users who manage multi-provider LLM credentials and quotas, it projects uncompromising precision, privacy, and speed.

The visual style blends **Native macOS Glassmorphism** with **Technical Minimalism**:
- **Atmosphere**: Deep Obsidian navy canvas (`#0B0F17`) paired with translucent, backdrop-blurred frosted glass containers (`rgba(18, 26, 43, 0.75)`).
- **Subtlety & Polish**: Hairline micro-borders (`1px solid rgba(255, 255, 255, 0.08)`), crisp radial blue glow highlights echoing the app's compass-needle motif, and frictionless system surfaces.
- **Integrity**: Factual, zero-fluff data presentation. Monospaced indicators display authentic status codes (`ROU`, `DS`, `GLM`, `VOL`, `NEW`) without decorative fabrication or deceptive progress bars.
- **Emotional Response**: Quiet power, technical confidence, native platform belonging, and absolute local privacy.

## Colors

The palette is tuned specifically for deep dark mode ergonomics, mirroring native macOS dark vibrancy standards.

### Functional Palette Structure
- **Canvas Base (`#0B0F17`)**: Pitch-black navy providing infinite depth and optical isolation for translucent glass layers.
- **Surface Containers (`rgba(18, 26, 43, 0.75)`)**: Subtly saturated graphite-blue frosted surfaces with layered backdrop blur (`20px - 32px`).
- **Electric Cyan & Blue Accents (`#0080FF` / `#38BDF8`)**: Directly derived from the compass arrow icon, used for active indicators, primary actions, and focused states.
- **System Telemetry Colors**:
  - `status-success` (`#10B981`): Healthy API connection, validated local credentials.
  - `status-warning` (`#F59E0B`): Expiring balance, rate-limit thresholds.
  - `status-danger` (`#EF4444`): Key revocation, network outage.
- **Provider Tags**: High-legibility, desaturated pastels mapped to distinct LLM provider chips (`ROU`, `DS`, `GLM`, `VOL`, `NEW`) ensuring clear differentiation in compact menu bar status items.

## Typography

The typography couples Apple-grade geometric sans-serif clarity (`Inter` tracking SF Pro's proportions) with developer-grade monospaced figures (`JetBrains Mono`).

### Application Guidelines
- **Display & Headings**: Crisp negative tracking (`-0.02em` to `-0.025em`) renders tight, commanding titles that mirror Apple marketing typography.
- **Body Text**: Generous line heights (`1.45` to `1.55`) ensure optimal readability against dark frosted surfaces. Text colors cascade from `#F8FAFC` (headlines) to `#94A3B8` (body) to avoid stark contrast fatigue.
- **Technical & Token Metrics**: All token balances, currency figures, status shortcodes (`ROU`, `DS`, etc.), and latency values must render in `jetbrainsMono` with tabular numerals enabled to maintain vertical alignment in dense tables and popover sheets.

## Layout & Spacing

Layouts adhere to an 8pt base grid with an intentional desktop-first bias that honors macOS window geometries.

### Desktop & Container Specs
- **Primary Content Container**: Centered max-width of `1200px` with fluid `24px` to `48px` horizontal gutters.
- **Popover Simulation Width**: Fixed at `440px` (standard macOS menu bar popover spec as specified in system requirements).
- **Aspect Ratio Ratios**: All graphic showcases strictly adhere to verified system canvases:
  - Hero scenes: `2:1`
  - Feature highlights: `16:9` and `16:10`
  - Deep inspect sheets: `4:3`
  - Menu Bar popover details: `3:4`

### Responsive Reflow
- **Desktop (≥1024px)**: 12-column grid, 24px gutters, double-card showcases, multi-column matrix tables.
- **Tablet (768px - 1023px)**: 8-column grid, 16px gutters, menu bar popover interactive previews remain locked to fixed width `440px` centered inside cards.
- **Mobile (<768px)**: 4-column layout, horizontal scrolling cards for matrix tables, and stacked action buttons.

## Elevation & Depth

Visual hierarchy uses macOS Sonoma/Sequoia style translucent materials rather than heavy drop shadows. Depth is communicated via three synchronized layers:

1. **Backdrop Blur**: `backdrop-filter: blur(20px) saturate(180%)` on cards, rising to `blur(32px)` on simulated menu bars and popovers.
2. **Hairline Top-Light Strokes**: Subtle gradient border (`1px solid rgba(255, 255, 255, 0.12)` fading to `rgba(255, 255, 255, 0.03)` at the bottom) emulating physical beveled glass catching overhead window light.
3. **Diffused Ambient Glow**:
   - **Level 1 (Card Rest)**: `0 4px 20px -2px rgba(0, 0, 0, 0.5)`
   - **Level 2 (Popover / Floating Sheet)**: `0 20px 40px -10px rgba(0, 0, 0, 0.65), 0 0 0 1px rgba(255, 255, 255, 0.08)`
   - **Level 3 (Electric Accent Hover / Glow)**: `0 0 24px 0 rgba(0, 128, 255, 0.25)` centered beneath primary calls to action.

## Shapes

The shape system matches the continuous corner curves (squircle curvature) of macOS native windows and buttons:

- **Cards & Popovers**: `rounded-2xl` (`1rem` to `1.25rem` / `16px - 20px`), mirroring native macOS application windows.
- **Buttons & Interactive Controls**: Pill-shaped or smoothed rounded rectangles (`rounded-lg` / `10px` for standard controls, full `rounded-full` for status indicators and quick pills).
- **Status Badges & Chips**: `rounded-md` (`6px`) with compact internal padding (`2px 6px`) to keep technical codes dense and structured.

## Components

### 1. macOS Menu Bar Simulation
A fixed or floating simulated dark menu bar with authentic SF-style system icons, right-aligned status indicators displaying live shortcode pills:
- Height: `32px`.
- Background: `rgba(11, 15, 23, 0.8)` with backdrop blur.
- Status item chips (`ROU`, `DS`, etc.): Monospaced, 11px font, with a live ping indicator dot (`6px` circle with pulse animation on active refresh).

### 2. Native Popover Card (440pt Container)
The centerpiece UI component representing the core app experience:
- Dimensions: Width fixed at `440px`, height auto-adjusting to token providers.
- Styling: `surface-popover` background, `border-hairline`, squircle corners, top caret notch pointing to the active menu bar item.
- Header: Provider name, active model status, and quota refresh timestamps (`JetBrains Mono`, muted).
- Content Row: Quota balance rendered in large tabular digits (`24px`), alongside daily burn rate micro-bars.

### 3. Buttons
- **Primary CTA ("Download for macOS")**: Pill-shaped or `rounded-xl`, electric blue gradient (`linear-gradient(135deg, #0080FF 0%, #0066CC 100%)`), crisp white text, hairline white inner glow (`inset 0 1px 0 rgba(255,255,255,0.3)`), and subtle drop shadow. Accompanied by Apple Silicon / Intel universal badge.
- **Secondary / Ghost**: Frosted glass surface (`surface-hover`), hairline border, text `#F8FAFC`, active scale feedback (`scale(0.98)`).

### 4. Credential & Provider Chips
- Fixed format: `[PROVIDER CODE] [KEY STATUS/MASKED KEY]`.
- Background: `rgba(255, 255, 255, 0.04)`.
- Border: `1px solid rgba(255, 255, 255, 0.06)`.
- Text: Primary shortcode in designated provider color (e.g. Cyan for `ROU`, Violet for `GLM`), with key string masked (`sk-...4x9q`) in `jetbrainsMono` muted.

### 5. Capability Matrix & Feature Cards
- Dark frosted glass grid items with micro-glow hover animations.
- Checkmarks and validation dots rendered in `status-success` (`#10B981`) with no vague progress bars. Every metric shows definitive numerical values or local storage confirmation tags (`Keychain AES-256`, `Zero Cloud Relay`).