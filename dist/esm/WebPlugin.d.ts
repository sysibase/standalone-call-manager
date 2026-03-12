import { WebPlugin } from '@capacitor/core';
import type { CallManagerPlugin, PermissionStatus, CallLog, GetCallLogsOptions, Contact } from './definitions';
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
    startRecording(): Promise<void>;
    pauseRecording(): Promise<void>;
    resumeRecording(): Promise<void>;
    stopRecording(): Promise<{
        filePath: string;
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
    showOverlay(_options: {
        number: string;
        name?: string;
        duration?: number;
        mode?: 'DURING_CALL' | 'AFTER_CALL';
    }): Promise<void>;
    hideOverlay(): Promise<void>;
}
