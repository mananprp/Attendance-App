package com.company.attendance.core.location

import android.content.Context
import com.company.attendance.core.interfaces.GeofenceResult
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationCheckerTest {

    private val context: Context = mockk(relaxed = true)
    private lateinit var locationChecker: LocationCheckerImpl

    @Before
    fun setUp() {
        locationChecker = LocationCheckerImpl(context)
    }

    @Test
    fun `calculateDistanceMeters returns approximately zero for identical coordinates`() {
        val lat = 28.613939
        val lng = 77.209021

        val distance = locationChecker.calculateDistanceMeters(lat, lng, lat, lng)
        assertEquals(0f, distance, 0.01f)
    }

    @Test
    fun `calculateDistanceMeters accurately calculates known distance between New Delhi landmarks`() {
        // India Gate (28.6129, 77.2295) to Rashtrapati Bhavan (28.6143, 77.1994)
        // Known geographic distance is ~2.94 km (2940 meters)
        val lat1 = 28.6129
        val lon1 = 77.2295
        val lat2 = 28.6143
        val lon2 = 77.1994

        val distance = locationChecker.calculateDistanceMeters(lat1, lon1, lat2, lon2)
        assertTrue("Distance should be around 2940m, was $distance", distance in 2900f..3000f)
    }

    @Test
    fun `small coordinate delta within 100m geofence calculates accurately`() {
        // Office center
        val officeLat = 28.613939
        val officeLng = 77.209021

        // 0.0003 degrees latitude delta is ~33.3 meters
        val userLat = officeLat + 0.0003
        val userLng = officeLng

        val distance = locationChecker.calculateDistanceMeters(officeLat, officeLng, userLat, userLng)
        assertTrue("Distance $distance should be within 100m radius", distance in 30f..40f)
    }

    @Test
    fun `large coordinate delta exceeding 100m geofence calculates accurately`() {
        // Office center
        val officeLat = 28.613939
        val officeLng = 77.209021

        // 0.002 degrees latitude delta is ~222 meters
        val userLat = officeLat + 0.002
        val userLng = officeLng

        val distance = locationChecker.calculateDistanceMeters(officeLat, officeLng, userLat, userLng)
        assertTrue("Distance $distance should exceed 100m radius", distance > 100f)
    }
}
