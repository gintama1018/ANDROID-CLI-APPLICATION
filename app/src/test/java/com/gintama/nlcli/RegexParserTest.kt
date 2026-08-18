package com.gintama.nlcli

import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.AppType
import com.gintama.nlcli.model.ParserResult
import com.gintama.nlcli.parser.RegexParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegexParserTest {

    private lateinit var parser: RegexParser

    @Before
    fun setUp() {
        parser = RegexParser()
    }

    @Test
    fun testWhatsAppStandardFormat() = runTest {
        val result = parser.parse("send whatsapp to Rahul: reaching in 10 mins")
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.WHATSAPP, cmd.app)
        assertEquals(ActionType.SEND_MESSAGE, cmd.action)
        assertEquals("Rahul", cmd.contact)
        assertEquals("reaching in 10 mins", cmd.payload)
    }

    @Test
    fun testWhatsAppShortFormat() = runTest {
        val result = parser.parse("whatsapp Rahul: reaching in 10 mins")
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.WHATSAPP, cmd.app)
        assertEquals("Rahul", cmd.contact)
        assertEquals("reaching in 10 mins", cmd.payload)
    }

    @Test
    fun testWhatsAppWaAlias() = runTest {
        val result = parser.parse("send wa to Rahul - on my way")
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.WHATSAPP, cmd.app)
        assertEquals("Rahul", cmd.contact)
        assertEquals("on my way", cmd.payload)
    }

    @Test
    fun testPhoneCall() = runTest {
        val result = parser.parse("call Mom")
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.PHONE, cmd.app)
        assertEquals(ActionType.CALL, cmd.action)
        assertEquals("Mom", cmd.contact)
    }

    @Test
    fun testSmsSend() = runTest {
        val result = parser.parse("send sms to John: See you tomorrow")
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.SMS, cmd.app)
        assertEquals(ActionType.SEND_MESSAGE, cmd.action)
        assertEquals("John", cmd.contact)
        assertEquals("See you tomorrow", cmd.payload)
    }

    @Test
    fun testAppLaunch() = runTest {
        val result = parser.parse("open YouTube")
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.SYSTEM, cmd.app)
        assertEquals(ActionType.OPEN_APP, cmd.action)
        assertEquals("YouTube", cmd.payload)
    }

    @Test
    fun testSystemCommands() = runTest {
        val helpResult = parser.parse("help")
        assertTrue(helpResult is ParserResult.Success)
        assertEquals(ActionType.HELP, (helpResult as ParserResult.Success).command.action)

        val statusResult = parser.parse("status")
        assertTrue(statusResult is ParserResult.Success)
        assertEquals(ActionType.STATUS, (statusResult as ParserResult.Success).command.action)

        val clearResult = parser.parse("clear")
        assertTrue(clearResult is ParserResult.Success)
        assertEquals(ActionType.CLEAR, (clearResult as ParserResult.Success).command.action)
    }

    @Test
    fun testDryRun() = runTest {
        val result = parser.parse("dryrun send whatsapp to Boss: Here is the report")
        assertTrue(result is ParserResult.Success)
        val cmd = (result as ParserResult.Success).command
        assertEquals(AppType.WHATSAPP, cmd.app)
        assertEquals(ActionType.DRY_RUN, cmd.action)
        assertEquals("Boss", cmd.contact)
        assertEquals("Here is the report", cmd.payload)
    }

    @Test
    fun testUnrecognizedCommand() = runTest {
        val result = parser.parse("gibberish nonexistent command")
        assertTrue(result is ParserResult.Failure)
    }
}
