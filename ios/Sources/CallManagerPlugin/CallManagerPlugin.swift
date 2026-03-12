import Foundation
import Capacitor
import Contacts

/**
 * =============================================================================
 * CallManagerPlugin.swift — iOS Implementation
 * =============================================================================
 * iOS is highly restrictive. This implementation provides base methods to 
 * ensure the plugin doesn't crash, while implementing allowed features like Contacts. //
 */
@objc(CallManagerPlugin)
public class CallManagerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "CallManagerPlugin"
    public let jsName = "CallManager"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestOverlayPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCallLogs", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "initCalling", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startCallListener", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopCallListener", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getContacts", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSMSThreads", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSMSMessages", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendSMS", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startRecording", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "pauseRecording", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resumeRecording", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopRecording", returnType: CAPPluginReturnPromise)
    ]

    // --- Permissions ---
    
    @objc func checkPermissions(_ call: CAPPluginCall) {
        let contactStatus = CNContactStore.authorizationStatus(for: .contacts)
        var result = [String: Any]()
        
        result["contacts"] = (contactStatus == .authorized) ? "granted" : "denied"
        result["callLog"] = "denied" // Not possible on iOS
        result["sms"] = "denied" // Reading not possible
        result["phoneState"] = "denied"
        result["callPhone"] = "granted" // Assuming can use tel://
        result["microphone"] = "denied"
        result["overlay"] = "denied"
        
        call.resolve(result)
    }

    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        let store = CNContactStore()
        store.requestAccess(for: .contacts) { [weak self] granted, error in
            self?.checkPermissions(call)
        }
    }

    @objc func requestOverlayPermission(_ call: CAPPluginCall) {
        call.resolve(["overlay": "denied"])
    }

    // --- Telephony ---

    @objc func initCalling(_ call: CAPPluginCall) {
        guard let number = call.getString("number") else {
            call.reject("INVALID_ARGUMENT", "Number is required")
            return
        }
        
        let cleanedNumber = number.components(separatedBy: CharacterSet.decimalDigits.inverted).joined()
        if let url = URL(string: "tel://\(cleanedNumber)"), UIApplication.shared.canOpenURL(url) {
            DispatchQueue.main.async {
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
                call.resolve()
            }
        } else {
            call.reject("FEATURE_NOT_SUPPORTED", "Unable to initiate call on this device")
        }
    }

    @objc func startCallListener(_ call: CAPPluginCall) {
        call.resolve() // CallKit integration requires heavy setup; stub for now.
    }

    @objc func stopCallListener(_ call: CAPPluginCall) {
        call.resolve()
    }

    // --- Contacts ---

    @objc func getContacts(_ call: CAPPluginCall) {
        let store = CNContactStore()
        store.requestAccess(for: .contacts) { [weak self] granted, error in
            if let error = error {
                call.reject("UNAVAILABLE", error.localizedDescription)
                return
            }
            if !granted {
                call.reject("PERMISSION_DENIED", "Contacts access denied")
                return
            }
            
            let search = call.getString("search")?.lowercased() ?? ""
            let limit = call.getInt("limit") ?? 500
            let offset = call.getInt("offset") ?? 0

            let keys = [CNContactIdentifierKey, CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey] as [CNKeyDescriptor]
            let request = CNContactFetchRequest(keysToFetch: keys)
            var contacts = [[String: Any]]()
            var uniqueCount = 0
            
            do {
                try store.enumerateContacts(with: request) { contact, _ in
                    let name = "\(contact.givenName) \(contact.familyName)".trimmingCharacters(in: .whitespaces)
                    
                    if !search.isEmpty {
                        let matchesName = name.lowercased().contains(search)
                        let matchesNumber = contact.phoneNumbers.contains { $0.value.stringValue.lowercased().contains(search) }
                        if !matchesName && !matchesNumber { return }
                    }

                    if uniqueCount < offset {
                        uniqueCount += 1
                        return
                    }

                    if contacts.count >= limit { return }
                    
                    var entry = [String: Any]()
                    entry["id"] = contact.identifier
                    entry["name"] = name.isEmpty ? "Unknown" : name
                    
                    var numbers = [String]()
                    for number in contact.phoneNumbers {
                        numbers.append(number.value.stringValue)
                    }
                    entry["numbers"] = numbers
                    contacts.append(entry)
                    uniqueCount += 1
                }
                call.resolve(["contacts": contacts, "total": contacts.count])
            } catch {
                call.reject("UNAVAILABLE", "Failed to fetch contacts")
            }
        }
    }

    // --- Stubs for restricted features ---

    @objc func getCallLogs(_ call: CAPPluginCall) {
        call.resolve(["logs": [], "total": 0])
    }

    @objc func getSMSThreads(_ call: CAPPluginCall) {
        call.resolve(["threads": []])
    }

    @objc func getSMSMessages(_ call: CAPPluginCall) {
        call.resolve(["messages": []])
    }

    @objc func sendSMS(_ call: CAPPluginCall) {
        call.reject("UNAVAILABLE", "Direct SMS without UI is restricted on iOS.")
    }

    @objc func startRecording(_ call: CAPPluginCall) {
        call.reject("FEATURE_NOT_SUPPORTED", "Microphone ambient recording not implemented natively.")
    }

    @objc func pauseRecording(_ call: CAPPluginCall) {
        call.reject("FEATURE_NOT_SUPPORTED", "Microphone ambient recording not implemented natively.")
    }

    @objc func resumeRecording(_ call: CAPPluginCall) {
        call.reject("FEATURE_NOT_SUPPORTED", "Microphone ambient recording not implemented natively.")
    }

    @objc func stopRecording(_ call: CAPPluginCall) {
        call.reject("FEATURE_NOT_SUPPORTED", "Microphone ambient recording not implemented natively.")
    }
}
