package com.careride.school

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

interface DoorProbe : AutoCloseable {
    fun read(report: (String) -> Unit)
}

class MainActivity : Activity() {
    private lateinit var panel: LinearLayout
    private lateinit var probe: DoorProbe
    private var ride = sampleRide()
    private var routeStatus = ""
    private var doorStatus = "조회 대기"
    private var doorReading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        probe = createDoorProbe(applicationContext)
        render()
    }

    override fun onDestroy() {
        probe.close()
        super.onDestroy()
    }

    private fun render() {
        val padding = (24 * resources.displayMetrics.density).toInt()
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        setContentView(ScrollView(this).apply { addView(panel) })

        label("CareRide School · 하원 출발 확인", 26f)
        label("정차 상황 시연 · 가상 명단, 안전벨트 및 내비 경로")
        label("예정 ${ride.roster.size}명 · 모의 체결 3석 · 1명 대조 필요")
        label("담당자가 학생별 실제 탑승 상태를 선택합니다.")

        ride.roster.forEach { student ->
            action("${student.name} · ${student.stop.name} · ${ride.status(student).label}") {
                val choices = Boarding.entries
                AlertDialog.Builder(this)
                    .setTitle(student.name)
                    .setSingleChoiceItems(choices.map { it.label }.toTypedArray(), choices.indexOf(ride.status(student))) { dialog, index ->
                        ride.record(student, choices[index])
                        routeStatus = ""
                        dialog.dismiss()
                        render()
                    }
                    .setNegativeButton("닫기", null)
                    .show()
            }
        }

        label("현재 시연 경로 · ${ride.activeRoute.joinToString(" → ") { it.name }}")
        val proposed = ride.proposedRoute()
        when {
            proposed == null -> label("명단을 모두 확인하면 변경 후보가 표시됩니다.")
            proposed.isEmpty() -> label("탑승 학생이 없습니다. 운행 취소 여부를 확인하세요.")
            proposed == ride.activeRoute -> label("시연 경로가 확인된 명단과 일치합니다.")
            else -> {
                val removed = ride.activeRoute - proposed.toSet()
                val restored = proposed - ride.activeRoute.toSet()
                if (removed.isNotEmpty()) label("제외 후보 · ${removed.joinToString { it.name }}")
                if (restored.isNotEmpty()) label("복구 후보 · ${restored.joinToString { it.name }}")
                action("변경 승인 · 모의 내비 갱신") {
                    ride.applySimulatedRoute(proposed)
                    routeStatus = "모의 내비 경로를 갱신했습니다."
                    render()
                }
            }
        }
        if (routeStatus.isNotEmpty()) label(routeStatus)

        action("시연 초기화") {
            ride = sampleRide()
            routeStatus = ""
            render()
        }
        label("Vehicle SDK · $doorStatus")
        action("운전석 도어 위치 조회", enabled = !doorReading) {
            doorReading = true
            doorStatus = "조회 중"
            render()
            probe.read { result ->
                runOnUiThread {
                    if (!isDestroyed) {
                        doorReading = false
                        doorStatus = result
                        // A roster edit may have rebuilt the screen during the async call.
                        render()
                    }
                }
            }
        }
    }

    private fun label(value: String, size: Float = 18f): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            panel.addView(this)
        }

    private fun action(value: String, enabled: Boolean = true, clicked: () -> Unit) {
        panel.addView(Button(this).apply {
            text = value
            isAllCaps = false
            isEnabled = enabled
            setOnClickListener { clicked() }
        })
    }
}
