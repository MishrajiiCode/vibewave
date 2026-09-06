# VibeWave Setup Script
# Run this after installing Flutter SDK

Write-Host "+----------------------------------+" -ForegroundColor Cyan
Write-Host "¦   VibeWave Setup Assistant ??    ¦" -ForegroundColor Cyan
Write-Host "+----------------------------------+" -ForegroundColor Cyan
Write-Host ""

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Step 1: Check Flutter
Write-Host "Step 1: Checking Flutter..." -ForegroundColor Yellow
$flutterPaths = @("C:\flutter\bin\flutter.bat", "$env:USERPROFILE\flutter\bin\flutter.bat")
$flutterCmd = $null
foreach ($path in $flutterPaths) {
    if (Test-Path $path) { $flutterCmd = $path; break }
}

if ($flutterCmd) {
    Write-Host "  ? Flutter found at $flutterCmd" -ForegroundColor Green
} else {
    Write-Host "  ? Flutter not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Please download Flutter from: https://flutter.dev/docs/get-started/install/windows" -ForegroundColor White
    Write-Host "  Extract to C:\flutter" -ForegroundColor White
    Write-Host "  Add C:\flutter\bin to PATH" -ForegroundColor White
    Write-Host ""
    Read-Host "Press Enter after installing Flutter, then re-run this script"
    exit
}

# Step 2: Check Firebase config
Write-Host ""
Write-Host "Step 2: Checking Firebase config..." -ForegroundColor Yellow
$firebaseOptions = Join-Path $projectDir "lib\firebase_options.dart"
$content = Get-Content $firebaseOptions -Raw
if ($content -match "YOUR_ANDROID_API_KEY") {
    Write-Host "  ??  Firebase not configured yet!" -ForegroundColor Yellow
    Write-Host "  You need to:" -ForegroundColor White
    Write-Host "    1. Create a Firebase project at https://console.firebase.google.com/" -ForegroundColor White
    Write-Host "    2. Enable Firestore, Authentication (Anonymous + Email/Password), Cloud Messaging" -ForegroundColor White
    Write-Host "    3. Download google-services.json to android\app\" -ForegroundColor White
    Write-Host "    4. Run: dart pub global activate flutterfire_cli" -ForegroundColor White
    Write-Host "    5. Run: flutterfire configure --project=YOUR_PROJECT_ID" -ForegroundColor White
    Write-Host ""
    $continue = Read-Host "Continue without Firebase? (y/n)"
    if ($continue -ne "y") { exit }
} else {
    Write-Host "  ? Firebase configured" -ForegroundColor Green
}

# Step 3: Check fonts
Write-Host ""
Write-Host "Step 3: Checking fonts..." -ForegroundColor Yellow
$fonts = @("Poppins-Regular.ttf", "Poppins-Medium.ttf", "Poppins-SemiBold.ttf", "Poppins-Bold.ttf")
$allFontsPresent = $true
foreach ($font in $fonts) {
    $fontPath = Join-Path $projectDir "assets\fonts\$font"
    if (Test-Path $fontPath) {
        Write-Host "  ? $font" -ForegroundColor Green
    } else {
        Write-Host "  ? $font missing" -ForegroundColor Red
        $allFontsPresent = $false
    }
}

# Step 4: Flutter pub get
Write-Host ""
Write-Host "Step 4: Installing Flutter packages..." -ForegroundColor Yellow
Set-Location $projectDir
& $flutterCmd pub get
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ? Packages installed" -ForegroundColor Green
} else {
    Write-Host "  ? Package installation failed" -ForegroundColor Red
    exit
}

# Step 5: Check connected devices
Write-Host ""
Write-Host "Step 5: Checking connected devices..." -ForegroundColor Yellow
& $flutterCmd devices

Write-Host ""
Write-Host "+---------------------------------------+" -ForegroundColor Cyan
Write-Host "¦   Setup complete! Ready to run ??     ¦" -ForegroundColor Cyan
Write-Host "+---------------------------------------+" -ForegroundColor Cyan
Write-Host ""
Write-Host "To run the app:" -ForegroundColor White
Write-Host "  flutter run" -ForegroundColor Green
Write-Host ""
Write-Host "To build release APK:" -ForegroundColor White
Write-Host "  flutter build apk --release" -ForegroundColor Green
Write-Host ""
Write-Host "Admin Panel login:" -ForegroundColor White
Write-Host "  Set up admin user in Firebase Console -> Authentication -> Users" -ForegroundColor Yellow
