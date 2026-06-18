package com.vordain.guard.vpn.session

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VpnSessionStateReducerTest {
    @Test
    fun initialSnapshotIsNotPrepared() {
        val snapshot = VpnSessionSnapshot.initial()

        assertEquals(VpnSessionState.NOT_PREPARED, snapshot.state)
        assertEquals(VpnSessionStateReason.INITIAL, snapshot.reason)
    }

    @Test
    fun prepareRequiringPermissionMovesToPermissionRequired() {
        val transition = reducer.reduce(
            previous = VpnSessionSnapshot.initial(),
            command = VpnSessionCommand.Prepare(permissionGranted = false),
            currentTimeMillis = 1_000L,
        )

        assertTrue(transition.accepted)
        assertEquals(VpnSessionState.PERMISSION_REQUIRED, transition.next.state)
        assertEquals(VpnSessionStateReason.USER_PERMISSION_REQUIRED, transition.next.reason)
    }

    @Test
    fun prepareGrantedMovesToReady() {
        val transition = reducer.reduce(
            previous = VpnSessionSnapshot.initial(),
            command = VpnSessionCommand.Prepare(permissionGranted = true),
            currentTimeMillis = 1_000L,
        )

        assertTrue(transition.accepted)
        assertEquals(VpnSessionState.READY, transition.next.state)
        assertEquals(VpnSessionStateReason.USER_PERMISSION_GRANTED, transition.next.reason)
    }

    @Test
    fun startFromReadyMovesToStarting() {
        val transition = reducer.reduce(
            previous = ready(),
            command = VpnSessionCommand.Start,
            currentTimeMillis = 2_000L,
        )

        assertTrue(transition.accepted)
        assertEquals(VpnSessionState.STARTING, transition.next.state)
        assertEquals(VpnSessionStateReason.START_REQUESTED, transition.next.reason)
    }

    @Test
    fun markStartedMovesStartingToRunning() {
        val transition = reducer.reduce(
            previous = starting(),
            command = VpnSessionCommand.MarkStarted,
            currentTimeMillis = 3_000L,
        )

        assertTrue(transition.accepted)
        assertEquals(VpnSessionState.RUNNING, transition.next.state)
        assertEquals(VpnSessionStateReason.STARTED, transition.next.reason)
    }

    @Test
    fun stopFromRunningMovesToStopping() {
        val transition = reducer.reduce(
            previous = running(),
            command = VpnSessionCommand.Stop,
            currentTimeMillis = 4_000L,
        )

        assertTrue(transition.accepted)
        assertEquals(VpnSessionState.STOPPING, transition.next.state)
        assertEquals(VpnSessionStateReason.STOP_REQUESTED, transition.next.reason)
    }

    @Test
    fun revokedFromAnyStateMovesToRevoked() {
        val states = listOf(
            VpnSessionSnapshot.initial(),
            ready(),
            starting(),
            running(),
            stopping(),
            stopped(),
            error(),
        )

        states.forEach { previous ->
            val transition = reducer.reduce(
                previous = previous,
                command = VpnSessionCommand.MarkRevoked,
                currentTimeMillis = 5_000L,
            )

            assertTrue(transition.accepted)
            assertEquals(VpnSessionState.REVOKED, transition.next.state)
            assertEquals(VpnSessionStateReason.REVOKED_BY_SYSTEM, transition.next.reason)
        }
    }

    @Test
    fun errorFromAnyStateMovesToError() {
        val transition = reducer.reduce(
            previous = running(),
            command = VpnSessionCommand.MarkError("platform failed"),
            currentTimeMillis = 6_000L,
        )

        assertTrue(transition.accepted)
        assertEquals(VpnSessionState.ERROR, transition.next.state)
        assertEquals(VpnSessionStateReason.PLATFORM_ERROR, transition.next.reason)
        assertEquals("platform failed", transition.next.message)
    }

    @Test
    fun invalidStartFromNotPreparedIsRejected() {
        val previous = VpnSessionSnapshot.initial()
        val transition = reducer.reduce(
            previous = previous,
            command = VpnSessionCommand.Start,
            currentTimeMillis = 7_000L,
        )

        assertFalse(transition.accepted)
        assertEquals(previous, transition.next)
    }

    @Test
    fun reducerUsesInjectedTimestamp() {
        val transition = reducer.reduce(
            previous = ready(),
            command = VpnSessionCommand.Start,
            currentTimeMillis = 99_000L,
        )

        assertEquals(99_000L, transition.next.updatedAtMillis)
    }

    @Test
    fun sourceHasNoAndroidGodClassOrTodoMarkers() {
        val sourceRoot = repositoryRoot().resolve("vpn/session/src/main/kotlin")

        assertSourceTreeDoesNotContain(sourceRoot, "android" + ".")
        assertSourceTreeDoesNotContain(sourceRoot, "Manager")
        assertSourceTreeDoesNotContain(sourceRoot, "TO" + "DO")
        assertSourceTreeDoesNotContain(sourceRoot, "FIX" + "ME")
    }

    private fun ready() = VpnSessionSnapshot(
        state = VpnSessionState.READY,
        reason = VpnSessionStateReason.USER_PERMISSION_GRANTED,
        updatedAtMillis = 1_000L,
        message = null,
    )

    private fun starting() = VpnSessionSnapshot(
        state = VpnSessionState.STARTING,
        reason = VpnSessionStateReason.START_REQUESTED,
        updatedAtMillis = 2_000L,
        message = null,
    )

    private fun running() = VpnSessionSnapshot(
        state = VpnSessionState.RUNNING,
        reason = VpnSessionStateReason.STARTED,
        updatedAtMillis = 3_000L,
        message = null,
    )

    private fun stopping() = VpnSessionSnapshot(
        state = VpnSessionState.STOPPING,
        reason = VpnSessionStateReason.STOP_REQUESTED,
        updatedAtMillis = 4_000L,
        message = null,
    )

    private fun stopped() = VpnSessionSnapshot(
        state = VpnSessionState.STOPPED,
        reason = VpnSessionStateReason.STOPPED_BY_APP,
        updatedAtMillis = 5_000L,
        message = null,
    )

    private fun error() = VpnSessionSnapshot(
        state = VpnSessionState.ERROR,
        reason = VpnSessionStateReason.PLATFORM_ERROR,
        updatedAtMillis = 6_000L,
        message = "platform failed",
    )

    private fun assertSourceTreeDoesNotContain(sourceRoot: File, forbiddenText: String) {
        val filesWithForbiddenText = kotlinFilesUnder(sourceRoot).filter { file ->
            file.readText().contains(forbiddenText)
        }

        assertTrue(
            actual = filesWithForbiddenText.isEmpty(),
            message = "Forbidden text $forbiddenText found in ${filesWithForbiddenText.map { it.path }}",
        )
    }

    private fun kotlinFilesUnder(sourceRoot: File): List<File> {
        val kotlinFiles = sourceRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .toList()

        assertTrue(kotlinFiles.isNotEmpty(), "Expected Kotlin files under ${sourceRoot.path}")
        return kotlinFiles
    }

    private fun repositoryRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) {
                return current
            }
            current = current.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }

    private companion object {
        val reducer = VpnSessionStateReducer()
    }
}
