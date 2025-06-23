import Foundation
import NetworkExtension
import SystemConfiguration.CaptiveNetwork

@objc(WifiManagerModule)
class WifiManagerModule: NSObject {
    
    @objc
    static func requiresMainQueueSetup() -> Bool {
        return false
    }
    
    @objc
    func scanWifiNetworks(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        // Note: iOS doesn't allow direct WiFi scanning like Android
        // This is a limitation of iOS security model
        reject("NOT_SUPPORTED", "WiFi scanning is not supported on iOS due to platform restrictions", nil)
    }
    
    @objc
    func connectToWifi(_ ssid: String, password: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        if #available(iOS 11.0, *) {
            let configuration = NEHotspotConfiguration(ssid: ssid, passphrase: password, isWEP: false)
            configuration.joinOnce = true
            
            NEHotspotConfigurationManager.shared.apply(configuration) { error in
                DispatchQueue.main.async {
                    if let error = error {
                        reject("CONNECTION_FAILED", error.localizedDescription, error)
                    } else {
                        resolve(true)
                    }
                }
            }
        } else {
            reject("VERSION_NOT_SUPPORTED", "iOS 11.0 or later is required", nil)
        }
    }
    
    @objc
    func disconnectFromWifi(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        if #available(iOS 11.0, *) {
            NEHotspotConfigurationManager.shared.removeConfiguration(forSSID: getCurrentSSID() ?? "")
            resolve(true)
        } else {
            reject("VERSION_NOT_SUPPORTED", "iOS 11.0 or later is required", nil)
        }
    }
    
    @objc
    func getCurrentWifiInfo(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        let ssid = getCurrentSSID()
        if let ssid = ssid {
            let info: [String: Any] = [
                "ssid": ssid,
                "signalStrength": -50, // iOS doesn't provide signal strength
                "networkId": 0
            ]
            resolve(info)
        } else {
            resolve(nil)
        }
    }
    
    @objc
    func isWifiEnabled(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        // iOS doesn't provide a direct way to check if WiFi is enabled
        // We can only check if we're connected to a WiFi network
        let ssid = getCurrentSSID()
        resolve(ssid != nil)
    }
    
    @objc
    func setWifiEnabled(_ enabled: Bool, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        // iOS doesn't allow programmatically enabling/disabling WiFi
        reject("NOT_SUPPORTED", "Enabling/disabling WiFi is not supported on iOS", nil)
    }
    
    private func getCurrentSSID() -> String? {
        guard let interfaces = CNCopySupportedInterfaces() as? [String] else {
            return nil
        }
        
        for interface in interfaces {
            guard let interfaceInfo = CNCopyCurrentNetworkInfo(interface as CFString) as? [String: Any] else {
                continue
            }
            return interfaceInfo[kCNNetworkInfoKeySSID as String] as? String
        }
        return nil
    }
} 