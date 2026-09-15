package com.example.util

enum class AppLanguage {
    MARATHI,
    ENGLISH
}

object Localization {

    fun isMarathi(lang: AppLanguage): Boolean = lang == AppLanguage.MARATHI

    // App Headers & General
    fun appTitle(lang: AppLanguage): String = if (isMarathi(lang)) "सहयोग" else "Sahayog"
    fun appSubtitle(lang: AppLanguage): String = if (isMarathi(lang)) "आपली कामगार सहकारी संस्था" else "Cooperative Workers Marketplace"
    fun offlineNotice(lang: AppLanguage): String = if (isMarathi(lang)) "स्थानिक सुरक्षित मोड • सर्व हिशोब फोनमध्ये सुरक्षित आहेत" else "Local Offline Mode • All data secured locally on device"
    fun switchRole(lang: AppLanguage): String = if (isMarathi(lang)) "भूमिका बदला" else "Switch Role"
    fun resetDemo(lang: AppLanguage): String = if (isMarathi(lang)) "डेमो रीसेट" else "Reset Demo"
    fun cancel(lang: AppLanguage): String = if (isMarathi(lang)) "रद्द करा" else "Cancel"
    fun close(lang: AppLanguage): String = if (isMarathi(lang)) "बंद करा" else "Close"

    // Role Selection / Onboarding
    fun onboardingWelcome(lang: AppLanguage): String =
        if (isMarathi(lang)) "सहयोग मध्ये आपले स्वागत आहे!" else "Welcome to Sahayog!"
    fun onboardingSubtitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "ग्रामीण व स्थानिक कामगारांची हक्काची आणि सुरक्षित सहकारी संस्था"
        else "Empowering rural & local workers with guaranteed wages & transparent escrow"
    fun chooseRolePrompt(lang: AppLanguage): String =
        if (isMarathi(lang)) "आपली भूमिका निवडा (खालीलपैकी एक निवडा):" else "Select your role to continue:"
    fun consumerRoleTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "मला सेवा हवी आहे (ग्राहक)" else "I Need Services (Consumer)"
    fun consumerRoleTagline(lang: AppLanguage): String =
        if (isMarathi(lang)) "घरातील व परिसरातील कामासाठी स्थानिक कुशल कारागीर बोलवा"
        else "Book verified, skilled local artisans for household & farm work"
    fun consumerBullet1(lang: AppLanguage): String =
        if (isMarathi(lang)) "🛡️ काम पूर्ण होईपर्यंत पैसे सुरक्षित (एस्क्रो ठेव)" else "🛡️ 100% Escrow Protection until work is verified"
    fun consumerBullet2(lang: AppLanguage): String =
        if (isMarathi(lang)) "📸 कामाचा फोटो पुरावा पाहूनच पैसे दिले जातात" else "📸 Worker must submit photo proof before release"
    fun consumerBullet3(lang: AppLanguage): String =
        if (isMarathi(lang)) "🪙 सहकारी संस्थेने ठरवलेला रास्त हमीभाव" else "🪙 Transparent cooperative wage floors"
    fun consumerBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "ग्राहक म्हणून सुरू करा ➔" else "Continue as Consumer ➔"

    fun providerRoleTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "मला काम हवे आहे (कामगार / कारागीर)" else "I Want to Work (Worker / Provider)"
    fun providerRoleTagline(lang: AppLanguage): String =
        if (isMarathi(lang)) "आपल्या कौशल्यानुसार काम मिळवा आणि हमीभाव कमवा"
        else "Find nearby jobs, earn guaranteed wages, and build your work history"
    fun providerBullet1(lang: AppLanguage): String =
        if (isMarathi(lang)) "💰 हमी किमान वेतन (कोणतीही मध्यस्थी किंवा पिळवणूक नाही)" else "💰 Guaranteed wage floor with 1.5x overtime after 8h"
    fun providerBullet2(lang: AppLanguage): String =
        if (isMarathi(lang)) "🔒 डिजिटल हिशोब नोंद (ब्लॉकचेन प्रमाणे सुरक्षित)" else "🔒 Cryptographic chained ledger for your earnings"
    fun providerBullet3(lang: AppLanguage): String =
        if (isMarathi(lang)) "🏥 कामगार कल्याण निधी व सामाजिक सुरक्षा" else "🏥 50% admin fee goes to Worker Welfare Fund"
    fun providerBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "कामगार म्हणून सुरू करा ➔" else "Continue as Worker ➔"

    fun adminRoleTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "🏛️ सहकारी संस्था समिती / Admin Login" else "🏛️ Co-op Committee / Admin Login"
    fun adminRoleDesc(lang: AppLanguage): String =
        if (isMarathi(lang)) "वाद निवारण, एस्क्रो निधी मंजुरी, न्याय्य काम वाटप व कल्याण निधी व्यवस्थापन"
        else "Dispute mediation, escrow payout release, fair dispatch & welfare oversight"
    fun adminBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "समिती लॉगिन ➔" else "Admin Login ➔"

    // Services
    fun serviceName(name: String, lang: AppLanguage): String {
        if (!isMarathi(lang)) return name
        return when (name.lowercase()) {
            "electrician" -> "इलेक्ट्रिशियन (वीज काम)"
            "plumber" -> "प्लंबर (नळ काम)"
            "carpenter" -> "सुतार (लाकडी काम)"
            "painter" -> "रंगारी (रंगकाम)"
            "cleaner" -> "सफाई कामगार (स्वच्छता)"
            "driver" -> "चालक (वाहन चालक)"
            "gardener" -> "माळी (बागकाम / शेती)"
            "caregiver" -> "काळजीवाहक (रुग्ण/वृद्ध सेवा)"
            "technician" -> "तंत्रज्ञ (उपकरण दुरुस्ती)"
            else -> name
        }
    }

    fun serviceDescription(name: String, defaultDesc: String, lang: AppLanguage): String {
        if (!isMarathi(lang)) return defaultDesc
        return when (name.lowercase()) {
            "electrician" -> "वायरिंग, पंखा, मोटर, स्विच बोर्ड व अर्थिंग दुरुस्ती"
            "plumber" -> "नळ गळती, पाईप जोडणी, टाकी व सॅनिटरी दुरुस्ती"
            "carpenter" -> "फर्निचर, दरवाजा, खिडकी, लाकडी कपाट दुरुस्ती व नवीन काम"
            "painter" -> "घर रंगवणे, भिंतीचे रंगकाम, डिस्टेंपर व ऑइल पेंट"
            "cleaner" -> "घर, स्वयंपाकघर, परिसर व पाण्याच्या टाकीची संपूर्ण स्वच्छता"
            "driver" -> "ट्रॅक्टर, चारचाकी गाडी चालवणे व मालवाहतूक"
            "gardener" -> "बागकाम, झाडांची छाटणी, गवत कापणे व शेती काम"
            "caregiver" -> "वृद्ध, आजारी किंवा लहान मुलांची काळजी व मदत"
            "technician" -> "टीव्ही, कुलर, फ्रिज, वॉशिंग मशीन व पंप दुरुस्ती"
            else -> defaultDesc
        }
    }

    // Pictorial Steps
    fun step1Title(lang: AppLanguage): String = if (isMarathi(lang)) "१. काम निवडा" else "1. Choose"
    fun step1Sub(lang: AppLanguage): String = if (isMarathi(lang)) "फोटो पाहून" else "Artisan"
    fun step2Title(lang: AppLanguage): String = if (isMarathi(lang)) "२. सुरक्षित ठेवा" else "2. Lock Escrow"
    fun step2Sub(lang: AppLanguage): String = if (isMarathi(lang)) "पैसे सुरक्षित" else "Protected"
    fun step3Title(lang: AppLanguage): String = if (isMarathi(lang)) "३. फोटो पुरावा" else "3. Photo Proof"
    fun step3Sub(lang: AppLanguage): String = if (isMarathi(lang)) "कामाची खात्री" else "Verified"
    fun step4Title(lang: AppLanguage): String = if (isMarathi(lang)) "४. हक्काचे पैसे" else "4. Fast Payout"
    fun step4Sub(lang: AppLanguage): String = if (isMarathi(lang)) "समाधानी हिशोब" else "Satisfied"

    // Booking Dialog
    fun bookDialogTitle(categoryName: String, lang: AppLanguage): String =
        if (isMarathi(lang)) "${serviceName(categoryName, lang)} बुक करा" else "Book $categoryName"
    fun locationLabel(lang: AppLanguage): String =
        if (isMarathi(lang)) "कामाचे ठिकाण / गाव व पत्ता" else "Service Location / Village"
    fun dateTimeLabel(lang: AppLanguage): String =
        if (isMarathi(lang)) "तारीख आणि सोयीची वेळ" else "Date & Preferred Time"
    fun durationLabel(lang: AppLanguage): String =
        if (isMarathi(lang)) "अंदाजे कामाचा वेळ" else "Estimated Duration"
    fun wageFloorNotice(lang: AppLanguage): String =
        if (isMarathi(lang)) "सहकारी हमी वेतन नियम: यापेक्षा कमी मोबदला देता येत नाही"
        else "Cooperative Wage Floor: Rate cannot be undercut"
    fun breakdownTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "पारदर्शक हिशोब तपशील:" else "Transparent Breakdown:"
    fun toWorker(lang: AppLanguage): String =
        if (isMarathi(lang)) "कामगाराची हक्काची कमाई" else "Direct to Worker"
    fun toWelfare(lang: AppLanguage): String =
        if (isMarathi(lang)) "कामगार कल्याण निधी (विमा व मदत)" else "Worker Welfare Fund (50% fee)"
    fun toAdmin(lang: AppLanguage): String =
        if (isMarathi(lang)) "सहकारी संस्था खर्च (५०% शुल्क)" else "Co-op Admin Operations"
    fun lockEscrowBtn(amount: String, lang: AppLanguage): String =
        if (isMarathi(lang)) "₹$amount एस्क्रोमध्ये सुरक्षित ठेवा आणि बुक करा" else "Hold ₹$amount in Escrow & Book"

    // Status Badges
    fun statusPending(lang: AppLanguage): String =
        if (isMarathi(lang)) "प्रतीक्षेत • कामगार शोधत आहे" else "PENDING • Waiting Worker"
    fun statusAccepted(lang: AppLanguage): String =
        if (isMarathi(lang)) "स्वीकारले • कामगार येणार आहे" else "ACCEPTED • Worker Assigned"
    fun statusInProgress(lang: AppLanguage): String =
        if (isMarathi(lang)) "काम चालू • फोटो पुरावा दिला" else "IN PROGRESS • Proof Uploaded"
    fun statusCompleted(lang: AppLanguage): String =
        if (isMarathi(lang)) "पूर्ण • पैसे कामगाराला मिळाले" else "COMPLETED • Escrow Released"
    fun statusDisputed(lang: AppLanguage): String =
        if (isMarathi(lang)) "तक्रार • समिती चौकशी करत आहे" else "DISPUTED • Under Review"
    fun statusRefunded(lang: AppLanguage): String =
        if (isMarathi(lang)) "परत केले • पैसे ग्राहकाला परत" else "REFUNDED • Returned to Customer"

    // Worker Screen
    fun workerDashboardTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "कारागीर डॅशबोर्ड" else "Artisan Dashboard"
    fun verifiedBadge(lang: AppLanguage): String =
        if (isMarathi(lang)) "✓ सहकारी प्रमाणित" else "✓ Cooperative Verified"
    fun totalEarned(lang: AppLanguage): String =
        if (isMarathi(lang)) "एकूण हक्काची कमाई" else "Total Earnings"
    fun availableJobsHeader(lang: AppLanguage): String =
        if (isMarathi(lang)) "उपलब्ध नवीन कामे" else "Available Jobs"
    fun acceptJobBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "काम स्वीकारा ✓" else "Accept Job ✓"
    fun activeJobsHeader(lang: AppLanguage): String =
        if (isMarathi(lang)) "माझी चालू कामे" else "My Active Jobs"
    fun submitProofBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "कामाचा फोटो पुरावा द्या 📷" else "Submit Photo Proof 📷"
    fun ledgerTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "डिजिटल हिशोब नोंदवही (ब्लॉकचेन प्रमाणे सुरक्षित)" else "Cryptographic Earnings Ledger"
    fun ledgerSubtitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "प्रत्येक कामाच्या कमाईची SHA-256 सुरक्षित नोंद. कोणालाही फेरफार करता येत नाही."
        else "Immutable SHA-256 hash-chained payout blocks. Tamper-evident and verifiable."

    // Admin Screen
    fun adminDashboardTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "सहकारी समिती प्रशासन" else "Cooperative Admin Panel"
    fun escrowInHoldTitle(lang: AppLanguage): String =
        if (isMarathi(lang)) "सध्या एस्क्रोमध्ये सुरक्षित रक्कम" else "Escrow Held in Trust"
    fun releaseEscrowBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "मंजूर करा व पैसे द्या (Release) ✓" else "Release Escrow to Worker ✓"
    fun refundBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "ग्राहकाला पैसे परत करा (Refund)" else "Refund Customer"
    fun disputeHeader(lang: AppLanguage): String =
        if (isMarathi(lang)) "तक्रार निवारण (ग्राहक तक्रारी)" else "Dispute Mediation Cases"
    fun fairDispatchBtn(lang: AppLanguage): String =
        if (isMarathi(lang)) "न्याय्य काम वाटप व जिनि गुणांक विश्लेषण ➔" else "Fair Dispatch & Gini Analysis ➔"
}
