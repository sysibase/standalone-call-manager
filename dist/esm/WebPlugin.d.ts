import { WebPlugin } from '@capacitor/core';
import type { CallManagerPlugin, PermissionStatus, CallLog, GetCallLogsOptions, Contact } from './definitions';
/**
 * CallManagerWeb — Web Compatibility Layer
 * =============================================================================
 * Yeh plugin primarily Android features (Telephony, Overlays) ke liye design kiya gaya hai.
 * Web version implementation nahi karta par 'Unimplemented' errors handle karta hai,
 * taaki apka React/Angular code desktop browsers pe crash na ho.
 * =============================================================================
 */
export declare class CallManagerWeb extends WebPlugin implements CallManagerPlugin {
    checkPermissions(): Promise<PermissionStatus>;
    requestPermissions(): Promise<PermissionStatus>;
    requestOverlayPermission(): Promise<PermissionStatus>;
    getCallLogs(_options: GetCallLogsOptions): Promise<{
        logs: CallLog[];
        total: number;
    }>;
    initCalling(_options: {
        number: string;
    }): Promise<void>;
    startCallListener(): Promise<void>;
    stopCallListener(): Promise<void>;
    getContacts(_options: {
        search?: string;
        limit?: number;
        offset?: number;
    }): Promise<{
        contacts: Contact[];
        total: number;
    }>;
    getPendingSubmissions(): Promise<{
        submissions: any[];
    }>;
    clearPendingSubmissions(): Promise<void>;
    setBackgroundServiceEnabled(_options: {
        enabled: boolean;
    }): Promise<void>;
    isBackgroundServiceEnabled(): Promise<{
        enabled: boolean;
    }>;
    setTrackingMode(_options: {
        mode: 'ALL' | 'SELECTED';
    }): Promise<void>;
    getTrackingMode(): Promise<{
        mode: 'ALL' | 'SELECTED';
    }>;
    addTrackedNumbers(_options: {
        items: any[];
    }): Promise<{
        success: boolean;
        count: number;
    }>;
    removeTrackedNumbers(_options: {
        numbers: string[];
    }): Promise<{
        success: boolean;
        count: number;
    }>;
    removeAllTrackedNumbers(): Promise<{
        success: boolean;
    }>;
    removeTrackedNumbersByEntity(_options: {
        entityType: string;
    }): Promise<{
        success: boolean;
        count: number;
    }>;
    removeTrackedNumbersByEntityId(_options: {
        entityId: string;
    }): Promise<{
        success: boolean;
        count: number;
    }>;
    getAllTrackedNumbers(): Promise<{
        items: any[];
    }>;
    getTrackedNumbersByEntity(_options: {
        entityType: string;
    }): Promise<{
        items: any[];
    }>;
    showOverlay(_options: {
        number: string;
        name?: string;
        duration?: number;
        mode?: 'DURING_CALL' | 'AFTER_CALL';
    }): Promise<void>;
    hideOverlay(): Promise<void>;
    setOverlayConfig(_options: any): Promise<void>;
    submitOverlayResult(_data: any): Promise<void>;
}
