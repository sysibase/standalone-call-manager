var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { WebPlugin, CapacitorException, ExceptionCode } from '@capacitor/core';
export class CallManagerWeb extends WebPlugin {
    checkPermissions() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    requestPermissions() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    requestOverlayPermission() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    getCallLogs(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    initCalling(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    startCallListener() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    stopCallListener() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    getContacts(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    startRecording() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    pauseRecording() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    resumeRecording() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
    stopRecording() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new CapacitorException('Not implemented on web.', ExceptionCode.Unimplemented);
        });
    }
}
//# sourceMappingURL=WebPlugin.js.map