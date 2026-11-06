package com.careride.school

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue

fun createDoorProbe(context: Context): DoorProbe = object : DoorProbe {
    private var vehicle: Vehicle? = null

    override fun read(report: (String) -> Unit) {
        fun failed(error: Exception) {
            Log.w("CareRideDoor", "Door position request failed", error)
            report("조회 실패 · ${error.message ?: error.javaClass.simpleName}")
        }

        try {
            val session = vehicle ?: Vehicle(context).also { created ->
                try {
                    created.initialize()
                } catch (error: Exception) {
                    runCatching { created.release() }
                    throw error
                }
                vehicle = created
            }
            session.getDoor().getCurrentDoorPosition(
                area = DoorArea(DoorValue.ROW_1_LEFT),
                onComplete = { position: Int? ->
                    Log.i("CareRideDoor", "Driver door position: $position")
                    report(position?.let { "운전석 도어 위치 $it" } ?: "현재 값을 읽을 수 없음")
                },
                onFailed = ::failed
            )
        } catch (error: Exception) {
            failed(error)
        }
    }

    override fun close() {
        runCatching { vehicle?.release() }
        vehicle = null
    }
}
