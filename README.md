# Capacitor Call Manager 📱

A powerful standalone Capacitor plugin for **Android** and **iOS** that provides comprehensive telephony features including Call Logs, SMS management, Contacts, and a sophisticated CRM Overlay system.

---

## 🚀 Features

- 📞 **Call Logs (Android only)**: Retrieve full call history with contact name lookup and filtering.
- 💬 **SMS Manager (Android only)**: Read message threads, fetch conversations, and send direct SMS.
- 👥 **Contacts (Cross-platform)**: High-speed contact list access with search.
- 📱 **CRM Overlay (Android only)**: 
    - **During Call Badge**: Floating indicator for active counseling sessions.
    - **After Call Form**: Professional form for quick notes and interest status.
- 🎙️ **Recording (Android only)**: Experimental call recording.
- 🔊 **Call Events**: Detect incoming, started, and ended calls in real-time.

---

## 📦 Step-by-Step Installation

### 1. Install the Package
Run the following command in your project root:
```bash
npm install https://github.com/ibase/capacitor-call-manager.git
```

### 2. Platform Configuration

#### Android Setup
1. Open `android/app/src/main/java/***/MainActivity.java`.
2. Ensure the plugin is registered (Capacitor 6+ does this automatically, but for older versions, add `registerPlugin(CallManagerPlugin.class)`).
3. **Overlay Permission**: The user must manually grant "Display over other apps" for the CRM form to appear.

#### iOS Setup
Add the following key to your `ios/App/App/Info.plist`:
```xml
<key>NSContactsUsageDescription</key>
<string>We need access to contacts to identify callers and manage your dialer.</string>
```

---

## 🛠️ Usage Guide

### 1. Initialize & Request Permissions
Always check and request permissions before accessing telephony data.

```typescript
import { CallManager } from 'capacitor-call-manager';

async function setupTelephony() {
  const status = await CallManager.requestPermissions();
  
  // Note: Overlay requires a separate system intent on Android
  if (status.overlay !== 'granted') {
    await CallManager.requestOverlayPermission();
  }
}
```

### 2. Monitoring Call Events (Android)
Start the listener to receive real-time updates and trigger the CRM overlay.

```typescript
// Start the native broadcast receiver
await CallManager.startCallListener();

// Handle call completion
CallManager.addListener('callEnded', (data) => {
  console.log(`Call with ${data.number} ended. Duration: ${data.duration}s`);
});

// Handle CRM Form submission from Overlay
CallManager.addListener('callOverlaySubmitted', (data) => {
  console.log('Counseling Details Saved:', data.status, data.notes);
  // Sync with your API here
});
```

### 3. Fetching Data

#### Call Logs (Android)
```typescript
const { logs } = await CallManager.getCallLogs({ 
  limit: 20, 
  type: 'MISSED',
  date: 'TODAY' 
});
```

#### Contacts (iOS & Android)
```typescript
const { contacts } = await CallManager.getContacts({ search: 'John' });
```

#### SMS (Android)
```typescript
// Get conversations
const { threads } = await CallManager.getSMSThreads();

// Send a message
await CallManager.sendSMS({ 
  number: '9876543210', 
  message: 'Hello from Ibase Dialer!' 
});
```

---

## 🛡️ Platform Limitations

| Feature | Android | iOS |
| :--- | :---: | :---: |
| Call Logs | ✅ | ❌ (Privacy Restricted) |
| Read SMS | ✅ | ❌ (Privacy Restricted) |
| CRM Overlay | ✅ | ❌ (Not Supported by iOS) |
| Contacts | ✅ | ✅ |
| Initiate Call | ✅ | ✅ |
| Recording | ✅ | ❌ |

---

## 📝 License
MIT
