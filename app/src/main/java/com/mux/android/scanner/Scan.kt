package com.mux.android.scanner

import com.google.mlkit.vision.barcode.common.Barcode

/** A decoded QR code / barcode, ready to show. */
class ScanResult(
    val value: String,          // raw text
    val format: String,         // QR CODE, EAN-13 ...
    val kind: String,           // LINK, WI-FI, TEXT ...
    val detail: String?,        // nicer text for wifi / contacts
    val openUri: String?,       // something we can open (link, tel:, mailto: ...)
    val secret: String?         // wifi password
) {
    companion object {
        fun formatName(f: Int): String = when (f) {
            Barcode.FORMAT_QR_CODE -> "QR CODE"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "CODE 128"
            Barcode.FORMAT_CODE_39 -> "CODE 39"
            Barcode.FORMAT_CODE_93 -> "CODE 93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "CODE"
        }

        fun from(b: Barcode): ScanResult? {
            val raw = b.rawValue ?: b.displayValue ?: return null
            if (raw.isBlank()) return null
            val format = formatName(b.format)
            var kind = "TEXT"
            var detail: String? = null
            var open: String? = null
            var secret: String? = null

            when (b.valueType) {
                Barcode.TYPE_URL -> {
                    kind = "LINK"
                    open = b.url?.url ?: raw
                }
                Barcode.TYPE_WIFI -> {
                    kind = "WI-FI"
                    val w = b.wifi
                    val sec = when (w?.encryptionType) {
                        Barcode.WiFi.TYPE_OPEN -> "Open"
                        Barcode.WiFi.TYPE_WPA -> "WPA / WPA2"
                        Barcode.WiFi.TYPE_WEP -> "WEP"
                        else -> "Unknown"
                    }
                    detail = "Network: ${w?.ssid.orEmpty()}\nPassword: ${w?.password.orEmpty()}\nSecurity: $sec"
                    secret = w?.password
                }
                Barcode.TYPE_PHONE -> {
                    kind = "PHONE"
                    open = b.phone?.number?.let { "tel:$it" }
                }
                Barcode.TYPE_EMAIL -> {
                    kind = "EMAIL"
                    open = b.email?.address?.let { "mailto:$it" }
                }
                Barcode.TYPE_SMS -> {
                    kind = "SMS"
                    open = b.sms?.phoneNumber?.let { "smsto:$it" }
                }
                Barcode.TYPE_GEO -> {
                    kind = "LOCATION"
                    open = b.geoPoint?.let { "geo:${it.lat},${it.lng}" }
                }
                Barcode.TYPE_CONTACT_INFO -> {
                    kind = "CONTACT"
                    val c = b.contactInfo
                    val lines = ArrayList<String>()
                    c?.name?.formattedName?.let { if (it.isNotBlank()) lines.add(it) }
                    c?.organization?.let { if (it.isNotBlank()) lines.add(it) }
                    c?.phones?.forEach { p -> p.number?.let { lines.add(it) } }
                    c?.emails?.forEach { e -> e.address?.let { lines.add(it) } }
                    if (lines.isNotEmpty()) detail = lines.joinToString("\n")
                }
                Barcode.TYPE_CALENDAR_EVENT -> {
                    kind = "EVENT"
                    detail = b.calendarEvent?.summary
                }
                Barcode.TYPE_PRODUCT -> kind = "PRODUCT"
                Barcode.TYPE_ISBN -> kind = "ISBN"
            }
            if (open == null && (raw.startsWith("http://", true) || raw.startsWith("https://", true))) {
                open = raw
                if (kind == "TEXT") kind = "LINK"
            }
            return ScanResult(raw, format, kind, detail, open, secret)
        }
    }
}
