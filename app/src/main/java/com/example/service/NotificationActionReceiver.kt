package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.viewmodel.ErpViewModel
import com.example.data.AppDatabase
import com.example.data.BusinessDocument
import com.example.data.DocumentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NOTIFICATION_CLICK = "com.example.ACTION_NOTIFICATION_CLICK"
        const val ACTION_TIMER_CONTINUE = "com.example.ACTION_TIMER_CONTINUE"
        const val ACTION_TIMER_COMPLETE = "com.example.ACTION_TIMER_COMPLETE"
        const val ACTION_NOTIFICATION_DISMISSED = "com.example.ACTION_NOTIFICATION_DISMISSED"

        const val EXTRA_JOB_ID = "extra_job_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val jobId = intent.getIntExtra(EXTRA_JOB_ID, -1)

        // 1. Silence the active alarm ringtone
        AlarmSoundManager.stop()

        // 2. Dismiss the notification visually
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (jobId != -1) {
            notificationManager.cancel(jobId)
        }

        when (action) {
            ACTION_NOTIFICATION_CLICK -> {
                // Stop sound and update tab
                ErpViewModel.instance?.let { vm ->
                    vm.setActiveTab("Sales")
                }

                // Open the MainActivity to bring the UI to the front
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigate_tab", "Sales")
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
            ACTION_TIMER_CONTINUE -> {
                if (jobId != -1) {
                    val vm = ErpViewModel.instance
                    if (vm != null) {
                        vm.continueRecurringTimer(jobId)
                    } else {
                        // Fallback DB operation if ViewModel is inactive
                        val db = AppDatabase.getDatabase(context)
                        val repository = DocumentRepository(db.documentDao())
                        CoroutineScope(Dispatchers.IO).launch {
                            val job = repository.getRecurringSOById(jobId)
                            if (job != null) {
                                val updated = job.copy(
                                    elapsedSeconds = 0,
                                    timerState = "RUNNING",
                                    isActive = true
                                )
                                repository.updateRecurringSO(updated)
                            }
                        }
                    }
                }
            }
            ACTION_TIMER_COMPLETE -> {
                if (jobId != -1) {
                    val vm = ErpViewModel.instance
                    if (vm != null) {
                        vm.completeRecurringTimerAndMoveToSO(jobId)
                    } else {
                        // Fallback DB operation if ViewModel is inactive
                        val db = AppDatabase.getDatabase(context)
                        val repository = DocumentRepository(db.documentDao())
                        CoroutineScope(Dispatchers.IO).launch {
                            val job = repository.getRecurringSOById(jobId)
                            if (job != null) {
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
                            }
                        }
                    }
                }
            }
            ACTION_NOTIFICATION_DISMISSED -> {
                // Just silenced, nothing extra needed
            }
        }
    }
}
