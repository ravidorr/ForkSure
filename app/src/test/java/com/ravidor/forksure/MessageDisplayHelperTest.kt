package com.ravidor.forksure

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageDisplayHelperTest {

    @Test
    fun userMessage_defaultDurations_matchByType() {
        val mSuccess = UserMessage("ok", MessageType.SUCCESS)
        val mError = UserMessage("bad", MessageType.ERROR)
        val mInfo = UserMessage("hi", MessageType.INFO)
        val mWarn = UserMessage("warn", MessageType.WARNING)

        assertThat(mSuccess.duration.name).isEqualTo("Short")
        assertThat(mError.duration.name).isEqualTo("Long")
        assertThat(mInfo.duration.name).isEqualTo("Short")
        assertThat(mWarn.duration.name).isEqualTo("Long")
    }
}
