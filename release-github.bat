@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"
set "APP_NAME=CreateWorkOrderProms"
set "DEFAULT_REMOTE_URL=https://github.com/kantapon-sam/CreateWorkOrderProms.git"
set "DEFAULT_REPO=kantapon-sam/CreateWorkOrderProms"
set "COMMAND_MODE="
if not "%~1"=="" set "COMMAND_MODE=1"

if /I "%~1"=="version" goto create_version
if /I "%~1"=="upload" goto upload_git
if /I "%~1"=="open" goto open_github
if /I "%~1"=="build" goto build_jar
if /I "%~1"=="package" goto package_zip

:menu
call :load_version
echo.
echo %APP_NAME% GitHub helper
echo Current version: !APP_VERSION!
echo.
echo 1. Create/update version
echo 2. Upload to GitHub
echo 3. Open GitHub release edit
echo 4. Build/test compile
echo 5. Create user download zip
echo 0. Exit
echo.
set /p "CHOICE=Choose: "
if "%CHOICE%"=="1" goto create_version
if "%CHOICE%"=="2" goto upload_git
if "%CHOICE%"=="3" goto open_github
if "%CHOICE%"=="4" goto build_jar
if "%CHOICE%"=="5" goto package_zip
if "%CHOICE%"=="0" exit /b 0
goto menu

:load_version
set "APP_VERSION=1.0.0"
if exist VERSION (
    set /p APP_VERSION=<VERSION
)
exit /b 0

:create_version
call :load_version
echo.
echo Current version: !APP_VERSION!
set /p "NEW_VERSION=New version, example 1.0.1: "
if "%NEW_VERSION%"=="" (
    echo Version was not changed.
    goto menu
)
> VERSION echo %NEW_VERSION%
powershell -NoProfile -ExecutionPolicy Bypass -Command "$path='src\com\java\myapp\AppVersion.java'; $version='%NEW_VERSION%'; $text=[System.IO.File]::ReadAllText($path); $replacement='    public static final String VERSION = ' + [char]34 + $version + [char]34 + ';'; $text=[regex]::Replace($text, '(?m)^\s*public static final String VERSION = .*;', $replacement); $utf8 = New-Object System.Text.UTF8Encoding($false); [System.IO.File]::WriteAllText($path, $text, $utf8)"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$path='README.md'; if (Test-Path $path) { $text=[System.IO.File]::ReadAllText($path); $text=[regex]::Replace($text, 'Current version: [^\r\n]+', 'Current version: %NEW_VERSION%'); $utf8 = New-Object System.Text.UTF8Encoding($false); [System.IO.File]::WriteAllText($path, $text, $utf8) }"
echo Version updated to %NEW_VERSION%.
call :build_jar
goto menu

:build_jar
echo.
where ant >nul 2>nul
if "%ERRORLEVEL%"=="0" (
    echo Building with Ant...
    ant clean jar
    if errorlevel 1 (
        echo Ant build failed.
        goto fail_or_menu
    )
    call :create_runnable_jar
    if errorlevel 1 (
        echo Runnable jar creation failed.
        goto fail_or_menu
    )
    call :create_updater_jar
    if errorlevel 1 (
        echo AutoUpdater jar creation failed.
        goto fail_or_menu
    )
    echo Build complete: dist\CreateWorkOrderProms.jar
    goto done_or_menu
)

echo Ant was not found in PATH. Running javac compile check instead...
if not exist build\classes mkdir build\classes
set "JAVAC_CP="
for %%j in (lib\*.jar) do (
    echo %%~nxj | findstr /I /C:"-sources.jar" >nul
    if errorlevel 1 (
        if defined JAVAC_CP (
            set "JAVAC_CP=!JAVAC_CP!;%%j"
        ) else (
            set "JAVAC_CP=%%j"
        )
    )
)
javac -encoding UTF-8 -source 1.8 -target 1.8 -cp "!JAVAC_CP!" -d build\classes src\com\java\myapp\*.java
if errorlevel 1 (
    echo Compile failed.
    goto fail_or_menu
)
call :create_runnable_jar
if errorlevel 1 (
    echo Runnable jar creation failed.
    goto fail_or_menu
)
call :create_updater_jar
if errorlevel 1 (
    echo AutoUpdater jar creation failed.
    goto fail_or_menu
)
echo Build complete: dist\CreateWorkOrderProms.jar
goto done_or_menu

:compile_for_package
where ant >nul 2>nul
if "%ERRORLEVEL%"=="0" (
    ant clean jar
    if errorlevel 1 exit /b %ERRORLEVEL%
    call :create_runnable_jar
    if errorlevel 1 exit /b %ERRORLEVEL%
    call :create_updater_jar
    exit /b %ERRORLEVEL%
)
if not exist build\classes mkdir build\classes
set "JAVAC_CP="
for %%j in (lib\*.jar) do (
    echo %%~nxj | findstr /I /C:"-sources.jar" >nul
    if errorlevel 1 (
        if defined JAVAC_CP (
            set "JAVAC_CP=!JAVAC_CP!;%%j"
        ) else (
            set "JAVAC_CP=%%j"
        )
    )
)
javac -encoding UTF-8 -source 1.8 -target 1.8 -cp "!JAVAC_CP!" -d build\classes src\com\java\myapp\*.java
if errorlevel 1 exit /b %ERRORLEVEL%
call :create_runnable_jar
if errorlevel 1 exit /b %ERRORLEVEL%
call :create_updater_jar
exit /b %ERRORLEVEL%

:create_runnable_jar
where jar >nul 2>nul
if errorlevel 1 (
    echo jar.exe was not found in PATH.
    exit /b 1
)
if not exist dist mkdir dist
powershell -NoProfile -ExecutionPolicy Bypass -Command "$libs=Get-ChildItem 'lib\*.jar' | Where-Object { $_.Name -notlike '*-sources.jar' } | ForEach-Object { 'lib/' + $_.Name }; $out=New-Object System.Collections.Generic.List[string]; $out.Add('Manifest-Version: 1.0'); $out.Add('Main-Class: com.java.myapp.CreateWorkOrderProms'); $line='Class-Path: ' + ($libs -join ' '); while ($line.Length -gt 70) { $cut=$line.LastIndexOf(' ', [Math]::Min(70, $line.Length - 1)); if ($cut -lt 12) { $cut=70 }; $out.Add($line.Substring(0,$cut)); $line=' ' + $line.Substring($cut) }; $out.Add($line); $out.Add(''); $utf8=New-Object System.Text.UTF8Encoding($false); [System.IO.File]::WriteAllLines('build\runnable-manifest.mf', $out, $utf8)"
if errorlevel 1 exit /b 1
jar cfm dist\CreateWorkOrderProms.jar build\runnable-manifest.mf -C build\classes .
exit /b %ERRORLEVEL%

:create_updater_jar
where jar >nul 2>nul
if errorlevel 1 (
    echo jar.exe was not found in PATH.
    exit /b 1
)
if not exist dist mkdir dist
powershell -NoProfile -ExecutionPolicy Bypass -Command "$out=@('Manifest-Version: 1.0','Main-Class: com.java.myapp.AutoUpdater',''); $utf8=New-Object System.Text.UTF8Encoding($false); [System.IO.File]::WriteAllLines('build\updater-manifest.mf', $out, $utf8)"
if errorlevel 1 exit /b 1
jar cfm dist\AutoUpdater.jar build\updater-manifest.mf -C build\classes com\java\myapp
exit /b %ERRORLEVEL%

:package_zip
call :create_package_files
if errorlevel 1 goto fail_or_menu
call :open_release_edit_no_menu
goto done_or_menu

:create_package_files
call :load_version
echo.
echo Creating user download package for v!APP_VERSION!...
call :compile_for_package
if errorlevel 1 (
    echo Build/compile failed. Package was not created.
    exit /b 1
)
if not exist release mkdir release
if exist build\release-package rmdir /s /q build\release-package
mkdir build\release-package
mkdir build\release-package\lib
for %%j in (lib\*.jar) do (
    echo %%~nxj | findstr /I /C:"-sources.jar" >nul
    if errorlevel 1 copy /Y "%%j" build\release-package\lib\ >nul
)
copy /Y dist\CreateWorkOrderProms.jar build\release-package\ >nul
copy /Y dist\AutoUpdater.jar build\release-package\lib\ >nul
copy /Y VERSION build\release-package\ >nul
copy /Y README.md build\release-package\ >nul
type nul > build\release-package\ListWorkOrder.csv
powershell -NoProfile -ExecutionPolicy Bypass -Command "$zip='release\CreateWorkOrderProms-v%APP_VERSION%.zip'; Get-ChildItem 'release\CreateWorkOrderProms*.zip' -ErrorAction SilentlyContinue | Remove-Item -Force; Compress-Archive -Path 'build\release-package\*' -DestinationPath $zip -Force"
if errorlevel 1 (
    echo Package zip failed.
    exit /b 1
)
echo.
echo Package created:
echo release\CreateWorkOrderProms-v!APP_VERSION!.zip
exit /b 0

:done_or_menu
if defined COMMAND_MODE exit /b 0
pause
goto menu

:fail_or_menu
if defined COMMAND_MODE exit /b 1
pause
goto menu

:ensure_git_repo
where git >nul 2>nul
if errorlevel 1 (
    echo Git was not found in PATH. Please install Git for Windows first.
    if not defined COMMAND_MODE pause
    exit /b 1
)
if not exist ".git" (
    echo Initializing Git repository in %CD%
    git init .
    if errorlevel 1 exit /b 1
)
exit /b 0

:upload_git
call :load_version
call :ensure_git_repo
if errorlevel 1 goto fail_or_menu
echo.
echo Preparing Git commit for v!APP_VERSION!...
git add .
if errorlevel 1 (
    echo Git add failed.
    goto fail_or_menu
)
git diff --cached --quiet
if errorlevel 1 (
    git commit -m "Release v!APP_VERSION!"
    if errorlevel 1 (
        echo Git commit failed.
        goto fail_or_menu
    )
) else (
    echo No staged changes to commit.
)
git tag -l "v!APP_VERSION!" | findstr /x "v!APP_VERSION!" >nul
if errorlevel 1 (
    git tag -a "v!APP_VERSION!" -m "Release v!APP_VERSION!"
    if errorlevel 1 (
        echo Git tag failed.
        goto fail_or_menu
    )
) else (
    echo Tag v!APP_VERSION! already exists.
)
git remote get-url origin >nul 2>nul
if errorlevel 1 (
    echo.
    echo No Git remote found.
    echo Press Enter to use:
    echo !DEFAULT_REMOTE_URL!
    set /p "REMOTE_URL=Remote URL: "
    if "!REMOTE_URL!"=="" (
        set "REMOTE_URL=!DEFAULT_REMOTE_URL!"
    )
    git remote add origin "!REMOTE_URL!"
    if errorlevel 1 (
        echo Git remote add failed.
        goto fail_or_menu
    )
)
git branch -M main
if errorlevel 1 (
    echo Git branch rename failed.
    goto fail_or_menu
)
git push -u origin main
if errorlevel 1 (
    echo Push failed.
    goto fail_or_menu
)
git push origin "v!APP_VERSION!"
if errorlevel 1 (
    echo Tag push failed.
    goto fail_or_menu
)
call :create_package_files
if errorlevel 1 goto fail_or_menu
call :upload_release_asset
if errorlevel 1 goto fail_or_menu
call :open_release_edit_no_menu
goto done_or_menu

:open_github
call :load_version
call :open_release_edit_no_menu
goto done_or_menu

:upload_release_asset
echo.
echo Uploading release asset CreateWorkOrderProms-v!APP_VERSION!.zip to v!APP_VERSION!...
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\upload-release-asset.ps1" -Repo "%DEFAULT_REPO%" -Tag "v!APP_VERSION!" -AssetPath "release\CreateWorkOrderProms-v!APP_VERSION!.zip" -AssetName "CreateWorkOrderProms-v!APP_VERSION!.zip"
if errorlevel 1 (
    echo Release asset upload failed.
    echo Please upload release\CreateWorkOrderProms-v!APP_VERSION!.zip to:
    echo https://github.com/kantapon-sam/CreateWorkOrderProms/releases/edit/v!APP_VERSION!
    exit /b 1
)
exit /b 0

:open_release_edit_no_menu
start "" "https://github.com/kantapon-sam/CreateWorkOrderProms/releases/edit/v!APP_VERSION!"
exit /b 0

:open_github_no_menu
set "REMOTE_URL="
for /f "delims=" %%u in ('git remote get-url origin 2^>nul') do set "REMOTE_URL=%%u"
if "%REMOTE_URL%"=="" (
    echo No Git remote found. Opening default GitHub repository.
    set "REMOTE_URL=%DEFAULT_REMOTE_URL%"
    start "" "https://github.com/kantapon-sam/CreateWorkOrderProms"
    exit /b 0
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "$u=$env:REMOTE_URL; if ($u -match '^git@github\.com:(.+?)(\.git)?$') { $url='https://github.com/' + $Matches[1] } elseif ($u -match '^https://github\.com/(.+?)(\.git)?$') { $url='https://github.com/' + $Matches[1] } else { $url=$u }; Start-Process $url"
exit /b 0
