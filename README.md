# Capacitor Call Manager 📱

A powerful standalone Capacitor plugin specifically for **Android** that provides comprehensive telephony features including Call Logs, Contacts, and a sophisticated CRM Overlay system.

---

## 🚀 Features

- 📞 **Call Logs**: Retrieve full call history with contact name lookup and filtering.
- 👥 **Contacts**: High-speed contact list access with search.
- 📱 **CRM Overlay**: 
    - **During Call Badge**: Floating indicator for active counseling sessions.
    - **After Call Form**: Professional form for quick notes and interest status.
- 🎙️ **Recording**: Experimental call recording.
- 🔊 **Call Events**: Detect incoming, started, and ended calls in real-time.

---

## 📦 Step-by-Step Installation

### 1. Install the Package
Run the following command in your project root:
```bash
npm install capacitor-call-manager
```

### 2. Platform Configuration

#### Android Setup
No Java/Kotlin code is required! Capacitor 6+ handles `@CapacitorPlugin` auto-registration completely. 

**Requirements:**
- Android 12+ (API 31 and above).

**Overlay Permission**:
The user must manually grant "Display over other apps" for the CRM form to appear natively during/after calls. Use `CallManager.requestOverlayPermission()` to prompt them.

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

### 2. Monitoring Call Events
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

#### Call Logs
```typescript
const { logs } = await CallManager.getCallLogs({ 
  limit: 20, 
  type: 'MISSED',
  date: 'TODAY' 
});
```

#### Contacts
```typescript
const { contacts } = await CallManager.getContacts({ search: 'John' });
```

---

## 🛡️ Platform Limitations

| Feature | Android |
| :--- | :---: |
| Call Logs | ✅ |
| CRM Overlay | ✅ |
| Contacts | ✅ |
| Initiate Call | ✅ |
| Recording | ✅ |

*Note: iOS is not supported.*

---

## 📝 License
MIT
