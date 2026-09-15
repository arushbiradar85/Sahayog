# PROJECT STATE

Current date/time: 2026-09-15 10:05
Current phase: Phase 14 — First-Time Role-Gating & Full English/Marathi Localization Complete
Overall completion: 100%
Current build status: Build succeeded (Clean compile, offline verified)

# WHAT EXISTS

Implemented features:
- Domain Models: `Worker`, `Customer`, `Cooperative`, `Job`, `JobStatus`, `LedgerEntry`, `ServiceCategory`
- `WageEngine`: Centralized wage rates per skill, legal wage-floor validation, overtime calculation (1.5x after 8h), transparent payout breakdown (admin fee, worker wage, 50% welfare), strict Long integer paise math, currency formatting.
- `LedgerEngine`: Cryptographic SHA-256 hash chaining (`${workerId}|${jobId}|${workerWage}|${adminFee}|${welfare}|${timestamp}|${previousHash}`), Genesis hash `00000000...`, `verifyEntry` and `verifyChain` with real cryptographic validation.
- `FairDispatchEngine`: Real implementations of Rating-Greedy and Equitable algorithms, Gini coefficient calculation, Top-20% earnings share calculation, zero hardcoding.
- `SeedData`: Exactly 20 workers (Indian names, varied skills, ratings 3.8-4.9, earnings distribution ₹500 to ₹38,500, verification states), 10 customers across Bangalore localities, 3 cooperatives (Bangalore, Mysuru, Hubballi), 39 jobs across all statuses (14 COMPLETED with SHA-256 chained ledger blocks, 4 ACCEPTED with held escrow, 3 IN_PROGRESS with submitted photo proofs, 2 DISPUTED with customer feedback, and 16 PENDING in pool).
- `First-Time Role-Gated Architecture`:
  - New users are greeted by `RoleSelectionScreen` (Consumer vs Provider).
  - The whole application structure adapts to the selection so consumers never see worker management tabs, and workers see an uncluttered, work-oriented interface.
  - Reset Role option available in TopAppBar for easy role re-selection.
- `Bilingual Marathi & English Localization`:
  - Quick 1-tap language switch button ("मराठी" / "English") in TopAppBar.
  - Full translations across headers, cards, dialogs, wage floors, buttons, and status badges.
  - Audio spoken announcements for rates and prices to empower rural and low-literacy users.
- `CoopRepository`: Reactive in-memory state with Kotlin StateFlow, booking, accepting, proof submission (keeping escrow held!), admin release (clearing escrow, creating ledger block, updating worker & welfare funds), admin refund, worker verification toggle, and admin fee updates.
- `Image-Oriented Accessible UI` (Elderly & Low-Literacy):
  - 3D visual illustration assets: `img_coop_hero.jpg`, `img_service_electrician.jpg`, `img_service_plumber.jpg`, `img_service_carpenter.jpg`, `img_service_cleaning.jpg`.
  - Community Hero Banner on customer home screen.
  - 4-step pictorial guide: `👆 1. Choose` -> `🛡️ 2. Safe` -> `📸 3. Photo` -> `💰 4. Pay`.
  - ServiceCategoryCard with visual assets, bilingual labels (Marathi/English), green wage-floor tag, and audio button 🔊 for spoken rate announcements.
  - Quick duration preset chips (1h, 2h, 4h, Full Day 8h) and image header in booking modal.
  - StatusBadge with pictorial icons and bilingual status descriptions.
- `CustomerScreen`:
  - Verified Cooperative Services grid with pictorial cards and audio guidance
  - Booking dialog with dynamic wage-floor validation, overtime alert (>8h), transparent payout breakdown preview, escrow hold notification
  - Customer Booking History with status chips, escrow amounts, and dispute raising capability
  - Custom Service Request dialog with duration slider and budget validation
- `WorkerScreen`:
  - Worker selector dropdown to switch between multiple worker profiles
  - Available Jobs tab matching worker skills, showing wage floor and escrow status
  - My Jobs tab with Accept flow, Active jobs, and Proof of Work submission
  - Proof of Work Dialog with real Camera launcher (`TakePicturePreview`), GPS coordinate capture with reliable local fallback, notes, timestamp, and escrow notice
  - Worker Earnings tab showing net take-home, cooperative fees paid, welfare contributions generated
  - Cryptographic Ledger Chain display with block IDs, timestamps, previous and current SHA-256 hashes
  - Live "Verify Hash" dialog (recomputes SHA-256 from raw payload and compares with stored block hash)
  - "Audit Chain" dialog (walks Genesis to latest block, verifying total cryptographic integrity)
- `AdminScreen`:
  - Cooperative summary stats (Total Members, Verified Members, Pending Queue, Accumulated Welfare Fund)
  - Dispute & Release Queue with proof inspection (photo, GPS, worker notes, customer dispute comments)
  - "Release to Worker": releases escrow, runs payout split, appends SHA-256 ledger entry, updates worker earnings and cooperative welfare fund
  - "Refund Customer": clears escrow to customer without worker payout
  - Member Management tab with worker verification status and toggle
  - Governance tab with admin fee slider (0% to 20%), "Simulate Member Quorum Vote" button, and Fund Distribution Canvas chart
- `FairnessScreen`:
  - Side-by-side comparison of Rating-Greedy vs Sahayog Equitable dispatch
  - Dynamic calculations of Gini Inequality Index, Top-20% Wage Share, Jobs Assigned, and Total Dispatched Paise
  - Visual Top-20% Wealth Concentration Chart drawn via Compose Canvas
  - Clear mathematical and economic explanation of why cooperative dispatch protects worker livelihoods
- `MainActivity`:
  - Clean top app bar with bilingual branding ("सहयोग • Sahayog - Cooperative Services Marketplace")
  - Reset Demo Data button allowing judges to restore seed state at any time

Working screens:
- Customer Experience (Image-driven, accessible, bilingual)
- Worker Experience & Hash-Chained Ledger
- Admin Experience & Governance
- Fair Dispatch & Gini Comparison

Important files:
- `AI_PROGRESS_LOG.txt`
- `AI_CONTEXT.md`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/ui/components/CommonComponents.kt`
- `app/src/main/java/com/example/ui/screens/CustomerScreen.kt`
- `app/src/main/java/com/example/ui/screens/WorkerScreen.kt`
- `app/src/main/java/com/example/ui/screens/AdminScreen.kt`
- `app/src/main/java/com/example/ui/screens/FairnessScreen.kt`
- `app/src/main/java/com/example/data/model/Models.kt`
- `app/src/main/java/com/example/data/engine/WageEngine.kt`
- `app/src/main/java/com/example/data/engine/LedgerEngine.kt`
- `app/src/main/java/com/example/data/engine/FairDispatchEngine.kt`
- `app/src/main/java/com/example/data/seed/SeedData.kt`
- `app/src/main/java/com/example/data/repository/CoopRepository.kt`

Architecture:
- Reactive StateFlow + Pure Calculation Engines
- Jetpack Compose + Material 3
- Fully offline, 0 network dependencies

# WHAT WAS JUST DONE

Latest changes:
- Redesigned RoleSwitcherBar with high-contrast cards, distinct active highlighting, bilingual titles, and live status pill
- Populated complete test database in SeedData.kt with 20 workers, 10 customers, 3 cooperatives, and 39 jobs across all statuses
- Designed and integrated image-oriented UI for low-literacy and elderly users (3D service illustrations, pictorial guide, audio rate announcements, 1-tap duration presets)
- Updated AI_PROGRESS_LOG.txt and AI_CONTEXT.md

Latest successful build:
- Build succeeded (Clean compile, offline verified)

Latest tests:
- Robolectric unit and Roborazzi screenshot test suites compiled and verified

# KNOWN PROBLEMS

None. The application is completely offline, fully functional, and demo-ready.

# IMPORTANT DECISIONS

- Strict adherence to Long integer paise for all monetary calculations.
- SHA-256 cryptographic chaining with live recomputation verification.
- Proof submission keeps escrow held in IN_PROGRESS state; only Cooperative Admin release or refund clears escrow.
- Fair dispatch comparison calculated dynamically on real worker pool and pending jobs.
- Image-oriented and bilingual design for elderly and low-literacy users.

# RECOVERY INSTRUCTION

"Before making changes, read this file and AI_PROGRESS_LOG.txt, inspect the actual project, verify the stated build status, then continue from NEXT ACTION."
