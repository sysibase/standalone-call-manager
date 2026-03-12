# Capacitor Call Manager 📱

[![NPM Version](https://img.shields.io/npm/v/capacitor-call-manager.svg)](https://www.npmjs.com/package/capacitor-call-manager)
[![Android Support](https://img.shields.io/badge/platform-android-brightgreen.svg)](https://developer.android.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A powerful, standalone Capacitor plugin for **Android** that provides comprehensive telephony features. Perfect for building Lead Management, CRM, or Call Identification applications.

[**🌐 View Online Documentation**](https://sys-ibase.github.io/standalone-call-manager/)

---

## 🎯 Why This Package? (Banane ki Wajah)

Standard telephony plugins for Capacitor are often limited to just reading logs. This package was born out of the need for a **Production-Ready CRM Engine** that:
1.  **Never Misses a Lead**: Works even if your app is killed, force-closed, or the phone just restarted.
2.  **Context is Everything**: Categorizes callers (Sales vs CRM) to show personalized data instantly.
3.  **No Boilerplate**: Provides a built-in, customizable floating overlay that looks professional out-of-the-box.
4.  **Privacy First**: Uses a local SQLite whitelist so you only track the numbers your business cares about.

---

## 🚀 Key Features

*   📞 **Call Logs**: High-performance retrieval of device call history with advanced filtering.
*   👥 **Contacts Access**: Fast contact list fetching.
*   🎭 **Multi-Entity Support (NEW)**: Manage Sales (Leads), CRM (Customers), and Internal (Employees) using unique IDs and custom designs for each.
*   🖼️ **CRM Overlay**: 
    *   **Context-Aware**: Automatically injects entity metadata into your UI.
    *   **React Design Support**: Use your own React components as overlays. No XML needed!
    *   **Direct Boot Support**: Your overlays work immediately after a phone restart, even if the device is locked.
    *   **Customizable UI**: Completely override the native design with your own branding.
*   🔄 **Hybrid Mode**: Easily toggle between Global Background mode and App-Only foreground mode.

---

## 📦 Installation

```bash
npm install capacitor-call-manager
npx cap sync
```

---

## 🛠️ Quick Start

### 1. Permissions
Add the following to your `AndroidManifest.xml` if not already present:
```xml
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- Android 13+ -->
```

### 2. Basic Setup (Telephony)
```typescript
import { CallManager } from 'capacitor-call-manager';

async function startPlugin() {
  // Check and Request Permissions
  const status = await CallManager.requestPermissions();
  
  if (status.overlay !== 'granted') {
    // Required for the CRM Overlay UI
    await CallManager.requestOverlayPermission();
  }

  // Start the background listener
  await CallManager.startCallListener();

  // Request Notifications (Required for Foreground Service on Android 13+)
  await CallManager.requestPermissions();
}
```

---

## 📱 CRM Overlay Guide

The CRM Overlay is a floating UI that appears during or after calls.

### Toggle Background Behavior
```typescript
// Enable Global Mode (Works even when app is closed) - Default
await CallManager.setBackgroundServiceEnabled({ enabled: true });

// Disable Global Mode (Overlays will ONLY appear if app is open)
await CallManager.setBackgroundServiceEnabled({ enabled: false });
```

### Capturing Submissions
```typescript
CallManager.addListener('callOverlaySubmitted', async (data) => {
  console.log('Lead Details:', data.notes, data.status);
  // Send to your backend
  await myApi.saveLead(data);
});

// Sync data captured while the app was closed
const { submissions } = await CallManager.getPendingSubmissions();
if (submissions.length > 0) {
  await myApi.bulkSave(submissions);
  await CallManager.clearPendingSubmissions(); // Clear queue after sync
}
```

---

## 🎨 UI & Customization

### Option A: Design in React (Recommended)
You can now design your overlay using React/CSS. No Android experience required.
Check out the [**WebView Portal Guide**](./brain/WEB_OVERLAY_GUIDE.md) for details.

### Option B: Native XML Branding
Override the internal XML layouts for maximum performance.
Check out the [**Native XML Guide**](./brain/CUSTOM_UI.md).

---

## 📚 API Reference

| Method | Description | Platform |
| :--- | :--- | :---: |
| `getCallLogs(options)` | Retrieve device call history. | Android |
| `getContacts(search)` | Search and fetch contacts. | Android |
| `addTrackedNumbers(items)`| Add numbers with metadata (Entity/ID) to whitelist. | Android |
| `getTrackedNumbersByEntity()`| Fetch tracked numbers for specific category. | Android |
| `showOverlay(data)` | Manually trigger the CRM popup. | Android |
| `getPendingSubmissions()` | Retrieve submissions from offline mode. | Android |
| `removeTrackedNumbersByEntity()`| Surgical cleanup of local database. | Android |

---

## 🆘 Troubleshooting

### Overlay not appearing?
1.  Ensure you have called `requestOverlayPermission()`. Users must manually toggle "Display over other apps".
2.  Ensure `startCallListener()` was called at least once in the app's lifecycle.

### Background service stops?
Android battery optimization may kill background services. For mission-critical lead management, instruct users to set the app's battery usage to "Unrestricted" in System Settings.

---

## ⚠️ Google Play Warning
This plugin uses high-risk permissions (`READ_CALL_LOG`). Ensure your app complies with Google's Core Functionality policy for Phone/Caller ID apps to avoid rejection.

---

## 📝 License
MIT
