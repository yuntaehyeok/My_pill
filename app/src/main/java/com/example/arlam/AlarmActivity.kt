package com.example.arlam

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 새로운 알람 화면 레이아웃 사용

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmTime = intent.getStringExtra("ALARM_TIME")
        val medicineName = intent.getStringExtra("MEDICINE_NAME")
        val alarmTitle = intent.getStringExtra("ALARM_TITLE")

        sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        vibrator.vibrate(longArrayOf(0, 1000, 1000), 0)

        // 알람 제목 표시
        val alarmTitleTextView = findViewById<TextView>(R.id.alarmTitleTextView)
        alarmTitleTextView.text = "알람 제목: $alarmTitle"
        // 알람 시간 표시
        val alarmTimeTextView = findViewById<TextView>(R.id.alarmTimeTextView)
        alarmTimeTextView.text = "알람 시간: ${formatAlarmTime(alarmTime)}" // 포맷된 시간으로 설정

        val medicineNameTextView = findViewById<TextView>(R.id.medicineNameTextView)
        medicineNameTextView.text = "약 이름: $medicineName"



        // 해제 버튼 클릭 시 알람 해제
        val stopButton = findViewById<Button>(R.id.stopAlarmButton)
        stopButton.setOnClickListener {
            if (alarmId != -1) {
            val alarmTitleTextView = findViewById<TextView>(R.id.alarmTitleTextView) // 알람 제목을 표시할 TextView
            alarmTitleTextView.setBackgroundColor(Color.BLACK) // 배경색 회색으로 변경
        }

            finish()
            stopAlarm()
            finish()
        }
    }

    private fun formatAlarmTime(alarmTime: String?): String {
        return alarmTime?.let {
            val parts = it.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull()
                val minute = parts[1].toIntOrNull()
                if (hour != null && minute != null) {
                    String.format("%d시 %02d분", hour, minute)
                } else {
                    "알람 시간 불명" // 기본값 설정
                }
            } else {
                "알람 시간 불명" // 기본값 설정
            }
        } ?: "알람 시간 불명" // 기본값 설정
    }

    private fun stopAlarm() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }
    }
}
