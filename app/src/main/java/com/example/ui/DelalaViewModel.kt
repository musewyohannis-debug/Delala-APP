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
    data class TryPlaceOrder(val listing: ListingEntity) : Screen()
    object FeedbackAndNotice : Screen()
    object AdminDashboard : Screen()
}

class DelalaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DelalaRepository(database, viewModelScope)

    // Customizable application name
    var appNameDynamic by mutableStateOf("Delala Marketplace")

    // Dark Theme preference state
    var darkThemeEnabled by mutableStateOf(false)
        private set

    init {
        SupabaseClient.init(application)
        val prefs = application.getSharedPreferences("delala_prefs", android.content.Context.MODE_PRIVATE)
        appNameDynamic = prefs.getString("custom_app_name", "Delala Marketplace") ?: "Delala Marketplace"
        darkThemeEnabled = prefs.getBoolean("dark_theme", false)
    }

    fun toggleDarkTheme(enabled: Boolean) {
        darkThemeEnabled = enabled
        val prefs = getApplication<Application>().getSharedPreferences("delala_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

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

    fun updateAppName(newAppName: String) {
        appNameDynamic = newAppName
        val prefs = getApplication<Application>().getSharedPreferences("delala_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("custom_app_name", newAppName).apply()
    }

    // Supabase Orders state for modern admin dashboard
    private val _supabaseOrders = MutableStateFlow<List<SupabaseOrder>>(emptyList())
    val supabaseOrders: StateFlow<List<SupabaseOrder>> = _supabaseOrders.asStateFlow()

    private val _isFetchingSupabaseOrders = MutableStateFlow(false)
    val isFetchingSupabaseOrders: StateFlow<Boolean> = _isFetchingSupabaseOrders.asStateFlow()

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
    fun loginUser(phone: String, passwordEntered: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (phone == "0953348822" || phone.lowercase() == "admin") {
                if (passwordEntered != "1364" && passwordEntered.lowercase() != "admin") {
                    onFinished(false, "Incorrect Admin passcode!")
                    return@launch
                }
                val targetPhone = if (phone.lowercase() == "admin") "0953348822" else phone
                // Admin fast-track account
                val admin = repository.getUserByPhone(targetPhone)
                if (admin != null) {
                    currentUser = admin
                    userRole = admin.role
                    userRegion = admin.location
                } else {
                    val defaultAdmin = UserEntity(
                        phone = targetPhone,
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
                val isVerifiedAdminMock = (role == "Admin" || phone == "0953348822")
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

    fun logoutAdmin() {
        currentUser = null
        userRole = "Guest"
        searchQuery = ""
        currentScreen = Screen.AdminDashboard
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

    fun submitOrder(
        customerName: String,
        phone: String,
        email: String,
        city: String,
        address: String,
        country: String,
        productName: String,
        productVariant: String,
        quantity: Int,
        notes: String,
        onFinished: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = SupabaseClient.insertOrder(
                customerName = customerName,
                phone = phone,
                email = email,
                city = city,
                address = address,
                country = country,
                productName = productName,
                productVariant = productVariant,
                quantity = quantity,
                notes = notes
            )
            if (success) {
                fetchSupabaseOrders()
            }
            onFinished(success)
        }
    }

    fun fetchSupabaseOrders() {
        viewModelScope.launch {
            _isFetchingSupabaseOrders.value = true
            val list = SupabaseClient.fetchOrders()
            _supabaseOrders.value = list
            _isFetchingSupabaseOrders.value = false
        }
    }

    fun updateSupabaseOrderStatus(orderId: Long, newStatus: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = SupabaseClient.updateOrderStatus(orderId, newStatus)
            if (success) {
                // Refresh list on success
                val updatedList = _supabaseOrders.value.map { order ->
                    if (order.id == orderId) order.copy(status = newStatus) else order
                }
                _supabaseOrders.value = updatedList
            }
            callback(success)
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
            "verified" to "ትክክለኛ ሻጭ",

            // Newly added Translations to complete buyer/seller flows
            "tabs_browse_products_custom" to "ደላላ የገበያ ቦታ",
            "tabs_wanted_board_custom" to "የፈላጊዎች ሰሌዳ\n(ጥያቄዎች)",
            "tabs_browse_products" to "ምርቶችን ይፈልጉ",
            "tabs_submit_request" to "ጥያቄ ያቅርቡ",
            "tabs_wanted_board" to "የሚፈለጉ ምርቶች ሰሌዳ",
            "search_placeholder" to "ኤሌክትሮኒክስ፣ ሽቶዎችና ጫማዎችን ይፈልጉ...",
            "reg_prefix" to "ክልል:",
            "no_matching_listings" to "ምንም ተዛማጅ ምርት አልተገኘም።",
            "try_clearing_tags" to "የመረጧቸውን ማጣሪያዎች ይቀይሩ ወይም የሚፈልጉትን ምርት ይጠይቁ!",

            "cat_all" to "ሁሉም",
            "cat_electronics" to "ኤሌክትሮኒክስ",
            "cat_wearables" to "አልባሳትና ጫማዎች",
            "cat_jewelry" to "ጌጣጌጥ",
            "cat_perfume" to "ሽቶዎች",
            "cat_cream" to "ክሬም/ውበት",
            "cat_household" to "የቤት ዕቃዎች",
            "cat_other" to "ሌሎች",

            "reg_all" to "ሁሉም",
            "reg_dire_dawa" to "ድሬዳዋ",
            "reg_moyale" to "ሞያሌ",
            "reg_other" to "ሌላ ቦታ",

            "cond_new" to "አዲስ",
            "cond_medium_used" to "በመጠኑ ያገለገለ",
            "cond_old" to "ያረጀ",

            "welcome_seller" to "እንኳን ደህና መጡ",
            "store_region" to "የርስዎ መደብር የሚገኝበት ክልል",
            "collapse_form" to "ፎርሙን ዝጋ",
            "new_offer_header" to "አዲስ የሚሸጥ ምርት ያውጡ",
            "product_title_label" to "የምርቱ ስም / ሞዴል",
            "category_label" to "ምድብ:",
            "condition_label" to "የእቃው ሁኔታ (Condition):",
            "price_etb_label" to "ዋጋ (በብር)",
            "detailed_desc_label" to "ዝርዝር መግለጫ",
            "contact_phone_label" to "የሻጭ ስልክ ቁጥር",
            "post_listing_btn" to "ምርቱን ለሽያጭ ይልቀቁ",

            "post_wanted_banner_title" to "የሚፈልጉትን ምርት ጥያቄ እዚህ ይለጥፉ!",
            "post_wanted_banner_subtitle" to "የሚፈልጉት እቃ ዝርዝር ውስጥ ከሌለ የሀገር ውስጥ ደላሎች በቀጥታ ሊደውሉልዎት ይችላሉ።",
            "product_wanted_model" to "የሚፈለገው ምርት / ሞዴል",
            "wanted_model_placeholder" to "ምሳሌ፡ ሳምሰንግ A54 ወይም ማብሰያ እቃ",
            "category_wanted_label" to "የሚፈለገው ምርት ምድብ:",
            "approx_budget_label" to "ሊከፍሉ የሚችሉት በጀት (በብር)",
            "prod_desc_req_label" to "የምርቱ መግለጫ እና ፍላጎቶችዎ",
            "post_wanted_btn" to "የፍላጎት ጥያቄውን ይለጥፉ",
            "no_wanted_requests" to "እስካሁን ምንም የፍላጎት ጥያቄ አልተለጠፈም።",

            "verified_platform_badge" to "የተረጋገጠ የደላላ መገናኛ መድረክ",
            "condition_prefix" to "ሁኔታ",
            "region_prefix" to "አድራሻ",
            "description_label" to "መግለጫ:",
            "seller_broker_info" to "የሻጭ/ደላላ መረጃ:",
            "seller_mobile_prefix" to "የሻጭ ስልክ ቁጥር",
            "call_seller_btn" to "ሻጩ ጋር ይደውሉ",
            "whatsapp_btn" to "ዋትስአፕ (WhatsApp)",
            "report_scam_btn" to "አጠራጣሪ ምርት ወይም ማጭበርበርን ሪፖርት ያድርጉ",

            "report_dialog_title" to "ለአስተዳዳሪው ሪፖርት መላኪኛ ፎርም",
            "report_dialog_instruct" to "እባክዎን ይህ ምርት ለምን የሀሰት ወይም አጠራጣሪ እንደሆነ ያብራሩ:",
            "report_dialog_placeholder" to "ምሳሌ፡ ከምርቱ ፍተሻ በፊት በባንክ ገንዘብ ጠይቆኛል",
            "submit_report_btn" to "ሪፖርቱን ላክ",
            "cancel_btn" to "ሰርዝ",

            "optional_photo_label" to "የምርት ፎቶ (ከተፈለገ ብቻ - አማራጭ)",
            "optional_photo_placeholder" to "ቀጥታ የምስል አድራሻ (URL) እዚህ መለጠፍ ይችላሉ",
            "optional_photo_helper" to "ወይም ከታች ካሉት ፈጣን ናሙናዎች መምረጥ ይችላሉ:"
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
            "verified" to "Mirkanaa'aa",

            // Newly added Translations to complete buyer/seller flows
            "tabs_browse_products_custom" to "Gabaa Waloo Delala",
            "tabs_wanted_board_custom" to "Gabatee Fedhii\n(Gaaffilee)",
            "tabs_browse_products" to "Meeshaalee Barbaadi",
            "tabs_submit_request" to "Gaaffii Dhiyeessi",
            "tabs_wanted_board" to "Gabaa Fedhii",
            "search_placeholder" to "Elektirooniksii, urgooftuu, kophee barbaadi...",
            "reg_prefix" to "Naannoo:",
            "no_matching_listings" to "Meeshaan wal-fakkaatu hin argamne.",
            "try_clearing_tags" to "Faayiloota calaluu sirreessi ykn gaaffii dhuunfaa barreessi!",

            "cat_all" to "Hunda",
            "cat_electronics" to "Elektirooniksii",
            "cat_wearables" to "Uffataa fi Kophee",
            "cat_jewelry" to "Faayaalee",
            "cat_perfume" to "Urgooftuu (Perfume)",
            "cat_cream" to "Kriimii fi Kuul",
            "cat_household" to "Meeshaalee Manaa",
            "cat_other" to "Kan biroo",

            "reg_all" to "Hunda",
            "reg_dire_dawa" to "Dirree Dhawaa",
            "reg_moyale" to "Mooyyaalee",
            "reg_other" to "Iddoo Biraa",

            "cond_new" to "Haaraa",
            "cond_medium_used" to "Hamma ta'e tajaajile",
            "cond_old" to "Dulfooma",

            "welcome_seller" to "Baga Nagaan Dhuftan",
            "store_region" to "Bakki daldala keessanii Naannoo",
            "collapse_form" to "Foomii Deebisi",
            "new_offer_header" to "Daldala Haaraa Gabaaf Dhiyeessi",
            "product_title_label" to "Maqaa / Moodela Meeshaa",
            "category_label" to "Category:",
            "condition_label" to "Haala Meeshaa:",
            "price_etb_label" to "Gatii (ETB)",
            "detailed_desc_label" to "Ibsa Meeshaa",
            "contact_phone_label" to "Lakk. Bilbila Gabaa",
            "post_listing_btn" to "Meeshaa Gabaaf Maxxansi",

            "post_wanted_banner_title" to "Meeshaa barbaaddan asitti gaafadhaa!",
            "post_wanted_banner_subtitle" to "Yoo gabaa keessatti dhabame, dalaalotni naannoo keessanii isiniif bilbiluu danda'u.",
            "product_wanted_model" to "Moodela Meeshaa Barbaadamu",
            "wanted_model_placeholder" to "fkn. Samsung A54 ykn Fryer",
            "category_wanted_label" to "Category Barbaadamu:",
            "approx_budget_label" to "Gatii Tilmaamaa (ETB)",
            "prod_desc_req_label" to "Ibsaa fi Ulaagaa Meeshaa",
            "post_wanted_btn" to "Gaaffii Fedhii Maxxansi",
            "no_wanted_requests" to "Gabaa kana irratti gaaffiin dhiyaate hin jiru.",

            "verified_platform_badge" to "Siriiba Dalaalaa Mirkanaa'aa",
            "condition_prefix" to "Haala",
            "region_prefix" to "Naannoo",
            "description_label" to "Ibsa:",
            "seller_broker_info" to "Odeeffannoo Gurguraa / Dalaalaa:",
            "seller_mobile_prefix" to "Bilbila Gurguraa",
            "call_seller_btn" to "Gurguraaf Bilbili",
            "whatsapp_btn" to "WhatsApp",
            "report_scam_btn" to "Daldala Shakkisiisaa ykn Hantuura Gabaasi",

            "report_dialog_title" to "Mula'ata Shakkisiisaa Gabaasi",
            "report_dialog_instruct" to "Maaloo gurgurtiin kun maaliif akka soba ykn shakkisiisaa ta'e ibsaa:",
            "report_dialog_placeholder" to "fkn. Osoo meeshaa hin ilaaliin dura qarshii ana gaafate",
            "submit_report_btn" to "Gabaasa Ergi",
            "cancel_btn" to "Haqi",

            "optional_photo_label" to "Suura Meeshaa (Filannoo)",
            "optional_photo_placeholder" to "Liinkii Suuraa (URL) asitti galchuu dandeessa",
            "optional_photo_helper" to "Yookaan suuraalee qophaa'an gadii filadhu:"
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
                    "verified" to "Verified Seller",

                    // Newly added Fallbacks
                    "tabs_browse_products_custom" to "Delala Marketplace",
                    "tabs_wanted_board_custom" to "Seekers Board\n(Requests)",
                    "tabs_browse_products" to "Browse Products",
                    "tabs_submit_request" to "Submit Request",
                    "tabs_wanted_board" to "Wanted Board",
                    "search_placeholder" to "Search electronic, perfume, shoes...",
                    "reg_prefix" to "Reg:",
                    "no_matching_listings" to "No matching listings found.",
                    "try_clearing_tags" to "Try clearing tags or posting a custom request!",

                    "cat_all" to "All",
                    "cat_electronics" to "Electronics",
                    "cat_wearables" to "Wearables",
                    "cat_jewelry" to "Jewelry",
                    "cat_perfume" to "Perfume",
                    "cat_cream" to "Cream",
                    "cat_household" to "Household Items",
                    "cat_other" to "Other",

                    "reg_all" to "All",
                    "reg_dire_dawa" to "Dire Dawa",
                    "reg_moyale" to "Moyale",
                    "reg_other" to "Other",

                    "cond_new" to "New",
                    "cond_medium_used" to "Medium Used",
                    "cond_old" to "Old",

                    "welcome_seller" to "Welcome",
                    "store_region" to "Your store is associated with Region",
                    "collapse_form" to "Collapse Form",
                    "new_offer_header" to "New Marketplace Offer",
                    "product_title_label" to "Product Title / Model",
                    "category_label" to "Category:",
                    "condition_label" to "Condition:",
                    "price_etb_label" to "Price (ETB)",
                    "detailed_desc_label" to "Detailed Description",
                    "contact_phone_label" to "Contact Phone Number",
                    "post_listing_btn" to "Post Marketplace Listing",

                    "post_wanted_banner_title" to "Post custom product wanted requests!",
                    "post_wanted_banner_subtitle" to "If details aren't found in feed, local brokers could call you directly.",
                    "product_wanted_model" to "Product Wanted Model",
                    "wanted_model_placeholder" to "e.g. Samsung A54 or Fryer",
                    "category_wanted_label" to "Category Wanted:",
                    "approx_budget_label" to "Approximate Budget (ETB)",
                    "prod_desc_req_label" to "Product Description & Requirements",
                    "post_wanted_btn" to "Post Wanted Request",
                    "no_wanted_requests" to "No wanted requests on the board.",

                    "verified_platform_badge" to "Verified Delala Connection Platform",
                    "condition_prefix" to "Condition",
                    "region_prefix" to "Region",
                    "description_label" to "Description:",
                    "seller_broker_info" to "Seller Broker Information:",
                    "seller_mobile_prefix" to "Seller Mobile",
                    "call_seller_btn" to "Call Seller",
                    "whatsapp_btn" to "WhatsApp",
                    "report_scam_btn" to "Report Suspicious Product / Scam",

                    "report_dialog_title" to "File Abuse Report on Item",
                    "report_dialog_instruct" to "Please explain why this product listing is fake or highly suspicious:",
                    "report_dialog_placeholder" to "e.g. Asking for money prior to product check",
                    "submit_report_btn" to "Submit Report",
                    "cancel_btn" to "Cancel",

                    "optional_photo_label" to "Product Photo (Optional)",
                    "optional_photo_placeholder" to "Direct product image URL (Optional)",
                    "optional_photo_helper" to "Or select an instant sample from below:",

                    "order_product_btn" to "Pre-Order / Place Order",
                    "order_form_title" to "Place New Order",
                    "customer_name_label" to "Your Full Name (Required)",
                    "customer_name_error" to "Please enter your full name",
                    "phone_label" to "Your Phone Number (Required)",
                    "phone_error" to "Please enter a valid phone number (at least 9 digits)",
                    "email_label" to "Your Email Address (Optional)",
                    "email_error" to "Please enter a valid email address",
                    "city_label" to "City (Required)",
                    "city_error" to "Please enter your city",
                    "address_label" to "Street Address / Landmark (Required)",
                    "address_error" to "Please enter your delivery address",
                    "country_label" to "Country (Required)",
                    "country_error" to "Please enter your country",
                    "product_name_label" to "Product Name",
                    "product_name_error" to "Product name cannot be empty",
                    "product_variant_label" to "Product Variant (Optional)",
                    "quantity_label" to "Quantity",
                    "quantity_error" to "Quantity must be at least 1",
                    "notes_label" to "Additional Delivery Notes (Optional)",
                    "submit_order_btn" to "Confirm & Submit Order",
                    "sending_order_loading" to "Submitting order to Supabase...",
                    "order_success_header" to "Order Confirmed!",
                    "order_success_msg" to "Your order has been inserted into the 'orders' table on Supabase! Status: Pending.",
                    "order_error_header" to "Order Submission Failed",
                    "order_error_msg" to "Failed to submit order to Supabase. Please verify your connection!"
                )
                englishMap[key] ?: key
            }
        }
    }
}
