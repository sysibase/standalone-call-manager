import { WebPlugin, CapacitorException, ExceptionCode } from '@capacitor/core';
import type { 
  CallManagerPlugin, 
  PermissionStatus, 
  CallLog, 
  GetCallLogsOptions, 
  Contact, 
  TrackedItem, 
  CallOverlaySubmittedPayload, 
  OverlayConfig 
} from './definitions';

/**
 * CallManagerWeb — Web Compatibility Layer
 * =============================================================================
 * Yeh plugin primarily Android features (Telephony, Overlays) ke liye design kiya gaya hai.
 * Web version implementation nahi karta par 'Unimplemented' errors handle karta hai,
 * taaki apka React/Angular code desktop browsers pe crash na ho.
 * =============================================================================
 */
export class CallManagerWeb extends WebPlugin implements CallManagerPlugin {
  async checkPermissions(): Promise<PermissionStatus> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async requestPermissions(): Promise<PermissionStatus> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async requestOverlayPermission(): Promise<PermissionStatus> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getCallLogs(_options: GetCallLogsOptions): Promise<{ success: boolean; logs: CallLog[]; total: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async initCalling(_options: { number: string }): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async startCallListener(): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async stopCallListener(): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getContacts(_options: { search?: string; limit?: number; offset?: number }): Promise<{ success: boolean; contacts: Contact[]; total: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getPendingSubmissions(): Promise<{ success: boolean; submissions: CallOverlaySubmittedPayload[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async clearPendingSubmissions(): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async setBackgroundServiceEnabled(_options: { enabled: boolean }): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async isBackgroundServiceEnabled(): Promise<{ success: boolean; enabled: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async setAutoOpenAppEnabled(_options: { enabled: boolean }): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async isAutoOpenAppEnabled(): Promise<{ success: boolean; enabled: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async setTrackingMode(_options: { mode: 'ALL' | 'SELECTED' }): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getTrackingMode(): Promise<{ success: boolean; mode: 'ALL' | 'SELECTED' }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async addTrackedNumbers(_options: { items: TrackedItem[] }): Promise<{ success: boolean; count: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async removeTrackedNumbers(_options: { numbers: string[] }): Promise<{ success: boolean; count: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async removeAllTrackedNumbers(): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async removeTrackedNumbersByEntity(_options: { entityType: string }): Promise<{ success: boolean; count: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async removeTrackedNumbersByEntityId(_options: { entityId: string }): Promise<{ success: boolean; count: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getAllTrackedNumbers(): Promise<{ success: boolean; items: TrackedItem[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getTrackedNumbersByEntity(_options: { entityType: string }): Promise<{ success: boolean; items: TrackedItem[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getTrackedNumbersByEntityId(_options: { entityId: string }): Promise<{ success: boolean; items: TrackedItem[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async showOverlay(_options: { number: string; name?: string; duration?: number; mode?: 'DURING_CALL' | 'AFTER_CALL' }): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async hideOverlay(): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async setOverlayConfig(_options: OverlayConfig): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async submitOverlayResult(_data: CallOverlaySubmittedPayload): Promise<{ success: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }
}
