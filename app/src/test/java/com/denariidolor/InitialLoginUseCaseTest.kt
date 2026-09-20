package com.denariidolor

import com.denariidolor.domain.auth.PinSecurityStore
import com.denariidolor.domain.usecase.InitialLoginResult
import com.denariidolor.domain.usecase.InitialLoginUseCase
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialLoginUseCaseTest {
    private val fakeStore = FakePinSecurityStore()
    private var wipeInvocations = 0
    private val useCase = InitialLoginUseCase(
        pinSecurityStore = fakeStore,
        wipeAllData = {
            wipeInvocations += 1
            fakeStore.clearPin()
        }
    )

    @Test
    fun setupOnlyAllowedOnce() {
        val first = useCase.setup("1234", "1234", "City?", "Rome")
        val second = useCase.setup("9999", "9999", "Pet?", "Fluffy")

        assertEquals(InitialLoginResult.Success, first)
        assertTrue(second is InitialLoginResult.Error)
    }

    @Test
    fun authenticateHandlesSuccessAndFailure() {
        useCase.setup("1234", "1234", "City?", "Rome")

        val success = useCase.authenticate("1234")
        val failure = useCase.authenticate("5555")

        assertEquals(InitialLoginResult.Success, success)
        assertTrue(failure is InitialLoginResult.Error)
    }

    @Test
    fun recoveryRequiresCorrectSecurityAnswer() {
        useCase.setup("1234", "1234", "City?", "Rome")

        val failure = useCase.recoverPin("Paris", "4567", "4567")
        val success = useCase.recoverPin("Rome", "4567", "4567")
        val authWithNewPin = useCase.authenticate("4567")

        assertTrue(failure is InitialLoginResult.Error)
        assertEquals(InitialLoginResult.Success, success)
        assertEquals(InitialLoginResult.Success, authWithNewPin)
    }

    @Test
    fun wipeCancellationDoesNotDeleteData() = runBlocking(EmptyCoroutineContext) {
        useCase.setup("1234", "1234", "City?", "Rome")

        val cancelled = useCase.wipeData(confirm = false)
        val authAfterCancel = useCase.authenticate("1234")

        assertTrue(cancelled is InitialLoginResult.Error)
        assertEquals(InitialLoginResult.Success, authAfterCancel)
        assertEquals(0, wipeInvocations)
    }

    @Test
    fun confirmedWipeReturnsToFirstTimeSetupState() = runBlocking(EmptyCoroutineContext) {
        useCase.setup("1234", "1234", "City?", "Rome")

        val wipeResult = useCase.wipeData(confirm = true)
        val authAfterWipe = useCase.authenticate("1234")

        assertEquals(InitialLoginResult.Success, wipeResult)
        assertTrue(authAfterWipe is InitialLoginResult.Error)
        assertTrue(useCase.isFirstTimeSetup())
        assertEquals(1, wipeInvocations)
    }

    private class FakePinSecurityStore : PinSecurityStore {
        private var pin: String? = null
        private var question: String? = null
        private var answer: String? = null

        override fun isPinConfigured(): Boolean = pin != null && question != null && answer != null

        override fun createPinSecurity(pin: String, securityQuestion: String, securityAnswer: String): Boolean {
            if (isPinConfigured()) return false
            this.pin = pin
            this.question = securityQuestion
            this.answer = securityAnswer
            return true
        }

        override fun verifyPin(pin: String): Boolean = this.pin == pin

        override fun getSecurityQuestion(): String? = question

        override fun verifySecurityAnswer(securityAnswer: String): Boolean = answer == securityAnswer

        override fun resetPin(pin: String): Boolean {
            if (!isPinConfigured()) return false
            this.pin = pin
            return true
        }

        override fun clearPin() {
            pin = null
            question = null
            answer = null
        }
    }
}
