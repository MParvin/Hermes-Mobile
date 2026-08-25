# Hermes Mobile — Permissions & Data Safety Documentation

This document provides a comprehensive audit of all Android runtime and special permissions requested by **Hermes Mobile**, the corresponding agent tool/feature that utilizes each permission, the user consent model, and the local-first privacy policy.

---

## 1. Core Operating Permissions

| Permission | Android Identifier | Purpose | Consent & Gating |
|---|---|---|---|
| **Foreground Service** | `android.permission.FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the autonomous agent active in the background for continuous scheduled cron jobs, Telegram multi-channel gateway polling, and long-running subagent tasks. | Visible ongoing status notification with live tool actions and emergency kill switch. |
| **Notifications** | `android.permission.POST_NOTIFICATIONS` | Delivers agent completion alerts, high-risk approval request prompts, and scheduled delivery summaries. | Requested upon initial launch / notification toggle. |
| **Boot Persistence** | `android.permission.RECEIVE_BOOT_COMPLETED` | Restores scheduled automations and background agent service after device reboot if background persistence is enabled. | Configurable in Settings under Persistence. |
| **Wake Lock** | `android.permission.WAKE_LOCK` | Allows the device CPU to finish active tool loops without getting prematurely killed during execution. | Managed internally by Foreground Service. |

---

## 2. Capability & Tool Permissions

Every capability is strictly gated by runtime user permissions, isolated inside the Tool Registry, and can be toggled on/off individually in the **Permissions & Access Dashboard**.

| Capability / Tool | Permissions Required | Risk Level | Description & Usage |
|---|---|---|---|
| **SMS Management** | `READ_SMS`, `SEND_SMS`, `RECEIVE_SMS` | **HIGH** | Allows Hermes to read recent text messages for inbox triage and draft/send SMS messages. Sending SMS always prompts for explicit user approval before execution. |
| **Phone & Call Log** | `CALL_PHONE`, `READ_CALL_LOG` | **HIGH** | Lets the agent inspect recent call history and initiate outbound calls on behalf of the owner upon explicit confirmation. |
| **Contacts** | `READ_CONTACTS`, `WRITE_CONTACTS` | **MEDIUM** | Enables searching for contact information (phone numbers, email addresses) and saving new contacts generated during tasks. |
| **Calendar** | `READ_CALENDAR`, `WRITE_CALENDAR` | **MEDIUM** | Reads upcoming schedule events and creates new calendar entries / meetings parsed from natural language requests. |
| **Location & Geocoding** | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | **MEDIUM** | Retrieves current GPS coordinates for local weather queries, location-based reminders, and nearby venue lookups. |
| **Camera & Vision** | `CAMERA` | **MEDIUM** | Captures photos for on-demand multimodal vision inspection, document OCR, and visual question answering. |
| **Voice & Audio** | `RECORD_AUDIO` | **LOW** | Enables real-time spoken voice conversation mode, voice memo transcription, and hands-free prompt dictation. |
| **Network & Telemetry** | `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `BLUETOOTH_CONNECT` | **LOW** | Inspects current Wi-Fi connection, battery percentage, RAM utilization, storage stats, and toggles connectivity when requested. |
| **App Usage & Launcher** | `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES` | **MEDIUM** | Lists installed applications and launches designated apps requested by the user. |
| **Overlay UI** | `SYSTEM_ALERT_WINDOW` | **LOW** | Optional floating quick-action bubble for invoking Hermes from anywhere on the device. |

---

## 3. Security, Safety, & Privacy Guarantees

1. **Owner-Only Control**: Only the verified device owner can control Hermes. Remote channels (such as the Telegram Bot bridge) verify the sender against the user's configured Owner Telegram Chat ID.
2. **Local-First Persistence**: All conversation history, semantic fact memories, custom skills, scheduled automations, and tool audit logs are stored exclusively in the local Room SQLite database on the device.
3. **Interactive Approval Flow**: High-risk tool calls (e.g. sending SMS, making calls, executing destructive operations) trigger an interactive approval card with full input parameters and an Approve / Deny dialogue.
4. **Tamper-Evident Audit Log**: Every tool execution, duration, payload, output, and approval decision is logged into the local database and reviewable in the Audit Log screen.
5. **Instant Kill Switch**: A single prominent Kill Switch stops the background service and revokes all tool execution immediately.
