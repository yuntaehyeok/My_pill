package com.example.pill2024.viewModels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.pill2024.AlarmFragment

class AlarmViewModel : ViewModel() {
    private val _alarms = mutableStateListOf<AlarmFragment.AlarmItem>()
    val alarms: List<AlarmFragment.AlarmItem> get() = _alarms

    fun addAlarm(alarm: AlarmFragment.AlarmItem) {
        _alarms.add(alarm)
    }

    fun removeAlarm(alarm: AlarmFragment.AlarmItem) {
        _alarms.remove(alarm)
    }

    fun loadAlarms(loadedAlarms: List<AlarmFragment.AlarmItem>) {
        _alarms.clear()
        _alarms.addAll(loadedAlarms)
    }

    fun editAlarms(updateAlarm: AlarmFragment.AlarmItem) {
        val index = _alarms.indexOfFirst { it.id == updateAlarm.id }
        if(index != -1) {
            _alarms[index] = updateAlarm
        }
    }
}
