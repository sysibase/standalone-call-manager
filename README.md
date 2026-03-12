# Capacitor Call Manager 📱

[![NPM Version](https://img.shields.io/npm/v/capacitor-call-manager.svg)](https://www.npmjs.com/package/capacitor-call-manager)
[![Android Support](https://img.shields.io/badge/platform-android-brightgreen.svg)](https://developer.android.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A powerful, standalone Capacitor plugin for **Android** that provides comprehensive telephony features. Perfect for building Lead Management, CRM, or Call Identification applications.

---

## 🚀 Key Features

*   📞 **Call Logs**: High-performance retrieval of device call history with advanced filtering.
*   👥 **Contacts Access**: Fast contact list fetching
*   🖼️ **CRM Overlay**: 
    *   **React Design Support (NEW)**: Use your own React components as overlays. No XML needed!
    *   **Persistent Design**: Works globally even if the app is force-closed or the phone is restarted.
    *   **Customizable UI**: Completely override the native design with your own branding.
    *   **Data Protection**: Native queuing ensures lead data is never lost during offline sessions.
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
| `showOverlay(data)` | Manually trigger the CRM popup. | Android |
| `hideOverlay()` | Close the active overlay. | Android |
| `getPendingSubmissions()` | Retrieve submissions from offline mode. | Android |

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
