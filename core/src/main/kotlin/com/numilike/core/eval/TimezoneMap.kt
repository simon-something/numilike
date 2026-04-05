package com.numilike.core.eval

import java.time.ZoneId

object TimezoneMap {
    private val abbreviations = mapOf(
        "UTC" to "UTC", "GMT" to "GMT",
        "EST" to "America/New_York", "EDT" to "America/New_York",
        "CST" to "America/Chicago", "CDT" to "America/Chicago",
        "MST" to "America/Denver", "MDT" to "America/Denver",
        "PST" to "America/Los_Angeles", "PDT" to "America/Los_Angeles",
        "HST" to "Pacific/Honolulu",
        "AKST" to "America/Anchorage", "AKDT" to "America/Anchorage",
        "CET" to "Europe/Paris", "CEST" to "Europe/Paris",
        "EET" to "Europe/Athens", "EEST" to "Europe/Athens",
        "WET" to "Europe/Lisbon", "WEST" to "Europe/Lisbon",
        "IST" to "Asia/Kolkata",
        "JST" to "Asia/Tokyo",
        "KST" to "Asia/Seoul",
        "HKT" to "Asia/Hong_Kong",
        "SGT" to "Asia/Singapore",
        "AEST" to "Australia/Sydney", "AEDT" to "Australia/Sydney",
        "NZST" to "Pacific/Auckland", "NZDT" to "Pacific/Auckland",
        "BRT" to "America/Sao_Paulo",
        "ART" to "America/Argentina/Buenos_Aires"
    )

    private val cities = mapOf(
        "new york" to "America/New_York",
        "los angeles" to "America/Los_Angeles",
        "chicago" to "America/Chicago",
        "san francisco" to "America/Los_Angeles",
        "london" to "Europe/London",
        "paris" to "Europe/Paris",
        "berlin" to "Europe/Berlin",
        "tokyo" to "Asia/Tokyo",
        "seoul" to "Asia/Seoul",
        "hong kong" to "Asia/Hong_Kong",
        "singapore" to "Asia/Singapore",
        "sydney" to "Australia/Sydney",
        "dubai" to "Asia/Dubai",
        "mumbai" to "Asia/Kolkata",
        "moscow" to "Europe/Moscow",
        "istanbul" to "Europe/Istanbul",
        "toronto" to "America/Toronto",
        "vancouver" to "America/Vancouver",
        "mexico city" to "America/Mexico_City",
        "sao paulo" to "America/Sao_Paulo",
        "cairo" to "Africa/Cairo",
        "auckland" to "Pacific/Auckland"
    )

    fun resolve(name: String): ZoneId? {
        abbreviations[name.uppercase()]?.let { return ZoneId.of(it) }
        cities[name.lowercase()]?.let { return ZoneId.of(it) }
        return try { ZoneId.of(name) } catch (_: Exception) { null }
    }

    fun allAbbreviations(): Set<String> = abbreviations.keys
}
