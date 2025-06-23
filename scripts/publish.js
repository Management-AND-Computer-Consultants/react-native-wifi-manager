#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('🚀 Preparing to publish React Native WiFi Manager...\n');

// Check if we're logged in to npm
try {
  execSync('npm whoami', { stdio: 'pipe' });
  console.log('✅ Logged in to npm');
} catch (error) {
  console.error('❌ Not logged in to npm. Please run "npm login" first.');
  process.exit(1);
}

// Check if package.json exists
const packageJsonPath = path.join(process.cwd(), 'package.json');
if (!fs.existsSync(packageJsonPath)) {
  console.error('❌ package.json not found');
  process.exit(1);
}

// Read package.json
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

console.log(`📦 Package: ${packageJson.name}`);
console.log(`📋 Version: ${packageJson.version}`);
console.log(`📝 Description: ${packageJson.description}\n`);

// Check if files exist
const requiredFiles = [
  'index.js',
  'index.d.ts',
  'src/WifiManager.tsx',
  'src/useWifiManager.ts',
  'android/src/main/java/com/wifimanager/WifiManagerModule.java',
  'android/src/main/java/com/wifimanager/WifiManagerPackage.java',
  'android/build.gradle',
  'ios/WifiManagerModule.swift',
  'ios/WifiManagerModule.m',
  'README.md',
  'CHANGELOG.md',
  'LICENSE'
];

console.log('🔍 Checking required files...');
const missingFiles = requiredFiles.filter(file => !fs.existsSync(file));

if (missingFiles.length > 0) {
  console.error('❌ Missing required files:');
  missingFiles.forEach(file => console.error(`   - ${file}`));
  process.exit(1);
} else {
  console.log('✅ All required files found\n');
}

// Check if .npmignore exists
if (!fs.existsSync('.npmignore')) {
  console.warn('⚠️  .npmignore not found. Consider creating one to exclude unnecessary files.');
}

// Ask for confirmation
console.log('📤 Ready to publish!');
console.log('This will publish the package to npm registry.');
console.log('Make sure you have:');
console.log('1. ✅ Updated version in package.json');
console.log('2. ✅ Updated CHANGELOG.md');
console.log('3. ✅ Tested the package locally');
console.log('4. ✅ Committed all changes to git\n');

const readline = require('readline');
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

rl.question('Do you want to continue? (y/N): ', (answer) => {
  rl.close();
  
  if (answer.toLowerCase() === 'y' || answer.toLowerCase() === 'yes') {
    console.log('\n🚀 Publishing...\n');
    try {
      execSync('npm publish', { stdio: 'inherit' });
      console.log('\n🎉 Successfully published!');
      console.log(`📦 Package: ${packageJson.name}@${packageJson.version}`);
      console.log(`🔗 npm: https://www.npmjs.com/package/${packageJson.name}`);
    } catch (error) {
      // Check if the error is about version already published
      if (error.message.includes('previously published versions') || error.message.includes('403 Forbidden')) {
        console.log('\n⚠️  Warning: npm returned an error, but this might be a false positive.');
        console.log('📦 The package may have been published successfully.');
        console.log(`🔍 Please check: https://www.npmjs.com/package/${packageJson.name}`);
        console.log('💡 If the package is not published, try again in a few minutes.');
      } else {
        console.error('\n❌ Failed to publish:', error.message);
        process.exit(1);
      }
    }
  } else {
    console.log('\n❌ Publishing cancelled');
    process.exit(0);
  }
}); 