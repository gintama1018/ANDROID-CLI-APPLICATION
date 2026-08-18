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
    fun testTorchCommands() = runTest {
        val onRes = parser.parse("torch on")
        assertTrue(onRes is ParserResult.Success)
        assertEquals(ActionType.TORCH, (onRes as ParserResult.Success).command.action)

        val offRes = parser.parse("flashlight off")
        assertTrue(offRes is ParserResult.Success)
        assertEquals(ActionType.TORCH, (offRes as ParserResult.Success).command.action)
    }

    @Test
    fun testVolumeCommands() = runTest {
        val volRes = parser.parse("volume 50")
        assertTrue(volRes is ParserResult.Success)
        assertEquals(ActionType.VOLUME, (volRes as ParserResult.Success).command.action)
        assertEquals("50", (volRes as ParserResult.Success).command.payload)

        val muteRes = parser.parse("mute")
        assertTrue(muteRes is ParserResult.Success)
        assertEquals(ActionType.VOLUME, (muteRes as ParserResult.Success).command.action)
    }

    @Test
    fun testDiagnostics() = runTest {
        val battRes = parser.parse("battery")
        assertTrue(battRes is ParserResult.Success)
        assertEquals(ActionType.BATTERY, (battRes as ParserResult.Success).command.action)

        val storageRes = parser.parse("storage")
        assertTrue(storageRes is ParserResult.Success)
        assertEquals(ActionType.STORAGE, (storageRes as ParserResult.Success).command.action)
    }

    @Test
    fun testMathAndConvert() = runTest {
        val calcRes = parser.parse("calc (450 * 18) / 100")
        assertTrue(calcRes is ParserResult.Success)
        assertEquals(ActionType.CALC, (calcRes as ParserResult.Success).command.action)

        val convRes = parser.parse("convert 5 miles to km")
        assertTrue(convRes is ParserResult.Success)
        assertEquals(ActionType.CONVERT, (convRes as ParserResult.Success).command.action)
    }

    @Test
    fun testNotesAndTodos() = runTest {
        val noteRes = parser.parse("note buy groceries")
        assertTrue(noteRes is ParserResult.Success)
        assertEquals(ActionType.NOTE, (noteRes as ParserResult.Success).command.action)

        val todoRes = parser.parse("todo call dentist")
        assertTrue(todoRes is ParserResult.Success)
        assertEquals(ActionType.TODO, (todoRes as ParserResult.Success).command.action)
    }

    @Test
    fun testAlarmsAndMedia() = runTest {
        val alarmRes = parser.parse("alarm 7:00 am")
        assertTrue(alarmRes is ParserResult.Success)
        assertEquals(ActionType.ALARM, (alarmRes as ParserResult.Success).command.action)

        val mediaRes = parser.parse("play music")
        assertTrue(mediaRes is ParserResult.Success)
        assertEquals(ActionType.MEDIA, (mediaRes as ParserResult.Success).command.action)
    }

    @Test
    fun testAliasesAndSnippets() = runTest {
        val aliasRes = parser.parse("alias gm = torch off; volume 100")
        assertTrue(aliasRes is ParserResult.Success)
        assertEquals(ActionType.MACRO, (aliasRes as ParserResult.Success).command.action)

        val snippetRes = parser.parse("snippet upi = user@bank")
        assertTrue(snippetRes is ParserResult.Success)
        assertEquals(ActionType.SNIPPET, (snippetRes as ParserResult.Success).command.action)
    }
}
