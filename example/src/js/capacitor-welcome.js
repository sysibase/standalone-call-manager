import { CallManager } from 'capacitor-call-manager';

window.customElements.define(
  'capacitor-welcome',
  class extends HTMLElement {
    constructor() {
      super();

      const root = this.attachShadow({ mode: 'open' });

      root.innerHTML = `
    <style>
      :host {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        display: block;
        width: 100%;
        height: 100%;
      }
      .button {
        display: inline-block;
        padding: 10px;
        background-color: #73B5F6;
        color: #fff;
        font-size: 0.9em;
        border: 0;
        border-radius: 3px;
        text-decoration: none;
        cursor: pointer;
        margin-right: 10px;
        margin-bottom: 10px;
      }
      main {
        padding: 15px;
      }
      #output {
        margin-top: 20px;
        background: #f4f4f4;
        padding: 10px;
        border-radius: 5px;
        max-height: 400px;
        overflow-y: auto;
        font-family: monospace;
        font-size: 12px;
        white-space: pre-wrap;
      }
    </style>
    <div>
      <main>
        <h2>Call Manager Demo</h2>
        <p>Testing the generic standalone capacitor plugin.</p>
        
        <div>
          <button class="button" id="btn-permissions">Request Permissions</button>
          <button class="button" id="btn-overlay">Request Overlay</button>
        </div>
        
        <div>
          <label style="display: flex; align-items: center; margin-bottom: 10px; cursor: pointer;">
            <input type="checkbox" id="chk-background" checked style="margin-right: 10px;">
            Enable Background Service (Global Mode)
          </label>
        </div>
        
        <hr/>
        
        <div>
          <button class="button" id="btn-logs">Get Call Logs</button>
          <button class="button" id="btn-contacts">Get Contacts</button>
          <button class="button" id="btn-sync">Sync Background Results</button>
        </div>

        <div style="background: #eee; padding: 10px; border-radius: 5px; margin-bottom: 10px;">
          <div style="margin-bottom: 10px;">
            <strong>Tracking Mode:</strong>
            <select id="sel-tracking-mode">
              <option value="ALL">All Calls</option>
              <option value="SELECTED">Tracked Numbers Only</option>
            </select>
          </div>
          <div style="display: flex; flex-direction: column; gap: 5px; margin-bottom: 5px;">
            <input type="text" id="txt-number" placeholder="Phone Number (+123...)" style="padding: 5px;">
            <input type="text" id="txt-name" placeholder="Name (e.g. John Doe)" style="padding: 5px;">
            <div style="display: flex; gap: 5px;">
              <select id="sel-entity-type" style="padding: 5px; flex: 1;">
                <option value="SALES">Sales (Lead)</option>
                <option value="CRM">CRM (Customer)</option>
                <option value="EMPLOYEE">Internal (Employee)</option>
              </select>
              <input type="text" id="txt-entity-id" placeholder="ID (e.g. lead_001)" style="padding: 5px; flex: 1;">
            </div>
            <button class="button" id="btn-add-number" style="margin:0;">Add with Metadata</button>
          </div>
          <div>
            <button class="button" id="btn-list-numbers" style="padding: 5px 10px;">List Tracked</button>
            <button class="button" id="btn-clear-numbers" style="padding: 5px 10px; background: #e53e3e;">Clear All</button>
          </div>
        </div>

        <div id="output">Output will appear here...</div>
      </main>
    </div>
    `;
    }

    connectedCallback() {
      const self = this;
      const output = self.shadowRoot.querySelector('#output');

      const logToView = (data) => {
        console.log(data);
        output.textContent = JSON.stringify(data, null, 2);
      };

      self.shadowRoot.querySelector('#btn-permissions').addEventListener('click', async () => {
        try {
          const res = await CallManager.requestPermissions();
          logToView(res);
        } catch (e) {
          logToView({ error: e.message });
        }
      });
      
      self.shadowRoot.querySelector('#btn-overlay').addEventListener('click', async () => {
        try {
          const res = await CallManager.requestOverlayPermission();
          logToView(res);
        } catch (e) {
          logToView({ error: e.message });
        }
      });

      self.shadowRoot.querySelector('#btn-logs').addEventListener('click', async () => {
        try {
          output.textContent = "Loading call logs...";
          const res = await CallManager.getCallLogs({ limit: 10 });
          logToView(res);
        } catch (e) {
          logToView({ error: e.message });
        }
      });

      self.shadowRoot.querySelector('#btn-contacts').addEventListener('click', async () => {
        try {
          output.textContent = "Loading contacts...";
          const res = await CallManager.getContacts({});
          logToView(res);
        } catch (e) {
          logToView({ error: e.message });
        }
      });

      self.shadowRoot.querySelector('#btn-sync').addEventListener('click', async () => {
        try {
          output.textContent = "Checking for background results...";
          const res = await CallManager.getPendingSubmissions();
          logToView(res);
          if (res.submissions && res.submissions.length > 0) {
              // Usually you'd send these to your server here, then clear.
              // For demo, we just clear immediately.
              await CallManager.clearPendingSubmissions();
              output.textContent += "\n\n[INFO] Cleared native storage.";
          }
        } catch (e) {
          logToView({ error: e.message });
        }
      });

      const backgroundChk = self.shadowRoot.querySelector('#chk-background');
      
      // Load initial state
      CallManager.isBackgroundServiceEnabled().then(res => {
          backgroundChk.checked = res.enabled;
      }).catch(e => console.error("Failed to load background state", e));

      backgroundChk.addEventListener('change', async (e) => {
          try {
              const enabled = e.target.checked;
              await CallManager.setBackgroundServiceEnabled({ enabled });
              logToView({ background_service_enabled: enabled });
          } catch (e) {
              logToView({ error: e.message });
              // Revert on failure
              backgroundChk.checked = !backgroundChk.checked;
          }
      });

      self.shadowRoot.querySelector('#btn-show-overlay').addEventListener('click', async () => {
        try {
          // Clear portal for native test
          await CallManager.setOverlayConfig({ url: "" });
          await CallManager.showOverlay({
              number: "+91 9876543210",
              name: "John Doe (Native)",
              duration: 125,
              mode: 'AFTER_CALL'
          });
        } catch (e) {
          logToView({ error: e.message });
        }
      });

      self.shadowRoot.querySelector('#btn-portal').addEventListener('click', async () => {
        try {
          output.textContent = "Setting up Web Portal...";
          // In a real app, this would be '/overlay'
          // For this test, we use a data URL or a relative path we'll create
          await CallManager.setOverlayConfig({ 
              url: "/overlay.html",
              height: 400,
              width: 320
          });
          
          await CallManager.showOverlay({
              number: "+91 9999999999",
              name: "Investor (React Design)",
              duration: 300,
              mode: 'AFTER_CALL'
          });
          logToView({ action: "setOverlayConfig", status: "success" });
        } catch (e) {
          logToView({ error: e.message });
        }
      });

      const trackingSelect = self.shadowRoot.querySelector('#sel-tracking-mode');
      const numberInput = self.shadowRoot.querySelector('#txt-number');

      // Load tracking mode
      CallManager.getTrackingMode().then(res => {
          trackingSelect.value = res.mode;
      });

      trackingSelect.addEventListener('change', async (e) => {
          try {
              await CallManager.setTrackingMode({ mode: e.target.value });
              logToView({ tracking_mode: e.target.value });
          } catch (err) {
              logToView({ error: err.message });
          }
      });

      self.shadowRoot.querySelector('#btn-add-number').addEventListener('click', async () => {
          try {
              const num = numberInput.value.trim();
              const name = self.shadowRoot.querySelector('#txt-name').value.trim();
              const entityType = self.shadowRoot.querySelector('#sel-entity-type').value;
              const entityId = self.shadowRoot.querySelector('#txt-entity-id').value.trim();
              
              if (!num) return alert("Enter a number");
              
              const item = { 
                  number: num, 
                  name: name || undefined, 
                  entityType: entityType, 
                  entityId: entityId || undefined 
              };
              
              await CallManager.addTrackedNumbers({ items: [item] });
              
              numberInput.value = "";
              self.shadowRoot.querySelector('#txt-name').value = "";
              self.shadowRoot.querySelector('#txt-entity-id').value = "";
              
              logToView({ action: "added", item });
          } catch (err) {
              logToView({ error: err.message });
          }
      });

      self.shadowRoot.querySelector('#btn-list-numbers').addEventListener('click', async () => {
          try {
              const res = await CallManager.getAllTrackedNumbers();
              logToView(res);
          } catch (err) {
              logToView({ error: err.message });
          }
      });

      self.shadowRoot.querySelector('#btn-clear-numbers').addEventListener('click', async () => {
          try {
              await CallManager.removeAllTrackedNumbers();
              logToView({ action: "cleared_all" });
          } catch (err) {
              logToView({ error: err.message });
          }
      });

      self.shadowRoot.querySelector('#btn-hide-overlay').addEventListener('click', async () => {
        try {
          await CallManager.hideOverlay();
          logToView({ action: "hideOverlay", message: "Overlay dismissed manually" });
        } catch (e) {
          logToView({ error: e.message });
        }
      });
    }
  },
);
