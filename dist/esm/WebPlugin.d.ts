import { WebPlugin } from '@capacitor/core';
import type { CallManagerPlugin, PermissionStatus, CallLog, GetCallLogsOptions, Contact, SMSThread, SMSMessage } from './definitions';
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
    getSMSThreads(): Promise<{
        threads: SMSThread[];
    }>;
    getSMSMessages(_options: {
        threadId: string;
    }): Promise<{
        messages: SMSMessage[];
    }>;
    sendSMS(_options: {
        number: string;
        message: string;
    }): Promise<void>;
    startRecording(): Promise<void>;
    pauseRecording(): Promise<void>;
    resumeRecording(): Promise<void>;
    stopRecording(): Promise<{
        filePath: string;
    }>;
}
