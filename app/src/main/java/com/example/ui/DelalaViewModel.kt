package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Welcome : Screen()
    object LanguageSelect : Screen()
    object Auth : Screen()
    object About : Screen()
    object RegionSelect : Screen()
    object RoleSelect : Screen()
    object SellerHome : Screen()
    object BuyerHome : Screen()
    data class ProductDetails(val listing: ListingEntity) : Screen()
    object FeedbackAndNotice : Screen()
    object AdminDashboard : Screen()
}

class DelalaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DelalaRepository(database, viewModelScope)

    // Flow resources for reactivity
    val listingsState: StateFlow<List<ListingEntity>> = repository.allListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val requestsState: StateFlow<List<WantedRequestEntity>> = repository.allRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedbackState: StateFlow<List<FeedbackEntity>> = repository.allFeedback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportsState: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usersState: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active OTP state for Verification
    var activeOtp by mutableStateOf("2512")
        private set

    fun generateAndSendOtp(phone: String, onFinished: (Boolean, String) -> Unit) {
        val code = (1000..9999).random().toString()
        activeOtp = code
        val success = sendSms(phone, code)
        if (success) {
            onFinished(true, "OTP verification code sent to $phone!")
        } else {
            onFinished(false, "SmsManager fail/no SIM. Code is: $code")
        }
    }

    private fun sendSms(phoneNumber: String, otpCode: String): Boolean {
        return try {
            val context = getApplication<Application>()
            val message = "Your Delala verification code is: $otpCode. Please do not share it."
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Language configuration (EN, AM, OM)
    var currentLanguage by mutableStateOf("English")
        private set

    // Current Navigation State with custom trace capability
    var currentScreen by mutableStateOf<Screen>(Screen.Welcome)
        private set
    private val navigationHistory = mutableListOf<Screen>()

    // Active Sessions & user parameters
    var currentUser by mutableStateOf<UserEntity?>(null)
        private set
    var userRole by mutableStateOf("Guest") // Guest, Buyer, Seller, Admin
        private set
    var userRegion by mutableStateOf("Dire Dawa") // Dire Dawa, Moyale, Other

    // Search and Filtering State
    var searchQuery by mutableStateOf("")
    var selectedCategoryFilter by mutableStateOf("All")
    var selectedRegionFilter by mutableStateOf("All")
    var maxPriceFilter by mutableStateOf(500000.0)

    // Bookmarked items
    var savedListingIds = mutableStateOf<Set<Int>>(emptySet())
        private set

    // Supabase activity visual logging
    val supabaseLogs: List<String>
        get() = SupabaseClient.syncLog

    val supabaseStatus: String
        get() = SupabaseClient.lastSyncStatus

    fun setLanguage(lang: String) {
        currentLanguage = lang
    }

    fun navigateTo(screen: Screen) {
        navigationHistory.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack(): Boolean {
        if (navigationHistory.isNotEmpty()) {
            currentScreen = navigationHistory.removeAt(navigationHistory.size - 1)
            return true
        }
        return false
    }

    fun resetNavigation() {
        navigationHistory.clear()
        currentScreen = Screen.Welcome
    }

    // Auth actions
    fun loginUser(phone: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (phone == "0912345678") {
                // Admin fast-track account
                val admin = repository.getUserByPhone(phone)
                if (admin != null) {
                    currentUser = admin
                    userRole = admin.role
                    userRegion = admin.location
                } else {
                    val defaultAdmin = UserEntity(
                        phone = "0912345678",
                        name = "Ephraim (Delala Admin)",
                        email = "ephraim@delala.app",
                        location = "Dire Dawa",
                        role = "Admin",
                        verified = true
                    )
                    repository.registerUser(defaultAdmin)
                    currentUser = defaultAdmin
                    userRole = "Admin"
                    userRegion = "Dire Dawa"
                }
                onFinished(true, "Admin Logged In Successfully!")
                navigateTo(Screen.AdminDashboard)
                return@launch
            }

            val user = repository.getUserByPhone(phone)
            if (user != null) {
                currentUser = user
                userRole = user.role
                userRegion = user.location
                onFinished(true, "Authentication Successful!")
                
                // Route conditionally based on roles
                when (user.role) {
                    "Seller" -> navigateTo(Screen.SellerHome)
                    "Buyer" -> navigateTo(Screen.BuyerHome)
                    "Admin" -> navigateTo(Screen.AdminDashboard)
                    else -> navigateTo(Screen.About)
                }
            } else {
                onFinished(false, "Phone number not registered. Please sign up!")
            }
        }
    }

    fun registerNewUser(
        name: String,
        phone: String,
        email: String?,
        location: String,
        role: String,
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.getUserByPhone(phone)
            if (existing != null) {
                onFinished(false, "Phone number already registered. Try logging in!")
            } else {
                val isVerifiedAdminMock = (role == "Admin" || phone == "0912345678")
                val newUser = UserEntity(
                    phone = phone,
                    name = name,
                    email = email,
                    location = location,
                    role = role,
                    verified = isVerifiedAdminMock
                )
                repository.registerUser(newUser)
                currentUser = newUser
                userRole = role
                userRegion = location
                onFinished(true, "User Profile Created! Verification code simulated.")
                
                // Proceed to Screen 4 (About Delala)
                navigateTo(Screen.About)
            }
        }
    }

    fun logout() {
        currentUser = null
        userRole = "Guest"
        searchQuery = ""
        selectedCategoryFilter = "All"
        selectedRegionFilter = "All"
        resetNavigation()
    }

    // Role, Region configuration during onboarding flows
    fun saveRegion(region: String) {
        userRegion = region
        viewModelScope.launch {
            currentUser?.let {
                val updated = it.copy(location = region)
                repository.registerUser(updated)
                currentUser = updated
            }
        }
    }

    fun saveRole(role: String) {
        userRole = role
        viewModelScope.launch {
            currentUser?.let {
                val updated = it.copy(role = role)
                repository.registerUser(updated)
                currentUser = updated
            }
        }
    }

    // Seller marketplace listings CRUD
    fun promoteListing(
        category: String,
        title: String,
        description: String,
        condition: String,
        price: Double,
        imageUri: String,
        location: String,
        contact: String,
        onFinished: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val sellerCode = currentUser?.phone ?: contact
            val sellerName = currentUser?.name ?: "Independent Seller"
            val newListing = ListingEntity(
                sellerId = sellerCode,
                sellerName = sellerName,
                category = category,
                title = title,
                description = description,
                condition = condition,
                price = price,
                imageUri = imageUri,
                location = location
            )
            repository.addListing(newListing)
            onFinished(true)
        }
    }

    fun removeListing(id: Int) {
        viewModelScope.launch {
            repository.deleteListing(id)
        }
    }

    // Buyer Requests creation
    fun postBuyerRequest(
        category: String,
        productWanted: String,
        budget: Double,
        description: String,
        location: String,
        onFinished: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val buyerCode = currentUser?.phone ?: "0900000000"
            val buyerName = currentUser?.name ?: "Anonymous Buyer"
            val wanted = WantedRequestEntity(
                buyerId = buyerCode,
                buyerName = buyerName,
                category = category,
                productWanted = productWanted,
                description = description,
                budget = budget,
                location = location
            )
            repository.addWantedRequest(wanted)
            onFinished(true)
        }
    }

    fun toggleSaveListing(id: Int) {
        val current = savedListingIds.value
        savedListingIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    // App Feedback
    fun submitUserFeedback(rating: Int, comment: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val reviewerId = currentUser?.phone ?: "Guest"
            val reviewerName = currentUser?.name ?: "Guest User"
            val feedback = FeedbackEntity(
                userId = reviewerId,
                userName = reviewerName,
                rating = rating,
                comment = comment
            )
            repository.addFeedback(feedback)
            onFinished(true)
        }
    }

    // Abuse / Fraud Reporting
    fun fileAbuseReport(targetType: String, targetId: String, targetName: String, reason: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val reporterCode = currentUser?.phone ?: "Guest Reporter"
            val report = ReportEntity(
                reporterId = reporterCode,
                targetType = targetType,
                targetId = targetId,
                targetName = targetName,
                reason = reason
            )
            repository.addReport(report)
            onFinished(true)
        }
    }

    // Support verification badge controls
    fun toggleUserVerification(phone: String, verified: Boolean) {
        viewModelScope.launch {
            repository.updateUserVerification(phone, verified)
            if (currentUser?.phone == phone) {
                currentUser = currentUser?.copy(verified = verified)
            }
        }
    }

    fun banUserProfile(phone: String) {
        viewModelScope.launch {
            repository.deleteUser(phone)
        }
    }

    fun deleteAdminReport(id: Int) {
        viewModelScope.launch {
            repository.deleteReport(id)
        }
    }

    // Simple robust custom Translation Engine for Ethiopia marketplace (English, Amharic, Afaan Oromoo)
    fun t(key: String): String {
        val amharicMap = mapOf(
            "app_slogan" to "የኢትዮጵያ ምርት ማስተዋወቂያ መድረክ",
            "welcome_to" to "እንኳን ወደ",
            "btn_get_started" to "ለመጀመር ይጫኑ",
            "btn_learn_more" to "ተጨማሪ መረጃ",
            "select_language" to "ቋንቋ ይምረጡ",
            "choose_region" to "ክልል/ከተማ ይምረጡ",
            "dire_dawa" to "ድሬዳዋ",
            "moyale" to "ሞያሌ",
            "other" to "ሌላ ቦታ",
            "seller" to "ሻጭ (ምርት ለመለጠፍ)",
            "buyer" to "ገዢ (ምርት ለመግዛት)",
            "choose_role" to "የተጠቃሚነት ሚና ይምረጡ",
            "phone_num" to "ስልክ ቁጥር",
            "password" to "የይለፍ ቃል",
            "login" to "ግባ",
            "register" to "ይመዝገቡ",
            "full_name" to "ሙሉ ስም",
            "location" to "አድራሻ",
            "email" to "ኢሜይል (አማራጭ)",
            "confirm_password" to "የይለፍ ቃል ያረጋግጡ",
            "submit" to "ይላኩ",
            "about_desc" to "ደላላ በኢትዮጵያ ውስጥ ገዢዎችን እና ሻጮችን በቀጥታ የሚያገናኝ ዘመናዊ መተግበርያ ነው። ዩኒቨርሲቲ ተማሪዎችን፣ ጀማሪ ነጋዴዎችን እና ደላሎችን ያበረታታል! በገዢ መልክ ምርቶችን መፈለግ እና መደወል ይቻላል፤ ሻጭ ሆነው ምርቶችን በቀላሉ ማቅረብ ይችላሉ።",
            "disclaimer_title" to "አስፈላጊ ማሳሰቢያ",
            "disclaimer_desc" to "እባክዎን ማንኛውንም ግብይት ከሻጩ ጋር በቀጥታ በመገናኘት ያጠናቅቁ። እቃውን በአካል አይተው ሳያረጋግጡ አስቀድመው ገንዘብ በባንክ ወይም በሞባይል አይላኩ! ፊልም፣ ማታለያ ወይም ኪሳራዎች ውስጥ ደላላ ሀላፊነት አይወስድም።",
            "condition" to "የእቃው ሁኔታ",
            "price" to "ዋጋ",
            "contact_no" to "የሻጭ ስልክ ቁጥር",
            "electronics" to "ኤሌክትሮኒክስ",
            "wearables" to "አልባሳትና ጫማዎች",
            "jewelry" to "ጌጣጌጥ",
            "perfume" to "ሽቶዎች",
            "cream" to "ክሬም/ውበት",
            "household" to "የቤት ውስጥ እቃዎች",
            "other_category" to "ሌሎች",
            "post_listing" to "ምርት ያውጡ",
            "active_listings" to "የለጠፏቸው ምርቶች",
            "wanted_products" to "ገዢዎች የፈለጓቸው ምርቶች",
            "no_listing_yet" to "እስካሁን ምንም የተለጠፈ እቃ የለም።",
            "add_wanted_request" to "የሚፈልጉትን እቃ ይጠይቁ",
            "rating_feedback" to "የመተግበሪያ አስተያየት እና ድጋፍ",
            "admin_panel" to "አስተዳዳሪ ሰሌዳ (Admin)",
            "ban" to "አግድ (Ban)",
            "verify_badge" to "ውሳኔ ስጥ",
            "verified" to "ትክክለኛ ሻጭ"
        )

        val oromoMap = mapOf(
            "app_slogan" to "Siriiba Gabaa Itoophiyaa Wal-qunnamsiisu",
            "welcome_to" to "Baga Gara",
            "btn_get_started" to "Eegaluuf",
            "btn_learn_more" to "Dabalata Baruuf",
            "select_language" to "Afaan Filadhu",
            "choose_region" to "Naannoo Keessan Filadhu",
            "dire_dawa" to "Dirree Dhawaa",
            "moyale" to "Mooyyaalee",
            "other" to "Iddoo Biraa",
            "seller" to "Daldalaa (Gurguraa)",
            "buyer" to "Bitataa (Maamiloota)",
            "choose_role" to "Qooda Keessan Filadhu",
            "phone_num" to "Lakk. Bilbilaa",
            "password" to "Jecha icciiti",
            "login" to "Seeni",
            "register" to "Galmaa'i",
            "full_name" to "Maqaa Guutuu",
            "location" to "Bakka Jireenyaa",
            "email" to "E-mail (Yoo jiraate)",
            "confirm_password" to "Icciitii Mirkaniisi",
            "submit" to "Ergi",
            "about_desc" to "Dalaalaan bittootaa fi gurgurtoota Itoophiyaa keessaa kallattiin walitti kan qunnamsiisudha. Barattoota yunivarsiitii, daldaltoota xaxxuulii fi giddu-galtoota gabaa ni jajjabeessa. Meeshaalee fedhan barbaadanii bilbilaan wal qunnamuun ni danda’ama.",
            "disclaimer_title" to "Hubachiisa Murteessaa",
            "disclaimer_desc" to "Maaloo bittootaa fi gurgurtoota kallattiin qunnamuun daldala keessan xumuraa. Meeshaa osoo harka keessan hin seenin dura qarshii hin erginaa. Dalaalaan gabaa walitti fiduf malee daldala dhuunfaa gidduu seenuuf itti gaafatamummaa hin fudhatu.",
            "condition" to "Haala Meeshaa",
            "price" to "Gatiin",
            "contact_no" to "Lakk. Qunnamtii",
            "electronics" to "Elektirooniksii",
            "wearables" to "Uffataa fi Kophee",
            "jewelry" to "Faayaalee",
            "perfume" to "Urgooftuu (Perfume)",
            "cream" to "Kriimii fi Kuul",
            "household" to "Meeshaalee Manaa",
            "other_category" to "Kan biroo",
            "post_listing" to "Meeshaa Gurguruuf Baasi",
            "active_listings" to "Meeshaalee Keessan",
            "wanted_products" to "Meeshaalee Barbaadaman",
            "no_listing_yet" to "Meeshaan hamma ammaatti hin maxxanfamu.",
            "add_wanted_request" to "Meeshaa Barbaaddan Gaafadhaa",
            "rating_feedback" to "Yaada fi Komii Keessan",
            "admin_panel" to "Seera Bulchaa (Admin)",
            "ban" to "Dhophi (Ban)",
            "verify_badge" to "Mirkaneessi",
            "verified" to "Mirkanaa'aa"
        )

        return when (currentLanguage) {
            "Amharic" -> amharicMap[key] ?: key
            "Afaan Oromoo" -> oromoMap[key] ?: key
            else -> {
                // English Default Map
                val englishMap = mapOf(
                    "app_slogan" to "Ethiopian Trusted Marketplace Platform",
                    "welcome_to" to "Welcome to",
                    "btn_get_started" to "Get Started",
                    "btn_learn_more" to "Learn More",
                    "select_language" to "Select Language",
                    "choose_region" to "Select Your Region",
                    "dire_dawa" to "Dire Dawa",
                    "moyale" to "Moyale",
                    "other" to "Other",
                    "seller" to "Seller (Post Listings)",
                    "buyer" to "Buyer (Browse & Direct Contact)",
                    "choose_role" to "Choose Your User Role",
                    "phone_num" to "Phone Number",
                    "password" to "Password",
                    "login" to "Log In",
                    "register" to "Register",
                    "full_name" to "Full Name",
                    "location" to "Location",
                    "email" to "Email Address (Optional)",
                    "confirm_password" to "Confirm Password",
                    "submit" to "Submit",
                    "about_desc" to "Delala connects buyers and sellers across Ethiopia with simplicity. It serves university students, local broker agents, and small business owners searching for trusted transactions. You can broker and post products for high exposure easily.",
                    "disclaimer_title" to "Important Notice",
                    "disclaimer_desc" to "Please complete all transactions through direct communication and in-person checks. Delala is not responsible for payments, online bank transfers, or any scam conducted outside. Always verify physical products before hand-to-hand money exchange.",
                    "condition" to "Condition",
                    "price" to "Price",
                    "contact_no" to "Contact Number",
                    "electronics" to "Electronics",
                    "wearables" to "Wearables",
                    "jewelry" to "Jewelry",
                    "perfume" to "Perfume",
                    "cream" to "Cream",
                    "household" to "Household Items",
                    "other_category" to "Other Category",
                    "post_listing" to "Post Product Listing",
                    "active_listings" to "Active Seller Listings",
                    "wanted_products" to "Wanted Requests Board",
                    "no_listing_yet" to "No listings available yet.",
                    "add_wanted_request" to "Post Wanted Product Request",
                    "rating_feedback" to "Feedback and App Suggestions",
                    "admin_panel" to "Admin Dashboard Panel",
                    "ban" to "Ban User",
                    "verify_badge" to "Toggle Verify",
                    "verified" to "Verified Seller"
                )
                englishMap[key] ?: key
            }
        }
    }
}
