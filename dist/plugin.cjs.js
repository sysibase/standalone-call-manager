'use strict';

var core = require('@capacitor/core');

var __awaiter = (undefined && undefined.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
class CallManagerWeb extends core.WebPlugin {
    checkPermissions() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    requestPermissions() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    requestOverlayPermission() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    getCallLogs(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    initCalling(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    startCallListener() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    stopCallListener() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    getContacts(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    getPendingSubmissions() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    clearPendingSubmissions() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    setBackgroundServiceEnabled(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    isBackgroundServiceEnabled() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    setTrackingMode(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    getTrackingMode() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    addTrackedNumbers(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    removeTrackedNumbers(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    removeAllTrackedNumbers() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    removeTrackedNumbersByEntity(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    removeTrackedNumbersByEntityId(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    getAllTrackedNumbers() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    getTrackedNumbersByEntity(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    showOverlay(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    hideOverlay() {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    setOverlayConfig(_options) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
    submitOverlayResult(_data) {
        return __awaiter(this, void 0, void 0, function* () {
            throw new core.CapacitorException('Not implemented on web.', core.ExceptionCode.Unimplemented);
        });
    }
}

exports.CallType = void 0;
(function (CallType) {
    CallType["INCOMING"] = "INCOMING";
    CallType["OUTGOING"] = "OUTGOING";
    CallType["MISSED"] = "MISSED";
    CallType["REJECTED"] = "REJECTED";
    CallType["UNKNOWN"] = "UNKNOWN";
})(exports.CallType || (exports.CallType = {}));
exports.CallLogDateFilter = void 0;
(function (CallLogDateFilter) {
    CallLogDateFilter["TODAY"] = "TODAY";
    CallLogDateFilter["YESTERDAY"] = "YESTERDAY";
    CallLogDateFilter["WEEK"] = "WEEK";
    CallLogDateFilter["MONTH"] = "MONTH";
    CallLogDateFilter["ALL"] = "ALL";
})(exports.CallLogDateFilter || (exports.CallLogDateFilter = {}));
exports.CallOverlayStatus = void 0;
(function (CallOverlayStatus) {
    CallOverlayStatus["INTERESTED"] = "INTERESTED";
    CallOverlayStatus["FOLLOW_UP"] = "FOLLOW_UP";
    CallOverlayStatus["NOT_INTERESTED"] = "NOT_INTERESTED";
})(exports.CallOverlayStatus || (exports.CallOverlayStatus = {}));
exports.CallManagerErrorCode = void 0;
(function (CallManagerErrorCode) {
    CallManagerErrorCode["PERMISSION_DENIED"] = "PERMISSION_DENIED";
    CallManagerErrorCode["INVALID_ARGUMENT"] = "INVALID_ARGUMENT";
    CallManagerErrorCode["FEATURE_NOT_SUPPORTED"] = "FEATURE_NOT_SUPPORTED";
    CallManagerErrorCode["UNAVAILABLE"] = "UNAVAILABLE";
    CallManagerErrorCode["CANCELED"] = "CANCELED";
})(exports.CallManagerErrorCode || (exports.CallManagerErrorCode = {}));

const CallManager = core.registerPlugin('CallManager', {
    web: () => new CallManagerWeb(),
});

exports.CallManager = CallManager;
//# sourceMappingURL=plugin.cjs.js.map
