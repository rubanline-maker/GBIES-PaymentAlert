# GBIES Payment Alert - instructions for the AI agent (Antigravity)

The owner has **zero technical knowledge**. Do all technical work yourself and report back in plain,
short language. Only hand back steps that need a human (logins, approvals), with exact click-by-click steps.

## What this project is
A free Android app (Kotlin, no server) for shopkeepers. It reads payment notifications (PhonePe, Google Pay,
Paytm, BHIM, bank apps, bank SMS notifications, other UPI apps) and speaks the received amount aloud at
full volume, lists every payment per day, keeps the shop's payment QR, and exports a day to PDF.
It is sold to many shopkeepers and shared as an APK link (GitHub Releases). iPhone is NOT supported by this
app (Apple blocks it); iPhone customers use a separate Shortcuts guide.

## Folder map
- `app/` - the Android app (package `com.paymentvoice.alert`). Main files in `app/src/main/java/com/paymentvoice/alert/`:
  `MainActivity.kt` (all UI, built in code), `PaymentListener.kt` (notification listener), `SessionService.kt`
  (keeps voice alerts alive while the app is "open", even with screen locked), `Parser.kt`, `Speaker.kt`,
  `Store.kt`, `LampView.kt`, `LampBus.kt`, `Wake.kt`, `AppState.kt`.
- `.github/workflows/build.yml` - builds the APK in GitHub Actions and publishes it as release `latest`
  (file `PaymentVoiceAlert.apk`). Repo must be PUBLIC for the share link to work.
- `preview/payment-voice-alert-preview.html` - a visual mock-up of the approved screen design (reference only).
- No Gradle wrapper is included on purpose; the workflow uses Gradle 8.7. Versions: AGP 8.5.2, Kotlin 1.9.24,
  JDK 17, compileSdk/targetSdk 34, minSdk 24.

## Approved design - DO NOT CHANGE
- Logo (GBIES) at the top. Pure hard-black background; all text real metallic gold (gradient).
  A Black/Sandal theme switch sits at the right of the "Active - payments will be announced" line.
  Sandal mode = sandal-coloured background (never pure white) with deeper gold text.
- Left column: three small switches (Notification access, English voice, Run always (battery)) plus
  "Flash alert" with a small classic lamp that blinks 3 times with the voice. Right column: payment QR box
  (tap = full page, tap again = shrink; Add/Change from gallery; Delete).
- Eye icons hide/show today's total and the Previous days amounts. Today is fresh each day; previous days can
  be opened and deleted. "Save / Print PDF of this day" button.
- No Tamil words on screen except the spoken test sentence. Voice: Tamil by default, English via switch.

## Required behaviour
1. Every payment is always saved to the list.
2. Voice (+ lamp) only while the app is "open": from opening the app until it is swiped away / Stop tapped,
   including when the screen is locked. Closed app = silent, list only.
3. Media volume is forced to 100 percent before every announcement.
4. No back camera flash.

## Your tasks
1. Open this folder as an Android project and build: `gradle assembleDebug` (or Android Studio).
   Fix ONLY real compile or build errors with minimal edits. This code was written without being compiled.
2. Review these risky areas and fix real bugs without changing the design or behaviour:
   Android 13/14 foreground service + notification permission, `SessionService` start/stop,
   show-over-lock-screen + screen wake, gallery pick + QR save, PDF export + FileProvider share,
   TextToSpeech on Android 11+ (queries entry), Parser regexes.
3. If an emulator or device is available, run it and test: add QR, switches, theme switch, eye icons,
   Test voice, PDF export.
4. Keep the GitHub workflow working. Do not add paid services or servers.
5. Report: what you changed (one line each), what you could not test, and the exact human steps left.

## Human steps the owner must do himself (give him click-by-click steps)
- Create a GitHub account and a PUBLIC repository, upload this folder's contents (including `.github`).
- On a phone: download the APK from the repo's Releases page, allow install from unknown sources,
  then switch on Notification access and "Run always (battery)" inside the app.
