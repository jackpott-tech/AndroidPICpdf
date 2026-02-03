# Foto-Seiten PDF (Android)

Offline-Android-App zum Zusammenstellen von Fotoseiten (4–6 Bilder pro A4-Seite) und Export als druckbares PDF. Die App arbeitet **lokal** ohne Cloud/Tracking.

## Features (v1)
- Mehrfachauswahl von Bildern über Android Photo Picker (scoped storage, keine unnötigen Berechtigungen).
- Sortierung nach Aufnahmedatum:
  1) EXIF `DateTimeOriginal`
  2) MediaStore `DATE_TAKEN`
  3) `DATE_MODIFIED`
- Projektmodell mit Seiten (4/5/6 Bilder pro Seite), automatische Raster-Layouts:
  - 4 → 2x2
  - 6 → 2x3
  - 5 → 2 oben, 3 unten (3 Spalten, 2 Reihen)
- Seitenüberschrift pro Seite, optionale Bildunterschriften pro Bild.
- Rahmen-Optionen (an/aus, Dünn/Mittel/Dick).
- Drag & Drop-Reihenfolge innerhalb einer Seite.
- PDF-Export in A4 (300 DPI) mit Text & Bildern (center-crop).
- Teilen via Android Sharesheet (E-Mail etc.) und „Öffnen mit…“ über FileProvider.

## Build & Run
1. Öffne das Projekt in Android Studio.
2. Sync Gradle.
3. Starte die App auf einem Gerät/Emulator (minSdk 26).

## Nutzung
1. **Projektliste:** Neues Projekt erstellen, öffnen oder löschen.
2. **Editor:** Bilder hinzufügen, Sortierung (Alt→Neu oder Neu→Alt), 4/5/6 pro Seite wählen, Rahmen konfigurieren.
3. **Bearbeiten:** Seitenüberschrift und Bildunterschriften editieren; Drag & Drop in der Reihenfolge.
4. **Export:** PDF erstellen und direkt teilen oder öffnen.

## Exportqualität
- A4 (Hochformat), gerendert mit 300 DPI (2480x3508 px pro Seite).
- Bilder werden auf Zielgröße heruntergesampelt, um Speicher zu sparen.
- Text wird als Vektor gerendert (scharf im Druck).

## Technischer Überblick
- Kotlin, Jetpack Compose + Material 3.
- MVVM + Repository, Coroutines/Flow.
- Persistenz: Room (lokale Projekt-Metadaten).
- Bilder: Coil (URI-basiert, downsampling durch Coil/BitmapFactory beim Export).
- PDF-Export: `android.graphics.pdf.PdfDocument` (stabil, offline, ohne externe Cloud-Libs).

## Nice-to-have (v1.1+ Ideen)
- Seiten-Reihenfolge ändern (Drag & Drop zwischen Seiten).
- Bild ersetzen direkt im Editor.
- Zusätzliche Templates (Rahmenfarben, Layouts, Querformat).
- Crop/Rotation pro Bild.

## Datenschutz
Alle Daten bleiben lokal auf dem Gerät. Es gibt keine Telemetrie, keine Online-Dienste, keine Analyse.
