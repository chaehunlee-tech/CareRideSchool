package com.careride.school

data class Stop(val name: String)
data class Student(val name: String, val stop: Stop)
enum class Boarding(val label: String) { UNCHECKED("확인 대기"), ONBOARD("탑승"), ABSENT("미탑승") }

class Ride(val stops: List<Stop>, val roster: List<Student>) {
    private val boarding = mutableMapOf<Student, Boarding>()
    var activeRoute = stops
        private set

    fun status(student: Student) = boarding[student] ?: Boarding.UNCHECKED

    fun record(student: Student, status: Boarding) {
        require(student in roster)
        boarding[student] = status
    }

    fun proposedRoute(): List<Stop>? {
        if (roster.any { status(it) == Boarding.UNCHECKED }) return null
        return stops.filter { stop ->
            roster.any { it.stop == stop && status(it) == Boarding.ONBOARD }
        }
    }

    fun applySimulatedRoute(route: List<Stop>) {
        require(route.isNotEmpty() && route == proposedRoute())
        activeRoute = route.toList()
    }
}

fun sampleRide(): Ride {
    val library = Stop("도서관")
    val park = Stop("중앙공원")
    val apartments = Stop("한빛아파트")
    return Ride(
        listOf(library, park, apartments),
        listOf(
            Student("학생 A", library),
            Student("학생 B", library),
            Student("학생 C", park),
            Student("학생 D", apartments)
        )
    )
}
