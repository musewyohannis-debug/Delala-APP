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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
                            .height(180.dp)
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
                placeholder = { Text("e.g. 0912345678") },
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
                        viewModel.loginUser(phone) { success, msg ->
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
    var imageUri by remember { mutableStateOf("https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=450&auto=format&fit=crop") }

    val categories = listOf("Electronics", "Wearables", "Jewelry", "Perfume", "Cream", "Household Items", "Other")

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
                        text = "Welcome, ${viewModel.currentUser?.name ?: "Seller"}",
                        color = DelalaWhite,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Your store is associated with Region: ${viewModel.userRegion}",
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
                Text(if (expandedForm) "Collapse Form" else viewModel.t("post_listing"))
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
                            text = "New Marketplace Offer",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = DelalaGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Product Title / Model") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_title"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Dropdown Simulator
                        Text("Category:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Condition selectors
                        Text("Condition:", style = MaterialTheme.typography.labelMedium)
                        Row {
                            listOf("New", "Medium Used", "Old").forEach { cond ->
                                FilterChip(
                                    selected = condition == cond,
                                    onClick = { condition = cond },
                                    label = { Text(cond) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price (ETB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("add_product_price"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Detailed Description") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_desc"),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = contactNo,
                            onValueChange = { contactNo = it },
                            label = { Text("Contact Phone Number") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_phone"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val price = priceStr.toDoubleOrNull()
                                if (title.isEmpty() || price == null || description.isEmpty() || contactNo.isEmpty()) {
                                    Toast.makeText(context, "Please configure all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.promoteListing(
                                        category = selectedCategory,
                                        title = title,
                                        description = description,
                                        condition = condition,
                                        price = price,
                                        imageUri = imageUri,
                                        location = location,
                                        contact = contactNo
                                    ) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Listing Posted! Auto-sync to Supabase triggered.", Toast.LENGTH_SHORT).show()
                                            // Reset form fields
                                            title = ""
                                            priceStr = ""
                                            description = ""
                                            expandedForm = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                            modifier = Modifier.fillMaxWidth().testTag("add_product_submit_button")
                        ) {
                            Text("Post Marketplace Listing")
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

    val context = LocalContext.current
    val listings by viewModel.listingsState.collectAsState()
    val requests by viewModel.requestsState.collectAsState()

    // Filters values
    val categories = listOf("All", "Electronics", "Wearables", "Jewelry", "Perfume", "Cream", "Household Items", "Other")
    val regions = listOf("All", "Dire Dawa", "Moyale", "Other")

    // Filter Logic
    val filteredListings = listings.filter { listing ->
        val matchesSearch = listing.title.contains(viewModel.searchQuery, ignoreCase = true) ||
                listing.description.contains(viewModel.searchQuery, ignoreCase = true)
        val matchesCat = viewModel.selectedCategoryFilter == "All" || listing.category == viewModel.selectedCategoryFilter
        val matchesReg = viewModel.selectedRegionFilter == "All" || listing.location == viewModel.selectedRegionFilter
        val matchesPrice = listing.price <= viewModel.maxPriceFilter
        matchesSearch && matchesCat && matchesReg && matchesPrice
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeTab
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Browse Products") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Submit Request") })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Wanted Board") })
        }

        if (activeTab == 0) {
            // FILTER & SEARCH LAYER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text("Search electronic, perfume, shoes...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("buyer_search_bar"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Category filters
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = viewModel.selectedCategoryFilter == cat,
                            onClick = { viewModel.selectedCategoryFilter = cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DelalaGreen,
                                selectedLabelColor = DelalaWhite
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reg:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DelalaGreen)
                    regions.forEach { reg ->
                        FilterChip(
                            selected = viewModel.selectedRegionFilter == reg,
                            onClick = { viewModel.selectedRegionFilter = reg },
                            label = { Text(reg) },
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
                            "No matching listings found.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "Try clearing tags or posting a custom request!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
        } else if (activeTab == 1) {
            // SCREEN 8 POST CUSTOM WANTED REQUESTS FORM
            var productWanted by remember { mutableStateOf("") }
            var categoryWanted by remember { mutableStateOf("Electronics") }
            var budgetStr by remember { mutableStateOf("") }
            var wantDesc by remember { mutableStateOf("") }
            var wantLocation by remember { mutableStateOf(viewModel.userRegion) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DelalaGold.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, DelalaGold)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, null, tint = DelalaGold, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Post custom product wanted requests!", fontWeight = FontWeight.Bold)
                            Text("If details aren't found in feed, local brokers could call you directly.", fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = productWanted,
                    onValueChange = { productWanted = it },
                    label = { Text("Product Wanted Model") },
                    placeholder = { Text("e.g. Samsung A54 or Fryer") },
                    modifier = Modifier.fillMaxWidth().testTag("req_item_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category Wanted:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Electronics", "Wearables", "Jewelry", "Home Furnishings", "Other").forEach { category ->
                        FilterChip(
                            selected = categoryWanted == category,
                            onClick = { categoryWanted = category },
                            label = { Text(category) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Approximate Budget (ETB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("req_item_budget"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = wantDesc,
                    onValueChange = { wantDesc = it },
                    label = { Text("Product Description & Requirements") },
                    modifier = Modifier.fillMaxWidth().testTag("req_item_desc"),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(28.dp))

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
                                    productWanted = ""
                                    budgetStr = ""
                                    wantDesc = ""
                                    activeTab = 2 // Redirect directly to show listings!
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("req_post_button")
                ) {
                    Text("Post Wanted Request")
                }
            }
        } else {
            // WANTED REQUESTS BOARD
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (requests.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.SpeakerNotes, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No wanted requests on the board.")
                        }
                    }
                } else {
                    items(requests) { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(req.productWanted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Badge(containerColor = DelalaGold, contentColor = Color.Black) {
                                        Text("ETB %,.0f".format(req.budget))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Posted by: ${req.buyerName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("Region: ${req.location} | Category: ${req.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(req.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
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
                    "Verified Delala Connection Platform",
                    color = DelalaGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Condition & location items
            Row {
                SuggestionChip(onClick = {}, label = { Text("Condition: ${listing.condition}") })
                Spacer(modifier = Modifier.width(8.dp))
                SuggestionChip(onClick = {}, label = { Text("Region: ${listing.location}") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Description:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
            Text("Seller Broker Information:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                        Text("Seller Mobile: ${listing.sellerId}", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

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
                    Text("Call Seller")
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
                    Text("WhatsApp", color = Color.White)
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
                Text("Report Suspicious Product / Scam")
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("File Abuse Report on Item") },
            text = {
                Column {
                    Text("Please explain why this product listing is fake or highly suspicious:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("e.g. Asking for money prior to product check") },
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
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
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

    var activeSubTab by remember { mutableStateOf(0) } // 0: Reports & Verification, 1: Analytics

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Delala Admin Oversight Board",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = DelalaGreen
        )
        Text("Anti-Fraud suspension dials and verification controls.", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // High-level statistics block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Users", fontSize = 11.sp)
                    Text("${users.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Offers", fontSize = 11.sp)
                    Text("${listings.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Wanted", fontSize = 11.sp)
                    Text("${requests.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = if (reports.isNotEmpty()) DelalaRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Reports", fontSize = 11.sp, color = if (reports.isNotEmpty()) DelalaRed else MaterialTheme.colorScheme.onSurface)
                    Text("${reports.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (reports.isNotEmpty()) DelalaRed else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = activeSubTab) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }, text = { Text("Flagged Items") })
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }, text = { Text("Activity Analytics") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubTab == 0) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Abuse & Scam Alerts:", style = MaterialTheme.typography.titleSmall, color = DelalaRed)
                }
                if (reports.isEmpty()) {
                    item {
                        Text(
                            "No user reports filed at the moment. Platform safety parameters green.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(reports) { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            border = BorderStroke(1.dp, DelalaRed.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Report #${r.id} | Target: ${r.targetType} (ID ID: ${r.targetId})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Reason: ${r.reason}", color = DelalaRed, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    Button(
                                        onClick = {
                                            viewModel.deleteAdminReport(r.id)
                                            Toast.makeText(context, "Report cleared/resolved successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DelalaGreen),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Dismiss", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    if (r.targetType == "Listing") {
                                        Button(
                                            onClick = {
                                                val idInt = r.targetId.toIntOrNull()
                                                if (idInt != null) {
                                                    viewModel.removeListing(idInt)
                                                    viewModel.deleteAdminReport(r.id)
                                                    Toast.makeText(context, "Fraud listing completely removed!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DelalaRed),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Remove Product", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Seller Profiles Verification:", style = MaterialTheme.typography.titleSmall)
                }

                if (users.isEmpty()) {
                    item {
                        Text("No users found.", fontSize = 12.sp)
                    }
                } else {
                    items(users) { usr ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(usr.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Phone: ${usr.phone} | Role: ${usr.role}", fontSize = 11.sp)
                                    Text(
                                        text = if (usr.verified) "Status: VERIFIED BADGE" else "Status: UNVERIFIED",
                                        fontSize = 11.sp,
                                        color = if (usr.verified) DelalaGreen else DelalaGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { viewModel.toggleUserVerification(usr.phone, !usr.verified) }
                                    ) {
                                        Icon(
                                            imageVector = if (usr.verified) Icons.Filled.VerifiedUser else Icons.Filled.Verified,
                                            contentDescription = "Verify User",
                                            tint = if (usr.verified) DelalaGreen else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.banUserProfile(usr.phone)
                                            Toast.makeText(context, "User banned from platform!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Block,
                                            contentDescription = "Ban User",
                                            tint = DelalaRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Analytics view displaying categories & regions distribution
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Marketplace Indicators", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Region Activity Shares:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            val direDawaCount = listings.filter { it.location == "Dire Dawa" }.size
                            val moyaleCount = listings.filter { it.location == "Moyale" }.size
                            val otherCount = listings.filter { it.location == "Other" }.size

                            ProgressBarChartItem(label = "Dire Dawa Hub", value = direDawaCount, maxVal = listings.size)
                            ProgressBarChartItem(label = "Moyale Border Crossing", value = moyaleCount, maxVal = listings.size)
                            ProgressBarChartItem(label = "Other Localities", value = otherCount, maxVal = listings.size)
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Supply Category Counts:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            val categoriesCount = listings.groupBy { it.category }.mapValues { it.value.size }

                            categoriesCount.forEach { (cat, count) ->
                                ProgressBarChartItem(label = cat, value = count, maxVal = listings.size)
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("User Satisfaction Scores:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (feedback.isEmpty()) {
                                Text("No suggestions or satisfaction metrics posted yet.")
                            } else {
                                val averageRating = feedback.map { it.rating }.average()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("%.1f".format(averageRating), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DelalaGold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Out of 5 Stars average", fontWeight = FontWeight.Bold)
                                        Text("Calculated from ${feedback.size} reviews.", fontSize = 11.sp)
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
