---
name: release
description: Процедура пре-релиза bit Hub — проверка CI и тега, запуск release.yml с prerelease=true, скачивание и верификация APK (подпись + package info), опциональная установка на устройство. Публикацию в не-пре-релиз никогда не делает.
---

# Пре-релиз bit Hub

Эта процедура только готовит и проверяет pre-release. Перевод релиза в публичный (`gh release edit --prerelease=false`) запрещён — это делает только пользователь вручную.

## Шаги

1. **Проверить main**
   - `gh run list -R bit-Tecnologies/bit_hub --branch main --limit 1 --json conclusion` — CI на main должен быть `success`.
   - Прочитать `app/build.gradle.kts`, взять `appVersionName` и `appVersionCode`.
   - Тег релиза = `v<appVersionName>`. Он должен совпадать — `release.yml` сам это проверит и упадёт при несовпадении, но лучше свериться заранее.

2. **Запустить pre-release**
   ```powershell
   gh workflow run release.yml -R bit-Tecnologies/bit_hub --ref main -f tag=v<appVersionName> -f prerelease=true
   ```
   Затем `gh run watch <run-id> -R bit-Tecnologies/bit_hub --exit-status`, дождаться завершения. Если упал — показать лог упавшего шага, не повторять запуск вслепую.

3. **Проверить созданный релиз**
   ```powershell
   gh release view v<appVersionName> -R bit-Tecnologies/bit_hub --json isPrerelease,assets,tagName
   ```
   Ожидается `isPrerelease: true` и один ассет `bithub-<appVersionCode>-v<appVersionName>-release.apk`.

4. **Скачать и проверить APK** (во временную папку вне репозитория, например `$env:TEMP\bithub-release`):
   ```powershell
   gh release download v<appVersionName> -R bit-Tecnologies/bit_hub --pattern "*.apk" --clobber
   ```
   - Подпись: `apksigner verify --print-certs <apk>` (apksigner — в `%ANDROID_HOME%\build-tools\<версия>\apksigner.bat`). SHA-256 отпечаток сверить с тем, что в `CLAUDE.md` (переменная `EXPECTED_CERT_SHA256`).
   - Пакет: `aapt2 dump badging <apk>` — проверить `package`, `versionCode`, `versionName` и что в `uses-permission` есть ожидаемые разрешения (например `UPDATE_PACKAGES_WITHOUT_USER_ACTION`).

5. **Устройство (опционально)**
   - `adb devices` — если в списке есть устройство со статусом `device`:
     ```powershell
     adb install -r <путь к проверенному APK>
     adb shell dumpsys package dev.cybernattor.hub | Select-String "versionName|versionCode|installerPackageName"
     ```
   - Если устройств нет — явно сказать об этом, не выдумывать результат.

## Что никогда не делать в рамках этой процедуры
- Не запускать `release.yml` с `prerelease=false`.
- Не вызывать `gh release edit` для смены pre-release статуса.
- Не трогать секреты и переменные репозитория.
- Не удалять данные на устройстве, не чистить APK, не трогать интерфейс приложения.
