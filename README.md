# SingleChat - Satu Kelompok, Satu Obrolan

SingleChat is a minimalist, high-contrast Android group chat application built with **Kotlin** and **Jetpack Compose (Material 3)**. It implements a dedicated single-group conversation model where team members coordinate tasks, share files, and initiate voice calls under a unified group token.

## Key Features

- **Single-Group Authentication & Enrollment**:
  - Sign in or create a group with a unique group token (e.g., `SC-XXXXXX`).
  - Auto-generation of new group tokens with team leader (Ketua) designation.
  - Quick-fill demo presets (`SC-ALPHA`, `SC-PROJECT`) for instant testing.
- **Minimalist Real-Time Chat (Obrolan `[ / ]`)**:
  - Differentiated message bubbles for incoming and outgoing messages.
  - Text messaging with timestamps.
  - Media sharing (images with preview dialog and documents/PDF attachment cards).
  - Voice Call integration (`[☎]`) launching group conference rooms via Jitsi Meet (`https://meet.jit.si/SingleChat-<Token>`).
- **Group Member Directory (Anggota `[ o ]`)**:
  - Monochromatic avatar initials and role badges (`KETUA` vs `ANGGOTA`).
  - Online/offline presence indicators.
  - 1-tap group token copying to clipboard and system sharing.
  - Interactive member invite flow.
- **Account & System Settings (Pengaturan `[ * ]`)**:
  - Profile identity management (display name and avatar customization).
  - Password management dialog.
  - Monochromatic Theme Toggle: Instant switching between High-Contrast Light and Pitch-Black Dark modes.
  - Secure session logout.

## Architecture & Tech Stack

- **Framework**: Jetpack Compose & Material 3
- **Language**: Kotlin 2.1.0 with Kotlin Coroutines & Flow
- **Data Persistence**: `SharedPreferences` session & local storage with Kotlinx Serialization
- **Image Loading**: Coil 2.7.0
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin (AGP) 8.8.0
