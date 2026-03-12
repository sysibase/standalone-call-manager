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
          await CallManager.showOverlay({
              number: "+91 9876543210",
              name: "John Doe (Lead)",
              duration: 125, // 2m 5s
              mode: 'AFTER_CALL'
          });
          logToView({ action: "showOverlay", message: "Overlay triggered manually" });
        } catch (e) {
          logToView({ error: e.message });
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
