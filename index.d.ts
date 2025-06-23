declare module 'management-and-computer-consultants-react-native-wifi-manager' {
  export interface WifiNetwork {
    ssid: string;
    bssid: string;
    signalStrength: number;
    capabilities: string;
    frequency: number;
    channel: number;
  }

  export interface WifiInfo {
    ssid: string;
    signalStrength: number;
    networkId: number;
  }

  export interface WifiManagerInterface {
    /**
     * Scan for available WiFi networks
     * @returns Promise<WifiNetwork[]> Array of available networks
     */
    scanWifiNetworks(): Promise<WifiNetwork[]>;

    /**
     * Connect to a WiFi network
     * @param ssid Network SSID
     * @param password Network password
     * @returns Promise<boolean> True if connection successful
     */
    connectToWifi(ssid: string, password: string): Promise<boolean>;

    /**
     * Disconnect from current WiFi network
     * @returns Promise<boolean> True if disconnection successful
     */
    disconnectFromWifi(): Promise<boolean>;

    /**
     * Get information about currently connected WiFi network
     * @returns Promise<WifiInfo | null> Current WiFi info or null if not connected
     */
    getCurrentWifiInfo(): Promise<WifiInfo | null>;

    /**
     * Check if WiFi is enabled
     * @returns Promise<boolean> True if WiFi is enabled
     */
    isWifiEnabled(): Promise<boolean>;

    /**
     * Enable or disable WiFi (Android only)
     * @param enabled True to enable, false to disable
     * @returns Promise<boolean> True if operation successful
     */
    setWifiEnabled(enabled: boolean): Promise<boolean>;
  }

  const WifiManager: WifiManagerInterface;
  export default WifiManager;
}

declare module 'management-and-computer-consultants-react-native-wifi-manager/src/WifiManager' {
  import { ComponentType } from 'react';

  export interface WifiManagerProps {
    /** Controls modal visibility */
    visible: boolean;
    /** Called when modal is closed */
    onClose: () => void;
    /** Called when successfully connected to a network */
    onConnect?: (ssid: string) => void;
  }

  const WifiManagerComponent: ComponentType<WifiManagerProps>;
  export default WifiManagerComponent;
}

declare module 'management-and-computer-consultants-react-native-wifi-manager/src/useWifiManager' {
  import { WifiNetwork, WifiInfo } from 'management-and-computer-consultants-react-native-wifi-manager';

  export interface UseWifiManagerReturn {
    /** Array of scanned networks */
    networks: WifiNetwork[];
    /** Current WiFi connection info */
    currentWifi: WifiInfo | null;
    /** Loading state */
    loading: boolean;
    /** Error message */
    error: string | null;
    /** Function to scan networks */
    scanNetworks: () => Promise<WifiNetwork[]>;
    /** Function to connect to WiFi */
    connectToWifi: (ssid: string, password: string) => Promise<boolean>;
    /** Function to disconnect from WiFi */
    disconnectFromWifi: () => Promise<boolean>;
    /** Function to get current WiFi info */
    getCurrentWifiInfo: () => Promise<WifiInfo | null>;
    /** Function to check WiFi status */
    isWifiEnabled: () => Promise<boolean>;
    /** Function to enable/disable WiFi */
    setWifiEnabled: (enabled: boolean) => Promise<boolean>;
    /** Function to clear error state */
    clearError: () => void;
  }

  export function useWifiManager(): UseWifiManagerReturn;
} 