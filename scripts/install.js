#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('🔧 Setting up React Native WiFi Manager...\n');

// Check if we're in a React Native project
const packageJsonPath = path.join(process.cwd(), 'package.json');
if (!fs.existsSync(packageJsonPath)) {
  console.error('❌ Error: package.json not found. Make sure you\'re in a React Native project root.');
  process.exit(1);
}

const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
if (!packageJson.dependencies?.['react-native'] && !packageJson.devDependencies?.['react-native']) {
  console.error('❌ Error: React Native not found in dependencies. Make sure you\'re in a React Native project.');
  process.exit(1);
}

console.log('✅ React Native project detected\n');

// Check Android manifest
const androidManifestPath = path.join(process.cwd(), 'android/app/src/main/AndroidManifest.xml');
if (fs.existsSync(androidManifestPath)) {
  const manifest = fs.readFileSync(androidManifestPath, 'utf8');
  const requiredPermissions = [
    'ACCESS_WIFI_STATE',
    'CHANGE_WIFI_STATE',
    'ACCESS_FINE_LOCATION',
    'ACCESS_COARSE_LOCATION',
    'ACCESS_NETWORK_STATE',
    'CHANGE_NETWORK_STATE'
  ];

  const missingPermissions = requiredPermissions.filter(permission => 
    !manifest.includes(`android.permission.${permission}`)
  );

  if (missingPermissions.length > 0) {
    console.log('⚠️  Missing Android permissions detected:');
    missingPermissions.forEach(permission => {
      console.log(`   - android.permission.${permission}`);
    });
    console.log('\nPlease add these permissions to your AndroidManifest.xml file.\n');
  } else {
    console.log('✅ Android permissions are properly configured\n');
  }
} else {
  console.log('⚠️  Android manifest not found. Make sure to add required permissions manually.\n');
}

// Check iOS Podfile
const iosPodfilePath = path.join(process.cwd(), 'ios/Podfile');
if (fs.existsSync(iosPodfilePath)) {
  console.log('✅ iOS Podfile found');
  console.log('   Run "cd ios && pod install" to install iOS dependencies\n');
} else {
  console.log('⚠️  iOS Podfile not found. iOS setup may be required.\n');
}

console.log('🎉 Setup complete!');
console.log('\nNext steps:');
console.log('1. For Android: Add required permissions to AndroidManifest.xml');
console.log('2. For iOS: Run "cd ios && pod install"');
console.log('3. Rebuild your app');
console.log('\nFor detailed instructions, see: https://github.com/Management-AND-Computer-Consultants/react-native-wifi-manager'); 