package com.vordain.guard.vpn.service

import com.vordain.guard.vpn.lab.InMemoryLabTrafficObserver
import com.vordain.guard.vpn.lab.LabTrafficObservationStats
import com.vordain.guard.vpn.lab.LabTrafficObserver

object LabCaptureDebugStatus {
    private val observer = InMemoryLabTrafficObserver()

    fun observer(): LabTrafficObserver = observer

    fun snapshot(): LabTrafficObservationStats = observer.snapshot()
}
