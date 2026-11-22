# bit Hub

## Пакет и подпись
- `applicationId` = `dev.cybernattor.hub`, `namespace` = `com.bit.bithub`. Не менять ни то, ни другое.
- Старый пакет `com.bit.bithub` (версии до 0.0.2.8 включительно) обновить до нового пакета нельзя — это разные приложения с точки зрения Android. Только ручная установка новой версии рядом, старую пользователь удаляет сам.
- Ключ подписи: PKCS12, alias `bithub`. Лежит только в секретах репозитория `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` (опционально `ANDROID_KEY_PASSWORD`). Публичный SHA-256 отпечаток сертификата:
  `A8:5B:E7:0C:4A:25:31:0A:74:D7:61:D0:B0:FC:FD:FC:63:8E:BD:EC:E8:3D:7C:EE:BD:F4:74:68:84:4D:A3:2A`
  Хранится в переменной репозитория `EXPECTED_CERT_SHA256`, `release.yml` проверяет его после подписи. Сам ключ и пароли никогда не читать и не выводить в чат.

## Релизы
- Тег релиза всегда `v<appVersionName>` из `app/build.gradle.kts`, и он должен совпадать — `release.yml` падает на несовпадении.
- Имя APK: `bithub-<versionCode>-v<versionName>-release.apk`. Формат жёстко зашит в регулярку апдейтера (`UpdateRepository.kt`) — не менять.
- Любой релиз сначала выходит как pre-release (`prerelease=true` при запуске `release.yml`). Перевод в публичный релиз делает только пользователь вручную, Claude это не запускает.

## Апдейтер
- Ходит на `bit-Tecnologies/bit_hub`: `/releases/latest` (без пре-релизов) или полный список `/releases` при включённой настройке "показывать пре-релизы".
- Установка через `PackageInstaller` с `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` на API 31+ (разрешение `UPDATE_PACKAGES_WITHOUT_USER_ACTION`), плюс обработка `STATUS_PENDING_USER_ACTION` как резервный путь, если тихая установка недоступна.

## Сайт и каталог
- Сайт (Astro, автодеплой на Vercel при push в main) содержит `public/.well-known/assetlinks.json` для верификации App Links. Путь к репозиторию сайта на диске — см. `CLAUDE.local.md`.
- Каталог приложений в Supabase (`apps`/`app_releases`) — для других приложений бренда bit Tecnologies, не для самого bit Hub. bit Hub себя в каталоге не публикует.

## Прочее
- Бренд bit Tecnologies, название приложения, организация — пока не переименовывать.
