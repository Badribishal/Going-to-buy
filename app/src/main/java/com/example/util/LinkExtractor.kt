package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedProductInfo(
    val title: String? = null,
    val price: Double? = null,
    val imageUrl: String? = null,
    val storeName: String? = null,
    val originalUrl: String,
    val isAutoExtracted: Boolean = false
)

object LinkExtractor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun extract(url: String): ExtractedProductInfo = withContext(Dispatchers.IO) {
        val cleanUrl = cleanUrlString(url)
        val domainStore = getStoreFromDomain(cleanUrl)

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ExtractedProductInfo(
                    storeName = domainStore,
                    originalUrl = cleanUrl,
                    isAutoExtracted = false
                )
            }

            val html = response.body?.string() ?: ""
            val title = extractMetaTag(html, "og:title")
                ?: extractMetaTag(html, "twitter:title")
                ?: extractTagContent(html, "title")
                ?: extractMetaTag(html, "title")

            val cleanedTitle = cleanTitle(title, domainStore)

            val image = extractMetaTag(html, "og:image")
                ?: extractMetaTag(html, "twitter:image")
                ?: extractMetaTag(html, "image")

            val siteName = extractMetaTag(html, "og:site_name") ?: domainStore

            val price = extractPrice(html)

            ExtractedProductInfo(
                title = cleanedTitle,
                price = price,
                imageUrl = image,
                storeName = siteName ?: domainStore,
                originalUrl = cleanUrl,
                isAutoExtracted = cleanedTitle != null || price != null
            )
        } catch (_: Exception) {
            ExtractedProductInfo(
                storeName = domainStore,
                originalUrl = cleanUrl,
                isAutoExtracted = false
            )
        }
    }

    private fun cleanUrlString(input: String): String {
        var trimmed = input.trim()
        val matcher = Pattern.compile("https?://\\S+").matcher(trimmed)
        if (matcher.find()) {
            trimmed = matcher.group()
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }

    private fun getStoreFromDomain(url: String): String {
        return try {
            val host = URI(url).host?.lowercase() ?: ""
            when {
                host.contains("amazon") -> "Amazon"
                host.contains("flipkart") -> "Flipkart"
                host.contains("myntra") -> "Myntra"
                host.contains("apple") -> "Apple"
                host.contains("nike") -> "Nike"
                host.contains("ajio") -> "Ajio"
                host.contains("meesho") -> "Meesho"
                host.contains("zara") -> "Zara"
                host.contains("hm.com") || host.contains("h&m") -> "H&M"
                host.contains("ikea") -> "IKEA"
                host.contains("croma") -> "Croma"
                host.contains("reliancedigital") -> "Reliance Digital"
                host.contains("tatacliq") -> "Tata CLiQ"
                host.contains("ebay") -> "eBay"
                host.contains("aliexpress") -> "AliExpress"
                else -> {
                    val parts = host.removePrefix("www.").split(".")
                    if (parts.isNotEmpty()) parts[0].replaceFirstChar { it.uppercase() } else "Store"
                }
            }
        } catch (_: Exception) {
            "Store"
        }
    }

    private fun extractMetaTag(html: String, property: String): String? {
        val patterns = listOf(
            "<meta\\s+property=[\"']$property[\"']\\s+content=[\"'](.*?)[\"']",
            "<meta\\s+content=[\"'](.*?)[\"']\\s+property=[\"']$property[\"']",
            "<meta\\s+name=[\"']$property[\"']\\s+content=[\"'](.*?)[\"']",
            "<meta\\s+content=[\"'](.*?)[\"']\\s+name=[\"']$property[\"']"
        )
        for (pat in patterns) {
            val matcher = Pattern.compile(pat, Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
            if (matcher.find()) {
                val match = matcher.group(1)?.trim()
                if (!match.isNullOrEmpty()) return decodeHtmlEntities(match)
            }
        }
        return null
    }

    private fun extractTagContent(html: String, tag: String): String? {
        val matcher = Pattern.compile("<$tag.*?>(.*?)</$tag>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
        if (matcher.find()) {
            val content = matcher.group(1)?.trim()
            if (!content.isNullOrEmpty()) return decodeHtmlEntities(content)
        }
        return null
    }

    private fun cleanTitle(title: String?, store: String?): String? {
        if (title == null) return null
        var cleaned = title.replace("\n", " ").replace("\r", " ").trim()
        if (store != null) {
            cleaned = cleaned.replace(Regex("(?i)\\s*\\|\\s*$store.*$"), "")
                .replace(Regex("(?i)\\s*-\\s*$store.*$"), "")
                .replace(Regex("(?i):\\s*Buy.*online.*at.*$"), "")
        }
        return cleaned.trim()
    }

    private fun extractPrice(html: String): Double? {
        val metaPricePatterns = listOf(
            "property=[\"']product:price:amount[\"']\\s+content=[\"']([0-9.]+)[\"']",
            "content=[\"']([0-9.]+)[\"']\\s+property=[\"']product:price:amount[\"']",
            "itemprop=[\"']price[\"']\\s+content=[\"']([0-9.]+)[\"']",
            "content=[\"']([0-9.]+)[\"']\\s+itemprop=[\"']price[\"']"
        )
        for (pattern in metaPricePatterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(html)
            if (matcher.find()) {
                val raw = matcher.group(1)
                val price = raw?.toDoubleOrNull()
                if (price != null && price > 0) return price
            }
        }

        // Search for ₹ symbol followed by numbers
        val rupeeMatcher = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)").matcher(html)
        if (rupeeMatcher.find()) {
            val priceStr = rupeeMatcher.group(1)?.replace(",", "")
            val parsed = priceStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0 && parsed < 10000000) {
                return parsed
            }
        }

        return null
    }

    private fun decodeHtmlEntities(input: String): String {
        return input.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }
}
