# Professional Tips for CRM Developers 🚀

Developing a high-performance CRM application using the `Capacitor Call Manager` plugin requires a strategic approach to background execution, data synchronization, and user experience.

---

### 1. CRM Synchronization Strategy (Syncing Whitelist)
The plugin includes a native SQLite-based white-list (`Selective Tracking`). This is critical for battery life and privacy.

**Recommended Workflow:**
1.  **On App Launch**: Fetch your entire "Active Leads" list from your server.
2.  **Sync Local DB**: Use `addTrackedNumbers([...])` to ensure these numbers trigger the overlay even if the app is closed.
3.  **Clean up**: Periodically use `removeAllTrackedNumbers()` and re-sync to remove numbers for leads that were marked as "Closed" or "Lost" on the server.

---

### 2. Real-time Lead Assignment (FCM Integration)
Want to show an overlay as soon as a manager assigns a lead?
- **FCM Data Message**: Send a push notification with `number`, `name`, and `leadId`.
- **Background Trigger**: In your app's push listener, call `CallManager.addTrackedNumbers({ numbers: [leadId] })`.
- **Immediate Alert**: You can even call `CallManager.showOverlay()` manually if you want the agent to see a popup immediately without an incoming call.

---

### 3. Deep-Linking: Transition from Catch to Close
The transition from a "floating overlay" to your "Main CRM Dashboard" should be seamless.

**Example (In your React Overlay):**
```javascript
const goToFullProfile = () => {
    // This will close the overlay and open your main app 
    // to the specific customer route
    window.CallManagerBridge.openApp(`my-crm://lead-details/${currentLeadId}`);
};
```

---

### 4. Multi-Entity Contextual Design (Sales vs CRM vs Internal)
You can now categorize your callers to show different overlay layouts.

**Step 1: Sync with Entity Metadata**
```javascript
await CallManager.addTrackedNumbers({
  items: [
    { number: '+123', name: 'Potential Lead', entityType: 'SALES', entityId: 'lead_789' },
    { number: '+456', name: 'Existing Client', entityType: 'CRM', entityId: 'cust_123' },
    { number: '+789', name: 'Team Member', entityType: 'EMPLOYEE', entityId: 'emp_001' }
  ]
});
```

**Step 2: Handle in React Overlay**
The plugin automatically injects `entityType` and `entityId` into your URL.
```javascript
const params = new URLSearchParams(window.location.search);
const type = params.get('entityType'); // 'SALES', 'CRM', etc.
const id = params.get('entityId');     // 'lead_789', 'cust_123', etc.

if (type === 'SALES') return <SalesFollowUpUI leadId={id} />;
if (type === 'CRM') return <CustomerSupportUI customerId={id} />;
if (type === 'EMPLOYEE') return <InternalCommunicationUI empId={id} />;
```

---

### 5. Engagement Analytics
Don't just guess if your agents are using the overlay. Track it!
- **`callOverlayOpened`**: Log this to see how often the overlay is triggered.
- **`callOverlayClosed`**: If the duration between `opened` and `closed` is < 2 seconds, the agent likely dismissed it without reading.
- **Notes Quality**: Check the length of the `notes` field in your `callOverlaySubmitted` listener.

---

### 5. Google Play Store Compliance (Checklist)
Using `READ_CALL_LOG` is extremely sensitive. To avoid rejection:

1.  **Prominent Disclosure**: Before the first permission request, show a dialog explaining: *"Our app helps you manage CRM leads. We need to access call logs to show your customer details during a call."*
2.  **Video Demo**: You **must** record a video of the phone actually receiving a call and the overlay appearing. Upload this to YouTube as an unlisted video for the Google Reviewer.
3.  **Policy Link**: Ensure your Privacy Policy explicitly mentions that call data is captured for lead management and NEVER sold to third parties.

---

### 6. Battery Optimization
Encourage your users to **Disable Battery Optimization** for your app. 
- Some Chinese OEMs (Xiaomi, Oppo, Vivo) aggressively kill background services.
- Provide a button in your settings to open the battery optimization settings page using `@capacitor/app`.

---
*Built for production-grade Lead Management.*
