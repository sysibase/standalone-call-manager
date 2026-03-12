export var CallType;
(function (CallType) {
    CallType["INCOMING"] = "INCOMING";
    CallType["OUTGOING"] = "OUTGOING";
    CallType["MISSED"] = "MISSED";
    CallType["REJECTED"] = "REJECTED";
    CallType["UNKNOWN"] = "UNKNOWN";
})(CallType || (CallType = {}));
export var CallLogDateFilter;
(function (CallLogDateFilter) {
    CallLogDateFilter["TODAY"] = "TODAY";
    CallLogDateFilter["YESTERDAY"] = "YESTERDAY";
    CallLogDateFilter["WEEK"] = "WEEK";
    CallLogDateFilter["MONTH"] = "MONTH";
    CallLogDateFilter["ALL"] = "ALL";
})(CallLogDateFilter || (CallLogDateFilter = {}));
export var SMSType;
(function (SMSType) {
    SMSType["INCOMING"] = "INCOMING";
    SMSType["OUTGOING"] = "OUTGOING";
})(SMSType || (SMSType = {}));
export var CallOverlayStatus;
(function (CallOverlayStatus) {
    CallOverlayStatus["INTERESTED"] = "INTERESTED";
    CallOverlayStatus["FOLLOW_UP"] = "FOLLOW_UP";
    CallOverlayStatus["NOT_INTERESTED"] = "NOT_INTERESTED";
})(CallOverlayStatus || (CallOverlayStatus = {}));
export var CallManagerErrorCode;
(function (CallManagerErrorCode) {
    CallManagerErrorCode["PERMISSION_DENIED"] = "PERMISSION_DENIED";
    CallManagerErrorCode["INVALID_ARGUMENT"] = "INVALID_ARGUMENT";
    CallManagerErrorCode["FEATURE_NOT_SUPPORTED"] = "FEATURE_NOT_SUPPORTED";
    CallManagerErrorCode["UNAVAILABLE"] = "UNAVAILABLE";
    CallManagerErrorCode["CANCELED"] = "CANCELED";
})(CallManagerErrorCode || (CallManagerErrorCode = {}));
//# sourceMappingURL=definitions.js.map