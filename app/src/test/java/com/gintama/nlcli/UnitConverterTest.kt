package com.gintama.nlcli

import com.gintama.nlcli.utility.UnitConverter
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UnitConverterTest {

    private lateinit var converter: UnitConverter

    @Before
    fun setUp() {
        converter = UnitConverter()
    }

    @Test
    fun testDistanceConversion() {
        val result = converter.convert(5.0, "miles", "km")
        assertTrue(result.contains("8.0467 km"))
    }

    @Test
    fun testTemperatureConversion() {
        val result = converter.convert(100.0, "c", "f")
        assertTrue(result.contains("212 F"))
    }

    @Test
    fun testWeightConversion() {
        val result = converter.convert(10.0, "kg", "lbs")
        assertTrue(result.contains("22.0462 lbs"))
    }
}
