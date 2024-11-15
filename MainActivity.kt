package com.example.arlam


import android.content.SharedPreferences
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.ui.unit.sp
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.arlam.ui.theme.ArlamTheme
import java.util.*



class MainActivity : ComponentActivity() {
    private val alarm = mutableStateListOf<AlarmItem>()
    private var backPressedTime: Long = 0
    private val gson = Gson()

    // SharedPreferences 사용
    private fun saveAlarmsToPreferences() {
        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val json = gson.toJson(alarm) // 알람 리스트를 JSON으로 변환
        editor.putString("alarms", json)
        editor.apply()
    }

    private fun loadAlarmsFromPreferences() {
        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val json = sharedPref.getString("alarms", null)
        val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
        val loadedAlarms: MutableList<AlarmItem>? = gson.fromJson(json, type)
        if (loadedAlarms != null) {
            alarm.clear()
            alarm.addAll(loadedAlarms)

            // 카운터 초기화
            loadedAlarms.forEach { loadedAlarm ->
                val counter = sharedPref.getInt("alarm_counter_${loadedAlarm.id}", 0)
                loadedAlarm.counter = counter
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // 알람 로드
        loadAlarmsFromPreferences()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }

        setContent {
            ArlamTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    AlarmScreen(
                        modifier = Modifier.padding(paddingValues),
                        saveAlarms = { saveAlarmsToPreferences() } // 저장 함수 전달
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() > backPressedTime + 2000) {
            Toast.makeText(this, "뒤로가기 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
            return
        }
        if (System.currentTimeMillis() <= backPressedTime + 2000) {
            super.onBackPressed()
        }
    }
}

data class AlarmItem(val id: Int, var hour: Int, val minute: Int, val title: String, val medicineName: String, var isEnabled: Boolean = true, var counter: Int = 0 )

fun setAlarm(context: Context, alarm: AlarmItem) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("ALARM_TITLE", alarm.title)
        putExtra("ALARM_ID", alarm.id)
        putExtra("ALARM_TIME", "${alarm.hour}:${alarm.minute}")
        putExtra("MEDICINE_NAME", alarm.medicineName) // 약 이름도 전달
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, alarm.id, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }
    }

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
}

@Composable
fun AlarmScreen(modifier: Modifier = Modifier, saveAlarms: () -> Unit) {
    val sb = FontFamily(Font(R.font.sb_b, FontWeight.Normal, FontStyle.Normal))
    val context = LocalContext.current
    val alarms = remember { mutableStateListOf<AlarmItem>() }
    var pickedHour by remember { mutableStateOf(0) }
    var pickedMinute by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var medicineName by remember { mutableStateOf(TextFieldValue("")) }

    val calendar = Calendar.getInstance()

    // 초기 로드 시 알람 리스트와 카운터 설정
    alarms.forEach { alarm ->
        val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val counter = sharedPref.getInt("alarm_counter_${alarm.id}", 0)
        alarm.counter = counter
    }

    Column(modifier = modifier.padding(16.dp)) {
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

        Button(onClick = {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    pickedHour = hourOfDay
                    pickedMinute = minute

                    val newAlarm = AlarmItem(
                        id = alarms.size,
                        hour = pickedHour,
                        minute = pickedMinute,
                        title = title.text,
                        medicineName = medicineName.text,
                        isEnabled = true
                    )
                    alarms.add(newAlarm)

                    // 알람 설정 및 저장
                    setAlarm(context, newAlarm)
                    saveAlarms() // 알람 저장

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
                        alarms[alarms.indexOf(alarm)] = updatedAlarm
                        saveAlarms() // 알람 저장
                    },
                    onEdit = { updatedAlarm ->
                        alarms[alarms.indexOf(alarm)] = updatedAlarm
                        saveAlarms() // 알람 저장
                    },
                    onDelete = { deletedAlarm ->
                        alarms.remove(deletedAlarm)
                        cancelAlarm(context, deletedAlarm)
                        saveAlarms() // 알람 저장
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
    var editedHour by remember { mutableStateOf(alarm.hour) }
    var editedMinute by remember { mutableStateOf(alarm.minute) }
    var totalCounter by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {

        // 구분 선 추가
        Divider(
            color = Color(0xFFF98000),
            thickness = 2.dp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
                        label = { Text("알람 제목", fontFamily = sb, fontWeight = FontWeight.Normal) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedMedicineName,
                        onValueChange = { editedMedicineName = it },
                        label = { Text("약 이름", fontFamily = sb, fontWeight = FontWeight.Normal) },
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
                            contentColor = Color(0xFF000000)
                        )
                    ) {
                        Text("저장", fontFamily = sb, fontWeight = FontWeight.Normal)
                    }
                }
            } else {
                Column {
                    Text(
                        text = String.format("[%02d:%02d]", alarm.hour, alarm.minute),
                        style = TextStyle(
                            fontFamily = sb,
                            fontWeight = FontWeight.Normal,
                            fontSize = 30.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "알람 제목 : ${alarm.title}",
                        style = TextStyle(
                            fontFamily = sb,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "약 이름 : ${alarm.medicineName}",
                        style = TextStyle(
                            fontFamily = sb,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row {
                        Button( modifier = Modifier.width(48.8.dp)
                            .height(30.dp)
                            .offset(y=6.5.dp),
                            onClick = {
                                if (totalCounter > 0) totalCounter--  // 카운터 감소
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE9700))
                        ) {
                            Text(text = "-", style = TextStyle(fontSize = 10.sp, fontFamily = sb, fontWeight = FontWeight.Normal, color = Color.Black ),
                                modifier = Modifier, textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(text = "약 복용 횟수 : $totalCounter", style = TextStyle(fontFamily = sb, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                            modifier = Modifier.offset(y = 12.dp))

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(modifier = Modifier.width(48.8.dp)
                            .height(30.dp)
                            .offset(y=6.5.dp),
                            onClick = {
                                totalCounter++  // 카운터 증가
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE9700))
                        ) {
                            Text(text = "+", style = TextStyle(fontSize = 10.sp, fontFamily = sb, fontWeight = FontWeight.Normal, color = Color.Black ),
                                modifier = Modifier, textAlign = TextAlign.Center)
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
                    Button(onClick = { isEditing = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFE9700),
                            contentColor = Color(0xFF000000)
                        )
                    ) {
                        Text("수정", fontFamily = sb, fontWeight = FontWeight.Normal)
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Button(onClick = {
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
                                setAlarm(context, updatedAlarm)
                                Toast.makeText(context, "알람 ON", Toast.LENGTH_SHORT).show()
                            } else {
                                cancelAlarm(context, updatedAlarm)
                                Toast.makeText(context, "알람 OFF", Toast.LENGTH_SHORT).show()
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

fun cancelAlarm(context: Context, alarm: AlarmItem) {
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
