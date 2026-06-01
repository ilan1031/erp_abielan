package com.example.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.GeminiService
import com.example.service.AlarmSoundManager
import com.example.service.NotificationActionReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Auth user state container
data class AuthState(
    val isLoggedIn: Boolean = false,
    val companyName: String = "Enterprise Corp",
    val ownerName: String = "Abielan Admin",
    val email: String = "admin@abielan.erp",
    val mobileNumber: String = "+91 9876543210",
    val gstNumber: String = "27AAAAA0000A1Z1",
    val businessType: String = "Consultancy & Tech Services",
    val planSelected: String = "Free Trial",
    val subscriptionExpiry: String = "N/A",
    val rolesAndPermissions: String = "Super Administrator - All Permissions Enabled"
)

// Main ERP State
data class ErpUiState(
    val auth: AuthState = AuthState(),
    val activeTab: String = "Dashboard", // "Dashboard", "Sales", "Finance", "Reports", "Settings"
    val isDarkMode: Boolean = false,
    val isSetupCompleted: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false,
    val isListGridToggle: Boolean = true, // true = List, false = Grid
    val searchQuery: String = "",
    val activeFilterTab: String = "ALL", // "ALL", "QUOTE", "INVOICE", "BILL", "PO", "SO"
    val activeSortMode: String = "DATE_DESC", // "DATE_DESC", "DATE_ASC", "AMOUNT_DESC", "AMOUNT_ASC"
    val showAuthScreen: String = "SPLASH", // "SPLASH", "LOGIN", "SIGNUP", "PLANS"
    val activeChartType: String = "BAR", // "BAR", "PIE", "LINE", "AREA", "DONUT"
    val activeTimeFilter: String = "Monthly", // "Daily", "Weekly", "Monthly", "Yearly"
    val razorpaySimulating: Boolean = false,
    val razorpaySuccessMessage: String? = null,
    
    // Timer details
    val activeSoTimer: BusinessDocument? = null, // The SO card currently running a timer
    val showTimerCompletionDialog: Boolean = false,
    val timerCompletionMessage: String = "",
    val timerCompletionSoId: Int? = null,
    
    // AI Panel
    val aiPrompt: String = "",
    val aiResponse: String = "",
    val aiLoading: Boolean = false,
    
    // Notifications State array
    val systemNotifications: List<String> = listOf(
        "Welcome to Abielan ERP! Enter your profile details today.",
        "Tip: Check the Sales Orders tab to track live hourly billable consulting jobs."
    ),
    val continuationDurationMinutes: Int = -1 // -1 means Use Original Template duration, e.g. 15, 30, 60, 120, etc.
)

class ErpViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        @Volatile
        var instance: ErpViewModel? = null

        @Volatile
        var pendingTabFromIntent: String? = null
    }

    private val repository: DocumentRepository
    private val _uiState = MutableStateFlow(ErpUiState())
    val uiState: StateFlow<ErpUiState> = _uiState.asStateFlow()

    // In-memory elapsed seconds cache for running/active items
    private val standardSoElapsed = MutableStateFlow<Map<Int, Long>>(emptyMap())
    private val recurringSoElapsed = MutableStateFlow<Map<Int, Long>>(emptyMap())

    // Room Database data flows
    val documents: StateFlow<List<BusinessDocument>>
    val recurringSOs: StateFlow<List<RecurringSO>>

    private var timerJob: Job? = null
    private var dbPersistCounter = 0

    init {
        instance = this
        if (pendingTabFromIntent != null) {
            _uiState.update { it.copy(activeTab = pendingTabFromIntent!!) }
            pendingTabFromIntent = null
        }
        val database = AppDatabase.getDatabase(application)
        repository = DocumentRepository(database.documentDao())

        // Load setup preferences on startup
        val sharedPrefs = application.getSharedPreferences("abielan_setup_prefs", Context.MODE_PRIVATE)
        val isCompleted = sharedPrefs.getBoolean("is_setup_completed", false)
        val isDark = sharedPrefs.getBoolean("is_dark_mode", false)
        val contMinutes = sharedPrefs.getInt("continuation_duration_minutes", -1)
        _uiState.update { 
            it.copy(
                isSetupCompleted = isCompleted,
                isDarkMode = isDark,
                continuationDurationMinutes = contMinutes
            )
        }

        // Map cold flows and combine with in-memory tick cache to hot StateFlows in viewModelScope
        documents = repository.allDocuments
            .combine(standardSoElapsed) { dbDocs, activeTicks ->
                dbDocs.map { doc ->
                    val tickedElapsed = activeTicks[doc.id]
                    if (tickedElapsed != null) {
                        val computedAmount = (tickedElapsed / 3600.0) * doc.hourlyRate
                        doc.copy(
                            elapsedSeconds = tickedElapsed,
                            totalAmount = computedAmount
                        )
                    } else {
                        doc
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        recurringSOs = repository.allRecurringSOs
            .combine(recurringSoElapsed) { dbSos, activeTicks ->
                dbSos.map { so ->
                    val tickedElapsed = activeTicks[so.id]
                    if (tickedElapsed != null) {
                        so.copy(elapsedSeconds = tickedElapsed)
                    } else {
                        so
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed initial mock template data if empty
        viewModelScope.launch {
            documents.first() // Wait for initial flow collect
            val records = database.documentDao().getAllDocuments().first()
            if (records.isEmpty()) {
                seedInitialData()
            }
        }

        // Start High-precision clock ticker coroutine for our Sales Order & Recurring timers
        startTimerLoop()
    }

    private suspend fun seedInitialData() {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 3600 * 1000L

        val initialDocs = listOf(
            BusinessDocument(
                type = "INVOICE",
                title = "Cloud Infrastructure Deployment",
                docNumber = "INV-2026-001",
                clientName = "Apple Retail Inc",
                issueDate = now - 5 * oneDay,
                dueDate = now + 10 * oneDay,
                totalAmount = 145000.0,
                status = "PAID",
                notes = "Initial server migration and custom setup"
            ),
            BusinessDocument(
                type = "INVOICE",
                title = "AI Model Fine-tuning Consulting",
                docNumber = "INV-2026-002",
                clientName = "Google Operations LLC",
                issueDate = now - 2 * oneDay,
                dueDate = now + 5 * oneDay,
                totalAmount = 85000.0,
                status = "PENDING",
                notes = "Strategic intelligence assessment"
            ),
            BusinessDocument(
                type = "QUOTE",
                title = "Mobile App Design System Proposal",
                docNumber = "QT-2026-104",
                clientName = "SpaceX Launch Systems",
                issueDate = now,
                dueDate = now + 30 * oneDay,
                totalAmount = 240000.0,
                status = "Draft",
                notes = "High fidelity Jetpack Compose visual specs"
            ),
            BusinessDocument(
                type = "BILL",
                title = "Monthly Office Lease",
                docNumber = "BL-5590",
                clientName = "DLF Office Parks",
                issueDate = now - 15 * oneDay,
                dueDate = now - 2 * oneDay, // Overdue
                totalAmount = 45000.0,
                status = "OVERDUE",
                notes = "Wing-B commercial rental"
            ),
            BusinessDocument(
                type = "BILL",
                title = "Hardware Servers Subscriptions",
                docNumber = "BL-4929",
                clientName = "NVIDIA Hardware Inc",
                issueDate = now - 1 * oneDay,
                dueDate = now + 12 * oneDay,
                totalAmount = 30000.0,
                status = "Pending",
                notes = "4x Tensor Core Virtual Workspaces"
            ),
            BusinessDocument(
                type = "PO",
                title = "Office IT Accessories Procurement",
                docNumber = "PO-2026-80",
                clientName = "Star Tech Logistics",
                issueDate = now - 1 * oneDay,
                dueDate = now + 4 * oneDay,
                totalAmount = 65000.0,
                status = "Approved",
                notes = "Laptops, keyboard docks, edge nodes"
            ),
            BusinessDocument(
                type = "SO",
                title = "Full-Stack ERP Dashboard Developer Work",
                docNumber = "SO-2026-200",
                clientName = "Tesla Motors",
                issueDate = now,
                dueDate = now + 15 * oneDay,
                totalAmount = 0.0, // Calculated dynamically by billing hourly timers
                status = "Active",
                hourlyRate = 1200.0,
                timerDurationMinutes = 20, // Short timer limit for visual demonstration and testing
                elapsedSeconds = 0,
                timerState = "IDLE",
                notes = "Enterprise custom workflow automation modules"
            ),
            BusinessDocument(
                type = "SO",
                title = "Strategic Business Audit Consultation",
                docNumber = "SO-2026-201",
                clientName = "Reliance Industries",
                issueDate = now,
                dueDate = now + 7 * oneDay,
                totalAmount = 0.0,
                status = "Active",
                hourlyRate = 800.0,
                timerDurationMinutes = 45,
                elapsedSeconds = 0,
                timerState = "IDLE",
                notes = "Annual financial compliance review documentation"
            )
        )

        initialDocs.forEach { repository.insertDocument(it) }

        // Seed Recurring templates
        val initialRecur = listOf(
            RecurringSO(
                serviceName = "Weekly AWS Cloud Consultation",
                numPersons = 2,
                hourlyRate = 1500.0,
                durationMinutes = 60,
                timerState = "IDLE"
            ),
            RecurringSO(
                serviceName = "Daily Business Bookkeeping Feed",
                numPersons = 1,
                hourlyRate = 600.0,
                durationMinutes = 120,
                timerState = "IDLE"
            )
        )
        initialRecur.forEach { repository.insertRecurringSO(it) }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                tickLocalTimers()
            }
        }
    }

    private suspend fun tickLocalTimers() {
        // --- 1. Tick standard SO timers ---
        val allDocs = documents.value
        val runningSo = allDocs.firstOrNull { it.type == "SO" && it.timerState == "RUNNING" }
        if (runningSo != null) {
            val currentInMem = standardSoElapsed.value[runningSo.id] ?: runningSo.elapsedSeconds
            val nextSeconds = currentInMem + 1
            
            // Update in-memory ticks map
            standardSoElapsed.update { it + (runningSo.id to nextSeconds) }

            // Check if timer reaches limit
            val isLimitReached = nextSeconds >= (runningSo.timerDurationMinutes * 60)
            if (isLimitReached) {
                // Remove from in-memory cache
                standardSoElapsed.update { it - runningSo.id }
                
                val computedAmount = (nextSeconds / 3600.0) * runningSo.hourlyRate
                val updatedDoc = runningSo.copy(
                    elapsedSeconds = nextSeconds,
                    totalAmount = computedAmount,
                    timerState = "COMPLETED",
                    status = "COMPLETED"
                )
                repository.updateDocument(updatedDoc)

                val alarmMessage = "Timer ended for SO [${runningSo.docNumber}] - ${runningSo.title}! Limit of ${runningSo.timerDurationMinutes}m reached. Accumulated: ₹${String.format("%.2f", computedAmount)}"
                _uiState.update { 
                    it.copy(
                        showTimerCompletionDialog = true,
                        timerCompletionMessage = alarmMessage,
                        timerCompletionSoId = runningSo.id,
                        systemNotifications = listOf("Alarm: SO timer completed!") + it.systemNotifications
                    )
                }
            } else {
                // If not finished, save to database only once every 10 ticks to avoid disk overhead
                if (dbPersistCounter % 10 == 0) {
                    val computedAmount = (nextSeconds / 3600.0) * runningSo.hourlyRate
                    repository.updateDocument(runningSo.copy(
                        elapsedSeconds = nextSeconds,
                        totalAmount = computedAmount
                    ))
                }
            }
        }

        // --- 2. Tick Recurring SO independent timers ---
        val allRecurs = recurringSOs.value
        val runningRecurs = allRecurs.filter { it.isActive && it.timerState == "RUNNING" }
        runningRecurs.forEach { job ->
            val currentInMem = recurringSoElapsed.value[job.id] ?: job.elapsedSeconds
            val nextSeconds = currentInMem + 1
            
            // Update in-memory state
            recurringSoElapsed.update { it + (job.id to nextSeconds) }

            val isLimitFinished = nextSeconds >= (job.durationMinutes * 60)
            if (isLimitFinished) {
                // Remove from in-memory cache
                recurringSoElapsed.update { it - job.id }

                val timedOutJob = job.copy(
                    elapsedSeconds = nextSeconds,
                    timerState = "TIMED_OUT",
                    isActive = false
                )
                repository.updateRecurringSO(timedOutJob)

                _uiState.update { 
                    it.copy(
                        systemNotifications = listOf(
                            "Alarm: Recurring service '${job.serviceName}' timed out. Continue or Complete?"
                        ) + it.systemNotifications
                    )
                }

                // Fire system mobile alarm notification
                sendAlarmNotification(
                    job,
                    "Completed duration of ${job.durationMinutes} minutes. Choose Continue or Complete."
                )
            } else {
                if (dbPersistCounter % 10 == 0) {
                    repository.updateRecurringSO(job.copy(elapsedSeconds = nextSeconds))
                }
            }
        }

        dbPersistCounter = (dbPersistCounter + 1) % 1000
    }

    private fun sendAlarmNotification(job: RecurringSO, message: String) {
        val context = getApplication<Application>()
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "recurring_timer_alarm_channel"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                
                val channel = NotificationChannel(
                    channelId,
                    "Timer Alarm Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Signals when hourly recurring service timers complete"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                    setSound(soundUri, android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                }
                notificationManager.createNotificationChannel(channel)
            }

            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            // Content Click Action
            val clickIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_NOTIFICATION_CLICK
                putExtra(NotificationActionReceiver.EXTRA_JOB_ID, job.id)
            }
            val clickPendingIntent = PendingIntent.getBroadcast(
                context,
                job.id * 4 + 1,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Continue Action
            val continueIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_TIMER_CONTINUE
                putExtra(NotificationActionReceiver.EXTRA_JOB_ID, job.id)
            }
            val continuePendingIntent = PendingIntent.getBroadcast(
                context,
                job.id * 4 + 2,
                continueIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Complete Action
            val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_TIMER_COMPLETE
                putExtra(NotificationActionReceiver.EXTRA_JOB_ID, job.id)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                job.id * 4 + 3,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Dismiss/Delete Action (swiped away)
            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_NOTIFICATION_DISMISSED
                putExtra(NotificationActionReceiver.EXTRA_JOB_ID, job.id)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                job.id * 4 + 4,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ Recurring Service Alarm")
                .setContentText("'${job.serviceName}' has completed duration. Choose action.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
                .setContentIntent(clickPendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .addAction(android.R.drawable.ic_media_play, "Continue", continuePendingIntent)
                .addAction(android.R.drawable.ic_menu_save, "Complete", completePendingIntent)

            notificationManager.notify(job.id, builder.build())
            
            // Loopingly play the ringtone via centralized AlarmSoundManager
            AlarmSoundManager.play(context, soundUri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- UI Controls ---

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setThemeMode(isDark: Boolean) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("abielan_setup_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_dark_mode", isDark).apply()
        _uiState.update { it.copy(isDarkMode = isDark) }
    }

    fun completeSetup(isDarkMode: Boolean, isLocationGranted: Boolean, isNotificationGranted: Boolean) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("abielan_setup_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("is_setup_completed", true)
            .putBoolean("is_dark_mode", isDarkMode)
            .apply()
        _uiState.update { 
            it.copy(
                isSetupCompleted = true,
                isDarkMode = isDarkMode,
                isLocationPermissionGranted = isLocationGranted,
                isNotificationPermissionGranted = isNotificationGranted,
                systemNotifications = listOf("Setup complete! Preferences & Permissions configured.") + it.systemNotifications
            )
        }
    }

    fun updatePermissionStatus(isLocationGranted: Boolean, isNotificationGranted: Boolean) {
        _uiState.update {
            it.copy(
                isLocationPermissionGranted = isLocationGranted,
                isNotificationPermissionGranted = isNotificationGranted
            )
        }
    }

    fun setContinuationDurationMinutes(minutes: Int) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("abielan_setup_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("continuation_duration_minutes", minutes).apply()
        _uiState.update {
            it.copy(continuationDurationMinutes = minutes)
        }
    }

    fun toggleListGrid() {
        _uiState.update { it.copy(isListGridToggle = !it.isListGridToggle) }
    }

    fun changeSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun setActiveFilterTab(type: String) {
        _uiState.update { it.copy(activeFilterTab = type) }
    }

    fun changeSortMode(sort: String) {
        _uiState.update { it.copy(activeSortMode = sort) }
    }

    fun changeChartType(type: String) {
        _uiState.update { it.copy(activeChartType = type) }
    }

    fun changeTimeFilter(filter: String) {
        _uiState.update { it.copy(activeTimeFilter = filter) }
    }

    fun updateCompanyDetails(company: String, owner: String, gst: String) {
        _uiState.update { 
            it.copy(
                auth = it.auth.copy(
                    companyName = company,
                    ownerName = owner,
                    gstNumber = gst
                )
            )
        }
    }

    fun closeTimerDialog() {
        _uiState.update { it.copy(showTimerCompletionDialog = false) }
    }

    // --- Auth Module Commands ---

    fun loginUser(emailInput: String, pwdInput: String) {
        _uiState.update { 
            it.copy(
                auth = it.auth.copy(
                    isLoggedIn = true,
                    email = emailInput,
                    companyName = "Abielan Premium Logistics",
                    ownerName = "Enterprise Admin"
                ),
                showAuthScreen = "DASHBOARD"
            )
        }
    }

    fun signupUser(
        company: String,
        owner: String,
        mobile: String,
        email: String,
        gst: String,
        bizType: String
    ) {
        _uiState.update { 
            it.copy(
                auth = AuthState(
                    isLoggedIn = false,
                    companyName = company,
                    ownerName = owner,
                    mobileNumber = mobile,
                    email = email,
                    gstNumber = gst.ifBlank { "Unregistered - No GST" },
                    businessType = bizType
                ),
                showAuthScreen = "PLANS" // Go directly toSubscription plans
            )
        }
    }

    fun selectSubscriptionPlan(plan: String) {
        _uiState.update { 
            it.copy(
                auth = it.auth.copy(planSelected = plan, subscriptionExpiry = "22-June-2026"),
                razorpaySimulating = true
            )
        }
        viewModelScope.launch {
            delay(1500) // Simulate processing payment gateway response
            _uiState.update { 
                it.copy(
                    razorpaySimulating = false,
                    razorpaySuccessMessage = "Razorpay Transaction Successful! Unlimited access unlocked.",
                    systemNotifications = listOf("Subscription updated to $plan!") + it.systemNotifications
                )
            }
        }
    }

    fun logout() {
        _uiState.update { 
            it.copy(
                auth = AuthState(isLoggedIn = false),
                showAuthScreen = "LOGIN",
                activeTab = "Dashboard"
            )
        }
    }

    fun setAuthScreen(screen: String) {
        _uiState.update { it.copy(showAuthScreen = screen) }
    }

    // --- Sales Order Timer Operations ---

    fun startSoTimer(soId: Int, durationMin: Int, hourlyRate: Double) {
        viewModelScope.launch {
            val dbSo = repository.getDocumentById(soId) ?: return@launch
            
            // Pausing raw standard active SO timers in-memory
            val runningIds = documents.value.filter { it.type == "SO" && it.timerState == "RUNNING" }.map { it.id }
            runningIds.forEach { id ->
                val currentInMem = standardSoElapsed.value[id]
                if (currentInMem != null) {
                    val soDoc = repository.getDocumentById(id)
                    if (soDoc != null) {
                        repository.updateDocument(soDoc.copy(
                            timerState = "PAUSED",
                            elapsedSeconds = currentInMem
                        ))
                    }
                }
            }
            standardSoElapsed.update { it - runningIds }

            // Set current starting cache value
            standardSoElapsed.update { it + (soId to dbSo.elapsedSeconds) }

            val updated = dbSo.copy(
                timerState = "RUNNING",
                timerDurationMinutes = durationMin,
                hourlyRate = hourlyRate
            )
            repository.updateDocument(updated)
            _uiState.update { 
                it.copy(
                    systemNotifications = listOf("Sales Order ${dbSo.docNumber} work timer has started.") + it.systemNotifications
                )
            }
        }
    }

    fun pauseSoTimer(soId: Int) {
        viewModelScope.launch {
            val dbSo = repository.getDocumentById(soId) ?: return@launch
            val currentInMem = standardSoElapsed.value[soId] ?: dbSo.elapsedSeconds
            
            standardSoElapsed.update { it - soId }

            val computedAmount = (currentInMem / 3600.0) * dbSo.hourlyRate
            val updated = dbSo.copy(
                timerState = "PAUSED",
                elapsedSeconds = currentInMem,
                totalAmount = computedAmount
            )
            repository.updateDocument(updated)
        }
    }

    fun resetSoTimer(soId: Int) {
        viewModelScope.launch {
            standardSoElapsed.update { it - soId }
            val dbSo = repository.getDocumentById(soId) ?: return@launch
            val updated = dbSo.copy(timerState = "IDLE", elapsedSeconds = 0, totalAmount = 0.0)
            repository.updateDocument(updated)
        }
    }

    fun selectTimerDetailsToConfigure(so: BusinessDocument) {
        _uiState.update { it.copy(activeSoTimer = so) }
    }

    fun clearActiveSoSelection() {
        _uiState.update { it.copy(activeSoTimer = null) }
    }

    // Alarm Action 1: Re-start or Continue work
    fun timerActionContinue(durationMinutes: Int) {
        val soId = _uiState.value.timerCompletionSoId ?: return
        viewModelScope.launch {
            val dbSo = repository.getDocumentById(soId) ?: return@launch
            
            // Set starting elapsed value
            standardSoElapsed.update { it + (soId to 0L) }

            val updated = dbSo.copy(
                timerState = "RUNNING",
                timerDurationMinutes = durationMinutes,
                elapsedSeconds = 0 // Restart countdown with same accrued hourly calculation potential
            )
            repository.updateDocument(updated)
            _uiState.update { 
                it.copy(
                    showTimerCompletionDialog = false,
                    timerCompletionSoId = null,
                    systemNotifications = listOf("Timer extended by $durationMinutes mins on [${dbSo.docNumber}]") + it.systemNotifications
                )
            }
        }
    }

    // Alarm Action 2: Conversion
    fun timerActionCompleteToInvoice() {
        val soId = _uiState.value.timerCompletionSoId ?: return
        viewModelScope.launch {
            standardSoElapsed.update { it - soId }
            val dbSo = repository.getDocumentById(soId) ?: return@launch
            
            // Mark SO as completed
            val updatedSo = dbSo.copy(
                timerState = "COMPLETED",
                status = "COMPLETED"
            )
            repository.updateDocument(updatedSo)

            // Convert automatically into INVOICE as mandated
            val finalInvoicePrice = if (dbSo.totalAmount > 0) dbSo.totalAmount else (dbSo.timerDurationMinutes * (dbSo.hourlyRate / 60))
            
            val autoInvoice = BusinessDocument(
                type = "INVOICE",
                title = "Invoiced from [Work Done] - ${dbSo.title}",
                docNumber = "INV-CONV-${System.currentTimeMillis().toString().takeLast(4)}",
                clientName = dbSo.clientName,
                issueDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 15 * 24 * 3600 * 1000L,
                totalAmount = finalInvoicePrice,
                status = "PENDING",
                notes = "Auto generated based on Sales Order hourly work. Rate: ₹${dbSo.hourlyRate}/Hr."
            )
            repository.insertDocument(autoInvoice)

            _uiState.update { 
                it.copy(
                    showTimerCompletionDialog = false,
                    timerCompletionSoId = null,
                    systemNotifications = listOf(
                        "SO Convert Successful! Invoice compiled for ₹${String.format("%.2f", finalInvoicePrice)}."
                    ) + it.systemNotifications
                )
            }
        }
    }

    // --- Recurring SO Operations (Up to 4 independent timers concurrently) ---

    fun addNewRecurringTemplate(serviceName: String, numPersons: Int, rate: Double, duration: Int) {
        // Enforce maximum of 4 active template templates
        if (recurringSOs.value.size >= 4) {
            _uiState.update { 
                it.copy(systemNotifications = listOf("Failed: Subscriptions limit! Standard tier limits up to 4 concurrent running recurring jobs only.") + it.systemNotifications)
            }
            return
        }

        viewModelScope.launch {
            val item = RecurringSO(
                serviceName = serviceName,
                numPersons = numPersons,
                hourlyRate = rate,
                durationMinutes = duration,
                timerState = "IDLE",
                isActive = false
            )
            repository.insertRecurringSO(item)
        }
    }

    fun startRecurringTimer(id: Int) {
        viewModelScope.launch {
            val job = repository.getRecurringSOById(id) ?: return@launch
            
            // Set memory cache
            recurringSoElapsed.update { it + (id to job.elapsedSeconds) }

            val updated = job.copy(
                timerState = "RUNNING",
                isActive = true
            )
            repository.updateRecurringSO(updated)
        }
    }

    fun pauseRecurringTimer(id: Int) {
        viewModelScope.launch {
            val job = repository.getRecurringSOById(id) ?: return@launch
            val currentInMem = recurringSoElapsed.value[id] ?: job.elapsedSeconds
            
            recurringSoElapsed.update { it - id }

            val updated = job.copy(
                timerState = "PAUSED",
                isActive = false,
                elapsedSeconds = currentInMem
            )
            repository.updateRecurringSO(updated)
        }
    }

    fun resetRecurringTimer(id: Int) {
        viewModelScope.launch {
            recurringSoElapsed.update { it - id }
            val job = repository.getRecurringSOById(id) ?: return@launch
            val updated = job.copy(
                timerState = "IDLE",
                isActive = false,
                elapsedSeconds = 0,
                associatedInvoiceId = null
            )
            repository.updateRecurringSO(updated)
        }
    }

    fun deleteRecurringTemplate(id: Int) {
        viewModelScope.launch {
            val job = repository.getRecurringSOById(id) ?: return@launch
            repository.deleteRecurringSO(job)
        }
    }

    fun updateRecurringTemplate(id: Int, serviceName: String, numPersons: Int, rate: Double, duration: Int) {
        viewModelScope.launch {
            val job = repository.getRecurringSOById(id) ?: return@launch
            val updated = job.copy(
                serviceName = serviceName,
                numPersons = numPersons,
                hourlyRate = rate,
                durationMinutes = duration
            )
            repository.updateRecurringSO(updated)
        }
    }

    fun continueRecurringTimer(id: Int) {
        viewModelScope.launch {
            val job = repository.getRecurringSOById(id) ?: return@launch
            val customMinutes = _uiState.value.continuationDurationMinutes
            val updatedDuration = if (customMinutes > 0) customMinutes else job.durationMinutes
            val updated = job.copy(
                durationMinutes = updatedDuration,
                elapsedSeconds = 0,
                timerState = "RUNNING",
                isActive = true
            )
            repository.updateRecurringSO(updated)
            recurringSoElapsed.update { it + (id to 0L) }
            
            _uiState.update { 
                it.copy(
                    systemNotifications = listOf("Recurring service '${job.serviceName}' timer restarted/continued successfully with duration of $updatedDuration minutes.") + it.systemNotifications
                )
            }
        }
    }

    fun completeRecurringTimerAndMoveToSO(id: Int) {
        viewModelScope.launch {
            val job = repository.getRecurringSOById(id) ?: return@launch
            val computedAmount = (job.elapsedSeconds / 3600.0) * job.hourlyRate * job.numPersons
            
            val salesOrder = BusinessDocument(
                type = "SO",
                title = "SO - ${job.serviceName}",
                docNumber = "SO-RAUTO-${System.currentTimeMillis().toString().takeLast(4)}",
                clientName = "Subscribed Enterprise",
                issueDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L,
                totalAmount = if (computedAmount > 0) computedAmount else ((job.durationMinutes / 60.0) * job.hourlyRate * job.numPersons),
                hourlyRate = job.hourlyRate,
                status = "PENDING",
                timerDurationMinutes = job.durationMinutes,
                elapsedSeconds = job.elapsedSeconds,
                timerState = "COMPLETED",
                notes = "Transferred from completed recurring service template '${job.serviceName}'."
            )
            repository.insertDocument(salesOrder)

            val updatedJob = job.copy(
                timerState = "IDLE",
                isActive = false,
                elapsedSeconds = 0
            )
            repository.updateRecurringSO(updatedJob)
            recurringSoElapsed.update { it - id }

            _uiState.update { 
                it.copy(
                    systemNotifications = listOf("Recurring '${job.serviceName}' completed. Formulated new Sales Order document.") + it.systemNotifications
                )
            }
        }
    }

    fun markPaymentAsCompleteAndMoveToInvoice(soId: Int) {
        viewModelScope.launch {
            val dbSo = repository.getDocumentById(soId) ?: return@launch
            
            val updatedSo = dbSo.copy(
                status = "COMPLETED",
                timerState = "COMPLETED"
            )
            repository.updateDocument(updatedSo)

            val invoice = BusinessDocument(
                type = "INVOICE",
                title = "Invoiced from [SO Payment] - ${dbSo.title}",
                docNumber = "INV-SO-${System.currentTimeMillis().toString().takeLast(4)}",
                clientName = dbSo.clientName,
                issueDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 15 * 24 * 3600 * 1000L,
                totalAmount = dbSo.totalAmount,
                status = "PAID",
                notes = "Billing invoice generated upon marking Sales Order ${dbSo.docNumber} payment complete."
            )
            repository.insertDocument(invoice)

            _uiState.update { 
                it.copy(
                    systemNotifications = listOf("SO Payment completed! Invoice compiled & marked PAID for ₹${String.format(java.util.Locale.US, "%,.2f", dbSo.totalAmount)}.") + it.systemNotifications
                )
            }
        }
    }

    // --- CRUD Document actions ---

    fun addNewDocument(
        type: String,
        title: String,
        client: String,
        amount: Double,
        status: String,
        notes: String,
        hourlyRate: Double = 0.0,
        dueDaysAhead: Int = 14,
        customDocNumber: String = "",
        timerDurationMinutes: Int = 0,
        customIssueDate: Long? = null,
        customDueDate: Long? = null
    ) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val issue = customIssueDate ?: calendar.timeInMillis
            
            val due = if (customDueDate != null) {
                customDueDate
            } else {
                val cal = Calendar.getInstance()
                cal.timeInMillis = issue
                cal.add(Calendar.DAY_OF_YEAR, dueDaysAhead)
                cal.timeInMillis
            }

            val serial = if (customDocNumber.isNotBlank()) {
                customDocNumber
            } else {
                when (type) {
                    "INVOICE" -> "INV-${System.currentTimeMillis().toString().takeLast(5)}"
                    "QUOTE" -> "QT-${System.currentTimeMillis().toString().takeLast(5)}"
                    "BILL" -> "BL-${System.currentTimeMillis().toString().takeLast(5)}"
                    "PO" -> "PO-${System.currentTimeMillis().toString().takeLast(5)}"
                    "SO" -> "SO-${System.currentTimeMillis().toString().takeLast(5)}"
                    else -> "DOC-${System.currentTimeMillis().toString().takeLast(5)}"
                }
            }

            val doc = BusinessDocument(
                type = type,
                title = title,
                docNumber = serial,
                clientName = client,
                issueDate = issue,
                dueDate = due,
                totalAmount = amount,
                status = status,
                hourlyRate = hourlyRate,
                timerDurationMinutes = timerDurationMinutes,
                notes = notes
            )
            repository.insertDocument(doc)
            _uiState.update { 
                it.copy(systemNotifications = listOf("New Document Created: $serial - $title") + it.systemNotifications)
            }
        }
    }

    fun updateDocumentStatus(docId: Int, newStatus: String) {
        viewModelScope.launch {
            val doc = repository.getDocumentById(docId) ?: return@launch
            val updated = doc.copy(status = newStatus)
            repository.updateDocument(updated)
            _uiState.update {
                it.copy(systemNotifications = listOf("Document '${doc.docNumber}' status updated to $newStatus.") + it.systemNotifications)
            }
        }
    }

    fun updateFullDocument(
        docId: Int,
        type: String,
        title: String,
        client: String,
        amount: Double,
        status: String,
        notes: String,
        hourlyRate: Double = 0.0,
        customDocNumber: String = "",
        timerDurationMinutes: Int = 0,
        customIssueDate: Long? = null,
        customDueDate: Long? = null
    ) {
        viewModelScope.launch {
            val doc = repository.getDocumentById(docId) ?: return@launch
            val updated = doc.copy(
                type = type,
                title = title,
                clientName = client,
                totalAmount = amount,
                status = status,
                notes = notes,
                hourlyRate = hourlyRate,
                docNumber = if (customDocNumber.isNotBlank()) customDocNumber else doc.docNumber,
                timerDurationMinutes = timerDurationMinutes,
                issueDate = customIssueDate ?: doc.issueDate,
                dueDate = customDueDate ?: doc.dueDate
            )
            repository.updateDocument(updated)
            _uiState.update {
                it.copy(systemNotifications = listOf("Document '${doc.docNumber}' updated successfully.") + it.systemNotifications)
            }
        }
    }

    fun convertQuoteToInvoice(quoteId: Int) {
        viewModelScope.launch {
            val quote = repository.getDocumentById(quoteId) ?: return@launch
            
            // Mark the quote as CLOSED
            val updatedQuote = quote.copy(status = "CLOSED")
            repository.updateDocument(updatedQuote)

            val invoice = BusinessDocument(
                type = "INVOICE",
                title = "Invoiced from [Quote] - ${quote.title}",
                docNumber = "INV-QT-${System.currentTimeMillis().toString().takeLast(4)}",
                clientName = quote.clientName,
                issueDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 14 * 24 * 3600 * 1000L,
                totalAmount = quote.totalAmount,
                status = "PENDING",
                notes = "Auto compiled from Quote ${quote.docNumber}. Original quote marked CLOSED."
            )
            repository.insertDocument(invoice)

            _uiState.update {
                it.copy(
                    systemNotifications = listOf("Quote ${quote.docNumber} converted to Invoice.") + it.systemNotifications
                )
            }
        }
    }

    fun deleteDocument(doc: BusinessDocument) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            _uiState.update { 
                it.copy(systemNotifications = listOf("Deleted Document - ${doc.docNumber}") + it.systemNotifications)
            }
        }
    }

    // --- AI Chat Command ---

    fun askAdvisoryAgent() {
        val prompt = _uiState.value.aiPrompt
        if (prompt.isBlank()) return

        _uiState.update { it.copy(aiLoading = true, aiResponse = "") }

        viewModelScope.launch {
            val systemInstruction = "You are Abielan ERP Business and Finance Intelligence Advisor. Provide short, executive summaries, actionable professional tips on tax planning, bookkeeping, enterprise profitability strategy, or help users interpret the metrics."
            val response = GeminiService.generateResponse(prompt, systemInstruction)
            _uiState.update { 
                it.copy(
                    aiResponse = response,
                    aiPrompt = "",
                    aiLoading = false
                )
            }
        }
    }

    fun setAiPromptText(txt: String) {
        _uiState.update { it.copy(aiPrompt = txt) }
    }

    override fun onCleared() {
        super.onCleared()
        if (instance == this) {
            instance = null
        }
    }
}
