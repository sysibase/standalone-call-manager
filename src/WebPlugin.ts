import { WebPlugin, CapacitorException, ExceptionCode } from '@capacitor/core';
import type { CallManagerPlugin, PermissionStatus, CallLog, GetCallLogsOptions, Contact, SMSThread, SMSMessage } from './definitions';

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

  async getSMSThreads(): Promise<{ threads: SMSThread[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async getSMSMessages(_options: { threadId: string }): Promise<{ messages: SMSMessage[] }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async sendSMS(_options: { number: string; message: string }): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async startRecording(): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async pauseRecording(): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async resumeRecording(): Promise<void> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }

  async stopRecording(): Promise<{ filePath: string }> {
    throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
  }
}
