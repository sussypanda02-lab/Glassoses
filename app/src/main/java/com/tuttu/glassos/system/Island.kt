package com.tuttu.glassos.system

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Things the simulated Dynamic Island can show. All of them come from supported Android APIs or are labelled simulations. */
sealed interface IslandItem {
    data class Charging(val percent: Int) : IslandItem
    data class Notice(val app: String, val title: String, val text: String) : IslandItem
    data class Timer(val startAt: Long, val endAt: Long) : IslandItem
}

class IslandController(private val scope: CoroutineScope) {
    var item by mutableStateOf<IslandItem?>(null)
        private set
    var expanded by mutableStateOf(false)
        private set
    private var job: Job? = null

    private fun run(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch { block() }
    }

    fun post(i: IslandItem, holdMs: Long = 4000) = run {
        item = i; expanded = false
        delay(250); expanded = true
        delay(holdMs); expanded = false
        delay(350); item = null
    }

    /** A simulated live activity (countdown). */
    fun startTimer(seconds: Int) = run {
        val now = System.currentTimeMillis()
        item = IslandItem.Timer(now, now + seconds * 1000L); expanded = false
        delay(seconds * 1000L)
        item = IslandItem.Notice("Timer", "Time's up", "Your simulated timer finished"); expanded = true
        delay(3000); expanded = false
        delay(350); item = null
    }

    fun toggle() { if (item != null) expanded = !expanded }
}
