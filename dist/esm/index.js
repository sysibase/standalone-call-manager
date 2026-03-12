import { registerPlugin } from '@capacitor/core';
import { CallManagerWeb } from './WebPlugin';
const CallManager = registerPlugin('CallManager', {
    web: () => new CallManagerWeb(),
});
export * from './definitions';
export { CallManager };
//# sourceMappingURL=index.js.map