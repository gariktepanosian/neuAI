# NutriHealth AI — User Guide & Product Presentation

> Your personal nutrition coach, meal delivery service, and health diagnostics platform — all in one app.

---

## What Is NutriHealth AI?

NutriHealth AI is a mobile application (iOS & Android) that brings together three things that used to require three separate services:

1. **A smart AI nutrition coach** that knows your health data and builds your daily meals around it
2. **A meal delivery subscription** that brings fresh, ready-to-eat meals matching your plan right to your door
3. **A personal health diagnostic viewer** that connects your lab results directly to your food choices

The result: you eat better, understand your health, and never have to worry about what to cook or where your food comes from.

---

## The Problem NutriHealth AI Solves

Most people who want to eat healthy face the same three roadblocks:

| Problem | What usually happens | What NutriHealth AI does |
|---|---|---|
| "I don't know what to eat for my health condition" | Generic internet advice, guesswork | AI reads your actual lab results and builds meals around your biomarkers |
| "Healthy meal plans are too complicated to follow" | Plans abandoned within a week | Fresh meals delivered daily, pre-portioned, ready to eat |
| "My doctor gives me results but I don't understand them" | Lab papers stuffed in a drawer | App explains your results in plain language and shows how they influence your meals |

---

## Core Features

### 1. AI-Personalised Daily Menu

Every morning you receive a freshly generated daily meal plan — breakfast, lunch, dinner, and snacks — personalised specifically for you.

**What makes it personal:**
- Your calorie target and macro goals (protein, carbs, fat)
- Your dietary preferences and food restrictions
- Your latest lab results (vitamin levels, blood markers, cholesterol, etc.)
- Foods you have disliked or excluded in the past

**How it works:**
- Tap the **Menu** tab to see today's plan
- Each meal shows full nutritional information: calories, protein, carbohydrates, fats, fibre
- Visual macro charts show your day at a glance
- Tap any meal to see ingredients, preparation notes, and why it was chosen for you
- Dislike a suggestion? Tap **Replace** and get a new AI-generated alternative instantly

**Example:** If your last blood test showed low vitamin D, the AI will include vitamin-D-rich meals (salmon, fortified dairy, eggs, mushrooms) in your plan until your levels normalise.

---

### 2. Meal Delivery Subscription

You can turn your AI-generated meal plan into a real delivery — fresh meals cooked and brought to your door.

**Subscription options:**
- **5-Day Weekday Plan** — Monday through Friday, ideal for busy work weeks
- **7-Day Full Week Plan** — Full coverage, no cooking required any day

**How delivery works:**
1. Subscribe once in the app (secure payment via Stripe)
2. Each day, a fresh meal matching your personalised menu is prepared
3. You track your courier on a live map as they approach
4. Meals arrive in an insulated box with full ingredient labels and nutritional breakdown

**Payment:** Managed automatically via Stripe. Your subscription renews weekly. You can pause or cancel at any time from the app.

---

### 3. Health Diagnostics & Lab Results

The app connects your health data to your diet in a way no nutrition app has done before.

**What you can do:**
- Upload lab result documents (PDF or photo) directly from your phone
- The app parses your results and displays them in a clean, readable format
- View your biomarker history over time with trend charts
- Each biomarker links to a plain-language explanation of what it means
- Your AI meal plan automatically incorporates your latest biomarkers

**Supported lab result formats:**
- PDF uploads from any clinic or lab
- FHIR R4 digital health records (standard used by modern hospitals and health apps)
- HL7 v2 records from legacy hospital laboratory systems

**Example biomarker insights:**
- Low ferritin → more iron-rich meals (red meat, leafy greens, lentils)
- High LDL cholesterol → reduced saturated fats, more omega-3 and fibre
- Low B12 → B12-fortified foods or animal-protein emphasis in meal plans

---

### 4. Live Courier Tracking

Once your daily meal is dispatched, you can watch your courier travel to you in real time on a Google Maps view inside the app.

- See the courier's exact location on the map
- View estimated arrival time
- Receive a push notification when the courier is nearby
- Mark delivery as received with one tap

---

### 5. Biometric Login

Secure and fast access to your account using Face ID or fingerprint — no passwords to remember, no typing required on a small screen.

- **Face ID** on iPhone (Face ID-enabled models)
- **Touch ID** on older iPhones and iPads
- **Fingerprint sensor** on Android devices
- Fallback to password login is always available

Your login credentials are stored in your device's secure hardware (iOS Secure Enclave / Android Keystore) — they never leave your device.

---

## How to Get Started (Step-by-Step)

### Step 1: Download the App

- **iOS**: Search "NutriHealth AI" in the App Store
- **Android**: Search "NutriHealth AI" in Google Play

### Step 2: Create Your Account

1. Open the app
2. Tap **Get Started**
3. Enter your email address and create a password
4. Verify your email
5. Complete your health profile:
   - Age, height, weight
   - Dietary preferences (vegetarian, vegan, gluten-free, nut allergy, etc.)
   - Health goals (weight loss, muscle gain, general wellbeing, manage a condition)
   - Daily calorie target (the app calculates a suggestion based on your profile)

### Step 3: Set Up Biometric Login (Recommended)

1. Go to **Account → Security**
2. Tap **Enable Face ID / Fingerprint**
3. Follow your device's biometric setup prompt
4. On future logins, a single glance or touch gets you in

### Step 4: Upload Your Lab Results

1. Go to the **Diagnostics** tab
2. Tap the **+** button (top right)
3. Choose **Upload PDF** or **Take a Photo** of your lab report
4. The app processes the document and extracts your biomarkers
5. Review the parsed results and tap **Confirm**
6. Your next daily menu will already incorporate your health data

### Step 5: View Your AI Daily Menu

1. Go to the **Menu** tab
2. See today's personalised meal plan
3. Browse each meal — tap any meal card for full details
4. If you want to swap a meal, tap **Replace** for an instant AI alternative

### Step 6: Subscribe for Meal Delivery

1. Go to **Menu → Subscribe for Delivery**
2. Choose your plan: **5-Day Weekday** or **7-Day Full Week**
3. Enter your delivery address
4. Complete payment with your card (secured by Stripe)
5. Your first delivery will be scheduled for the next available day

### Step 7: Track Your Delivery

1. On delivery day, go to the **Tracking** tab
2. See your courier's live location on the map
3. You'll receive a push notification when the courier is 10 minutes away
4. Tap **Confirm Delivery Received** when your meal arrives

---

## Understanding Your Dashboard

### Menu Tab

| Element | What it shows |
|---|---|
| Today's date and greeting | Personalised welcome with your first name |
| Daily macro ring chart | Visual breakdown: calories, protein, carbs, fat for the day |
| Breakfast / Lunch / Dinner / Snacks | Individual meal cards with photo, name, and calorie count |
| Biomarker badge | Indicator when today's plan was influenced by your health data |

### Diagnostics Tab

| Element | What it shows |
|---|---|
| Biomarker list | All extracted values from your uploaded reports |
| Trend charts | How each biomarker has changed across your last N reports |
| Status indicators | Green (normal range), Yellow (borderline), Red (out of range) |
| Nutrition link | Which biomarkers are currently influencing your meal plan |

### Tracking Tab

| Element | What it shows |
|---|---|
| Live map | Your courier's real-time GPS location (updates every 5 seconds) |
| ETA panel | Estimated arrival time and courier name |
| Delivery history | Past deliveries with date, menu, and rating |

### Account Tab

| Element | What you can do |
|---|---|
| Profile | Update height, weight, dietary preferences |
| Health goals | Adjust your calorie target and wellness objectives |
| Subscription | View plan, pause, change, or cancel |
| Security | Enable/disable biometrics, change password |
| Notifications | Configure push notification preferences |

---

## Privacy & Security

Your health data is the most sensitive information we store. Here is exactly how it is protected:

| Protection | How it works |
|---|---|
| **Encrypted health data** | All biomarker values are encrypted with AES-256-GCM before being stored. Only you can access your results. |
| **JWT authentication** | Your login session uses a cryptographic token that expires and rotates automatically — even if intercepted, it cannot be reused. |
| **Biometric data stays on your device** | Face ID and fingerprint data never leaves your phone. We only receive a cryptographic attestation that your device confirmed your identity. |
| **Secure payment** | All payments are handled by Stripe, a PCI-DSS Level 1 certified payment processor. We never store your card number. |
| **HTTPS everywhere** | All communication between the app and our servers is encrypted in transit (TLS 1.3). |
| **No selling of data** | Your health data is never sold to third parties or used for advertising. |

---

## Frequently Asked Questions

**Q: How does the AI know what meals to recommend?**
It reads your uploaded lab results, your stated dietary preferences and restrictions, your calorie target, and any meals you have previously disliked or excluded. Google's Gemini 1.5 Pro model generates a personalised menu based on all of these inputs simultaneously.

**Q: Do I have to subscribe for delivery to use the meal planning features?**
No. The AI meal planning, lab result viewer, and biomarker tracking are all fully available without a delivery subscription. Delivery is an optional add-on.

**Q: How fresh are the delivered meals?**
Meals are prepared fresh on the day of delivery. They are delivered in insulated packaging and should be refrigerated if not eaten within 2 hours of delivery.

**Q: What happens if I don't upload any lab results?**
The app still creates personalised meal plans based on your dietary preferences, restrictions, and calorie goals. Lab result personalisation activates automatically the moment you upload a report.

**Q: Can I use the app if I have a dietary condition like diabetes or coeliac disease?**
Yes. You can specify dietary conditions and restrictions in your profile, and the AI will never suggest foods that conflict with them. However, this app does not replace medical advice — always consult your doctor or dietitian.

**Q: How do I cancel my delivery subscription?**
Go to **Account → Subscription → Cancel Subscription**. You can cancel at any time with no penalty. Your access continues until the end of the current billing period.

**Q: Is my lab report data shared with doctors or clinics?**
No. Your lab data is only used to personalise your meal plan within the app. It is not shared with any third party.

**Q: Which lab result formats are supported?**
PDF files from any clinic or lab, FHIR R4 digital records (modern standard), and HL7 v2 records from hospital laboratory systems. If your clinic can export your results digitally, it will almost certainly work.

---

## System Requirements

| Platform | Minimum requirement |
|---|---|
| iOS | iOS 14 or later; Face ID or Touch ID optional but recommended |
| Android | Android 8.0 (API 26) or later; fingerprint sensor optional |
| Internet | Required for meal plan generation, delivery tracking, and lab result processing. Core features work offline after initial load. |

---

## What's Coming Next

- **Push notifications for delivery** — real-time alerts when your courier is dispatched and when they are nearby
- **Meal ratings and feedback** — rate individual meals to improve future recommendations
- **Wearable integration** — connect Apple Health or Google Fit to incorporate activity data into calorie targets
- **Nutrient trend insights** — weekly and monthly charts showing how your diet has changed over time
- **Partner clinic integration** — direct connection to supported clinics for automatic lab result delivery (no manual upload needed)
