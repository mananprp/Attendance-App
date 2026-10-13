package com.company.attendance.core.time

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

interface SntpClient {
    suspend fun fetchNtpTime(serverList: List<String> = DEFAULT_NTP_SERVERS): Long?

    companion object {
        val DEFAULT_NTP_SERVERS = listOf(
            "pool.ntp.org",
            "time.google.com",
            "time.cloudflare.com",
            "time.apple.com"
        )
    }
}

@Singleton
class SntpClientImpl @Inject constructor() : SntpClient {

    override suspend fun fetchNtpTime(serverList: List<String>): Long? = withContext(Dispatchers.IO) {
        val client = NTPUDPClient().apply {
            defaultTimeout = 3000
        }

        for (host in serverList) {
            try {
                val address = InetAddress.getByName(host)
                val timeInfo = client.getTime(address)
                timeInfo.computeDetails()
                val offset = timeInfo.offset ?: 0L
                val systemTime = System.currentTimeMillis()
                val calculatedNtpTime = systemTime + offset
                return@withContext calculatedNtpTime
            } catch (_: Exception) {
                // Try next host in fallback list
            }
        }
        null
    }
}
