import type { PermissionState } from '@capacitor/core';

export interface PermissionStatus {
  callLog: PermissionState;
  phoneState: PermissionState;
  callPhone: PermissionState;
  contacts: PermissionState;
  microphone: PermissionState;
  overlay: PermissionState;
}

export enum CallType {
  INCOMING = 'INCOMING',
  OUTGOING = 'OUTGOING',
  MISSED = 'MISSED',
  REJECTED = 'REJECTED',
  UNKNOWN = 'UNKNOWN',
}

export enum CallLogDateFilter {
  TODAY = 'TODAY',
  YESTERDAY = 'YESTERDAY',
  WEEK = 'WEEK',
  MONTH = 'MONTH',
  ALL = 'ALL',
}

export interface CallLog {
  id: string;
  number: string;
  name: string;
  type: CallType;
  date: number;
  duration: number;
}

export interface GetCallLogsOptions {
  /** Filter logs by type. Defaults to 'ALL' */
  type?: 'ALL' | CallType;
  /** Partial search by name or number */
  search?: string;
  /** Date filter for logs */
  date?: CallLogDateFilter | string;
  /** Maximum number of logs to return. Defaults to 500 */
  limit?: number;
  /** Offset for pagination. Defaults to 0 */
  offset?: number;
}

export interface Contact {
  id: string;
  name: string;
  numbers: string[];
  photoUri?: string;
}

export enum CallOverlayStatus {
  INTERESTED = 'INTERESTED',
  FOLLOW_UP = 'FOLLOW_UP',
  NOT_INTERESTED = 'NOT_INTERESTED',
}

export interface CallOverlaySubmittedPayload {
  number: string;
  name: string;
  status: CallOverlayStatus | string;
  notes: string;
  duration: number;
  timestamp: number;
}

export interface CallManagerPlugin {
  // --- Permissions ---
  
  /**
   * Check the status of all necessary permissions.
   */
  checkPermissions(): Promise<PermissionStatus>;

  /**
   * Request standard telephony and contact permissions.
   */
  requestPermissions(): Promise<PermissionStatus>;

  /**
   * Request system overlay permission (Android only).
   * Opens the system settings screen.
   */
  requestOverlayPermission(): Promise<PermissionStatus>;

  // --- Call Logs ---

  /**
   * Retrieve device call history.
   * Android: Requires `callLog` permission.
   * iOS: Apple restricts this API; gracefully returns an empty array.
   * Web: Throws Unimplemented.
   */
  getCallLogs(options: GetCallLogsOptions): Promise<{ logs: CallLog[]; total: number }>;
  
  // --- Telephony ---

  /**
   * Initiate a phone call to the given number.
   */
  initCalling(options: { number: string }): Promise<void>;

  /**
   * Start listening to native call state changes to trigger overlays (Android only).
   */
  startCallListener(): Promise<void>;

  /**
   * Stop listening to native call state changes.
   */
  stopCallListener(): Promise<void>;

  // --- Contacts ---

  /**
   * Retrieve a list of generic device contacts.
   * Android: Requires `contacts` permission.
   * iOS: Requires `contacts` permission. Returns identical format.
   * Web: Throws Unimplemented.
   */
  getContacts(options: { search?: string; limit?: number; offset?: number }): Promise<{ contacts: Contact[]; total: number }>;

  // --- Recording ---

  /**
   * Start microphone recording (Android only).
   */
  startRecording(): Promise<void>;

  /**
   * Stop recording and retrieve the file path.
   */
  stopRecording(): Promise<{ filePath: string }>;

  // --- Events ---

  /** Listens for an incoming ringing call */
  addListener(eventName: 'callIncoming', listenerFunc: (data: { number: string; name: string; timestamp: number }) => void): any;
  /** Listens for a connected/active call */
  addListener(eventName: 'callStarted', listenerFunc: (data: { number: string; name: string; startTime: number }) => void): any;
  /** Listens for call completion */
  addListener(eventName: 'callEnded', listenerFunc: (data: { number: string; name: string; duration: number; endTime: number }) => void): any;
  /** Fired when the native post-call CRM overlay is submitted (Android only) */
  addListener(eventName: 'callOverlaySubmitted', listenerFunc: (data: CallOverlaySubmittedPayload) => void): any;

  /** Remove all active listeners */
  removeAllListeners(): Promise<void>;
}

export enum CallManagerErrorCode {
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  INVALID_ARGUMENT = 'INVALID_ARGUMENT',
  FEATURE_NOT_SUPPORTED = 'FEATURE_NOT_SUPPORTED',
  UNAVAILABLE = 'UNAVAILABLE',
  CANCELED = 'CANCELED',
}
