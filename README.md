# Vault Voice

Private, on-device voice notes — nothing ever leaves your phone.

Vault Voice turns speech into text without ever sending your voice anywhere. Every transcription happens entirely on your device using Android's built-in offline speech engine — no internet connection, no cloud processing, no servers involved. Vault Voice doesn't even request the Internet permission, because it doesn't need one.

- Tap to record, tap to stop — you're in control of when a recording ends
- Add more to any saved note to keep building on a topic over time
- Edit any transcript by hand to fix mistakes, no need to re-record
- Copy, share, or delete notes in one tap
- No accounts, no logins, no ads, no analytics, no tracking of any kind

Your notes are stored only in the app's private storage on your phone. Nothing is uploaded, nothing is synced, nothing is collected. If you delete the app, your notes go with it — that's the whole point.

## Install

### Obtainium (recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) tracks this repo's GitHub Releases and keeps Vault Voice updated automatically, no app store needed.

1. Install Obtainium.
2. Add app → paste this repo's URL: `https://github.com/arrma2022/vault-voice`
3. Install.

### Manual APK

Grab the latest `.apk` from the [Releases](https://github.com/arrma2022/vault-voice/releases) page and sideload it.

### Google Play

A paid version is also planned for the Play Store as a way to support development directly, once it clears Google's testing requirements. This repo will always have a free, fully-featured build available.

## Building from source

```
./gradlew assembleRelease
```

Requires a `keystore.properties` (not tracked in this repo) pointing at your own signing key if you want a signed release build; debug builds work without one.

## License

GPLv3 — see [LICENSE](LICENSE).
