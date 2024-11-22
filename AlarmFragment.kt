package com.example.pill2024

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.pill2024.ui.theme.AlarmTheme
import com.example.pill2024.viewModels.AlarmViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AlarmFragment : Fragment() {
    private val alarmViewModel: AlarmViewModel by viewModels()
    private val gson = Gson()

    private fun startAlarmService() {
        // Android 13 이상에서는 알림 권한을 요청해야 합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                return // 권한 요청 후 다시 서비스 시작
            }
        }

        // 권한이 허용되었거나 Android 13 미만인 경우 서비스 시작
        val intent = Intent(requireContext(), AlarmService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun saveAlarmsToPreferences() {
        val sharedPref = requireContext().getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val json = gson.toJson(alarmViewModel.alarms) // 알람 리스트를 JSON으로 변환
        editor.putString("alarms", json)
        editor.apply()
    }

    private fun loadAlarmsFromPreferences() {
        val sharedPref = requireContext().getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val json = sharedPref.getString("alarms", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
            val loadedAlarms: MutableList<AlarmItem> = gson.fromJson(json, type)
            alarmViewModel.loadAlarms(loadedAlarms)

        } else {
            Log.d("AlarmFragment", "저장된 알람이 없습니다.")
        }

        val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
        val loadedAlarms: MutableList<AlarmItem>? = gson.fromJson(json, type)
        loadedAlarms?.let {
            alarmViewModel.loadAlarms(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AlarmFragment", "onCreate called")
        loadAlarmsFromPreferences()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            Log.d("AlarmFragment", "onCreateView called")
            setContent {
                AlarmTheme {
                    AlarmScreen(
                        alarms = alarmViewModel.alarms,
                        saveAlarms = { saveAlarmsToPreferences() },
                        onAddAlarms = { alarm ->
                            alarmViewModel.addAlarm(alarm)
                            setAlarm(requireContext(), alarm)
                            saveAlarmsToPreferences()
                        },

                        onRemoveAlarms = { alarm ->
                            alarmViewModel.removeAlarm(alarm)
                            cancelAlarm(requireContext(), alarm)
                            saveAlarmsToPreferences()
                        }
                    )
                }
            }
        }
        return composeView
    }


    data class AlarmItem(
        val id: Int, var hour: Int, val minute: Int, val title: String,
        val medicineName: String, var isEnabled: Boolean = true, var counter: Int = 0
    )

    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarm(context: Context, alarm: AlarmItem) {
        // Android 12 이상에서 정확한 알람 허용 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { intent ->
                    context.startActivity(intent)
                    return // 정확한 알람을 요청한 후 메서드를 종료
                }
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TITLE", alarm.title)
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_TIME", "${alarm.hour}:${alarm.minute}")
            putExtra("MEDICINE_NAME", alarm.medicineName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    @Composable
    fun AlarmScreen(
        modifier: Modifier = Modifier,
        alarms: List<AlarmItem>,
        onAddAlarms: (AlarmItem) -> Unit,
        onRemoveAlarms: (AlarmItem) -> Unit,
        saveAlarms: () -> Unit
    ) {
        val sb = FontFamily(Font(R.font.sb_b, FontWeight.Normal, FontStyle.Normal))
        val context = LocalContext.current
        var pickedHour by remember { mutableIntStateOf(0) }
        var pickedMinute by remember { mutableIntStateOf(0) }
        var title by remember { mutableStateOf(TextFieldValue("")) }
        var medicineName by remember { mutableStateOf(TextFieldValue("")) }

        val calendar = Calendar.getInstance()

        Column(modifier = modifier.padding(16.dp)) {
            Spacer(modifier = Modifier.height(50.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("알람 제목", fontFamily = sb, fontWeight = FontWeight.Normal) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = medicineName,
                onValueChange = { medicineName = it },
                label = { Text("약 이름", fontFamily = sb, fontWeight = FontWeight.Normal) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            pickedHour = hourOfDay
                            pickedMinute = minute

                            // 새로운 알람 아이템 추가
                            val newAlarm = AlarmItem(
                                id = alarms.size,
                                hour = pickedHour,
                                minute = pickedMinute,
                                title = title.text,
                                medicineName = medicineName.text,
                                isEnabled = true
                            )
                            // 알람 설정 및 저장
                            onAddAlarms(newAlarm)
                            setAlarm(context, newAlarm)
                            saveAlarms()

                            // 텍스트 필드 초기화
                            title = TextFieldValue("")
                            medicineName = TextFieldValue("")
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFE9700),
                    contentColor = Color(0xFF000000)
                )
            ) {
                Text(text = "알림 설정", fontFamily = sb, fontWeight = FontWeight.Normal)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alarms) { alarm ->
                    AlarmItemView(
                        alarm = alarm,
                        onAlarmToggle = { updatedAlarm ->
                            alarmViewModel.editAlarms(updatedAlarm)
                            if(updatedAlarm.isEnabled) {
                                activityAlarm(context, updatedAlarm)
                                Toast.makeText(context,"알람 on", Toast.LENGTH_SHORT).show()
                            }else{
                                cancelAlarm(context, updatedAlarm)
                                Toast.makeText(context, "알람 off", Toast.LENGTH_SHORT).show()
                            }
                            saveAlarms()
                        },
                        onEdit = { updatedAlarm ->
                            alarmViewModel.editAlarms(updatedAlarm)
                            saveAlarms()
                        },
                        onDelete = { deletedAlarm ->
                            onRemoveAlarms(deletedAlarm)
                            cancelAlarm(context, deletedAlarm)
                            saveAlarms()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun AlarmItemView(
        alarm: AlarmItem,
        onAlarmToggle: (AlarmItem) -> Unit,
        onEdit: (AlarmItem) -> Unit,
        onDelete: (AlarmItem) -> Unit
    ) {
        val sb = FontFamily(Font(R.font.sb_b, FontWeight.Normal, FontStyle.Normal))
        var isEditing by remember { mutableStateOf(false) }
        var editedTitle by remember { mutableStateOf(TextFieldValue(alarm.title)) }
        var editedMedicineName by remember { mutableStateOf(TextFieldValue(alarm.medicineName)) }
        var editedHour by remember { mutableIntStateOf(alarm.hour) }
        var editedMinute by remember { mutableIntStateOf(alarm.minute) }
        var totalCounter by remember { mutableIntStateOf(0) }

        val context = LocalContext.current

        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                thickness = 2.dp,
                color = Color(0xFFF98000)
            )

            Row(
                modifier = Modifier
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text("알람 제목", fontFamily = sb, fontWeight = FontWeight.Normal)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editedMedicineName,
                            onValueChange = { editedMedicineName = it },
                            label = { Text("약 이름", fontFamily = sb) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    editedHour = hourOfDay
                                    editedMinute = minute
                                },
                                editedHour,
                                editedMinute,
                                true
                            ).show()
                        },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFE9700),
                                contentColor = Color(0xFF000000)
                            )
                        ) {
                            Text(text = String.format("시간 수정", editedHour, editedMinute), fontFamily = sb, fontWeight = FontWeight.Normal)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            val updatedAlarm = alarm.copy(
                                title = editedTitle.text,
                                medicineName = editedMedicineName.text,
                                hour = editedHour,
                                minute = editedMinute
                            )
                            onEdit(updatedAlarm)
                            isEditing = false
                            setAlarm(context, updatedAlarm)
                            Toast.makeText(context, "알람 수정 완료", Toast.LENGTH_SHORT).show()
                        },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFE9700),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("저장", fontFamily = sb, fontWeight = FontWeight.Normal)
                        }
                    }
                } else {
                    Column {
                        Text(text = String.format("[%02d:%02d]", alarm.hour, alarm.minute), style = TextStyle(fontFamily = sb, fontWeight = FontWeight.Normal, fontSize = 30.sp
                        )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "알람 제목 : ${alarm.title}", style = TextStyle(fontFamily = sb, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "약 이름 : ${alarm.medicineName}", style = TextStyle(fontFamily = sb, fontWeight = FontWeight.Normal, fontSize = 16.sp
                        )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row {
                            Button(
                                modifier = Modifier.width(48.8.dp)
                                    .height(30.dp)
                                    .offset(y = 6.5.dp),
                                onClick = {
                                    if (totalCounter > 0) totalCounter--  // 카운터 감소
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFFE9700
                                    )
                                )
                            ) {
                                Text(text = "-", style = TextStyle(fontSize = 10.sp, fontFamily = sb, fontWeight = FontWeight.Normal, color = Color.Black),
                                    modifier = Modifier,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(text = "약 복용 횟수 : $totalCounter", style = TextStyle(fontFamily = sb, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                                modifier = Modifier.offset(y = 12.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))

                            Button(modifier = Modifier.width(48.8.dp)
                                .height(30.dp)
                                .offset(y = 6.5.dp),
                                onClick = {
                                    totalCounter++  // 카운터 증가
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFFE9700
                                    )
                                )
                            ) {
                                Text(text = "+", style = TextStyle(fontSize = 10.sp, fontFamily = sb, fontWeight = FontWeight.Normal, color = Color.Black),
                                    modifier = Modifier, textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    if (!isEditing) { // 수정 모드가 아닐 때만 스위치 보이기
                        Button(
                            onClick = { isEditing = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFE9700),
                                contentColor = Color(0xFF000000)
                            )
                        ) {
                            Text("수정", fontFamily = sb, fontWeight = FontWeight.Normal)
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Button(
                            onClick = {
                                onDelete(alarm)
                                Toast.makeText(context, "알람 삭제 완료", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFE9700),
                                contentColor = Color(0xFF000000)
                            )
                        ) {
                            Text("삭제", fontFamily = sb, fontWeight = FontWeight.Normal)
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Switch(
                            checked = alarm.isEnabled,
                            onCheckedChange = { isChecked ->
                                val updatedAlarm = alarm.copy(isEnabled = isChecked)
                                onAlarmToggle(updatedAlarm)

                                if (isChecked) {
                                    activityAlarm(context, updatedAlarm)
                                } else {
                                    cancelAlarm(context, updatedAlarm)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFE9700),
                                checkedTrackColor = Color(0xFFF1E395),
                                uncheckedThumbColor = Color(0xB9E98A11),
                                uncheckedTrackColor = Color(0xFFC9BD7B)
                            )
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun activityAlarm(context: Context, alarm: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TITLE", alarm.title)
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_TIME", "${alarm.hour}:${alarm.minute}")
            putExtra("MEDICINE_NAME", alarm.medicineName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(context: Context, alarm: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 알람 취소
        alarmManager.cancel(pendingIntent)
    }
}