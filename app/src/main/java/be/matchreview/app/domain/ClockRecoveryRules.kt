package be.matchreview.app.domain

enum class ClockRecoveryConfidence { SAME_BOOT, REBOOT_RECOVERED, NEEDS_CONFIRMATION }

data class ClockRecoveryAssessment(
    val recoveredMatchTimeMs: Long,
    val confidence: ClockRecoveryConfidence,
    val explanation: String
)

object ClockRecoveryRules {
    const val WALL_CLOCK_DRIFT_WARNING_MS = 2 * 60 * 1000L

    fun assess(
        segmentStartMatchTimeMs: Long,
        monotonicStartMs: Long,
        wallClockStartMs: Long,
        monotonicNowMs: Long,
        wallClockNowMs: Long
    ): ClockRecoveryAssessment {
        val monotonicElapsed = monotonicNowMs - monotonicStartMs
        val wallElapsed = wallClockNowMs - wallClockStartMs
        val rebooted = monotonicElapsed < 0 ||
            monotonicNowMs + MatchClockCalculator.REBOOT_DETECTION_TOLERANCE_MS < wallElapsed
        val recovered = MatchClockCalculator.recoveredCurrentMatchTimeMs(
            segmentStartMatchTimeMs, monotonicStartMs, wallClockStartMs, monotonicNowMs, wallClockNowMs
        )
        val suspicious = wallElapsed < 0 ||
            (monotonicElapsed >= 0 && kotlin.math.abs(wallElapsed - monotonicElapsed) > WALL_CLOCK_DRIFT_WARNING_MS)
        return when {
            suspicious -> ClockRecoveryAssessment(recovered, ClockRecoveryConfidence.NEEDS_CONFIRMATION, "Device time changed while the match clock was active.")
            rebooted -> ClockRecoveryAssessment(recovered, ClockRecoveryConfidence.REBOOT_RECOVERED, "Clock recovered from its saved wall-clock checkpoint.")
            else -> ClockRecoveryAssessment(recovered, ClockRecoveryConfidence.SAME_BOOT, "Clock recovered from monotonic elapsed time.")
        }
    }
}
