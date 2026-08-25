package com.example.engine.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.local.entities.AutonomousGoalEntity
import com.example.data.local.entities.ScheduledTaskEntity
import com.example.data.local.entities.SkillEntity
import com.example.data.local.entities.SubagentTaskEntity
import com.example.data.model.DeliverChannel
import com.example.data.model.MemoryCategory
import com.example.data.model.RiskLevel
import com.example.data.model.TaskStatus
import com.example.data.repository.HermesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

// 1. Web Search Tool
class WebSearchTool : HermesTool {
    override val name = "web_search"
    override val displayName = "Live Web Search"
    override val description = "Searches the web for real-time information, news, documentation, or facts."
    override val category = "Web & Info"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions = listOf(Manifest.permission.INTERNET)
    override val parameterSchema = """{"query": "string"}"""

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val query = params["query"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing query parameter", "Missing query"
        )
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; HermesAgent/1.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            val html = response.body?.string() ?: ""

            // Simple fast text snippet extractor from HTML
            val snippets = mutableListOf<String>()
            val regex = Regex("""class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val matches = regex.findAll(html)
            for (match in matches.take(5)) {
                val clean = match.groupValues[1]
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&quot;", "\"")
                    .replace("&#x27;", "'")
                    .replace("&amp;", "&")
                    .trim()
                if (clean.isNotEmpty()) {
                    snippets.add(clean)
                }
            }

            val resultJson = JSONObject().apply {
                put("query", query)
                put("resultsCount", snippets.size)
                put("snippets", JSONArray(snippets))
                if (snippets.isEmpty()) {
                    put("rawExtract", html.take(1000).replace(Regex("<[^>]*>"), " ").trim())
                }
            }.toString()

            ToolExecutionResult(
                isSuccess = true,
                resultJson = resultJson,
                summary = "Found ${snippets.size} web search results for '$query'"
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                resultJson = JSONObject().put("error", e.message).toString(),
                summary = "Web search failed for '$query'",
                error = e.localizedMessage
            )
        }
    }
}

// 2. Read SMS Tool
class ReadSmsTool : HermesTool {
    override val name = "read_sms"
    override val displayName = "Read Recent SMS"
    override val description = "Reads recent SMS inbox messages for message triage, OTP codes, or notifications."
    override val category = "Communication"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions = listOf(Manifest.permission.READ_SMS)
    override val parameterSchema = """{"limit": "number (default 10)"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ToolExecutionResult(
                false, "{}", "Permission READ_SMS not granted by user", "Permission Denied"
            )
        }
        try {
            val limit = (params["limit"] as? Number)?.toInt() ?: 10
            val uri = Uri.parse("content://sms/inbox")
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("_id", "address", "body", "date"),
                null,
                null,
                "date DESC LIMIT $limit"
            )

            val messages = mutableListOf<JSONObject>()
            cursor?.use {
                val addressCol = it.getColumnIndex("address")
                val bodyCol = it.getColumnIndex("body")
                val dateCol = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = if (addressCol >= 0) it.getString(addressCol) else "Unknown"
                    val body = if (bodyCol >= 0) it.getString(bodyCol) else ""
                    val date = if (dateCol >= 0) it.getLong(dateCol) else 0L
                    messages.add(JSONObject().apply {
                        put("sender", address)
                        put("body", body)
                        put("timestamp", date)
                        put("dateFormatted", Date(date).toString())
                    })
                }
            }

            val resultJson = JSONObject().apply {
                put("totalMessages", messages.size)
                put("messages", JSONArray(messages))
            }.toString()

            ToolExecutionResult(
                isSuccess = true,
                resultJson = resultJson,
                summary = "Retrieved ${messages.size} recent SMS messages"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to read SMS: ${e.message}", e.message)
        }
    }
}

// 3. Send SMS Tool (HIGH RISK - Requires Approval)
class SendSmsTool : HermesTool {
    override val name = "send_sms"
    override val displayName = "Send Outbound SMS"
    override val description = "Sends an SMS text message to a recipient phone number. High-risk tool requiring approval."
    override val category = "Communication"
    override val riskLevel = RiskLevel.HIGH
    override val requiredPermissions = listOf(Manifest.permission.SEND_SMS)
    override val parameterSchema = """{"phoneNumber": "string", "message": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val phoneNumber = params["phoneNumber"]?.toString()
        val message = params["message"]?.toString()

        if (phoneNumber.isNullOrBlank() || message.isNullOrBlank()) {
            return@withContext ToolExecutionResult(false, "{}", "Missing phoneNumber or message", "Invalid params")
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ToolExecutionResult(false, "{}", "Permission SEND_SMS not granted", "Permission Denied")
        }

        try {
            val smsManager = context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }

            val resultJson = JSONObject().apply {
                put("status", "SENT")
                put("recipient", phoneNumber)
                put("messageLength", message.length)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            ToolExecutionResult(
                isSuccess = true,
                resultJson = resultJson,
                summary = "Sent SMS to $phoneNumber: '$message'"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to send SMS: ${e.message}", e.message)
        }
    }
}

// 4. Phone Calls Tool (HIGH RISK)
class MakePhoneCallTool : HermesTool {
    override val name = "make_phone_call"
    override val displayName = "Initiate Phone Call"
    override val description = "Places an outbound phone call to a specified contact or number."
    override val category = "Communication"
    override val riskLevel = RiskLevel.HIGH
    override val requiredPermissions = listOf(Manifest.permission.CALL_PHONE)
    override val parameterSchema = """{"phoneNumber": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.Main) {
        val phoneNumber = params["phoneNumber"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing phoneNumber", "Invalid params"
        )
        try {
            val intent = if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            }.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            ToolExecutionResult(
                isSuccess = true,
                resultJson = JSONObject().put("status", "CALL_INITIATED").put("number", phoneNumber).toString(),
                summary = "Initiated phone call to $phoneNumber"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to initiate call: ${e.message}", e.message)
        }
    }
}

// 5. Read Call Log Tool
class ReadCallLogTool : HermesTool {
    override val name = "read_call_log"
    override val displayName = "Read Call History"
    override val description = "Retrieves recent incoming, outgoing, and missed phone calls."
    override val category = "Communication"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions = listOf(Manifest.permission.READ_CALL_LOG)
    override val parameterSchema = """{"limit": "number (default 10)"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ToolExecutionResult(false, "{}", "READ_CALL_LOG permission not granted", "Permission Denied")
        }
        try {
            val limit = (params["limit"] as? Number)?.toInt() ?: 10
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.CACHED_NAME),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $limit"
            )

            val calls = mutableListOf<JSONObject>()
            cursor?.use {
                val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
                val durCol = it.getColumnIndex(CallLog.Calls.DURATION)
                val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                while (it.moveToNext()) {
                    val number = if (numCol >= 0) it.getString(numCol) else ""
                    val type = if (typeCol >= 0) it.getInt(typeCol) else 0
                    val date = if (dateCol >= 0) it.getLong(dateCol) else 0L
                    val duration = if (durCol >= 0) it.getLong(durCol) else 0L
                    val name = if (nameCol >= 0) it.getString(nameCol) else null

                    val typeStr = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                        else -> "OTHER"
                    }

                    calls.add(JSONObject().apply {
                        put("number", number)
                        put("callerName", name ?: "Unknown")
                        put("type", typeStr)
                        put("durationSeconds", duration)
                        put("dateFormatted", Date(date).toString())
                    })
                }
            }

            ToolExecutionResult(
                isSuccess = true,
                resultJson = JSONObject().put("callsCount", calls.size).put("calls", JSONArray(calls)).toString(),
                summary = "Retrieved ${calls.size} recent call log records"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to query call log: ${e.message}", e.message)
        }
    }
}

// 6. Read Contacts Tool
class ReadContactsTool : HermesTool {
    override val name = "read_contacts"
    override val displayName = "Search Contacts"
    override val description = "Searches device address book for contact names, phone numbers, and emails."
    override val category = "Personal Data"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions = listOf(Manifest.permission.READ_CONTACTS)
    override val parameterSchema = """{"query": "string (optional)", "limit": "number (default 20)"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ToolExecutionResult(false, "{}", "READ_CONTACTS permission not granted", "Permission Denied")
        }
        try {
            val query = params["query"]?.toString()
            val limit = (params["limit"] as? Number)?.toInt() ?: 20

            val selection = if (!query.isNullOrBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            } else null
            val selectionArgs = if (!query.isNullOrBlank()) arrayOf("%$query%") else null

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"
            )

            val contacts = mutableListOf<JSONObject>()
            cursor?.use {
                val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = if (nameCol >= 0) it.getString(nameCol) else ""
                    val number = if (numCol >= 0) it.getString(numCol) else ""
                    contacts.add(JSONObject().apply {
                        put("name", name)
                        put("phoneNumber", number)
                    })
                }
            }

            ToolExecutionResult(
                isSuccess = true,
                resultJson = JSONObject().put("contactsCount", contacts.size).put("contacts", JSONArray(contacts)).toString(),
                summary = "Found ${contacts.size} contacts matching '${query ?: "all"}'"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to read contacts: ${e.message}", e.message)
        }
    }
}

// 7. Calendar Read/Write Tool
class CalendarTool : HermesTool {
    override val name = "calendar_events"
    override val displayName = "Calendar Schedules"
    override val description = "Reads upcoming events or inserts a new event in the device calendar."
    override val category = "Personal Data"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    override val parameterSchema = """{"action": "read|create", "title": "string", "startTimeMillis": "number", "endTimeMillis": "number", "description": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val action = params["action"]?.toString() ?: "read"

        if (action == "create") {
            val title = params["title"]?.toString() ?: "Hermes Scheduled Task"
            val desc = params["description"]?.toString() ?: "Created by Hermes Autonomous Agent"
            val start = (params["startTimeMillis"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 3600_000L)
            val end = (params["endTimeMillis"] as? Number)?.toLong() ?: (start + 3600_000L)

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, desc)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            withContext(Dispatchers.Main) {
                context.startActivity(intent)
            }

            ToolExecutionResult(
                isSuccess = true,
                resultJson = JSONObject().put("status", "EVENT_OPENED_IN_CALENDAR").put("title", title).toString(),
                summary = "Created calendar event: '$title'"
            )
        } else {
            // Read calendar events
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return@withContext ToolExecutionResult(false, "{}", "READ_CALENDAR permission not granted", "Permission Denied")
            }
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.DESCRIPTION),
                "${CalendarContract.Events.DTSTART} >= ?",
                arrayOf(System.currentTimeMillis().toString()),
                "${CalendarContract.Events.DTSTART} ASC LIMIT 10"
            )
            val events = mutableListOf<JSONObject>()
            cursor?.use {
                val titleCol = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startCol = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val descCol = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                while (it.moveToNext()) {
                    val t = if (titleCol >= 0) it.getString(titleCol) else ""
                    val s = if (startCol >= 0) it.getLong(startCol) else 0L
                    val d = if (descCol >= 0) it.getString(descCol) else ""
                    events.add(JSONObject().apply {
                        put("title", t)
                        put("startTime", s)
                        put("dateFormatted", Date(s).toString())
                        put("description", d)
                    })
                }
            }
            ToolExecutionResult(
                isSuccess = true,
                resultJson = JSONObject().put("eventsCount", events.size).put("events", JSONArray(events)).toString(),
                summary = "Found ${events.size} upcoming calendar events"
            )
        }
    }
}

// 8. Device Location Tool
class DeviceLocationTool : HermesTool {
    override val name = "get_device_location"
    override val displayName = "Device GPS Location"
    override val description = "Retrieves current device GPS coordinates (latitude, longitude) for weather and venue queries."
    override val category = "Sensors & Hardware"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    override val parameterSchema = """{}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return@withContext ToolExecutionResult(false, "{}", "Location permission not granted", "Permission Denied")
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLoc: android.location.Location? = null

            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                    bestLoc = loc
                }
            }

            if (bestLoc != null) {
                val json = JSONObject().apply {
                    put("latitude", bestLoc.latitude)
                    put("longitude", bestLoc.longitude)
                    put("accuracyMeters", bestLoc.accuracy)
                    put("provider", bestLoc.provider)
                    put("timestamp", bestLoc.time)
                }.toString()
                ToolExecutionResult(
                    isSuccess = true,
                    resultJson = json,
                    summary = "Location: Lat ${bestLoc.latitude}, Lng ${bestLoc.longitude} (accuracy ±${bestLoc.accuracy}m)"
                )
            } else {
                ToolExecutionResult(
                    isSuccess = true,
                    resultJson = JSONObject().put("status", "NO_LOCATION_FIX").put("note", "GPS enabled but no fix yet").toString(),
                    summary = "GPS active, waiting for satellite lock"
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to fetch location: ${e.message}", e.message)
        }
    }
}

// 9. Hardware & System Telemetry Tool
class DeviceTelemetryTool : HermesTool {
    override val name = "get_device_telemetry"
    override val displayName = "Device Telemetry & Specs"
    override val description = "Fetches battery level, charging status, available memory, disk space, and network connectivity."
    override val category = "Sensors & Hardware"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions = listOf(Manifest.permission.ACCESS_NETWORK_STATE)
    override val parameterSchema = """{}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = batteryManager?.isCharging == true

            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val freeGb = bytesAvailable / (1024 * 1024 * 1024.0)
            val totalGb = bytesTotal / (1024 * 1024 * 1024.0)

            val runtime = Runtime.getRuntime()
            val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024.0)
            val maxMemMb = runtime.maxMemory() / (1024 * 1024.0)

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val netType = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular 4G/5G"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Offline / None"
            }

            val json = JSONObject().apply {
                put("batteryPercent", batteryLevel)
                put("isCharging", isCharging)
                put("storageFreeGB", "%.2f".format(freeGb))
                put("storageTotalGB", "%.2f".format(totalGb))
                put("ramUsedMB", "%.1f".format(usedMemMb))
                put("ramMaxMB", "%.1f".format(maxMemMb))
                put("networkType", netType)
                put("androidVersion", Build.VERSION.RELEASE)
                put("apiLevel", Build.VERSION.SDK_INT)
                put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
            }.toString()

            ToolExecutionResult(
                isSuccess = true,
                resultJson = json,
                summary = "Battery: $batteryLevel% (${if (isCharging) "Charging" else "Discharging"}), Storage: ${"%.1f".format(freeGb)}GB free, Net: $netType"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to fetch telemetry: ${e.message}", e.message)
        }
    }
}

// 10. Installed Apps & Launcher Tool
class InstalledAppsTool : HermesTool {
    override val name = "get_installed_apps"
    override val displayName = "Installed Applications"
    override val description = "Lists installed apps on the device or launches a specific app."
    override val category = "System"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"action": "list|launch", "packageName": "string", "query": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val action = params["action"]?.toString() ?: "list"

        if (action == "launch") {
            val pkg = params["packageName"]?.toString() ?: return@withContext ToolExecutionResult(
                false, "{}", "Missing packageName to launch", "Invalid params"
            )
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                withContext(Dispatchers.Main) {
                    context.startActivity(launchIntent)
                }
                ToolExecutionResult(
                    isSuccess = true,
                    resultJson = JSONObject().put("status", "APP_LAUNCHED").put("packageName", pkg).toString(),
                    summary = "Launched application: $pkg"
                )
            } else {
                ToolExecutionResult(false, "{}", "App not launchable: $pkg", "Launch failed")
            }
        } else {
            val query = params["query"]?.toString()?.lowercase()
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(mainIntent, 0)

            val apps = mutableListOf<JSONObject>()
            for (info in resolved) {
                val appName = info.loadLabel(pm).toString()
                val pkgName = info.activityInfo.packageName
                if (query == null || appName.lowercase().contains(query) || pkgName.lowercase().contains(query)) {
                    apps.add(JSONObject().apply {
                        put("appName", appName)
                        put("packageName", pkgName)
                    })
                }
            }

            ToolExecutionResult(
                isSuccess = true,
                resultJson = JSONObject().put("count", apps.size).put("apps", JSONArray(apps.take(40))).toString(),
                summary = "Found ${apps.size} installed apps"
            )
        }
    }
}

// 11. Post System Notification Tool
class PostNotificationTool : HermesTool {
    override val name = "post_system_notification"
    override val displayName = "Post Agent Notification"
    override val description = "Displays an Android system status notification with custom title and body."
    override val category = "System"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions = listOf(Manifest.permission.POST_NOTIFICATIONS)
    override val parameterSchema = """{"title": "string", "message": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val title = params["title"]?.toString() ?: "Hermes Notification"
        val message = params["message"]?.toString() ?: "Agent task completed"

        try {
            val notification = NotificationCompat.Builder(context, "hermes_agent_notifications")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notification)

            ToolExecutionResult(
                isSuccess = true,
                resultJson = JSONObject().put("status", "NOTIFICATION_POSTED").put("title", title).toString(),
                summary = "Dispatched notification: '$title'"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Failed to post notification: ${e.message}", e.message)
        }
    }
}

// 12. Arbitrary HTTP Request Tool
class HttpRequestTool : HermesTool {
    override val name = "http_request"
    override val displayName = "HTTP & Webhook Client"
    override val description = "Executes custom HTTP requests (GET, POST, PUT, DELETE) with custom headers and JSON payloads."
    override val category = "Web & Info"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions = listOf(Manifest.permission.INTERNET)
    override val parameterSchema = """{"url": "string", "method": "GET|POST|PUT|DELETE", "headers": "object", "body": "string"}"""

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val url = params["url"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing url", "Invalid params"
        )
        val method = params["method"]?.toString()?.uppercase() ?: "GET"
        val bodyStr = params["body"]?.toString()

        try {
            val reqBuilder = Request.Builder().url(url)
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

            when (method) {
                "POST" -> reqBuilder.post((bodyStr ?: "").toRequestBody(mediaType))
                "PUT" -> reqBuilder.put((bodyStr ?: "").toRequestBody(mediaType))
                "DELETE" -> reqBuilder.delete()
                else -> reqBuilder.get()
            }

            val resp = client.newCall(reqBuilder.build()).execute()
            val respBody = resp.body?.string() ?: ""

            val json = JSONObject().apply {
                put("statusCode", resp.code)
                put("isSuccessful", resp.isSuccessful)
                put("responseBody", respBody.take(4000))
            }.toString()

            ToolExecutionResult(
                isSuccess = resp.isSuccessful,
                resultJson = json,
                summary = "HTTP $method $url -> Code ${resp.code} (${respBody.length} bytes)"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "HTTP request failed: ${e.message}", e.message)
        }
    }
}

// 13. Save Learned Fact Memory Tool
class SaveLearnedFactTool : HermesTool {
    override val name = "save_learned_fact"
    override val displayName = "Store Long-Term Fact"
    override val description = "Persists learned user preferences, facts, or system knowledge into long-term memory."
    override val category = "Agent Core"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"subject": "string", "content": "string", "category": "PREFERENCE|FACT|WORK|CONTACT|SYSTEM"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val subject = params["subject"]?.toString() ?: "General"
        val content = params["content"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing content", "Invalid params"
        )
        val catStr = params["category"]?.toString()?.uppercase() ?: "FACT"
        val category = try { MemoryCategory.valueOf(catStr) } catch (_: Exception) { MemoryCategory.FACT }

        val id = repository.saveMemory(category, subject, content, 1.0f, "Autonomous Loop")
        val json = JSONObject().apply {
            put("id", id)
            put("subject", subject)
            put("content", content)
            put("category", category.name)
        }.toString()

        ToolExecutionResult(
            isSuccess = true,
            resultJson = json,
            summary = "Stored memory fact #$id: '$subject' -> '$content'"
        )
    }
}

// 14. Query Memory Tool
class QueryMemoryTool : HermesTool {
    override val name = "query_memory"
    override val displayName = "Query Long-Term Memory"
    override val description = "Searches the episodic and semantic long-term memory store for stored knowledge."
    override val category = "Agent Core"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"query": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val query = params["query"]?.toString()?.lowercase() ?: ""
        val memories = repository.getTopMemoriesForContext()
        val filtered = if (query.isBlank()) {
            memories
        } else {
            memories.filter {
                it.subject.lowercase().contains(query) || it.content.lowercase().contains(query)
            }
        }

        val jsonList = filtered.map {
            JSONObject().apply {
                put("id", it.id)
                put("category", it.category.name)
                put("subject", it.subject)
                put("content", it.content)
                put("confidence", it.confidence)
            }
        }

        ToolExecutionResult(
            isSuccess = true,
            resultJson = JSONObject().put("count", jsonList.size).put("memories", JSONArray(jsonList)).toString(),
            summary = "Found ${jsonList.size} stored memories matching '$query'"
        )
    }
}

// 15. Self-Writing Skill Tool
class SaveSkillTool : HermesTool {
    override val name = "save_skill"
    override val displayName = "Synthesize Reusable Skill"
    override val description = "Saves a reusable multi-step automation skill that can be triggered dynamically or scheduled."
    override val category = "Agent Core"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"name": "string", "displayName": "string", "description": "string", "triggerPattern": "string", "instructions": "string", "tools": "array"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val name = params["name"]?.toString() ?: "skill_${System.currentTimeMillis()}"
        val displayName = params["displayName"]?.toString() ?: name
        val description = params["description"]?.toString() ?: "Custom synthesized skill"
        val trigger = params["triggerPattern"]?.toString() ?: ""
        val instructions = params["instructions"]?.toString() ?: ""
        val toolsJson = params["tools"]?.toString() ?: "[]"

        val skill = SkillEntity(
            name = name,
            displayName = displayName,
            description = description,
            triggerPattern = trigger,
            instructions = instructions,
            toolSequenceJson = toolsJson,
            isAutoSaved = true
        )
        repository.saveSkill(skill)

        ToolExecutionResult(
            isSuccess = true,
            resultJson = JSONObject().put("skillName", name).put("displayName", displayName).toString(),
            summary = "Synthesized and saved skill '$displayName' ($name)"
        )
    }
}

// 16. Schedule Automation Tool
class ScheduleAutomationTool : HermesTool {
    override val name = "schedule_automation"
    override val displayName = "Schedule Cron Automation"
    override val description = "Creates a persistent background cron or interval job to execute prompts/tasks."
    override val category = "Agent Core"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"title": "string", "prompt": "string", "cronExpression": "string (e.g. interval:60 or 0 8 * * *)", "channel": "IN_APP|TELEGRAM|ALL"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val title = params["title"]?.toString() ?: "Automated Task"
        val prompt = params["prompt"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing prompt", "Invalid params"
        )
        val cron = params["cronExpression"]?.toString() ?: "interval:60"
        val chStr = params["channel"]?.toString()?.uppercase() ?: "ALL"
        val channel = try { DeliverChannel.valueOf(chStr) } catch (_: Exception) { DeliverChannel.ALL }

        val id = UUID.randomUUID().toString().take(8)
        val task = ScheduledTaskEntity(
            id = id,
            title = title,
            naturalLanguagePrompt = prompt,
            cronExpression = cron,
            targetChannel = channel,
            nextRunTime = System.currentTimeMillis() + 60_000L
        )
        repository.saveScheduledTask(task)

        ToolExecutionResult(
            isSuccess = true,
            resultJson = JSONObject().put("taskId", id).put("title", title).put("cron", cron).toString(),
            summary = "Created scheduled cron automation '$title' [ID: $id]"
        )
    }
}

// 17. Spawn Subagent Task Tool
class SpawnSubagentTool : HermesTool {
    override val name = "spawn_subagent"
    override val displayName = "Spawn Parallel Subagent"
    override val description = "Spawns an isolated background sub-task to run asynchronously and report back."
    override val category = "Agent Core"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"title": "string", "objective": "string", "assignedModel": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val title = params["title"]?.toString() ?: "Subagent Task"
        val objective = params["objective"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing objective", "Invalid params"
        )
        val model = params["assignedModel"]?.toString() ?: "GEMINI"

        val id = "sub_${UUID.randomUUID().toString().take(8)}"
        val subagent = SubagentTaskEntity(
            id = id,
            title = title,
            objective = objective,
            assignedModel = model,
            status = TaskStatus.RUNNING,
            progressPercent = 10,
            logs = "Subagent initialized. Objective: $objective\n"
        )
        repository.saveSubagent(subagent)

        ToolExecutionResult(
            isSuccess = true,
            resultJson = JSONObject().put("subagentId", id).put("title", title).put("status", "SPAWNED").toString(),
            summary = "Spawned subagent '$title' [ID: $id]"
        )
    }
}

// 18. Autonomous Goal Loop Tool
class ExecuteGoalLoopTool : HermesTool {
    override val name = "execute_goal_loop"
    override val displayName = "Autonomous Goal & Judge Loop"
    override val description = "Initializes an autonomous multi-step loop that requires concrete evidence and judge verification."
    override val category = "Agent Core"
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"objective": "string", "successCriteria": "string", "maxIterations": "number (default 5)"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val objective = params["objective"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing objective", "Invalid params"
        )
        val successCriteria = params["successCriteria"]?.toString() ?: "Evidence verified"
        val maxIters = (params["maxIterations"] as? Number)?.toInt() ?: 5

        val goalId = "goal_${UUID.randomUUID().toString().take(8)}"
        val goal = AutonomousGoalEntity(
            id = goalId,
            objective = objective,
            successCriteria = successCriteria,
            maxIterations = maxIters,
            currentIteration = 1,
            status = TaskStatus.RUNNING,
            evidenceLogs = JSONArray().put(JSONObject().put("step", 1).put("action", "Goal initiated").put("timestamp", System.currentTimeMillis())).toString()
        )
        repository.saveGoal(goal)

        ToolExecutionResult(
            isSuccess = true,
            resultJson = JSONObject().put("goalId", goalId).put("objective", objective).put("status", "ACTIVE").toString(),
            summary = "Initialized autonomous goal loop '$objective' [ID: $goalId]"
        )
    }
}

// 19. Math & Expression Calculator Tool
class MathCalculatorTool : HermesTool {
    override val name = "calculate_math_expression"
    override val displayName = "Exact Math Computing"
    override val description = "Evaluates exact mathematical expressions, arithmetic, conversions, and statistics."
    override val category = "Agent Core"
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissions: List<String> = emptyList()
    override val parameterSchema = """{"expression": "string"}"""

    override suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult = withContext(Dispatchers.Default) {
        val expr = params["expression"]?.toString() ?: return@withContext ToolExecutionResult(
            false, "{}", "Missing expression", "Invalid params"
        )
        try {
            // Safe clean math parser
            val clean = expr.replace(" ", "").replace("x", "*").replace("X", "*")
            val result = evaluateSimpleExpression(clean)

            val json = JSONObject().apply {
                put("expression", expr)
                put("result", result)
            }.toString()

            ToolExecutionResult(
                isSuccess = true,
                resultJson = json,
                summary = "$expr = $result"
            )
        } catch (e: Exception) {
            ToolExecutionResult(false, "{}", "Math calculation error: ${e.message}", e.message)
        }
    }

    private fun evaluateSimpleExpression(str: String): Double {
        // Simple recursive descent parser for +, -, *, /, %, ^
        val parser = object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        eat('%'.code) -> x %= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return +parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.code)) x = Math.pow(x, parseFactor())
                return x
            }
        }
        return parser.parse()
    }
}
