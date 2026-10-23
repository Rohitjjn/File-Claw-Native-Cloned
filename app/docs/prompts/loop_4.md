# 🦞 FILE CLAW — LOOP 4 PROMPT: ACTION PILL, GRID VIEW & IN-VIEWER SEARCH

## 🎯 MISSION
Implement unified floating viewer controls, rich grid/list toggle, favorites system, and in-viewer search.

---

### CATEGORY 1: UI-RELATED CHANGES
1. **Viewer Floating Action Pill**:
   - Unified glassmorphic bottom floating pill across PDF, Image, and Text viewers.
   - Auto-fade on pan/scroll and tap toggle.
2. **File List Grid/List View Toggle & Cards**:
   - 2-column grid view with preview thumbnails, colored `FileExtensionBadge`, formatted size, and star actions.

---

### CATEGORY 2: CORE FEATURES
1. **Recent Files & Favorites Tab**:
   - Instant file starring (`Icons.Filled.Star`) and persistent favorite filtering.
2. **In-Viewer Text Search (PDF & Text files)**:
   - Floating search overlay with match count navigation (`Match X of Y`).
   - Pure-Kotlin `PdfTextSearcher` parsing uncompressed and FlateDecode streams.
