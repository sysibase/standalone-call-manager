import { registerPlugin } from '@capacitor/core';
import type { CallManagerPlugin } from './definitions';
import { CallManagerWeb } from './WebPlugin';

const CallManager = registerPlugin<CallManagerPlugin>('CallManager', {
  web: () => new CallManagerWeb(),
});

export * from './definitions';
export { CallManager };
