import { WebPlugin } from '@capacitor/core';
import type { CallManagerPlugin, PermissionStatus, CallLog, GetCallLogsOptions, Contact, TrackedItem, CallOverlaySubmittedPayload, OverlayConfig } from './definitions';
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
        success: boolean;
        logs: CallLog[];
        total: number;
    }>;
    initCalling(_options: {
        number: string;
    }): Promise<{
        success: boolean;
    }>;
    startCallListener(): Promise<{
        success: boolean;
    }>;
    stopCallListener(): Promise<{
        success: boolean;
    }>;
    getContacts(_options: {
        search?: string;
        limit?: number;
        offset?: number;
    }): Promise<{
        success: boolean;
        contacts: Contact[];
        total: number;
    }>;
    getPendingSubmissions(): Promise<{
        success: boolean;
        submissions: CallOverlaySubmittedPayload[];
    }>;
    clearPendingSubmissions(): Promise<{
        success: boolean;
    }>;
    setBackgroundServiceEnabled(_options: {
        enabled: boolean;
    }): Promise<{
        success: boolean;
    }>;
    isBackgroundServiceEnabled(): Promise<{
        success: boolean;
        enabled: boolean;
    }>;
    setAutoOpenAppEnabled(_options: {
        enabled: boolean;
    }): Promise<{
        success: boolean;
    }>;
    isAutoOpenAppEnabled(): Promise<{
        success: boolean;
        enabled: boolean;
    }>;
    setTrackingMode(_options: {
        mode: 'ALL' | 'SELECTED';
    }): Promise<{
        success: boolean;
    }>;
    getTrackingMode(): Promise<{
        success: boolean;
        mode: 'ALL' | 'SELECTED';
    }>;
    addTrackedNumbers(_options: {
        items: TrackedItem[];
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
        success: boolean;
        items: TrackedItem[];
    }>;
    getTrackedNumbersByEntity(_options: {
        entityType: string;
    }): Promise<{
        success: boolean;
        items: TrackedItem[];
    }>;
    getTrackedNumbersByEntityId(_options: {
        entityId: string;
    }): Promise<{
        success: boolean;
        items: TrackedItem[];
    }>;
    showOverlay(_options: {
        number: string;
        name?: string;
        duration?: number;
        mode?: 'DURING_CALL' | 'AFTER_CALL';
    }): Promise<{
        success: boolean;
    }>;
    hideOverlay(): Promise<{
        success: boolean;
    }>;
    setOverlayConfig(_options: OverlayConfig): Promise<{
        success: boolean;
    }>;
    submitOverlayResult(_data: CallOverlaySubmittedPayload): Promise<{
        success: boolean;
    }>;
}
