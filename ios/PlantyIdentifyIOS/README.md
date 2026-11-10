# Planty Identify iOS

SwiftUI migration scaffold for the Android Planty Identify flow.

## Current Flow

- Pick or capture a plant photo.
- Choose `Nhận diện` or `Chẩn đoán`.
- Upload image to the existing Planty backend:
  - `POST /api/v1/plant/scan`
  - `POST /api/v1/plant/disease`
- Send `language=vi`.
- Keep scientific names unchanged.
- Display plant names, diagnosis labels, descriptions, care text, and video labels in Vietnamese.
- Generate care video links dynamically from the scanned plant name and scientific name.

## TestFlight Next Steps

Open `PlantyIdentifyIOS.xcodeproj` in full Xcode, select your Apple Developer Team, then use:

`Product > Archive > Distribute App > App Store Connect > TestFlight`

This machine currently has Command Line Tools selected, not full Xcode, so terminal archive is not available until Xcode is installed/selected.
