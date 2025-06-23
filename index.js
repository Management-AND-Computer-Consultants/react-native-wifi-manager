import { NativeModules, Platform } from 'react-native';

const { WifiManager } = NativeModules;

if (!WifiManager) {
  throw new Error('WifiManager native module is not available. Make sure to link the library properly.');
}

export default WifiManager; 