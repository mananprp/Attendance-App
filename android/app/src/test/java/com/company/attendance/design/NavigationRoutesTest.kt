package com.company.attendance.design

import com.company.attendance.core.model.PunchType
import com.company.attendance.design.components.BottomNavItem
import com.company.attendance.design.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutesTest {

    @Test
    fun `verify screen routes and parameterized punch route generation`() {
        assertEquals("login", Screen.Login.route)
        assertEquals("onboarding", Screen.OnboardingWizard.route)
        assertEquals("model_download", Screen.ModelDownload.route)
        assertEquals("home", Screen.Home.route)
        assertEquals("calendar", Screen.Calendar.route)
        assertEquals("profile_settings", Screen.ProfileSettings.route)
        assertEquals("component_gallery", Screen.ComponentGallery.route)

        assertEquals("punch_capture/IN", Screen.PunchCapture.createRoute(PunchType.IN))
        assertEquals("punch_capture/OUT", Screen.PunchCapture.createRoute(PunchType.OUT))
    }

    @Test
    fun `verify bottom navigation bar routes`() {
        assertTrue(Screen.shouldShowBottomBar(Screen.Home.route))
        assertTrue(Screen.shouldShowBottomBar(Screen.Calendar.route))

        assertFalse(Screen.shouldShowBottomBar(Screen.Login.route))
        assertFalse(Screen.shouldShowBottomBar(Screen.OnboardingWizard.route))
        assertFalse(Screen.shouldShowBottomBar(Screen.ModelDownload.route))
        assertFalse(Screen.shouldShowBottomBar(Screen.PunchCapture.route))
        assertFalse(Screen.shouldShowBottomBar(Screen.ProfileSettings.route))
        assertFalse(Screen.shouldShowBottomBar(null))
    }

    @Test
    fun `verify BottomNavItem enum matches routes`() {
        assertEquals("home", BottomNavItem.HOME.route)
        assertEquals("calendar", BottomNavItem.CALENDAR.route)
    }
}
