package com.example.tinyphish

import android.net.Uri
import org.json.JSONObject
import java.net.InetAddress

class PhishingDetector(private val model: JSONObject) {

    private val left = model.getJSONArray("children_left")
    private val right = model.getJSONArray("children_right")
    private val features = model.getJSONArray("features")
    private val thresholds = model.getJSONArray("thresholds")
    private val values = model.getJSONArray("values")

    private fun extractFeatures(url: String): DoubleArray {
        val lower = url.lowercase()

        val normalized = if (url.contains("://")) {
            url
        } else {
            "http://$url"
        }

        val host = Uri.parse(normalized).host.orEmpty()

        val isIp = try {
            val ipHost = host.removePrefix("[").removeSuffix("]")

            ipHost.isNotEmpty() && (
                    (
                            ipHost.matches(Regex("[0-9.]+")) &&
                                    InetAddress.getByName(ipHost).address.size == 4
                            ) || (
                            ipHost.contains(":") &&
                                    InetAddress.getByName(ipHost).address.size == 16
                            )
                    )
        } catch (e: Exception) {
            false
        }

        return doubleArrayOf(
            url.length.toDouble(),
            url.count { it == '.' }.toDouble(),
            url.count { it == '-' }.toDouble(),
            if (lower.startsWith("https://")) 1.0 else 0.0,
            if ('@' in url) 1.0 else 0.0,
            url.count { it.isDigit() }.toDouble(),
            url.count { it == '/' }.toDouble(),
            if (isIp) 1.0 else 0.0
        )
    }

    // Temporary helper for feature testing
    fun testFeatures(url: String): DoubleArray {
        return extractFeatures(url)
    }

    // Day 6, Fix 6: Explain URL characteristics
    fun getUrlEvidence(url: String): List<String> {
        val evidence = mutableListOf<String>()

        val normalized = if (url.contains("://")) {
            url
        } else {
            "https://$url"
        }

        val host = Uri.parse(normalized).host.orEmpty()
        val ipHost = host.removePrefix("[").removeSuffix("]")

        val isIp = try {
            ipHost.isNotEmpty() && (
                    (
                            ipHost.matches(Regex("[0-9.]+")) &&
                                    InetAddress.getByName(ipHost).address.size == 4
                            ) || (
                            ipHost.contains(":") &&
                                    InetAddress.getByName(ipHost).address.size == 16
                            )
                    )
        } catch (e: Exception) {
            false
        }

        if (isIp) {
            evidence.add("The hostname is an IP address.")
        }

        if ('@' in url) {
            evidence.add("The URL contains an @ symbol.")
        }

        if (url.length > 100) {
            evidence.add("The URL is unusually long (over 100 characters).")
        }

        if (url.startsWith("https://", ignoreCase = true)) {
            evidence.add(
                "The URL uses HTTPS; this does not guarantee safety."
            )
        } else {
            evidence.add("The URL does not start with HTTPS.")
        }

        if (evidence.isEmpty()) {
            evidence.add("No listed URL characteristics were found.")
        }

        return evidence
    }

    // Predict phishing or benign
    fun predict(url: String): Int {
        val row = extractFeatures(url)
        var node = 0

        while (left.getInt(node) != -1) {
            val featureIndex = features.getInt(node)
            val threshold = thresholds.getDouble(node)

            node = if (row[featureIndex] <= threshold) {
                left.getInt(node)
            } else {
                right.getInt(node)
            }
        }

        val classCounts = values.getJSONArray(node)

        val benignCount = classCounts.getDouble(0)
        val phishingCount = classCounts.getDouble(1)
        val total = benignCount + phishingCount

        val phishingProbability = if (total > 0.0) {
            phishingCount / total
        } else {
            0.0
        }

        return if (phishingProbability >= 0.30) 1 else 0
    }
}
