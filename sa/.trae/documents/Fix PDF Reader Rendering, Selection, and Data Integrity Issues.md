I will fix the critical issues in `ModernPdfReader.vue` regarding text rendering, selection precision, UI state management, and data integrity.

**1. Fix Blurry Text & Imprecise Selection (Visual & Coordinate Mismatch)**

* **Root Cause**: The Canvas element is rendered with high-DPI dimensions (e.g., `width=2000` for a 1000px viewport on Retina), but lacks explicit CSS `width/height` styles. This causes the browser to render it at its natural pixel size (large) or scale it incorrectly, misaligning it with the `textLayer`.

* **Action**: Uncomment and enforce `canvas.style.width` and `canvas.style.height` in `renderPage` to match the logical viewport size.

**2. Fix Floating Action Bubble Persistence (UI State)**

* **Root Cause**: `handleTextSelection` returns early when the selection is empty (collapsed) but fails to set `bubbleVisible = false`. This leaves the toolbar stuck on screen.

* **Action**: Update `handleTextSelection` to explicitly hide the bubble when the selection is cleared.

**3. Fix "Architecture Issue" with Data Integrity (Rotation & State)**

* **Root Cause**:

  1. Rotation logic (`rotateLeft`/`rotateRight`) clears `renderedPages` but fails to clear `baseViewports` and `pageDimensions` caches. Subsequent renders reuse viewports with the *old* rotation, causing coordinate calculations (`getSelectionCoords`) to be invalid.
  2. Stuck bubble (from Issue #2) allows users to trigger actions on stale `noteForm` data (wrong position/page), leading to data corruption.

* **Action**:

  * Clear `baseViewports` and `pageDimensions` in rotation handlers.

  * Hiding the bubble (Fix #2) ensures users can't click actions for stale selections.

**4. Safety Checks**

* **Action**: Add validation in `createHighlight` and `saveNote` to ensure `documentId` and `coord` are present, providing clear user feedback if not.

**Implementation Steps**:

1. Modify `src/components/Reader/ModernPdfReader.vue` to apply the fixes.

