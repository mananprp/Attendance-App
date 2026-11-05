package com.company.attendance.design

import com.company.attendance.design.components.isSwipeThresholdReached
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeToPunchThresholdTest {

    @Test
    fun `drag below 85 percent threshold returns false`() {
        val maxDrag = 200f
        assertFalse(isSwipeThresholdReached(0f, maxDrag))
        assertFalse(isSwipeThresholdReached(100f, maxDrag))
        assertFalse(isSwipeThresholdReached(169f, maxDrag)) // 84.5%
    }

    @Test
    fun `drag at or above 85 percent threshold returns true`() {
        val maxDrag = 200f
        assertTrue(isSwipeThresholdReached(170f, maxDrag)) // Exactly 85%
        assertTrue(isSwipeThresholdReached(190f, maxDrag)) // 95%
        assertTrue(isSwipeThresholdReached(200f, maxDrag)) // 100%
        assertTrue(isSwipeThresholdReached(220f, maxDrag)) // Past max
    }

    @Test
    fun `zero or negative maxDrag returns false`() {
        assertFalse(isSwipeThresholdReached(50f, 0f))
        assertFalse(isSwipeThresholdReached(50f, -10f))
    }
}