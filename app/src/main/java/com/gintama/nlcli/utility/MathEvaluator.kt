package com.gintama.nlcli.utility

import java.text.DecimalFormat
import kotlin.math.pow

class MathEvaluator {

    fun evaluate(expression: String): String {
        val sanitized = expression
            .replace("x", "*", ignoreCase = true)
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        if (sanitized.isBlank()) return "0"

        val parser = ExpressionParser(sanitized)
        val result = parser.parse()

        val format = DecimalFormat("#.########")
        return format.format(result)
    }

    private class ExpressionParser(private val input: String) {
        private var pos = 0
        private var ch = if (input.isNotEmpty()) input[0] else '\u0000'

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Unexpected character '${ch}' at index $pos")
            return x
        }

        // Grammar:
        // expression = term | expression `+` term | expression `-` term
        // term = factor | term `*` factor | term `/` factor | term `%` factor
        // factor = `+` factor | `-` factor | `(` expression `)` | number | factor `^` factor

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return +parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing closing parenthesis")
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected token '${ch}' at index $pos")
            }

            if (eat('^')) x = x.pow(parseFactor())

            return x
        }
    }
}
