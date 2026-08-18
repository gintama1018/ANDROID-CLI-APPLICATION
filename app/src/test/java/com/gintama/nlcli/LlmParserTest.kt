package com.gintama.nlcli

import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.AppType
import com.gintama.nlcli.model.ParserResult
import com.gintama.nlcli.parser.LlmParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LlmParserTest {

    private lateinit var parser: LlmParser

    @Before
    fun setUp() {
        parser = LlmParser()
    }

    @Test
    fun testLlmWhatsAppWithDoubleQuotes() = runTest {
        val input = "tell Rahul on whatsapp that the \"meeting\" is at 5"
        val result = parser.parse(input)
        assertTrue("Expected Success but got $result", result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.WHATSAPP, cmd.app)
        assertEquals(ActionType.SEND_MESSAGE, cmd.action)
        assertEquals("Rahul", cmd.contact)
        assertEquals("the \"meeting\" is at 5", cmd.payload)
    }

    @Test
    fun testLlmSmsWithQuotesAndSymbols() = runTest {
        val input = "tell Alex on sms: \"Urgent: call me back!\""
        val result = parser.parse(input)
        assertTrue("Expected Success but got $result", result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.SMS, cmd.app)
        assertEquals(ActionType.SEND_MESSAGE, cmd.action)
        assertEquals("Alex", cmd.contact)
        assertEquals("\"Urgent: call me back!\"", cmd.payload)
    }

    @Test
    fun testLlmCallIntent() = runTest {
        val input = "give a ring to Mom"
        val result = parser.parse(input)
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.PHONE, cmd.app)
        assertEquals(ActionType.CALL, cmd.action)
        assertEquals("Mom", cmd.contact)
    }
}
