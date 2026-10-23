
# TinyPhish

**An ultra-lite phishing URL detector (< 1 MB model) that runs fully offline on entry-level Android smartphones.**

TinyPhish checks whether a web link looks like phishing **entirely on the phone**. It needs no internet connection, cloud service or machine-learning library. The detection model is a single decision tree stored as a **33 KB JSON file**, which is about 3% of the 1 MB budget set for the project.

> B.Sc. (Engg.) project, Department of Information and Communication Technology, Islamic University, Bangladesh
> Author: **Asif Mahamud Ruhan** · Supervisor: **Professor Dr. Md Alamgir Hossain**

---

## Highlights

| | |
|---|---|
| **Model size** | 32.98 KB JSON (645 nodes, depth 10) |
| **Works offline** | Yes, verified in Airplane mode |
| **Permissions** | None (the app cannot send data anywhere) |
| **Accuracy** | 93.82% on 26,234 held-out URLs |
| **Recall (phishing caught)** | 98.76% |
| **Min. Android version** | 8.0 (API 26) |
| **Release APK** | 4.84 MB |

---

## How it works

```
User enters URL ─► Validate input ─► Extract 8 features ─► Walk decision tree ─► Show result
                   (http/https,      (Kotlin, same as       (≤ 10 comparisons)   (red / green
                    valid host)       Python training)                             + clues)
```

1. The model was trained in **Python (scikit-learn)** and exported as five plain arrays in JSON.
2. The app reads the JSON once at start-up.
3. For each URL it computes eight features and walks the tree in a few dozen lines of **Kotlin**.
4. A URL is flagged as phishing when the leaf's phishing probability is **≥ 0.30**. This threshold was chosen on a validation split to favour catching phishing.

### The eight URL features

| # | Feature | Description |
|---|---|---|
| 0 | URL length | Number of characters |
| 1 | Dot count | Number of `.` |
| 2 | Hyphen count | Number of `-` |
| 3 | Uses HTTPS | 1 if the URL starts with `https://` |
| 4 | Has @ symbol | 1 if `@` appears |
| 5 | Digit count | Number of digits |
| 6 | Slash count | Number of `/` |
| 7 | Host is an IP | 1 if the host is a valid IPv4 or IPv6 address |

---

## Repository contents

| File | Purpose |
|---|---|
| `MainActivity.kt` | User interface: loads the model, validates input and shows the result card |
| `PhishingDetector.kt` | Feature extraction, decision-tree walk and plain-language clues |
| `activity_main.xml` | Single-screen layout (input box, Check URL button, result card) |
| `build.gradle.kts` | App-level Gradle configuration (minSdk 26, target/compile SDK 37) |
| `phishing_tree_final_cw3.json` | **Final model used by the app** (trained on PhishBD_2026) |
| `phishing_tree.json` | Earlier model from development, kept for reference (not loaded by the app) |
| `app-release-1.apk` | Signed release build, ready to install |

---

## Install the app (APK)

1. Download `app-release-1.apk` to an Android phone running **Android 8.0 or newer**.
2. Open it from the Files app. If Android asks, allow installing unknown apps for the Files app.
3. Tap **Install**, then **Open**. No permissions are requested.

## Use the app

1. Copy a suspicious link (from SMS, WhatsApp, email…) **without opening it**.
2. Paste it into **Check a URL** and tap **Check URL** (or press Go on the keyboard).
3. Read the result:

| Result | Meaning |
|---|---|
| 🔴 **PHISHING DETECTED** | The link matches phishing patterns. Do not open it or enter passwords, PINs or OTPs. |
| 🟢 **NO PHISHING DETECTED** | No warning signs found in the address. Stay careful; this is not a guarantee. |
| 🟠 **Invalid URL** | The text is not a valid web address. |

---

## Build from source

This repository contains the key source files rather than the full Android Studio project. To build:

1. Create a new **Empty Views Activity** project in Android Studio with package name `com.example.tinyphish`, language **Kotlin** and minimum SDK **API 26**.
2. Copy `MainActivity.kt` and `PhishingDetector.kt` into `app/src/main/java/com/example/tinyphish/`.
3. Copy `activity_main.xml` into `app/src/main/res/layout/`.
4. Create `app/src/main/assets/` and copy `phishing_tree_final_cw3.json` into it.
5. Replace the app-level `build.gradle.kts` with the one in this repository, then sync Gradle.
6. Add the string resources referenced by the layout (`app_name`, `tagline`, `url_label`, `url_hint`, `check_url`, `scan_result`, `ready_result`, `enter_url_message`, `offline_note`, `safety_note`) to `res/values/strings.xml`.
7. Run on a device or emulator, or build a signed APK via **Build → Generate Signed App Bundle or APK**.

> Release optimisation (R8) is disabled in `build.gradle.kts` because it caused a start-up crash in the release build.

### Swapping the model

Any tree exported in the same format can replace the model without code changes:

```json
{
  "children_left":  [ ... ],
  "children_right": [ ... ],
  "features":       [ ... ],
  "thresholds":     [ ... ],
  "values":         [ [legitimate, phishing], ... ]
}
```

---

## Results

Final model on the PhishBD_2026 held-out test set (26,234 URLs, threshold 0.30):

| Metric | Value |
|---|---|
| Accuracy | 93.82% |
| Precision | 92.86% |
| Recall | 98.76% |
| F1-score | 95.72% |

|  | Predicted legitimate | Predicted phishing |
|---|---|---|
| **Actual legitimate** | 6,475 | 1,395 |
| **Actual phishing** | 227 | 18,137 |

The Android app's predictions matched the Python model on every test URL, including an IPv6 address.

---

## Limitations

- **Path bias:** the legitimate URLs in the training data are almost all bare home pages, so genuine links that include a path (e.g. `https://example.com/login`) are often flagged.
- **False alarms:** about 18% of legitimate test URLs were flagged.
- **URL text only:** the page itself is never inspected, so a carefully crafted phishing URL can pass. URL shorteners and punycode look-alikes are not specifically handled.
- A benign result is **not a guarantee of safety**.

## Future work

- Retrain with legitimate URLs that include realistic paths.
- Add a few lightweight features to reduce false alarms.
- Measure speed and memory on low-end phones (1–2 GB RAM).
- Share-sheet integration and a Bangla interface.

---

## Datasets

- **PhishBD_2026** (final model): A. P. Chowdhury, M. M. R. Khan and F. Nur, Mendeley Data, Version 2, 2026. DOI: [10.17632/8d6zsfwc7z.2](https://doi.org/10.17632/8d6zsfwc7z.2), CC BY 4.0.
- **URL-Phish** (development): L. Dam Minh and H. Tran Cong, Mendeley Data, Version 2, 2026. DOI: [10.17632/65z9twcx3r.2](https://doi.org/10.17632/65z9twcx3r.2), CC BY 4.0.

## Acknowledgement

Thanks to **Professor Dr. Md Alamgir Hossain** for supervision and guidance, and to the creators of the PhishBD_2026 and URL-Phish datasets for making their work openly available.

## Disclaimer

TinyPhish is an academic project. It gives a quick, private first check, not a complete defence against phishing. Always verify suspicious links through official channels.
