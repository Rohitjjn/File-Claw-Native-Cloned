package com.example.formats.pptx

import org.junit.Assert.assertEquals
import org.junit.Test

class PresentationZoomPolicyTest {
    @Test fun coercesIntoPresentationRange() {
        assertEquals(0.5f, PresentationZoomPolicy.coerce(0.1f), 0.0001f)
        assertEquals(3.0f, PresentationZoomPolicy.coerce(9.0f), 0.0001f)
        assertEquals(1.5f, PresentationZoomPolicy.coerce(1.5f), 0.0001f)
        assertEquals(1.0f, PresentationZoomPolicy.coerce(Float.NaN), 0.0001f)
    }

    @Test fun doubleTapTogglesBetweenFitAndReadingZoom() {
        assertEquals(2.0f, PresentationZoomPolicy.toggleDoubleTap(1.0f), 0.0001f)
        assertEquals(1.0f, PresentationZoomPolicy.toggleDoubleTap(2.0f), 0.0001f)
    }

    @Test fun percentConversionMatchesBounds() {
        assertEquals(100, PresentationZoomPolicy.toPercent(1.0f))
        assertEquals(50, PresentationZoomPolicy.toPercent(0.1f))
        assertEquals(300, PresentationZoomPolicy.toPercent(9.0f))
        assertEquals(1.5f, PresentationZoomPolicy.fromPercent(150), 0.0001f)
    }
}
