/**
 * CallManager Definitions — Central API Contract
 * =============================================================================
 * Yeh file plugin ke sare public methods, events aur data models ko define karti hai.
 * React/Angular apps in interfaces ko use karke type-safe implementation kar sakte hain.
 * =============================================================================
 */
import type { PermissionState } from '@capacitor/core';

export interface PermissionStatus {
  callLog: PermissionState;
  phoneState: PermissionState;
  callPhone: PermissionState;
  contacts: PermissionState;
  overlay: PermissionState;
  notifications: PermissionState;
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

export interface OverlayConfig {
  /**
   * The relative or absolute URL to load in the overlay WebView.
   * Example: '/call-overlay' or 'http://localhost/call-overlay'
   */
  url?: string;
  /**
   * Optional custom height for the floating window in DP.
   */
  height?: number;
  /**
   * Optional custom width for the floating window in DP.
   */
  width?: number;
}

/**
 * TrackedItem Interface
 * =============================================================================
 * Database mein save hone wala har lead ya contact is format mein hota hai.
 * 'entityType' aur 'entityId' ki wajah se overlay ko context milta hai.
 */
export interface TrackedItem {
  number: string;
  name?: string;
  entityType?: string;
  entityId?: string;
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
  getCallLogs(options: GetCallLogsOptions): Promise<{ success: boolean; logs: CallLog[]; total: number }>;
  
  // --- Telephony ---

  /**
   * Initiate a phone call to the given number.
   */
  initCalling(options: { number: string }): Promise<{ success: boolean }>;

  /**
   * Start listening to native call state changes to trigger overlays (Android only).
   */
  startCallListener(): Promise<{ success: boolean }>;

  /**
   * Stop listening to native call state changes.
   */
  stopCallListener(): Promise<{ success: boolean }>;

  // --- Contacts ---

  /**
   * Retrieve a list of generic device contacts.
   * Android: Requires `contacts` permission.
   * iOS: Requires `contacts` permission. Returns identical format.
   * Web: Throws Unimplemented.
   */
  /** Search for contacts */
  getContacts(options: { search?: string; limit?: number; offset?: number }): Promise<{ success: boolean; contacts: Contact[]; total: number }>;

  // --- Native Overlay Sync ---

  /**
   * Retrieve any overlay results that were saved while the app was closed (Android only).
   */
  getPendingSubmissions(): Promise<{ success: boolean; submissions: CallOverlaySubmittedPayload[] }>;

  /**
   * Clear the local storage of pending submissions (Android only).
   */
  clearPendingSubmissions(): Promise<{ success: boolean }>;

  /**
   * Enable or disable the background call receiver (Android only).
   * If disabled, overlays will ONLY appear if the app is currently running.
   */
  setBackgroundServiceEnabled(options: { enabled: boolean }): Promise<{ success: boolean }>;

  /**
   * Check if the background service is currently enabled.
   */
  isBackgroundServiceEnabled(): Promise<{ success: boolean; enabled: boolean }>;

  /**
   * Enable or disable auto-opening the app via deep link instead of showing overlay (Android only).
   */
  setAutoOpenAppEnabled(options: { enabled: boolean }): Promise<{ success: boolean }>;

  /**
   * Check if auto-open app feature is enabled.
   */
  isAutoOpenAppEnabled(): Promise<{ success: boolean; enabled: boolean }>;

  /**
   * Set the tracking mode for overlays (Android only).
   * 'ALL' - Show overlay for every call.
   * 'SELECTED' - Only show for numbers in the tracked list.
   */
  setTrackingMode(options: { mode: 'ALL' | 'SELECTED' }): Promise<{ success: boolean }>;

  /**
   * Get the current tracking mode.
   */
  getTrackingMode(): Promise<{ success: boolean; mode: 'ALL' | 'SELECTED' }>;

  /**
   * Add numbers to the white-list for selective tracking (Android only).
   */
  addTrackedNumbers(options: { items: TrackedItem[] }): Promise<{ success: boolean; count: number }>;

  /**
   * Remove specific numbers from the white-list (Android only).
   */
  removeTrackedNumbers(options: { numbers: string[] }): Promise<{ success: boolean; count: number }>;

  /**
   * Clear the entire white-list (Android only).
   */
  removeAllTrackedNumbers(): Promise<{ success: boolean }>;

  /**
   * Remove all tracked numbers belonging to a specific entity type (Android only).
   */
  removeTrackedNumbersByEntity(options: { entityType: string }): Promise<{ success: boolean; count: number }>;

  /**
   * Remove all tracked numbers belonging to a specific entity ID (Android only).
   */
  removeTrackedNumbersByEntityId(options: { entityId: string }): Promise<{ success: boolean; count: number }>;

  /**
   * Get all currently tracked numbers.
   */
  getAllTrackedNumbers(): Promise<{ success: boolean; items: TrackedItem[] }>;

  /**
   * Get all tracked numbers belonging to a specific entity type (Android only).
   */
  getTrackedNumbersByEntity(options: { entityType: string }): Promise<{ success: boolean; items: TrackedItem[] }>;

  /**
   * Get all tracked numbers belonging to a specific entity ID (Android only).
   */
  getTrackedNumbersByEntityId(options: { entityId: string }): Promise<{ success: boolean; items: TrackedItem[] }>;

  /**
   * Manually trigger the CRM overlay (Android only).
   */
  showOverlay(options: { 
    number: string; 
    name?: string; 
    duration?: number; 
    mode?: 'DURING_CALL' | 'AFTER_CALL' 
  }): Promise<{ success: boolean }>;

  /**
   * Manually dismiss any active overlay (Android only).
   */
  hideOverlay(): Promise<{ success: boolean }>;

  /**
   * Configure a web-based overlay portal (Android only).
   * Providing a URL will disable the default native XML UI.
   */
  setOverlayConfig(options: OverlayConfig): Promise<{ success: boolean }>;

  /**
   * Submit data from a Web Overlay portal back to the main app (Android only).
   */
  submitOverlayResult(data: CallOverlaySubmittedPayload): Promise<{ success: boolean }>;

  // --- Events ---

  /** Listens for an incoming ringing call */
  addListener(eventName: 'callIncoming', listenerFunc: (data: { number: string; name: string; entityType?: string; entityId?: string; timestamp: number }) => void): any;
  /** Listens for a connected/active call */
  addListener(eventName: 'callStarted', listenerFunc: (data: { number: string; name: string; entityType?: string; entityId?: string; startTime: number }) => void): any;
  /** Listens for call completion */
  addListener(eventName: 'callEnded', listenerFunc: (data: { number: string; name: string; entityType?: string; entityId?: string; duration: number; endTime: number }) => void): any;
  /** Fired when the native post-call CRM overlay is submitted (Android only) */
  addListener(eventName: 'callOverlaySubmitted', listenerFunc: (data: CallOverlaySubmittedPayload) => void): any;
  /** Fired when the call overlay is successfully opened (Android only) */
  addListener(eventName: 'callOverlayOpened', listenerFunc: (data: { number: string; entityType?: string; entityId?: string; timestamp: number }) => void): any;
  /** Fired when the call overlay is closed (Android only) */
  addListener(eventName: 'callOverlayClosed', listenerFunc: (data: { timestamp: number }) => void): any;

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
