package com.example.arlam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 알람 ID와 시간을 Intent에서 추출합니다.
        val alarmId = intent.getIntExtra("ALARM_ID", -1) // 알람 ID 가져오기
        val alarmTime = intent.getStringExtra("ALARM_TIME") // 알람 시간 가져오기
        val medicineName = intent.getStringExtra("MEDICINE_NAME")
        val alarmtitle = intent.getStringExtra("ALARM_TITLE")

            // AlarmActivity를 시작합니다.
            val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK // 새로운 태스크로 Activity 시작
                putExtra("ALARM_ID", alarmId)  // 알람 ID 전달
                putExtra("ALARM_TIME", alarmTime) // 알람 시간 전달
                putExtra("MEDICINE_NAME", medicineName)
                putExtra("ALARM_TITLE", alarmtitle)
            }
            context.startActivity(alarmIntent) // AlarmActivity 시작
        }
    }

