package com.wifimanager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.List;

public class WifiManagerModule extends ReactContextBaseJavaModule {
    private static final String TAG = "WifiManagerModule";
    private static final String MODULE_NAME = "WifiManager";
    
    private final ReactApplicationContext reactContext;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;
    private boolean isScanning = false;
    private boolean isReceiverRegistered = false;
    private Promise currentScanPromise = null;

    public WifiManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.wifiManager = (WifiManager) reactContext.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    @NonNull
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void scanWifiNetworks(Promise promise) {
        if (!checkPermissions()) {
            promise.reject("PERMISSION_DENIED", "Location permission is required for WiFi scanning");
            return;
        }

        if (isScanning) {
            promise.reject("ALREADY_SCANNING", "WiFi scan is already in progress");
            return;
        }

        try {
            isScanning = true;
            currentScanPromise = promise;
            
            // Unregister any existing receiver first
            unregisterScanReceiver();
            
            wifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        // Check if context and intent are valid
                        if (context == null || intent == null) {
                            Log.e(TAG, "Invalid context or intent in broadcast receiver");
                            handleScanFailure("Invalid broadcast data");
                            return;
                        }

                        // Check if the action matches
                        if (!WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                            Log.w(TAG, "Received unexpected broadcast action: " + intent.getAction());
                            return;
                        }

                        boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                        if (success) {
                            handleScanSuccess();
                        } else {
                            handleScanFailure("WiFi scan failed");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in broadcast receiver", e);
                        handleScanFailure("Broadcast receiver error: " + e.getMessage());
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            
            try {
                reactContext.registerReceiver(wifiScanReceiver, intentFilter);
                isReceiverRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to register broadcast receiver", e);
                isScanning = false;
                currentScanPromise = null;
                promise.reject("RECEIVER_ERROR", "Failed to register broadcast receiver: " + e.getMessage());
                return;
            }

            boolean scanStarted = wifiManager.startScan();
            if (!scanStarted) {
                Log.e(TAG, "Failed to start WiFi scan");
                unregisterScanReceiver();
                isScanning = false;
                currentScanPromise = null;
                promise.reject("SCAN_FAILED", "Failed to start WiFi scan");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting WiFi scan", e);
            unregisterScanReceiver();
            isScanning = false;
            currentScanPromise = null;
            promise.reject("SCAN_ERROR", e.getMessage());
        }
    }

    private void handleScanSuccess() {
        try {
            if (currentScanPromise == null) {
                Log.w(TAG, "Scan promise is null, ignoring scan results");
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            if (results == null) {
                Log.w(TAG, "Scan results are null");
                currentScanPromise.reject("SCAN_FAILED", "No scan results available");
            } else {
                WritableArray networks = convertScanResultsToArray(results);
                sendEvent("wifiScanResults", networks);
                currentScanPromise.resolve(networks);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling scan success", e);
            if (currentScanPromise != null) {
                currentScanPromise.reject("SCAN_ERROR", "Error processing scan results: " + e.getMessage());
            }
        } finally {
            cleanupScan();
        }
    }

    private void handleScanFailure(String error) {
        try {
            if (currentScanPromise != null) {
                currentScanPromise.reject("SCAN_FAILED", error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling scan failure", e);
        } finally {
            cleanupScan();
        }
    }

    private void cleanupScan() {
        isScanning = false;
        currentScanPromise = null;
        unregisterScanReceiver();
    }

    private void unregisterScanReceiver() {
        if (wifiScanReceiver != null && isReceiverRegistered) {
            try {
                reactContext.unregisterReceiver(wifiScanReceiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering scan receiver", e);
            }
        }
        wifiScanReceiver = null;
    }

    @ReactMethod
    public void connectToWifi(String ssid, String password, Promise promise) {
        if (!checkPermissions()) {
            promise.reject("PERMISSION_DENIED", "Location permission is required for WiFi connection");
            return;
        }

        try {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + ssid + "\"";
            wifiConfig.preSharedKey = "\"" + password + "\"";

            int networkId = wifiManager.addNetwork(wifiConfig);
            if (networkId == -1) {
                promise.reject("CONNECTION_FAILED", "Failed to add network configuration");
                return;
            }

            boolean connected = wifiManager.enableNetwork(networkId, true);
            if (connected) {
                promise.resolve(true);
                sendEvent("wifiConnected", ssid);
            } else {
                promise.reject("CONNECTION_FAILED", "Failed to connect to WiFi network");
            }
        } catch (Exception e) {
            promise.reject("CONNECTION_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void disconnectFromWifi(Promise promise) {
        try {
            boolean disconnected = wifiManager.disconnect();
            if (disconnected) {
                promise.resolve(true);
                sendEvent("wifiDisconnected", null);
            } else {
                promise.reject("DISCONNECTION_FAILED", "Failed to disconnect from WiFi");
            }
        } catch (Exception e) {
            promise.reject("DISCONNECTION_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getCurrentWifiInfo(Promise promise) {
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                WritableMap info = Arguments.createMap();
                info.putString("ssid", wifiInfo.getSSID().replace("\"", ""));
                info.putInt("signalStrength", wifiInfo.getRssi());
                info.putInt("networkId", wifiInfo.getNetworkId());
                promise.resolve(info);
            } else {
                promise.resolve(null);
            }
        } catch (Exception e) {
            promise.reject("INFO_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void isWifiEnabled(Promise promise) {
        try {
            boolean enabled = wifiManager.isWifiEnabled();
            promise.resolve(enabled);
        } catch (Exception e) {
            promise.reject("ENABLED_CHECK_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void setWifiEnabled(boolean enabled, Promise promise) {
        try {
            boolean success = wifiManager.setWifiEnabled(enabled);
            promise.resolve(success);
        } catch (Exception e) {
            promise.reject("ENABLE_ERROR", e.getMessage());
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasLocationPermission = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            
            // For Android 13+ (API 33+), also check for NEARBY_WIFI_DEVICES permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boolean hasNearbyWifiPermission = ActivityCompat.checkSelfPermission(reactContext,
                        Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
                // For backward compatibility, only require location permission if NEARBY_WIFI_DEVICES is not available
                return hasLocationPermission || hasNearbyWifiPermission;
            }
            
            return hasLocationPermission;
        }
        return true;
    }

    private String getMissingPermissions() {
        StringBuilder missingPermissions = new StringBuilder();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.append("ACCESS_FINE_LOCATION ");
            }
            
            // For Android 13+ (API 33+), also check for NEARBY_WIFI_DEVICES permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(reactContext,
                        Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.append("NEARBY_WIFI_DEVICES ");
                }
            }
        }
        
        return missingPermissions.toString().trim();
    }

    private WritableArray convertScanResultsToArray(List<ScanResult> results) {
        WritableArray networks = Arguments.createArray();
        
        for (ScanResult result : results) {
            WritableMap network = Arguments.createMap();
            network.putString("ssid", result.SSID);
            network.putString("bssid", result.BSSID);
            network.putInt("signalStrength", result.level);
            network.putString("capabilities", result.capabilities);
            network.putInt("frequency", result.frequency);
            network.putInt("channel", getChannelFromFrequency(result.frequency));
            networks.pushMap(network);
        }
        
        return networks;
    }

    private int getChannelFromFrequency(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            return (frequency - 2412) / 5 + 1;
        } else if (frequency >= 5170 && frequency <= 5825) {
            return (frequency - 5170) / 5 + 34;
        }
        return 0;
    }

    private void sendEvent(String eventName, Object data) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, data);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        try {
            // Clean up any ongoing scan
            if (isScanning) {
                Log.w(TAG, "Module destroyed while scan was in progress");
                cleanupScan();
            }
            
            // Unregister receiver if still registered
            unregisterScanReceiver();
        } catch (Exception e) {
            Log.e(TAG, "Error during module destruction", e);
        }
    }
} 