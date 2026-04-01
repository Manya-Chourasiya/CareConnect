package com.example.careconnect

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.careconnect.ui.*
import com.example.careconnect.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    lateinit var tts: TextToSpeech
    lateinit var database: AppDatabase
    lateinit var repository: CareConnectRepository
    lateinit var viewModel: CareConnectViewModel

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "careconnect_db"
        ).fallbackToDestructiveMigration().build()

        repository = CareConnectRepositoryImpl(
            database.userDao(),
            database.appointmentDao(),
            database.medicalRecordDao(),
            database.prescriptionDao()
        )
        viewModel = CareConnectViewModel(repository)

        // Auto-login a default user for simplicity
        viewModel.login("user@careconnect.com", UserRole.PATIENT)

        requestPermissions()

        enableEdgeToEdge()
        setContent {
            CareConnectTheme {
                MainScreen(database, viewModel, tts)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(android.Manifest.permission.RECORD_AUDIO)
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase, viewModel: CareConnectViewModel, tts: TextToSpeech) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            if (currentDestination != "splash" && currentDestination != null) {
                CareTopAppBar(navController)
            }
        },
        bottomBar = {
            if (currentDestination != "splash" && currentDestination != "voice" && currentDestination != null) {
                CareBottomNavigation(navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(database, viewModel, navController, tts)
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        // Decorative blobs
        Box(
            modifier = Modifier
                .offset(x = 150.dp, y = (-200).dp)
                .size(300.dp)
                .alpha(0.1f)
                .blur(80.dp)
                .background(PrimaryFixed, CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(x = (-150).dp, y = 300.dp)
                .size(400.dp)
                .alpha(0.15f)
                .blur(100.dp)
                .background(SecondaryFixed, CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Primary, PrimaryContainer))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "CareConnect",
                style = MaterialTheme.typography.displayMedium,
                color = Primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Your gentle companion in care.",
                style = MaterialTheme.typography.titleLarge,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.8f)
            )

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotAnimation(delayUnit = 0)
                DotAnimation(delayUnit = 300)
                DotAnimation(delayUnit = 600)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "ENTERING SANCTUARY",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurface,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(0.4f),
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Secure & Private Connection",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DotAnimation(delayUnit: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, delayMillis = delayUnit, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(alpha)
            .background(Secondary, CircleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareTopAppBar(navController: NavHostController) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "CareConnect",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate("medical") }) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Primary, modifier = Modifier.size(32.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Background
        )
    )
}

@Composable
fun CareBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(100.dp).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "medicine",
            onClick = { navController.navigate("medicine") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Reminders") },
            label = { Text("Reminders") }
        )
        NavigationBarItem(
            selected = currentRoute == "contacts",
            onClick = { navController.navigate("contacts") },
            icon = { Icon(Icons.Default.ContactPhone, contentDescription = "Contacts") },
            label = { Text("Contacts") }
        )
        NavigationBarItem(
            selected = currentRoute == "sos",
            onClick = { navController.navigate("sos") },
            icon = { Icon(Icons.Default.Emergency, contentDescription = "SOS") },
            label = { Text("SOS") }
        )
    }
}

@Composable
fun HomeScreen(navController: NavHostController, viewModel: CareConnectViewModel, database: AppDatabase, tts: TextToSpeech) {
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val command = resultList?.get(0)?.lowercase() ?: ""
            handleVoiceCommand(command, navController, database, tts, context)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            VoicePulseButton {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command")
                }
                voiceLauncher.launch(intent)
            }
        }

        Text(
            "Tap or speak your command",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "\"Call my daughter\" or \"Add medicine Vitamin D at 9:00 AM\"",
            style = MaterialTheme.typography.bodyLarge,
            color = Outline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BentoCard(
                modifier = Modifier.weight(1f),
                title = "Medical Info",
                subtitle = "Blood type A+, Allergies",
                icon = Icons.Default.MedicalInformation,
                containerColor = SecondaryContainer,
                contentColor = OnSecondaryContainer,
                onClick = { navController.navigate("medical") }
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Contacts",
                    subtitle = "Emergency Contacts",
                    icon = Icons.Default.ContactPhone,
                    containerColor = PrimaryFixed,
                    contentColor = Primary,
                    onClick = { navController.navigate("contacts") }
                )
                BentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Reminders",
                    subtitle = "Medication",
                    icon = Icons.Default.AddCircle,
                    containerColor = TertiaryContainer,
                    contentColor = OnTertiaryContainer,
                    onClick = { navController.navigate("medicine") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("sos") },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Error),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.EmergencyShare, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("SOS EMERGENCY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun VoicePulseButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .border(1.dp, Primary.copy(alpha = 0.1f), CircleShape)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    Brush.linearGradient(listOf(Primary, PrimaryContainer)),
                    CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = OnPrimary,
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 18.sp)
                if (subtitle != null) {
                    Text(subtitle, color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

fun handleVoiceCommand(command: String, navController: NavHostController, database: AppDatabase, tts: TextToSpeech, context: Context) {
    Log.d("VoiceCommand", "Received: $command")
    when {
        command.contains("add medicine") || command.contains("remind me to take") -> {
            val regex = Regex("(add medicine|remind me to take) (.+) at (\\d{1,2}:?\\d{0,2}\\s*(am|pm))")
            val match = regex.find(command)
            
            if (match != null) {
                val medicineName = match.groupValues[2].trim()
                var timeString = match.groupValues[3].trim().uppercase()
                
                if (!timeString.contains(":")) {
                    timeString = if (timeString.contains("AM")) timeString.replace("AM", ":00 AM") else timeString.replace("PM", ":00 PM")
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val medicine = Medicine(name = medicineName, time = timeString)
                    database.medicineDao().insert(medicine)
                    val triggerTime = convertToMillis(timeString)
                    scheduleNotification(context, "Medicine Reminder", "Take $medicineName", triggerTime)
                    
                    launch(Dispatchers.Main) {
                        tts.speak("Medicine $medicineName added for $timeString", TextToSpeech.QUEUE_FLUSH, null, null)
                        Toast.makeText(context, "Reminder added for $medicineName at $timeString", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                tts.speak("I couldn't understand. Please say add medicine name at time like 9:00 AM", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
        command.contains("reminder") -> {
            tts.speak("Opening reminders", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("medicine")
        }
        command.contains("help") || command.contains("sos") -> {
            tts.speak("Opening emergency screen", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("sos")
        }
        command.contains("contact") -> {
            tts.speak("Opening contacts", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("contacts")
        }
        command.contains("medical") || command.contains("profile") -> {
            tts.speak("Opening medical information", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("medical")
        }
        else -> {
            tts.speak("Sorry, I did not understand. Try saying add medicine name at time.", TextToSpeech.QUEUE_FLUSH, null, null)
            Toast.makeText(context, "Command not recognized: $command", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppNavigation(database: AppDatabase, viewModel: CareConnectViewModel, navController: NavHostController, tts: TextToSpeech) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("home") {
            HomeScreen(navController, viewModel, database, tts)
        }
        composable("medicine") {
            MedicineScreen(database = database)
        }
        composable("sos") {
            SOSScreen()
        }
        composable("contacts") {
            ContactsScreen()
        }
        composable("medical") {
            MedicalScreen()
        }
    }
}

@Composable
fun MedicineScreen(database: AppDatabase) {
    var medicineName by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("09:00 AM") }
    val context = LocalContext.current
    val medicines = remember { mutableStateListOf<Medicine>() }

    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            selectedTime = String.format("%02d:%02d %s", hour, minute, amPm)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    LaunchedEffect(Unit) {
        database.medicineDao().getAllMedicines().collect { list ->
            medicines.clear()
            medicines.addAll(list)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Text("Add Reminder", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Medicine name", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    placeholder = { Text("e.g. Vitamin D") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Primary.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Set time", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White, CircleShape)
                        .clickable { timePickerDialog.show() }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(selectedTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (medicineName.isNotBlank()) {
                    val name = medicineName
                    val timeValue = selectedTime
                    CoroutineScope(Dispatchers.Main).launch {
                        val medicine = Medicine(name = name, time = timeValue)
                        database.medicineDao().insert(medicine)
                        val triggerTime = convertToMillis(timeValue)
                        scheduleNotification(context, "Medicine Reminder", "Take $name at $timeValue", triggerTime)
                    }
                    medicineName = ""
                    Toast.makeText(context, "Reminder saved!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Saved Medicines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(medicines) { medicine ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(48.dp).background(SecondaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = OnSecondaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(medicine.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(medicine.time, color = Outline)
                            }
                        }
                        IconButton(onClick = { CoroutineScope(Dispatchers.IO).launch { database.medicineDao().delete(medicine) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SOSScreen() {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale"
    )

    var currentAddress by remember { mutableStateOf("Fetching Indian location...") }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location: Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (location != null) {
                    val geocoder = Geocoder(context, Locale("en", "IN"))
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        currentAddress = addresses[0].getAddressLine(0)
                    }
                } else {
                    currentAddress = "7, Lok Kalyan Marg, New Delhi, Delhi 110011, India" 
                }
            } catch (e: Exception) {
                currentAddress = "Location not available"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Emergency Help", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Press or say \"Help\"", color = Outline, modifier = Modifier.padding(bottom = 48.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(Error.copy(alpha = 0.1f), CircleShape)
            )
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:112") }
                    context.startActivity(intent)
                },
                modifier = Modifier.size(240.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmergencyShare, contentDescription = null, modifier = Modifier.size(80.dp))
                    Text("SOS", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Your Current Location", color = Outline, fontSize = 12.sp)
                    Text(currentAddress, fontWeight = FontWeight.Bold, maxLines = 3)
                }
            }
        }
    }
}

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Your Network", color = Primary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                Text("Contacts", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { 
                    val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                    contactLauncher.launch(intent)
                },
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add contact")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Secondary)
        ) {
            Icon(Icons.Default.Contacts, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Phone Contacts")
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Emergency Contacts", color = Outline, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp).background(SecondaryFixed, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = OnSecondaryContainer, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sarah Miller", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Daughter", color = Outline)
                }
                IconButton(
                    onClick = { 
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:100"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(56.dp).background(PrimaryFixed, CircleShape)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Primary)
                }
            }
        }
    }
}

@Composable
fun MedicalScreen() {
    var name by remember { mutableStateOf("John Doe") }
    var bloodGroup by remember { mutableStateOf("A+") }
    var allergies by remember { mutableStateOf("Peanuts, Penicillin") }
    var medications by remember { mutableStateOf("Vitamin D3, Lisinopril") }

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Text("Medical Profile", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Keep your vital information updated.", color = Outline)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                EditableMedicalField(label = "Name", value = name, onValueChange = { name = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditableMedicalField(modifier = Modifier.weight(1f), label = "Blood group", value = bloodGroup, onValueChange = { bloodGroup = it })
                    EditableMedicalField(modifier = Modifier.weight(1f), label = "Allergies", value = allergies, onValueChange = { allergies = it })
                }
                EditableMedicalField(label = "Chronic Medications", value = medications, onValueChange = { medications = it }, isTextArea = true)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { /* Save logic */ },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = CircleShape
        ) {
            Text("Save Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EditableMedicalField(modifier: Modifier = Modifier, label: String, value: String, onValueChange: (String) -> Unit, isTextArea: Boolean = false) {
    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isTextArea) 120.dp else 56.dp),
            shape = RoundedCornerShape(if (isTextArea) 24.dp else 28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Primary.copy(alpha = 0.5f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

fun scheduleNotification(context: Context, title: String, message: String, triggerTime: Long) {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("message", message)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}

fun convertToMillis(time: String): Long {
    val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return try {
        val date = format.parse(time)
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        if (date != null) {
            val temp = Calendar.getInstance()
            temp.time = date
            calendar.set(java.util.Calendar.HOUR_OF_DAY, temp.get(java.util.Calendar.HOUR_OF_DAY))
            calendar.set(java.util.Calendar.MINUTE, temp.get(java.util.Calendar.MINUTE))
            calendar.set(java.util.Calendar.SECOND, 0)
            if (calendar.before(now)) calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        calendar.timeInMillis
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
