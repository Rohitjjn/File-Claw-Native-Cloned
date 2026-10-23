# 🦞 FILE CLAW — LOOP 6 PROMPT: FOCUS MODE, SYNTAX THEMES, ATOMIC EXPORT & OUTLINE

## 🎯 MISSION
Implement double-tap immersive full-screen focus mode, syntax theme & typography picker, atomic file export, and document outline/TOC.

---

### CATEGORY 1: UI-RELATED CHANGES
1. **Double-Tap / Immersive Full-Screen Focus Mode**:
   - Double-tap or fullscreen icon hides toolbars and system navigation without black gaps.
   - Unobtrusive floating exit pill and back button.
2. **Syntax Theme & Typography Picker**:
   - Themes: Claude Glass (Default), Nord Dark, Warm Parchment.
   - Font size adjuster (+ / - buttons, 11sp to 22sp) with persisted settings.

---

### CATEGORY 2: CORE FEATURES
1. **Atomic In-App File Save & Export Modified Copy**:
   - "Save As / Export Copy" dialog saving atomic copies using `AtomicFileWriter`.
   - Backup creation before file overwrite guaranteeing zero data loss.
2. **Markdown & Document Table of Contents (TOC) Outline**:
   - `DocumentOutlineBottomSheet.kt` parsing `#` headings with jumpable navigation.
