package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.data.*
import com.example.ui.theme.DelalaGold
import com.example.ui.theme.DelalaGreen
import com.example.ui.theme.DelalaRed
import com.example.ui.theme.DelalaWhite

// Custom adaptive grid depending on screens
@Composable
fun AppResponsiveGrid(
    modifier: Modifier = Modifier,
    items: List<ListingEntity>,
    onItemClick: (ListingEntity) -> Unit,
    savedIds: Set<Int>,
    onSaveToggle: (Int) -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val columns = if (configuration.screenWidthDp > 600) 3 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { listing ->
            ProductGridCard(
                listing = listing,
                isSaved = savedIds.contains(listing.id),
                onClick = { onItemClick(listing) },
                onSaveToggle = { onSaveToggle(listing.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelalaMasterLayout(
    viewModel: DelalaViewModel,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    var showSyncLogs by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = DelalaGreen,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "D",
                                    color = DelalaGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "DELALA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (viewModel.currentScreen != Screen.Welcome) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkTheme(!viewModel.darkThemeEnabled) }) {
                        Icon(
                            imageVector = if (viewModel.darkThemeEnabled) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (viewModel.darkThemeEnabled) DelalaGold else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showSyncLogs = true }) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = "Supabase Status",
                            tint = if (viewModel.supabaseStatus.contains("failed") || viewModel.supabaseStatus.contains("offline")) DelalaRed else DelalaGreen
                        )
                    }
                    if (viewModel.currentUser != null) {
                        IconButton(onClick = {
                            viewModel.logout()
                            Toast.makeText(context, "Logged out!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ExitToApp,
                                contentDescription = "Sign Out"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Show custom branded bottom bar if user is authenticated
            if (viewModel.currentUser != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val isSeller = viewModel.userRole == "Seller"
                    val isAdmin = viewModel.userRole == "Admin"

                    NavigationBarItem(
                        selected = viewModel.currentScreen is Screen.BuyerHome || viewModel.currentScreen is Screen.SellerHome,
                        onClick = {
                            if (isSeller) viewModel.navigateTo(Screen.SellerHome)
                            else viewModel.navigateTo(Screen.BuyerHome)
                        },
                        icon = { Icon(Icons.Filled.Home, "Feed") },
                        label = { Text(viewModel.t("active_listings")) }
                    )

                    NavigationBarItem(
                        selected = viewModel.currentScreen is Screen.FeedbackAndNotice,
                        onClick = { viewModel.navigateTo(Screen.FeedbackAndNotice) },
                        icon = { Icon(Icons.Filled.Feedback, "Feedback") },
                        label = { Text(viewModel.t("rating_feedback")) }
                    )

                    if (isAdmin) {
                        NavigationBarItem(
                            selected = viewModel.currentScreen is Screen.AdminDashboard,
                            onClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                            icon = { Icon(Icons.Filled.Dashboard, "Admin") },
                            label = { Text("Admin") }
                        )
                    } else {
                        NavigationBarItem(
                            selected = viewModel.currentScreen is Screen.About,
                            onClick = { viewModel.navigateTo(Screen.About) },
                            icon = { Icon(Icons.Filled.Info, "About") },
                            label = { Text(viewModel.t("btn_learn_more")) }
                        )
                    }
                }
            }
        },
        content = content
    )

    if (showSyncLogs) {
        var showConfigForm by remember { mutableStateOf(false) }
        var urlInput by remember { mutableStateOf(SupabaseClient.getUrl()) }
        var keyInput by remember { mutableStateOf(SupabaseClient.getKey()) }

        AlertDialog(
            onDismissRequest = { showSyncLogs = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudQueue, contentDescription = null, tint = DelalaGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supabase Sync Center")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Status: ${viewModel.supabaseStatus}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (viewModel.supabaseStatus.contains("success")) DelalaGreen else DelalaGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Realtime Logs:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(viewModel.supabaseLogs) { log ->
                                Text(
                                    log,
                                    fontSize = 11.sp,
                                    color = Color.Green,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showConfigForm = !showConfigForm }) {
                        Text(if (showConfigForm) "Hide Custom Database Settings" else "Configure Custom Supabase Database")
                    }
                    
                    if (showConfigForm) {
                        Column {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("Supabase URL") },
                                placeholder = { Text("https://xxx.supabase.co/rest/v1") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it },
                                label = { Text("Anon API Key") },
                                placeholder = { Text("your_anon_public_key") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                singleLine = true
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        SupabaseClient.updateCredentials(context, urlInput, keyInput)
                                        Toast.makeText(context, "Supabase credentials updated!", Toast.LENGTH_SHORT).show()
                                        showConfigForm = false
                                    }
                                ) {
                                    Text("Save Settings")
                                }
                                OutlinedButton(
                                    onClick = {
                                        urlInput = ""
                                        keyInput = ""
                                        SupabaseClient.updateCredentials(context, "", "")
                                        Toast.makeText(context, "Reset to default sandbox database!", Toast.LENGTH_SHORT).show()
                                        showConfigForm = false
                                    }
                                ) {
                                    Text("Reset Default")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncLogs = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ==================== SCREEN 1 - WELCOME SCREEN ====================
@Composable
fun WelcomeScreen(viewModel: DelalaViewModel) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            DelalaGreen.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().testTag("welcome_screen_content")
        ) {
            // Visual Logo with Gold Ring
            Surface(
                modifier = Modifier
                    .size(130.dp)
                    .border(4.dp, DelalaGold, CircleShape),
                shape = CircleShape,
                color = DelalaGreen,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ደላላ",
                            color = DelalaWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp
                        )
                        Text(
                            "DELALA",
                            color = DelalaGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "${viewModel.t("welcome_to")} Delala",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = viewModel.t("app_slogan"),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { viewModel.navigateTo(Screen.LanguageSelect) },
                colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("get_started_button")
            ) {
                Text(
                    viewModel.t("btn_get_started"),
                    color = DelalaWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.About) },
                border = BorderStroke(2.dp, DelalaGold),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("learn_more_button")
            ) {
                Text(
                    viewModel.t("btn_learn_more"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==================== SCREEN 2 - LANGUAGE SELECTION ====================
@Composable
fun LanguageSelectScreen(viewModel: DelalaViewModel) {
    val languages = listOf("English", "Amharic", "Afaan Oromoo")
    val translations = mapOf("English" to "English", "Amharic" to "አማርኛ", "Afaan Oromoo" to "Afaan Oromoo")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = null,
            tint = DelalaGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = viewModel.t("select_language"),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        languages.forEach { lang ->
            val isSelected = viewModel.currentLanguage == lang
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { viewModel.setLanguage(lang) }
                    .testTag("lang_card_$lang"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DelalaGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) DelalaGreen else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = translations[lang] ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (lang == "English") "English Global" else if (lang == "Amharic") "የኢትዮጵያ የስራ ቋንቋ" else "Oromiyaa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.setLanguage(lang) },
                        colors = RadioButtonDefaults.colors(selectedColor = DelalaGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.Auth) },
            colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("lang_continue_button")
        ) {
            Text(
                viewModel.t("submit"),
                color = DelalaWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== SCREEN 3 - REGISTRATION & LOGIN ====================
@Composable
fun AuthScreen(viewModel: DelalaViewModel) {
    val context = LocalContext.current
    var isLoginTab by remember { mutableStateOf(true) }

    // Forms
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Dire Dawa") }
    var roleSelection by remember { mutableStateOf("Buyer") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // SMS OTP Verification
    var showOtpDialog by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("") }

    val triggerOtpSend = {
        viewModel.generateAndSendOtp(phone) { _, msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            showOtpDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Sms Permission granted! Sending OTP...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS Permission denied. Falling back to simulated mode.", Toast.LENGTH_LONG).show()
        }
        triggerOtpSend()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isLoginTab) viewModel.t("login") else viewModel.t("register"),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
        )
        Text(
            text = if (isLoginTab) "Access Delala Marketplace" else "Join the Ethiopian Trade Revolution",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tabs switcher
        TabRow(
            selectedTabIndex = if (isLoginTab) 0 else 1
        ) {
            Tab(
                selected = isLoginTab,
                onClick = { isLoginTab = true },
                text = { Text(viewModel.t("login"), fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = !isLoginTab,
                onClick = { isLoginTab = false },
                text = { Text(viewModel.t("register"), fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoginTab) {
            // LOGIN FORM
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(viewModel.t("phone_num")) },
                placeholder = { Text("e.g. 0911223344") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_phone_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(viewModel.t("password")) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (phone.length < 9 || password.isEmpty()) {
                        Toast.makeText(context, "Please enter valid credentials", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.loginUser(phone, password) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_submit_button")
            ) {
                Text(
                    viewModel.t("login"),
                    color = DelalaWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            // REGISTER FORM
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(viewModel.t("full_name")) },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(viewModel.t("phone_num")) },
                placeholder = { Text("e.g. 09xxxxxxxx") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("reg_phone_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(viewModel.t("email")) },
                leadingIcon = { Icon(Icons.Filled.Email, null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("reg_email_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location choice spinner simulator
            Text(
                "Select Region / Location:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                color = DelalaGreen
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Dire Dawa", "Moyale", "Other").forEach { loc ->
                    val isLocSel = location == loc
                    FilterChip(
                        selected = isLocSel,
                        onClick = { location = loc },
                        label = { Text(loc) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DelalaGreen,
                            selectedLabelColor = DelalaWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Role selection during register
            Text(
                "Initial User Role:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                color = DelalaGreen
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Buyer", "Seller").forEach { role ->
                    val isRoleSel = roleSelection == role
                    FilterChip(
                        selected = isRoleSel,
                        onClick = { roleSelection = role },
                        label = { Text(role) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DelalaGreen,
                            selectedLabelColor = DelalaWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(viewModel.t("password")) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("reg_password_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(viewModel.t("confirm_password")) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Filled.Check, null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("reg_confirm_password_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (name.isEmpty() || phone.length < 9) {
                        Toast.makeText(context, "Full Name and Phone are required", Toast.LENGTH_SHORT).show()
                    } else if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                    } else {
                        val hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.SEND_SMS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        if (hasSmsPermission) {
                            triggerOtpSend()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.SEND_SMS)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("reg_submit_button")
            ) {
                Text(
                    viewModel.t("register"),
                    color = DelalaWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // Interactive OTP dialog simulating SMS verification
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = { Text("Verify Phone (SMS OTP)") },
            text = {
                Column {
                    Text(
                        "We have sent a 4-digit verification code to $phone. Enter it below to register.\n(YOUR DYNAMIC OTP: ${viewModel.activeOtp})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { enteredOtp = it },
                        label = { Text("4-Digit OTP Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("otp_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredOtp == viewModel.activeOtp) {
                            showOtpDialog = false
                            viewModel.registerNewUser(name, phone, email, location, roleSelection) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Invalid Verification Code!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==================== SCREEN 4 - ABOUT DELALA ====================
@Composable
fun AboutScreen(viewModel: DelalaViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = DelalaGold,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "About Delala",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = viewModel.t("about_desc"),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Start,
                        lineHeight = 24.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(20.dp))

                AboutInfoRow(
                    icon = Icons.Filled.School,
                    title = "Supports Students",
                    desc = "Promotes micro-trading activities on campuses across Dire Dawa and Moyale."
                )

                Spacer(modifier = Modifier.height(16.dp))

                AboutInfoRow(
                    icon = Icons.Filled.Storefront,
                    title = "Broker Friendly (Delala)",
                    desc = "Allows micro-brokers to market items and facilitate hand-to-hand transactions easily."
                )

                Spacer(modifier = Modifier.height(16.dp))

                AboutInfoRow(
                    icon = Icons.Filled.Shield,
                    title = "Disclaimer & Safety Rules",
                    desc = "Always verify product quality and source in person before initiating payments."
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.RegionSelect) },
            colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("about_continue_button")
        ) {
            Text(
                "Continue",
                color = DelalaWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AboutInfoRow(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = DelalaGreen.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = DelalaGreen)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ==================== SCREEN 5 - REGION SELECTION ====================
@Composable
fun RegionSelectScreen(viewModel: DelalaViewModel) {
    val regions = listOf("Dire Dawa", "Moyale", "Other")
    val translations = mapOf("Dire Dawa" to "dire_dawa", "Moyale" to "moyale", "Other" to "other")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = DelalaGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = viewModel.t("choose_region"),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        regions.forEach { reg ->
            val isSelected = viewModel.userRegion == reg
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { viewModel.saveRegion(reg) }
                    .testTag("region_card_$reg"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DelalaGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) DelalaGreen else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.t(translations[reg] ?: reg),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (reg == "Dire Dawa") "Queen City of East Ethiopia" else if (reg == "Moyale") "Southern Border Trading Hub" else "Regions & Other Localities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.saveRegion(reg) },
                        colors = RadioButtonDefaults.colors(selectedColor = DelalaGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.RoleSelect) },
            colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("region_continue_button")
        ) {
            Text(
                "Save & Continue",
                color = DelalaWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== SCREEN 6 - CHOOSE ROLE ====================
@Composable
fun RoleSelectScreen(viewModel: DelalaViewModel) {
    val roles = listOf("Seller", "Buyer")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SupervisedUserCircle,
            contentDescription = null,
            tint = DelalaGold,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = viewModel.t("choose_role"),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        roles.forEach { r ->
            val isSelected = viewModel.userRole == r
            val icon = if (r == "Seller") Icons.Filled.Storefront else Icons.Filled.ShoppingBag

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clickable { viewModel.saveRole(r) }
                    .testTag("role_card_$r"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DelalaGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) DelalaGreen else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) DelalaGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.t(if (r == "Seller") "seller" else "buyer"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (r == "Seller") "Gain commissions by marketing various listings." else "Browse and connect with verified local brokers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.saveRole(r) },
                        colors = RadioButtonDefaults.colors(selectedColor = DelalaGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (viewModel.userRole == "Seller") {
                    viewModel.navigateTo(Screen.SellerHome)
                } else {
                    viewModel.navigateTo(Screen.BuyerHome)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("role_continue_button")
        ) {
            Text(
                "Enter Marketplace",
                color = DelalaWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== SCREEN 7 - SELLER LISTING PAGE ====================
@Composable
fun SellerHome(viewModel: DelalaViewModel) {
    val context = LocalContext.current
    val listings by viewModel.listingsState.collectAsState()
    val sellerPhone = viewModel.currentUser?.phone ?: ""
    val sellerListings = listings.filter { it.sellerId == sellerPhone }

    var expandedForm by remember { mutableStateOf(false) }

    // Form states
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Electronics") }
    var condition by remember { mutableStateOf("New") }
    var priceStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(viewModel.userRegion) }
    var contactNo by remember { mutableStateOf(viewModel.currentUser?.phone ?: "") }
    var imageUri by remember { mutableStateOf("") }

    val samplePhotos = mapOf(
        "Electronics" to listOf(
            "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=450&auto=format&fit=crop"
        ),
        "Wearables" to listOf(
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=450&auto=format&fit=crop"
        ),
        "Jewelry" to listOf(
            "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?w=450&auto=format&fit=crop"
        ),
        "Perfume" to listOf(
            "https://images.unsplash.com/photo-1541643600914-78b084683601?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=450&auto=format&fit=crop"
        ),
        "Cream" to listOf(
            "https://images.unsplash.com/photo-1608248597481-496100c8c836?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1621151602684-6997ed19aeae?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=450&auto=format&fit=crop"
        ),
        "Household Items" to listOf(
            "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1581557991964-125469da3b8a?w=450&auto=format&fit=crop"
        ),
        "Other" to listOf(
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=450&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=450&auto=format&fit=crop"
        )
    )

    val categories = listOf("Electronics", "Wearables", "Jewelry", "Perfume", "Cream", "Household Items", "Other")

    val getCatName = { c: String ->
        when (c) {
            "All" -> viewModel.t("cat_all")
            "Electronics" -> viewModel.t("cat_electronics")
            "Wearables" -> viewModel.t("cat_wearables")
            "Jewelry" -> viewModel.t("cat_jewelry")
            "Perfume" -> viewModel.t("cat_perfume")
            "Cream" -> viewModel.t("cat_cream")
            "Household Items" -> viewModel.t("cat_household")
            else -> viewModel.t("cat_other")
        }
    }

    val getCondName = { cond: String ->
        when (cond) {
            "New" -> viewModel.t("cond_new")
            "Medium Used" -> viewModel.t("cond_medium_used")
            else -> viewModel.t("cond_old")
        }
    }

    val regionKey = when (viewModel.userRegion) {
        "Dire Dawa" -> "dire_dawa"
        "Moyale" -> "moyale"
        else -> "other"
    }
    val translatedRegion = viewModel.t(regionKey)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DelalaGreen)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${viewModel.t("welcome_seller")}, ${viewModel.currentUser?.name ?: viewModel.t("seller")}",
                        color = DelalaWhite,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${viewModel.t("store_region")}: $translatedRegion",
                        color = DelalaGold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Button(
                onClick = { expandedForm = !expandedForm },
                modifier = Modifier.fillMaxWidth().testTag("add_listing_toggle"),
                colors = ButtonDefaults.buttonColors(containerColor = if (expandedForm) DelalaRed else DelalaGreen)
            ) {
                Icon(if (expandedForm) Icons.Filled.Close else Icons.Filled.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (expandedForm) viewModel.t("collapse_form") else viewModel.t("post_listing"))
            }
        }

        if (expandedForm) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = viewModel.t("new_offer_header"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = DelalaGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(viewModel.t("product_title_label")) },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_title"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Dropdown Simulator
                        Text(viewModel.t("category_label"), style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(getCatName(cat)) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Condition selectors
                        Text(viewModel.t("condition_label"), style = MaterialTheme.typography.labelMedium)
                        Row {
                            listOf("New", "Medium Used", "Old").forEach { cond ->
                                FilterChip(
                                    selected = condition == cond,
                                    onClick = { condition = cond },
                                    label = { Text(getCondName(cond)) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text(viewModel.t("price_etb_label")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("add_product_price"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(viewModel.t("detailed_desc_label")) },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_desc"),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = contactNo,
                            onValueChange = { contactNo = it },
                            label = { Text(viewModel.t("contact_phone_label")) },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_phone"),
                            singleLine = true
                        )

                        // Optional Photo Section
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PhotoCamera, null, tint = DelalaGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = viewModel.t("optional_photo_label"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = imageUri,
                                    onValueChange = { imageUri = it },
                                    label = { Text(viewModel.t("optional_photo_placeholder")) },
                                    placeholder = { Text("https://example.com/photo.jpg") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_product_image_url_field"),
                                    singleLine = true,
                                    trailingIcon = {
                                        if (imageUri.isNotEmpty()) {
                                            IconButton(onClick = { imageUri = "" }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick suggested categories selector
                                Text(
                                    text = viewModel.t("optional_photo_helper"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                val categoryPresets = samplePhotos[selectedCategory] ?: samplePhotos["Other"] ?: emptyList()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categoryPresets.forEachIndexed { index, url ->
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (imageUri == url) 3.dp else 1.dp,
                                                    color = if (imageUri == url) DelalaGreen else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    imageUri = url
                                                }
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = "Sample photo $index",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                if (imageUri.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Live Selected Preview:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DelalaGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = "Live preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val price = priceStr.toDoubleOrNull()
                                if (title.isEmpty() || price == null || description.isEmpty() || contactNo.isEmpty()) {
                                    Toast.makeText(context, "Please configure all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    val finalImageUri = if (imageUri.isBlank()) {
                                        when (selectedCategory) {
                                            "Electronics" -> "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=450&auto=format&fit=crop"
                                            "Wearables" -> "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=450&auto=format&fit=crop"
                                            "Jewelry" -> "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=450&auto=format&fit=crop"
                                            "Perfume" -> "https://images.unsplash.com/photo-1541643600914-78b084683601?w=450&auto=format&fit=crop"
                                            "Cream" -> "https://images.unsplash.com/photo-1608248597481-496100c8c836?w=450&auto=format&fit=crop"
                                            "Household Items" -> "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?w=450&auto=format&fit=crop"
                                            else -> "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=450&auto=format&fit=crop"
                                        }
                                    } else {
                                        imageUri
                                    }

                                    viewModel.promoteListing(
                                        category = selectedCategory,
                                        title = title,
                                        description = description,
                                        condition = condition,
                                        price = price,
                                        imageUri = finalImageUri,
                                        location = location,
                                        contact = contactNo
                                    ) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Listing Posted! Auto-sync to Supabase triggered.", Toast.LENGTH_SHORT).show()
                                            // Reset form fields
                                            title = ""
                                            priceStr = ""
                                            description = ""
                                            imageUri = ""
                                            expandedForm = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                            modifier = Modifier.fillMaxWidth().testTag("add_product_submit_button")
                        ) {
                            Text(viewModel.t("post_listing_btn"))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.t("active_listings") + " (${sellerListings.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Dire Dawa / Moyale / Other",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sellerListings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.List, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(viewModel.t("no_listing_yet"))
                    }
                }
            }
        } else {
            items(sellerListings) { listing ->
                SellerListingCard(listing = listing, onDelete = { viewModel.removeListing(listing.id) })
            }
        }
    }
}

@Composable
fun SellerListingCard(listing: ListingEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = listing.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(listing.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Category: ${listing.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("ETB %,.2f".format(listing.price), color = DelalaGreen, fontWeight = FontWeight.Black)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DelalaRed)
            }
        }
    }
}

// ==================== SCREEN 8 - BUYER FEED & REQUESTS ====================
@Composable
fun BuyerHome(viewModel: DelalaViewModel) {
    var activeTab by remember { mutableStateOf(0) } // 0: Browse Feed, 1: Create request, 2: Wanted Requests board
    var showPostDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val listings by viewModel.listingsState.collectAsState()
    val requests by viewModel.requestsState.collectAsState()

    // Filters values
    val categories = listOf("All", "Electronics", "Wearables", "Jewelry", "Perfume", "Cream", "Household Items", "Other")
    val regions = listOf("All", "Dire Dawa", "Moyale", "Other")

    val getCatName = { c: String ->
        when (c) {
            "All" -> viewModel.t("cat_all")
            "Electronics" -> viewModel.t("cat_electronics")
            "Wearables" -> viewModel.t("cat_wearables")
            "Jewelry" -> viewModel.t("cat_jewelry")
            "Perfume" -> viewModel.t("cat_perfume")
            "Cream" -> viewModel.t("cat_cream")
            "Household Items" -> viewModel.t("cat_household")
            else -> viewModel.t("cat_other")
        }
    }

    val getRegionName = { r: String ->
        when (r) {
            "All" -> viewModel.t("reg_all")
            "Dire Dawa" -> viewModel.t("reg_dire_dawa")
            "Moyale" -> viewModel.t("reg_moyale")
            else -> viewModel.t("reg_other")
        }
    }

    // Filter Logic
    val filteredListings = listings.filter { listing ->
        val matchesSearch = listing.title.contains(viewModel.searchQuery, ignoreCase = true) ||
                listing.description.contains(viewModel.searchQuery, ignoreCase = true)
        val matchesCat = viewModel.selectedCategoryFilter == "All" || listing.category == viewModel.selectedCategoryFilter
        val matchesReg = viewModel.selectedRegionFilter == "All" || listing.location == viewModel.selectedRegionFilter
        val matchesPrice = listing.price <= viewModel.maxPriceFilter
        matchesSearch && matchesCat && matchesReg && matchesPrice
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF111713))) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // CUSTOM TAB SWITCHER MATCHING SCREENSHOT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color(0xFF1B241E), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(14.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab 0: "ደላላ የገበያ ቦታ"
                val isTab0Selected = activeTab == 0
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTab0Selected) DelalaGreen else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        tint = if (isTab0Selected) Color.White else Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.t("tabs_browse_products_custom"),
                        color = if (isTab0Selected) Color.White else Color(0xFF9CA3AF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Tab 2: "የፈላጊዎች ሰሌዳ (ጥያቄዎች)"
                val isTab2Selected = activeTab == 2 || activeTab == 1
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTab2Selected) DelalaGreen else Color.Transparent)
                        .clickable { activeTab = 2 }
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = null,
                        tint = if (isTab2Selected) Color.White else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.t("tabs_wanted_board_custom"),
                        color = if (isTab2Selected) Color.White else Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (activeTab == 0) {
                // SEARCH & FILTER COMPONENT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        placeholder = { Text(viewModel.t("search_placeholder")) },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buyer_search_bar"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = viewModel.selectedCategoryFilter == cat,
                                onClick = { viewModel.selectedCategoryFilter = cat },
                                label = { Text(getCatName(cat)) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DelalaGreen,
                                    selectedLabelColor = DelalaWhite
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(viewModel.t("reg_prefix"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DelalaGreen)
                        regions.forEach { reg ->
                            FilterChip(
                                selected = viewModel.selectedRegionFilter == reg,
                                onClick = { viewModel.selectedRegionFilter = reg },
                                label = { Text(getRegionName(reg)) },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                if (filteredListings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                viewModel.t("no_matching_listings"),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                viewModel.t("try_clearing_tags"),
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        AppResponsiveGrid(
                            items = filteredListings,
                            onItemClick = { listing -> viewModel.navigateTo(Screen.ProductDetails(listing)) },
                            savedIds = viewModel.savedListingIds.value,
                            onSaveToggle = { id -> viewModel.toggleSaveListing(id) }
                        )
                    }
                }
            } else {
                // WANTED REQUESTS BOARD
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "What Buyers across Ethiopia are looking for:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        if (requests.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SpeakerNotes,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = viewModel.t("no_wanted_requests"),
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            items(requests) { req ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B241E)), // Dark card as requested
                                    border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = req.productWanted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color(0xFF10B981), // Bright attractive green title
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            Surface(
                                                color = Color(0xFFFFE082).copy(alpha = 0.15f), // Soft gold badge
                                                border = BorderStroke(1.dp, DelalaGold),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Budget: ${req.budget} ETB",
                                                    color = DelalaGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = req.description,
                                            color = Color(0xFFD1D5DB),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Seeker: ",
                                                color = Color(0xFF9CA3AF),
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = req.buyerName,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Phone: ${req.buyerId} • Town: ${getRegionName(req.location)}",
                                                color = Color(0xFF9CA3AF),
                                                fontSize = 11.sp,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Button(
                                                onClick = {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                        data = android.net.Uri.parse("tel:${req.buyerId}")
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(34.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Call,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Contact Buyer",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FLOATING ACTION BUTTON
        FloatingActionButton(
            onClick = { showPostDialog = true },
            containerColor = DelalaGreen,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("submit_wanted_fab"),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Post a request",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // MODAL POST DIALOG FOR SUBMITTING WANTED REQUESTS
    if (showPostDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPostDialog = false }) {
            var productWanted by remember { mutableStateOf("") }
            var categoryWanted by remember { mutableStateOf("Electronics") }
            var budgetStr by remember { mutableStateOf("") }
            var wantDesc by remember { mutableStateOf("") }
            var wantLocation by remember { mutableStateOf(viewModel.userRegion) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B241E)),
                border = BorderStroke(1.dp, Color(0xFF2E2E2E))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Campaign, null, tint = DelalaGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = viewModel.t("tabs_submit_request"),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showPostDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = productWanted,
                        onValueChange = { productWanted = it },
                        label = { Text(viewModel.t("product_wanted_model"), color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text(viewModel.t("wanted_model_placeholder")) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DelalaGreen,
                            unfocusedBorderColor = Color(0xFF4B5563)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("req_item_name"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(viewModel.t("category_wanted_label"), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                        listOf("Electronics", "Wearables", "Jewelry", "Perfume", "Cream", "Household Items", "Other").forEach { category ->
                            FilterChip(
                                selected = categoryWanted == category,
                                onClick = { categoryWanted = category },
                                label = { Text(getCatName(category)) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it },
                        label = { Text(viewModel.t("approx_budget_label"), color = Color.White.copy(alpha = 0.7f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DelalaGreen,
                            unfocusedBorderColor = Color(0xFF4B5563)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("req_item_budget"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = wantDesc,
                        onValueChange = { wantDesc = it },
                        label = { Text(viewModel.t("prod_desc_req_label"), color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DelalaGreen,
                            unfocusedBorderColor = Color(0xFF4B5563)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("req_item_desc"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val budget = budgetStr.toDoubleOrNull()
                            if (productWanted.isEmpty() || budget == null || wantDesc.isEmpty()) {
                                Toast.makeText(context, "Please initialize all inputs.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.postBuyerRequest(
                                    category = categoryWanted,
                                    productWanted = productWanted,
                                    budget = budget,
                                    description = wantDesc,
                                    location = wantLocation
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Wanted Request Posted!", Toast.LENGTH_SHORT).show()
                                        showPostDialog = false
                                        activeTab = 2 // Redirect directly to show listings!
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("req_post_button")
                    ) {
                        Text(viewModel.t("post_wanted_btn"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(
    listing: ListingEntity,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSaveToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${listing.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                AsyncImage(
                    model = listing.imageUri,
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Region label
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp, topEnd = 0.dp, bottomStart = 0.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = listing.location,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                // Save Bookmark Button (Accessibility 48dp target)
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.TopEnd)
                        .offset((-6).dp, 6.dp)
                ) {
                    IconButton(onClick = onSaveToggle) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save Offer",
                            tint = if (isSaved) DelalaGold else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = listing.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ETB %,.0f".format(listing.price),
                        color = DelalaGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified Broker",
                        tint = DelalaGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        listing.condition,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ==================== SCREEN 9 - PRODUCT DETAILS ====================
@Composable
fun ProductDetailsScreen(viewModel: DelalaViewModel, screen: Screen.ProductDetails) {
    val listing = screen.listing
    val context = LocalContext.current
    var isSaved = viewModel.savedListingIds.value.contains(listing.id)

    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    val getCatName = { c: String ->
        when (c) {
            "All" -> viewModel.t("cat_all")
            "Electronics" -> viewModel.t("cat_electronics")
            "Wearables" -> viewModel.t("cat_wearables")
            "Jewelry" -> viewModel.t("cat_jewelry")
            "Perfume" -> viewModel.t("cat_perfume")
            "Cream" -> viewModel.t("cat_cream")
            "Household Items" -> viewModel.t("cat_household")
            else -> viewModel.t("cat_other")
        }
    }

    val getCondName = { cond: String ->
        when (cond) {
            "New" -> viewModel.t("cond_new")
            "Medium Used" -> viewModel.t("cond_medium_used")
            else -> viewModel.t("cond_old")
        }
    }

    val getRegionName = { r: String ->
        when (r) {
            "All" -> viewModel.t("reg_all")
            "Dire Dawa" -> viewModel.t("reg_dire_dawa")
            "Moyale" -> viewModel.t("reg_moyale")
            else -> viewModel.t("reg_other")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            AsyncImage(
                model = listing.imageUri,
                contentDescription = listing.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Header elements overlay
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopEnd)
                    .offset((-12).dp, 12.dp)
            ) {
                IconButton(onClick = { viewModel.toggleSaveListing(listing.id) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) DelalaGold else Color.White
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                )
                Badge(containerColor = DelalaGreen) {
                    Text("ETB %,.2f".format(listing.price), color = DelalaWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Verified, "Verified", tint = DelalaGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    viewModel.t("verified_platform_badge"),
                    color = DelalaGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Condition & location items
            Row {
                SuggestionChip(onClick = {}, label = { Text("${viewModel.t("condition_prefix")}: ${getCondName(listing.condition)}") })
                Spacer(modifier = Modifier.width(8.dp))
                SuggestionChip(onClick = {}, label = { Text("${viewModel.t("region_prefix")}: ${getRegionName(listing.location)}") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(viewModel.t("description_label"), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                listing.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // Broker Contacts section
            Text(viewModel.t("seller_broker_info"), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = DelalaGold, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(listing.sellerName.take(1), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(listing.sellerName, fontWeight = FontWeight.Bold)
                        Text("${viewModel.t("seller_mobile_prefix")}: ${listing.sellerId}", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pre-Order Action Button
            Button(
                onClick = { viewModel.navigateTo(Screen.TryPlaceOrder(listing)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("pre_order_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.ShoppingCart, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.t("order_product_btn"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact Actions
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:${listing.sellerId}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("call_seller_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen)
                ) {
                    Icon(Icons.Filled.Call, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.t("call_seller_btn"))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        // WhatsApp Direct simulation link
                        val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=+251${listing.sellerId.takeLast(9)}")
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Filled.Chat, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.t("whatsapp_btn"), color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showReportDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("report_product_button"),
                border = BorderStroke(1.dp, DelalaRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DelalaRed)
            ) {
                Icon(Icons.Filled.Report, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(viewModel.t("report_scam_btn"))
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(viewModel.t("report_dialog_title")) },
            text = {
                Column {
                    Text(viewModel.t("report_dialog_instruct"))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text(viewModel.t("report_dialog_placeholder")) },
                        modifier = Modifier.fillMaxWidth().testTag("report_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isEmpty()) {
                            Toast.makeText(context, "Explanation required", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.fileAbuseReport(
                                targetType = "Listing",
                                targetId = listing.id.toString(),
                                targetName = listing.title,
                                reason = reportReason
                            ) { success ->
                                if (success) {
                                    Toast.makeText(context, "Item reported back to Admins! Sync queued.", Toast.LENGTH_LONG).show()
                                    showReportDialog = false
                                    reportReason = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DelalaRed)
                ) {
                    Text(viewModel.t("submit_report_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(viewModel.t("cancel_btn"))
                }
            }
        )
    }
}

// ==================== SCREEN 10 - TRUST DISCLAIMER & USER FEEDBACK ====================
@Composable
fun FeedbackAndNoticeScreen(viewModel: DelalaViewModel) {
    val context = LocalContext.current
    var rating by remember { mutableStateOf(5) }
    var suggestions by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // DISCLAIMER BLOCK
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DelalaRed.copy(alpha = 0.08f)),
            border = BorderStroke(2.dp, DelalaRed)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Alert",
                        tint = DelalaRed,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = viewModel.t("disclaimer_title"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = DelalaRed
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = viewModel.t("disclaimer_desc"),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // FEEDBACK BLOCK
        Text(
            text = "Rate your Delala Experience",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Tell us how we can safeguard commerce in Ethiopia.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Star rating bar (Accessibility 48dp target)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..5) {
                IconButton(
                    onClick = { rating = i },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "$i Stars",
                        tint = DelalaGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = suggestions,
            onValueChange = { suggestions = it },
            label = { Text("App suggestions or issues reported") },
            placeholder = { Text("How was your direct broker transaction?") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("feedback_suggestions_input"),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (suggestions.isEmpty()) {
                    Toast.makeText(context, "Feedback comment is required", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.submitUserFeedback(rating, suggestions) { success ->
                        if (success) {
                            Toast.makeText(context, "Thank you! Feedback synced with Supabase.", Toast.LENGTH_SHORT).show()
                            suggestions = ""
                            rating = 5
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_feedback_button")
        ) {
            Text(
                "Submit App Feedback",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ==================== ADMIN PANEL / ANALYTICS ====================
@Composable
fun AdminDashboardScreen(viewModel: DelalaViewModel) {
    val context = LocalContext.current
    val users by viewModel.usersState.collectAsState()
    val listings by viewModel.listingsState.collectAsState()
    val requests by viewModel.requestsState.collectAsState()
    val reports by viewModel.reportsState.collectAsState()
    val feedback by viewModel.feedbackState.collectAsState()

    // Supabase Orders fetched states
    val supabaseOrders by viewModel.supabaseOrders.collectAsState()
    val isFetchingOrders by viewModel.isFetchingSupabaseOrders.collectAsState()

    // Trigger initial load when admin logs in or screen opens
    LaunchedEffect(viewModel.userRole) {
        if (viewModel.userRole == "Admin") {
            viewModel.fetchSupabaseOrders()
        }
    }

    // Login state if not Admin role
    if (viewModel.userRole != "Admin") {
        var phoneVal by remember { mutableStateOf("") }
        var passcodeVal by remember { mutableStateOf("") }
        var isLoggingIn by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB)), // Cool light Apple Gray background
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern Minimalist Secure Logo
                    Surface(
                        modifier = Modifier.size(64.dp),
                        color = DelalaGreen.copy(alpha = 0.08f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Security lock",
                                tint = DelalaGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "COD SaaS Order Office",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "Enter secure administrator credentials ('admin' and 'admin') or use fast-track bypass below",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Close, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMessage ?: "", color = Color(0xFF991B1B), fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phoneVal,
                        onValueChange = {
                            phoneVal = it
                            errorMessage = null
                        },
                        label = { Text("Phone Number / Username") },
                        placeholder = { Text("e.g. admin or 0953348822") },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth().testTag("admin_login_phone_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passcodeVal,
                        onValueChange = {
                            passcodeVal = it
                            errorMessage = null
                        },
                        label = { Text("Passcode PIN") },
                        placeholder = { Text("e.g. admin or 1364") },
                        leadingIcon = { Icon(Icons.Filled.Style, null, tint = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth().testTag("admin_login_pin_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Standard credential Submission
                    Button(
                        onClick = {
                            if (phoneVal.isBlank()) {
                                errorMessage = "Please enter admin phone identifier"
                                return@Button
                            }
                            if (passcodeVal.isBlank()) {
                                errorMessage = "Please enter admin PIN"
                                return@Button
                            }
                            isLoggingIn = true
                            viewModel.loginUser(phoneVal, passcodeVal) { success, msg ->
                                isLoggingIn = false
                                if (!success) {
                                    errorMessage = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_auth_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Secure Login Now", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fast track bypass sandbox helper button
                    Button(
                        onClick = {
                            isLoggingIn = true
                            viewModel.loginUser("0953348822", "1364") { success, msg ->
                                isLoggingIn = false
                                if (success) {
                                    Toast.makeText(context, "Welcome Developer! Bypass login successful.", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("admin_bypass_sandbox_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fast-track Sandbox Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // Main Admin Dashboard Panel Once Authorized
        var activeTabMenu by remember { mutableStateOf(0) } // 0: COD Orders, 1: Platform Oversight, 2: Portal Settings
        var searchQueryInDashboard by remember { mutableStateOf("") }
        var selectedStatusFilter by remember { mutableStateOf("All") }
        var selectedCityFilter by remember { mutableStateOf("All") }

        // State used for order status editing dialog
        var editingOrder by remember { mutableStateOf<SupabaseOrder?>(null) }
        var updatingStatusInProgress by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB)) // Apple minimalist background
        ) {
            // Elegant Apple Top Navigation Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(DelalaGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = viewModel.appNameDynamic,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF111827)
                                )
                            }
                            Text(
                                text = "Apple-Style COD SaaS Order Desk",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }

                        // Logout button
                        IconButton(
                            onClick = {
                                viewModel.logoutAdmin()
                                Toast.makeText(context, "De-authorized session successfully", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .testTag("admin_dashboard_logout_btn")
                                .background(Color(0xFFF3F4F6), CircleShape)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Log out of Admin Portal", tint = Color(0xFFEF4444))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clean modern selector tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val subTabs = listOf("Direct COD Orders", "Platform Oversight", "Portal Settings")
                        val icons = listOf(Icons.Filled.ShoppingCart, Icons.Filled.VerifiedUser, Icons.Filled.Settings)
                        subTabs.forEachIndexed { i, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeTabMenu == i) DelalaGreen else Color(0xFFFAFAFA))
                                    .border(1.dp, if (activeTabMenu == i) DelalaGreen else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                                    .clickable { activeTabMenu = i }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icons[i],
                                        contentDescription = title,
                                        tint = if (activeTabMenu == i) Color.White else Color(0xFF4B5563),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTabMenu == i) Color.White else Color(0xFF4B5563)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tabs Content Panel
            when (activeTabMenu) {
                0 -> {
                    // Direct COD Orders
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // View Order Statistics Grid Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total statistics card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("TOTALS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${supabaseOrders.size}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFF111827)
                                    )
                                    Text("Direct orders", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                }
                            }

                            // Pending Card
                            val pendingCount = supabaseOrders.count { it.status.lowercase() == "pending" }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("PENDING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$pendingCount",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFFD97706)
                                    )
                                    Text("Awaiting checks", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                }
                            }

                            // Delivered success Card
                            val successCount = supabaseOrders.count {
                                val s = it.status.lowercase()
                                s == "completed" || s == "success" || s == "delivered"
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("COMPLETED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$successCount",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFF059669)
                                    )
                                    Text("Paid & Delivered", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Modern Light Apple Filters & Search Toolbox
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Search input line
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.Search, null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = searchQueryInDashboard,
                                        onValueChange = { searchQueryInDashboard = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_orders_search_field"),
                                        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF111827)),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            if (searchQueryInDashboard.isEmpty()) {
                                                Text("Search client name, phone or product...", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQueryInDashboard.isNotEmpty()) {
                                        IconButton(onClick = { searchQueryInDashboard = "" }, modifier = Modifier.size(18.dp)) {
                                            Icon(Icons.Filled.Close, "Clear search query", tint = Color(0xFF4B5563))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Status selective filters row
                                Text("Filter Status:", fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val states = listOf("All", "Pending", "Success / Completed", "Shipped", "Cancelled")
                                    states.forEach { state ->
                                        item {
                                            val isSelected = selectedStatusFilter == state
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) DelalaGreen.copy(alpha = 0.12f) else Color(0xFFF9FAFB))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) DelalaGreen else Color(0xFFE5E7EB),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedStatusFilter = state }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = state,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) DelalaGreen else Color(0xFF4B5563)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // City selective filters row
                                Text("Filter Locality City:", fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val cities = listOf("All", "Dire Dawa", "Moyale", "Ethiopia", "Other")
                                    cities.forEach { city ->
                                        item {
                                            val isSelected = selectedCityFilter == city
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF9FAFB))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color(0xFF0284C7) else Color(0xFFE5E7EB),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedCityFilter = city }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = city,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFF0369A1) else Color(0xFF4B5563)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Orders Logs Lists view
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtered Logs Database:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF374151)
                            )
                            IconButton(onClick = { viewModel.fetchSupabaseOrders() }) {
                                Icon(Icons.Filled.Refresh, "Refresh Supabase Database", tint = DelalaGreen)
                            }
                        }

                        if (isFetchingOrders) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = DelalaGreen)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Pinging Supabase backend REST API...", fontSize = 12.sp, color = Color(0xFF6B7280))
                                }
                            }
                        } else {
                            // Filter logic execution
                            val filteredList = supabaseOrders.filter { order ->
                                val matchSearch = searchQueryInDashboard.isEmpty() ||
                                        order.customerName.contains(searchQueryInDashboard, ignoreCase = true) ||
                                        order.phone.contains(searchQueryInDashboard, ignoreCase = true) ||
                                        order.productName.contains(searchQueryInDashboard, ignoreCase = true)

                                val matchStatus = when (selectedStatusFilter) {
                                    "Pending" -> order.status.lowercase() == "pending"
                                    "Success / Completed" -> {
                                        val s = order.status.lowercase()
                                        s == "completed" || s == "success" || s == "delivered"
                                    }
                                    "Shipped" -> order.status.lowercase() == "shipped" || order.status.lowercase() == "in transit"
                                    "Cancelled" -> order.status.lowercase() == "cancelled" || order.status.lowercase() == "failed"
                                    else -> true // All
                                }

                                val matchCity = when (selectedCityFilter) {
                                    "All" -> true
                                    "Dire Dawa" -> order.city.contains("Dire Dawa", ignoreCase = true)
                                    "Moyale" -> order.city.contains("Moyale", ignoreCase = true)
                                    "Ethiopia" -> order.country.contains("Ethiopia", ignoreCase = true) || order.city.contains("Ethiopia", ignoreCase = true)
                                    else -> !order.city.contains("Dire Dawa", ignoreCase = true) && !order.city.contains("Moyale", ignoreCase = true)
                                }

                                matchSearch && matchStatus && matchCity
                            }

                            if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.ShoppingCart, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("No matching COD orders found on Supabase", fontSize = 13.sp, color = Color(0xFF9CA3AF))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredList) { order ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { editingOrder = order },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                // Top line client name and status badge
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = order.customerName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = Color(0xFF111827)
                                                        )
                                                        Text("Tel: ${order.phone}", fontSize = 11.sp, color = Color(0xFF6B7280))
                                                    }

                                                    // Visual status pastel pill indicator
                                                    val statusBg = when (order.status.lowercase()) {
                                                        "pending" -> Color(0xFFFEF3C7) // Yellow pastel
                                                        "shipped", "in transit" -> Color(0xFFDBEAFE) // Blue pastel
                                                        "completed", "delivered", "success" -> Color(0xFFD1FAE5) // Green pastel
                                                        else -> Color(0xFFFEE2E2) // Red pastel
                                                    }
                                                    val statusText = when (order.status.lowercase()) {
                                                        "pending" -> Color(0xFFD97706)
                                                        "shipped", "in transit" -> Color(0xFF1E40AF)
                                                        "completed", "delivered", "success" -> Color(0xFF065F46)
                                                        else -> Color(0xFF991B1B)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(statusBg)
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = order.status.uppercase(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = statusText
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Middle line Product Purchased + variant
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Filled.ShoppingBag, null, tint = DelalaGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${order.productName} (x${order.quantity})",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF374151)
                                                    )
                                                    if (order.productVariant.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "[${order.productVariant}]",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF6B7280)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Delivery Address details line
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.LocationCity, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${order.address}, ${order.city} (${order.country})",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF4B5563)
                                                    )
                                                }

                                                if (order.notes.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "Notes: ${order.notes}",
                                                            fontSize = 11.sp,
                                                            fontStyle = FontStyle.Italic,
                                                            modifier = Modifier.padding(6.dp),
                                                            color = Color(0xFF4B5563)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Action triggers
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Click to Edit Status",
                                                        fontSize = 11.sp,
                                                        color = DelalaGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Edit, null, tint = DelalaGreen, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Platform Oversight (Original safety controls inside a beautifully framed scrollable box)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Oversight Matrix",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF111827)
                                )
                                Text("Anti-Fraud suspension dials and verification controls.", fontSize = 11.sp, color = Color(0xFF6B7280))
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))) {
                                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Users", fontSize = 10.sp, color = Color(0xFF4B5563))
                                            Text("${users.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))) {
                                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Offers", fontSize = 10.sp, color = Color(0xFF4B5563))
                                            Text("${listings.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))) {
                                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Reports", fontSize = 10.sp, color = Color(0xFF4B5563))
                                            Text("${reports.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (reports.isNotEmpty()) DelalaRed else Color.Unspecified)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger visual sub components
                        Text("1. platform user registers", style = MaterialTheme.typography.titleSmall, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        RegistersTable(users = users, listings = listings, requests = requests)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("2. platform flagged fraud logs", style = MaterialTheme.typography.titleSmall, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (reports.isEmpty()) {
                            Text("No flag reports. General security is GREEN.", fontSize = 12.sp, color = Color(0xFF6B7280))
                        } else {
                            reports.forEach { r ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Report #${r.id} | Target: ${r.targetType} (ID: ${r.targetId})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Reason: ${r.reason}", color = DelalaRed, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row {
                                            Button(
                                                onClick = { viewModel.deleteAdminReport(r.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Clear Flag", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Portal Settings (Customize the application name in settings + live logs view)
                    val scope = rememberCoroutineScope()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Portal Administration Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Modify App Branding properties and monitor database log activity live.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom App Name card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Portal Customize Brand",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This sets the application title displayed across the master dash headers",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                var brandNameInput by remember { mutableStateOf(viewModel.appNameDynamic) }

                                OutlinedTextField(
                                    value = brandNameInput,
                                    onValueChange = { brandNameInput = it },
                                    label = { Text("Application Custom Title") },
                                    placeholder = { Text("e.g. Delala Office Admin") },
                                    modifier = Modifier.fillMaxWidth().testTag("settings_custom_app_name_field"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (brandNameInput.isBlank()) {
                                            Toast.makeText(context, "App Title cannot be blank!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.updateAppName(brandNameInput)
                                            Toast.makeText(context, "Branding applied and persisted successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("apply_custom_app_name_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Verified, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply & Save App Name", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Database Sandbox Seeding Card
                        var isSeeding by remember { mutableStateOf(false) }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Database Sandbox Seeding",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Generate 5 realistic test COD orders on your Supabase instance to test dashboard statistics and filters.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        isSeeding = true
                                        val mockData = listOf(
                                            listOf("Abebe Kebede", "0911223344", "abebe@example.com", "Dire Dawa", "Kezira, H.No 45", "Ethiopia", "iPhone 15 Pro Max", "Titanium Gray, 256GB", "1", "Deliver in afternoon"),
                                            listOf("Sarah Jenkins", "0929876543", "sarah@example.com", "Moyale", "Green Valley St", "Ethiopia", "Samsung Galaxy S24 Ultra", "Black Obsidian, 512GB", "1", "Please call before dispatch"),
                                            listOf("Betty Tsegaye", "0944112233", "betty@example.com", "Dire Dawa", "Biherawi, Block 12", "Ethiopia", "Sony WH-1000XM5", "Silver", "2", "Fragile COD order"),
                                            listOf("Mohammed Ali", "0955113322", "mohammed@example.com", "Addis Ababa", "Bole Road, Central Mall", "Ethiopia", "Apple Watch Series 9", "Starlight Aluminium, 45mm", "1", "Check item on delivery"),
                                            listOf("Elena Vance", "0933778899", "elena@example.com", "Hawassa", "Lakefront Drive", "Ethiopia", "iPad Air", "Blue, 128GB Wifi", "1", "")
                                        )
                                        
                                        scope.launch {
                                            var count = 0
                                            mockData.forEach { list ->
                                                val done = com.example.data.SupabaseClient.insertOrder(
                                                    customerName = list[0],
                                                    phone = list[1],
                                                    email = list[2],
                                                    city = list[3],
                                                    address = list[4],
                                                    country = list[5],
                                                    productName = list[6],
                                                    productVariant = list[7],
                                                    quantity = list[8].toInt(),
                                                    notes = list[9]
                                                )
                                                if (done) count++
                                            }
                                            isSeeding = false
                                            viewModel.fetchSupabaseOrders()
                                            Toast.makeText(context, "Seeded $count mock orders directly to Supabase!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isSeeding,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isSeeding) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Seeding Supabase Table...")
                                    } else {
                                        Icon(Icons.Filled.AddCircle, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.0.dp))
                                        Text("Seed Mock Orders to Database", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Database status and Logs debug inspector card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Live Supabase Connection Activity",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF111827)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Database Ping logs processed regarding orders REST transactions",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Status block
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (viewModel.supabaseStatus.contains("success", ignoreCase = true) || viewModel.supabaseStatus.contains("submitted", ignoreCase = true)) DelalaGreen else Color(0xFFF59E0B))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Status: ${viewModel.supabaseStatus}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Supabase Sync logs:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(Color(0xFF1F2937), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    if (viewModel.supabaseLogs.isEmpty()) {
                                        Text("No sync logs processed yet.", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                                    } else {
                                        viewModel.supabaseLogs.forEach { logLine ->
                                            Text(
                                                text = logLine,
                                                color = Color(0xFF10B981),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Elegant Status Update Dialog Overlay If Triggered
        editingOrder?.let { order ->
            AlertDialog(
                onDismissRequest = { editingOrder = null },
                title = {
                    Text(
                        text = "Edit Order Status #${order.id}",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Customer: ${order.customerName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Product: ${order.productName}",
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563)
                        )
                        Text(
                            text = "Current Status: ${order.status.uppercase()}",
                            fontSize = 12.sp,
                            color = DelalaGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Select New COD Status:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val statuses = listOf("PENDING", "SHIPPED", "COMPLETED", "CANCELLED")
                        val colors = listOf(Color(0xFFD97706), Color(0xFF2563EB), Color(0xFF059669), Color(0xFFDC2626))

                        statuses.forEachIndexed { index, statusValue ->
                            Button(
                                onClick = {
                                    updatingStatusInProgress = true
                                    viewModel.updateSupabaseOrderStatus(order.id, statusValue.lowercase()) { success ->
                                        updatingStatusInProgress = false
                                        if (success) {
                                            Toast.makeText(context, "Status updated to $statusValue!", Toast.LENGTH_SHORT).show()
                                            editingOrder = null
                                        } else {
                                            Toast.makeText(context, "Supabase connection error. Try again!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(40.dp)
                                    .testTag("status_select_${statusValue.lowercase()}"),
                                colors = ButtonDefaults.buttonColors(containerColor = colors[index]),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !updatingStatusInProgress
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(statusValue, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { editingOrder = null },
                        enabled = !updatingStatusInProgress
                    ) {
                        Text(text = "Dismiss")
                    }
                }
            )
        }
    }
}


@Composable
fun ProgressBarChartItem(label: String, value: Int, maxVal: Int) {
    val progress = if (maxVal > 0) value.toFloat() / maxVal.toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp)
            Text("$value posts (${"%.0f".format(progress * 100)}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = DelalaGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun RegistersTable(
    users: List<UserEntity>,
    listings: List<ListingEntity>,
    requests: List<WantedRequestEntity>
) {
    Card(
        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DelalaGreen.copy(alpha = 0.12f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = "Database",
                    tint = DelalaGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registers Live Database View",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DelalaGreen
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Horizontal scrolling grid sheet container
            Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                Column {
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("Name", width = 130.dp, isHeader = true)
                        TableCell("Phone Number", width = 125.dp, isHeader = true)
                        TableCell("Email", width = 180.dp, isHeader = true)
                        TableCell("Seller/Buyer", width = 115.dp, isHeader = true)
                        TableCell("Place", width = 110.dp, isHeader = true)
                        TableCell("The Item Category", width = 140.dp, isHeader = true)
                        TableCell("Item", width = 160.dp, isHeader = true)
                        TableCell("Items Posted Before (Seller)", width = 220.dp, isHeader = true)
                        TableCell("The Item Wanted (Buyer)", width = 220.dp, isHeader = true)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)

                    // Data rows
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(users) { usr ->
                            val userListings = listings.filter { it.sellerId == usr.phone }
                            val userRequests = requests.filter { it.buyerId == usr.phone }

                            val activeCategory = if (usr.role == "Seller") {
                                userListings.firstOrNull()?.category ?: "N/A"
                            } else {
                                userRequests.firstOrNull()?.category ?: "N/A"
                            }

                            val activeItem = if (usr.role == "Seller") {
                                userListings.firstOrNull()?.title ?: "N/A (No active offers)"
                            } else {
                                userRequests.firstOrNull()?.productWanted ?: "N/A (No active requests)"
                            }

                            val postedBefore = if (usr.role == "Seller") {
                                if (userListings.isEmpty()) "None yet" else userListings.map { it.title }.joinToString(", ")
                            } else {
                                "N/A (Buyer)"
                            }

                            val wantedIfBuyer = if (usr.role == "Buyer") {
                                if (userRequests.isEmpty()) "None yet" else userRequests.map { it.productWanted }.joinToString(", ")
                            } else {
                                "N/A (Seller)"
                            }

                            Row(
                                modifier = Modifier
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell(usr.name, width = 130.dp, isHeader = false, isBold = true)
                                TableCell(usr.phone, width = 125.dp, isHeader = false)
                                TableCell(usr.email ?: "N/A", width = 180.dp, isHeader = false)
                                TableCell(usr.role, width = 115.dp, isHeader = false, isRoleBadge = true)
                                TableCell(usr.location, width = 110.dp, isHeader = false)
                                TableCell(activeCategory, width = 140.dp, isHeader = false)
                                TableCell(activeItem, width = 160.dp, isHeader = false)
                                TableCell(postedBefore, width = 220.dp, isHeader = false)
                                TableCell(wantedIfBuyer, width = 220.dp, isHeader = false)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    width: Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    isRoleBadge: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(end = 12.dp)
    ) {
        if (isRoleBadge) {
            val containerColor = if (text == "Seller") DelalaGreen.copy(alpha = 0.15f) else if (text == "Buyer") DelalaGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            val textColor = if (text == "Seller") DelalaGreen else if (text == "Buyer") DelalaGold else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(containerColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        } else {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isHeader) FontWeight.Black else if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaceOrderScreen(viewModel: DelalaViewModel, screen: Screen.TryPlaceOrder) {
    val listing = screen.listing
    val context = LocalContext.current

    // Fields state tracking
    var customerName by remember { mutableStateOf(viewModel.currentUser?.name ?: "") }
    var phone by remember { mutableStateOf(viewModel.currentUser?.phone ?: "") }
    var email by remember { mutableStateOf(viewModel.currentUser?.email ?: "") }
    var city by remember { mutableStateOf(viewModel.currentUser?.location ?: viewModel.userRegion) }
    var address by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Ethiopia") }
    var productName by remember { mutableStateOf(listing.title) }
    var productVariant by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    // Submission states
    var isSending by remember { mutableStateOf(false) }
    var submissionSuccess by remember { mutableStateOf<Boolean?>(null) }

    // Validation errors state
    var customerNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var countryError by remember { mutableStateOf<String?>(null) }
    var productNameError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // High level custom Screen Header
        Text(
            text = viewModel.t("order_form_title"),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Ethiopian Trusted Direct Broker Connection",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Product Quick Info Card Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = listing.imageUri,
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(listing.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text("Listed by: ${listing.sellerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ETB %,.2f".format(listing.price),
                        color = DelalaGreen,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // State Banners Feedback Panel
        submissionSuccess?.let { success ->
            if (success) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("order_success_banner"),
                    colors = CardDefaults.cardColors(containerColor = DelalaGreen.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, DelalaGreen)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = DelalaGreen,
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = viewModel.t("order_success_header"),
                                fontWeight = FontWeight.Black,
                                color = DelalaGreen,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = viewModel.t("order_success_msg"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("order_error_banner"),
                    colors = CardDefaults.cardColors(containerColor = DelalaRed.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, DelalaRed)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = DelalaRed,
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Error, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = viewModel.t("order_error_header"),
                                fontWeight = FontWeight.Black,
                                color = DelalaRed,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = viewModel.t("order_error_msg"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Customer Info Title block
        Text("1. Customer Delivery Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(10.dp))

        // Full Name Field
        OutlinedTextField(
            value = customerName,
            onValueChange = {
                customerName = it
                customerNameError = null
            },
            label = { Text(viewModel.t("customer_name_label")) },
            leadingIcon = { Icon(Icons.Filled.Person, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_customer_name_field"),
            isError = customerNameError != null,
            singleLine = true,
            enabled = !isSending
        )
        customerNameError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Phone Number Field
        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                phoneError = null
            },
            label = { Text(viewModel.t("phone_label")) },
            leadingIcon = { Icon(Icons.Filled.Phone, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_customer_phone_field"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneError != null,
            singleLine = true,
            enabled = !isSending
        )
        phoneError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text(viewModel.t("email_label")) },
            leadingIcon = { Icon(Icons.Filled.Email, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_customer_email_field"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError != null,
            singleLine = true,
            enabled = !isSending
        )
        emailError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // City & Location Field
        OutlinedTextField(
            value = city,
            onValueChange = {
                city = it
                cityError = null
            },
            label = { Text(viewModel.t("city_label")) },
            leadingIcon = { Icon(Icons.Filled.LocationCity, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_customer_city_field"),
            isError = cityError != null,
            singleLine = true,
            enabled = !isSending
        )
        cityError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Delivery address field
        OutlinedTextField(
            value = address,
            onValueChange = {
                address = it
                addressError = null
            },
            label = { Text(viewModel.t("address_label")) },
            leadingIcon = { Icon(Icons.Filled.PinDrop, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_customer_address_field"),
            isError = addressError != null,
            enabled = !isSending
        )
        addressError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Country Field
        OutlinedTextField(
            value = country,
            onValueChange = {
                country = it
                countryError = null
            },
            label = { Text(viewModel.t("country_label")) },
            leadingIcon = { Icon(Icons.Filled.Public, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_customer_country_field"),
            isError = countryError != null,
            singleLine = true,
            enabled = !isSending
        )
        countryError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Product Customization Title block
        Text("2. Product Customization", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(10.dp))

        // Product Name Field
        OutlinedTextField(
            value = productName,
            onValueChange = {
                productName = it
                productNameError = null
            },
            label = { Text(viewModel.t("product_name_label")) },
            leadingIcon = { Icon(Icons.Filled.ShoppingBag, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_product_name_field"),
            isError = productNameError != null,
            singleLine = true,
            enabled = !isSending
        )
        productNameError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Material / Product Variant field
        OutlinedTextField(
            value = productVariant,
            onValueChange = { productVariant = it },
            label = { Text(viewModel.t("product_variant_label")) },
            placeholder = { Text("e.g. Blue Color, Size Large, 128GB") },
            leadingIcon = { Icon(Icons.Filled.Style, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_product_variant_field"),
            singleLine = true,
            enabled = !isSending
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quantity Field
        OutlinedTextField(
            value = quantityStr,
            onValueChange = {
                quantityStr = it
                quantityError = null
            },
            label = { Text(viewModel.t("quantity_label")) },
            leadingIcon = { Icon(Icons.Filled.Filter9Plus, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().testTag("order_product_quantity_field"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = quantityError != null,
            singleLine = true,
            enabled = !isSending
        )
        quantityError?.let {
            Text(it, color = DelalaRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notes and Special instructions
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(viewModel.t("notes_label")) },
            placeholder = { Text("e.g. Please deliver after 5:00 PM or call me prior") },
            leadingIcon = { Icon(Icons.Filled.Sms, null, tint = DelalaGreen) },
            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("order_product_notes_field"),
            enabled = !isSending
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Trigger Order Submit button or Spinner Loader
        if (isSending) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = DelalaGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(viewModel.t("sending_order_loading"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Button(
                onClick = {
                    // Check and execute Validation before trigger
                    var isValid = true

                    if (customerName.isBlank()) {
                        customerNameError = viewModel.t("customer_name_error")
                        isValid = false
                    }
                    if (phone.isBlank() || phone.length < 9) {
                        phoneError = viewModel.t("phone_error")
                        isValid = false
                    }
                    if (city.isBlank()) {
                        cityError = viewModel.t("city_error")
                        isValid = false
                    }
                    if (address.isBlank()) {
                        addressError = viewModel.t("address_error")
                        isValid = false
                    }
                    if (country.isBlank()) {
                        countryError = viewModel.t("country_error")
                        isValid = false
                    }
                    if (productName.isBlank()) {
                        productNameError = viewModel.t("product_name_error")
                        isValid = false
                    }
                    
                    val qty = quantityStr.toIntOrNull() ?: 0
                    if (qty < 1) {
                        quantityError = viewModel.t("quantity_error")
                        isValid = false
                    }

                    if (isValid) {
                        isSending = true
                        submissionSuccess = null
                        
                        viewModel.submitOrder(
                            customerName = customerName,
                            phone = phone,
                            email = email,
                            city = city,
                            address = address,
                            country = country,
                            productName = productName,
                            productVariant = productVariant,
                            quantity = qty,
                            notes = notes
                        ) { success ->
                            isSending = false
                            submissionSuccess = success
                            if (success) {
                                Toast.makeText(context, "Order submitted to Supabase successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Database connection error. Try again!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fix the red form validation errors", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_order_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.DoneOutline, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.t("submit_order_btn"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

