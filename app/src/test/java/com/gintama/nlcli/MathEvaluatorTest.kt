package com.gintama.nlcli

import com.gintama.nlcli.utility.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MathEvaluatorTest {

    private lateinit var evaluator: MathEvaluator

    @Before
    fun setUp() {
        evaluator = MathEvaluator()
    }

    @Test
    fun testBasicArithmetic() {
        assertEquals("15", evaluator.evaluate("10 + 5"))
        assertEquals("6", evaluator.evaluate("10 - 4"))
        assertEquals("50", evaluator.evaluate("10 * 5"))
        assertEquals("4", evaluator.evaluate("20 / 5"))
    }

    @Test
    fun testPrecedenceAndParentheses() {
        assertEquals("81", evaluator.evaluate("(450 * 18) / 100"))
        assertEquals("26", evaluator.evaluate("2 + 3 * 8"))
        assertEquals("40", evaluator.evaluate("(2 + 3) * 8"))
    }

    @Test
    fun testPowerAndModulo() {
        assertEquals("1074", evaluator.evaluate("2^10 + 50"))
        assertEquals("2", evaluator.evaluate("17 % 5"))
    }

    @Test
    fun testDecimals() {
        assertEquals("7.5", evaluator.evaluate("2.5 * 3"))
        assertEquals("3.14", evaluator.evaluate("1.14 + 2.0"))
    }
}
