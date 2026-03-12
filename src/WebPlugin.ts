import { WebPlugin, CapacitorException, ExceptionCode } from '@capacitor/core';
import type { CallManagerPlugin, PermissionStatus, CallLog, GetCallLogsOptions, Contact } from './definitions';

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

  async getCallLogs(_options: GetCallLogsOptions): Promise<{ logs: CallLog[]; total: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async initCalling(_options: { number: string }): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async startCallListener(): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async stopCallListener(): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getContacts(_options: { search?: string; limit?: number; offset?: number }): Promise<{ contacts: Contact[]; total: number }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getPendingSubmissions(): Promise<{ submissions: any[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async clearPendingSubmissions(): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async setBackgroundServiceEnabled(_options: { enabled: boolean }): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async isBackgroundServiceEnabled(): Promise<{ enabled: boolean }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async setTrackingMode(_options: { mode: 'ALL' | 'SELECTED' }): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getTrackingMode(): Promise<{ mode: 'ALL' | 'SELECTED' }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async addTrackedNumbers(_options: { items: any[] }): Promise<{ success: boolean; count: number }> {
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

  async getAllTrackedNumbers(): Promise<{ items: any[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getTrackedNumbersByEntity(_options: { entityType: string }): Promise<{ items: any[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async showOverlay(_options: { number: string; name?: string; duration?: number; mode?: 'DURING_CALL' | 'AFTER_CALL' }): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async hideOverlay(): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async setOverlayConfig(_options: any): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async submitOverlayResult(_data: any): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }
}
