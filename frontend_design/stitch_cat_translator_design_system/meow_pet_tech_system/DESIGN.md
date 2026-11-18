---
name: Meow Pet-Tech System
colors:
  surface: '#fff8f6'
  surface-dim: '#e5d7d2'
  surface-bright: '#fff8f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff1ec'
  surface-container: '#f9ebe6'
  surface-container-high: '#f3e5e0'
  surface-container-highest: '#ede0db'
  on-surface: '#211a17'
  on-surface-variant: '#584235'
  inverse-surface: '#362f2c'
  inverse-on-surface: '#fceee9'
  outline: '#8c7263'
  outline-variant: '#e0c0af'
  surface-tint: '#994700'
  primary: '#994700'
  on-primary: '#ffffff'
  primary-container: '#ff7a00'
  on-primary-container: '#5c2800'
  inverse-primary: '#ffb68b'
  secondary: '#8f4d00'
  on-secondary: '#ffffff'
  secondary-container: '#ffa046'
  on-secondary-container: '#6e3a00'
  tertiary: '#5953aa'
  on-tertiary: '#ffffff'
  tertiary-container: '#9c96f2'
  on-tertiary-container: '#312980'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbc8'
  primary-fixed-dim: '#ffb68b'
  on-primary-fixed: '#321200'
  on-primary-fixed-variant: '#753400'
  secondary-fixed: '#ffdcc2'
  secondary-fixed-dim: '#ffb77b'
  on-secondary-fixed: '#2e1500'
  on-secondary-fixed-variant: '#6d3a00'
  tertiary-fixed: '#e3dfff'
  tertiary-fixed-dim: '#c5c0ff'
  on-tertiary-fixed: '#140166'
  on-tertiary-fixed-variant: '#413a91'
  background: '#fff8f6'
  on-background: '#211a17'
  surface-variant: '#ede0db'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 34px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: '0'
  body-bold:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: '0'
  body-base:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: '0'
  label-caps:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.04em
  stat-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.02em
  stat-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.01em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system expresses a joyful synthesis of organic feline warmth and machine-learning audio clarity. Built specifically for pet guardians seeking to understand their cats, the personality is empathetic, radiant, and scientifically grounded. It rejects cold, clinical diagnostic aesthetics in favor of a ginger-toned, sun-drenched palette that feels as comforting as a purring companion.

The design movement blends **Tactile Softness** with **Modern Clean Utility**:
- **Tactile Softness:** Pillowy, high-radius squircle surfaces, friendly volumetric action buttons, and breathing ambient glows that echo biological audio rhythms.
- **Modern Clean Utility:** Crisp, high-contrast information hierarchy, uncluttered probability bars, and structured baseline alignments that support bilingual RTL (Persian) and LTR (English) reading patterns seamlessly.
- **Emotional Intent:** To transform feline bio-acoustics into an approachable, delightful daily conversation, instilling confidence and mutual understanding between human and cat.

## Colors

The palette is rooted in the luminous warmth of ginger coats, sunlit cream interiors, and distinct intent markers derived from feline behavioral acoustic models.

### Palette Architecture
- **Primary (`#FF7A00`)**: Vibrant Ginger Orange. Acts as the primary brand signature, driving key actions, primary state transitions, and high-energy interactive hubs.
- **Secondary (`#FF9F45`)**: Golden Marmalade. Provides soft interactive fills, active tab highlights, and supportive accents.
- **Tertiary (`#7E78D2`)**: Soft Iris Lavender. Balances the energetic citrus palette with calm composure; used for secondary acoustic data, model architecture metadata, and introspective behavioral states.
- **Neutral Base (`#2B2421`)**: Deep Espresso Ink. Replaces harsh synthetic black with rich, organic warmth for optimal contrast across typography and iconography.

### Semantic Intent Mapping
Audio classification categories are mapped to distinct, emotionally coherent hues:
- **Food & Hunger (`#F59E0B` / Container `#FEF3C7`)**: Warm Honey Amber.
- **Attention & Affection (`#FF6B6B` / Container `#FFE8E8`)**: Salmon Coral.
- **Distress & Alert (`#8B5CF6` / Container `#EDE9FE`)**: Lavender Periwinkle.
- **Live Recording Pulse (`#FF4D4D`)**: High-luminance crimson indicator for live microphone listening rings.
- **Canvas (`#FFFBF7`)**: Warm milky ivory, creating a soft substrate that prevents optical fatigue.

## Typography

The typography structure delivers high legibility and geometric warmth across both numerical data readouts and bilingual text interfaces.

### Cross-Language Compatibility
- **Latin & Numerical Contexts:** Built on **Plus Jakarta Sans** for friendly curved counters, contemporary humanist geometry, and optical harmony with Persian letterforms.
- **Persian / RTL Text Strings:** Interleaved with **Vazirmatn** at equivalent weights. Both families share complementary x-heights and open interior spaces.
- **Baseline Clearance:** Persian script demands generous vertical clearance to accommodate diacritics (اعراب) without clipping; all text tiers maintain a minimum line-height ratio of 1.6x.
- **Data Stability:** All numeric match percentages and spectrogram timestamps leverage tabular numbers (`font-variant-numeric: tabular-nums`) to prevent layout shifting during real-time acoustic inference.

## Layout & Spacing

The layout is optimized for high-frequency mobile interaction and single-handed thumb navigation.

### Structural Framework
- **Grid Architecture:** Single-column fluid mobile viewport spanning 375px to 430px wide, capped at a centered container of `max-width: 480px` on desktop and tablet environments.
- **Rhythm & Multiples:** Built on an intentional 4px/8px incremental scale. Section gutters are locked at `1rem` (16px) on mobile viewports and expand to `1.5rem` (24px) on tablet viewports.
- **Thumb Zone Allocation:** Primary interactive hubs (the voice recording node, immediate feedback verification chips, and audio upload triggers) reside strictly in the lower 40% of the display viewport.
- **RTL Fluidity:** Layout matrices, card paddings, and horizontal metadata pairs invert automatically based on the `dir="rtl"` attribute, keeping numeric ratios right-anchored in Persian modes.

## Elevation & Depth

Elevation is established through warm chromatic layering and diffused ambient glow rather than neutral grey shadows.

### Elevation Hierarchy
1. **Canvas Level (0dp):** Substrate background tint `#FFFBF7`. Flat and non-reflective.
2. **Resting Containers (Level 1):** Subtle peach containers (`#FAF1E8`) framed with a 1px border of `#EADFD7`. Casts no harsh drop shadow; separation is achieved purely through chromatic contrast.
3. **Elevated Result Cards (Level 2):** Pure white cards (`#FFFFFF`) featuring a delicate, expansive ambient shadow: `0 14px 34px -8px rgba(43, 36, 33, 0.06)`. Accompanied by a 1.5px intent-tinted hairline stroke.
4. **Floating Action Elements (Level 3):** The primary recording hub and active modal drawers utilize colored ambient halos: `0 12px 28px -6px rgba(255, 122, 0, 0.38)`.
5. **Acoustic Pulse Glow (Active Live State):** Dual expanding rings with `rgba(255, 77, 77, 0.28)` radiating outward to indicate live microphone capture.

## Shapes

The geometric signature is defined by friendly, organic squircle curvature. Sharp, angular corners are avoided to reinforce a soft, tactile, pet-friendly atmosphere.

### Radius Assignments
- **Card Containers:** 1rem to 1.5rem (`rounded-lg` to `rounded-xl`) for main prediction boards, audio visualizer modules, and feedback drawers.
- **Pill Chips & Progress Bars:** Full continuous radius (`rounded-full` / 9999px) for category filters, confidence indicator chips, and audio progress channels.
- **Interactive Buttons:** 1rem (`rounded-lg`) squircle curves for primary touch buttons, providing broad hit surfaces with comfortable corners.

## Components

### 1. Voice Recording Hub (FAB State Machine)
- **Idle:** 80x80px circular button in `#FF7A00` with soft ginger glow. Houses a paw-microphone icon in `#FFFFFF`. Label below: "برای گوش دادن لمس کنید".
- **Recording:** Transforms to `#FF4D4D` with dual pulsing concentric rings that scale and fade outward. Displays real-time audio amplitude bars above the button.
- **Analyzing:** Central icon cross-fades into a dual-arc spinner (`#FF7A00` over `#FFE9D6`) with a status label: "در حال تحلیل میو...".

### 2. Probability Match Card (Hero Domain Component)
- **Container:** Pure `#FFFFFF` surface, `1.5rem` squircle radius, 24px internal padding, framed by a category-tinted 1.5px border.
- **Header:** Accommodates a 44px contextual emoji/glyph badge, high-contrast category title (`headline-md`), and a confidence pill badge (`#D1FAE5` with `#065F46` label reading "۹۴.۲٪ اطمینان").
- **Distribution Module:** Triple stacked progress channels (8px height, `rounded-full` track in `#F5EAE0`). Each progress fill animates horizontally using semantic intent tokens (`#F59E0B` for Food, `#FF6B6B` for Attention, `#8B5CF6` for Distress).

### 3. Feedback Loop Drawer
- **Surface:** `#FAF1E8` light peach container, `1rem` radius.
- **Action Chips:** Dual response pills ("بله 👍" / "خیر 👎") with white backgrounds and 1.5px active selection strokes.
- **Correction Module:** Revealing drawer presenting the three supervised ML target classes as selectable chips to feed the active model training loop.

### 4. Audio Spectrogram Visualizer
- **Track Container:** Rounded container (`#FAF1E8`) with 24 frequency amplitude columns in `#FF8A3D`.
- **Playback Cursor:** 2px vertical indicator in `#FF7A00` sliding across the waveform track.

### 5. Buttons & Navigation
- **Primary CTA:** 54px touch height, solid `#FF7A00`, `#FFFFFF` typography (`body-bold`), `rounded-lg`.
- **Secondary Ghost:** 48px touch height, `#FFE9D6` container, `#7A3200` typography.
- **Bottom Navigation Dock:** Floating capsule bar with frosted glass finish (`rgba(255, 251, 247, 0.88)` with 20px backdrop blur), 1px border `#EADFD7`, and active ginger indicator dots.