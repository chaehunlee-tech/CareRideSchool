package com.careride.school

import android.content.Context

@Suppress("UNUSED_PARAMETER")
fun createDoorProbe(context: Context): DoorProbe = object : DoorProbe {
    override fun read(report: (String) -> Unit) {
        report("모의 운전석 도어 위치 0")
    }

    override fun close() = Unit
}
