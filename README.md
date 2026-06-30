# CreateWorkOrderProms

Current version: 1.0.2

Java/NetBeans automation for creating PROMs work orders.

GitHub repository: https://github.com/kantapon-sam/CreateWorkOrderProms.git

User download page: https://github.com/kantapon-sam/CreateWorkOrderProms/releases/latest

## Local Files

- Saved login is stored in `User/login.properties`. This folder is ignored by Git.
- Put work orders in `ListWorkOrder.csv` in this format: `SITE_ID,PROJECT_CODE,DAY,MONTH,YEAR`.
- `chromedriver.exe`, build output, and run logs are ignored by Git.

## Auto Update

The app checks `VERSION` on the GitHub `main` branch at startup. If a newer version exists, it asks before running `lib/AutoUpdater.jar`. The updater shows progress while downloading/installing, then opens the updated app after the user clicks OK.

Normal users should download the versioned zip file such as `CreateWorkOrderProms-v1.0.0.zip` from GitHub Releases, extract it, fill `ListWorkOrder.csv`, and open `CreateWorkOrderProms.jar`.

## Build Or Test Compile

Run `release-github.bat` and choose:

- `1` to set a new app version.
- `2` to commit, push, package, and upload `CreateWorkOrderProms-v<version>.zip` to the GitHub Release.
- `3` to open the GitHub Release edit page for the current version.
- `4` to build with Ant if available, or compile with `javac` as a fallback.
- `5` to create one user download file: `release/CreateWorkOrderProms-v<version>.zip`.

Dry run mode:

```bat
java -Dcreateworkorderproms.dryRun=true -jar dist\CreateWorkOrderProms.jar
```
