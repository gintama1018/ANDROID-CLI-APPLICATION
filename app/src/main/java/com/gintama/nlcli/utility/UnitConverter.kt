package com.gintama.nlcli.utility

import java.text.DecimalFormat
import java.util.Locale

class UnitConverter {

    private val df = DecimalFormat("#.####")

    fun convert(value: Double, fromUnit: String, toUnit: String): String {
        val from = normalizeUnit(fromUnit)
        val to = normalizeUnit(toUnit)

        // Temperature conversions
        if (isTemperature(from) && isTemperature(to)) {
            val result = convertTemperature(value, from, to)
            return "${df.format(value)} ${from.uppercase()} = ${df.format(result)} ${to.uppercase()}"
        }

        // Distance conversions (base: meter)
        val distanceFactors = mapOf(
            "km" to 1000.0,
            "m" to 1.0,
            "cm" to 0.01,
            "mm" to 0.001,
            "mi" to 1609.344,
            "yd" to 0.9144,
            "ft" to 0.3048,
            "in" to 0.0254
        )
        if (distanceFactors.containsKey(from) && distanceFactors.containsKey(to)) {
            val meters = value * distanceFactors[from]!!
            val result = meters / distanceFactors[to]!!
            return "${df.format(value)} $from = ${df.format(result)} $to"
        }

        // Weight conversions (base: gram)
        val weightFactors = mapOf(
            "kg" to 1000.0,
            "g" to 1.0,
            "mg" to 0.001,
            "lb" to 453.59237,
            "lbs" to 453.59237,
            "oz" to 28.349523125,
            "ton" to 1000000.0
        )
        if (weightFactors.containsKey(from) && weightFactors.containsKey(to)) {
            val grams = value * weightFactors[from]!!
            val result = grams / weightFactors[to]!!
            return "${df.format(value)} $from = ${df.format(result)} $to"
        }

        // Speed conversions (base: m/s)
        val speedFactors = mapOf(
            "kmh" to 0.277778,
            "km/h" to 0.277778,
            "mph" to 0.44704,
            "ms" to 1.0,
            "m/s" to 1.0,
            "knot" to 0.514444
        )
        if (speedFactors.containsKey(from) && speedFactors.containsKey(to)) {
            val ms = value * speedFactors[from]!!
            val result = ms / speedFactors[to]!!
            return "${df.format(value)} $from = ${df.format(result)} $to"
        }

        // Digital Storage conversions (base: byte)
        val dataFactors = mapOf(
            "b" to 1.0,
            "kb" to 1024.0,
            "mb" to 1024.0 * 1024.0,
            "gb" to 1024.0 * 1024.0 * 1024.0,
            "tb" to 1024.0 * 1024.0 * 1024.0 * 1024.0
        )
        if (dataFactors.containsKey(from) && dataFactors.containsKey(to)) {
            val bytes = value * dataFactors[from]!!
            val result = bytes / dataFactors[to]!!
            return "${df.format(value)} ${from.uppercase()} = ${df.format(result)} ${to.uppercase()}"
        }

        throw IllegalArgumentException("Unsupported unit conversion from '$fromUnit' to '$toUnit'")
    }

    private fun normalizeUnit(unit: String): String {
        return when (unit.lowercase().trim()) {
            "kilometer", "kilometers", "kms" -> "km"
            "meter", "meters" -> "m"
            "centimeter", "centimeters" -> "cm"
            "millimeter", "millimeters" -> "mm"
            "mile", "miles" -> "mi"
            "yard", "yards" -> "yd"
            "foot", "feet" -> "ft"
            "inch", "inches" -> "in"
            "celsius", "c", "centigrade" -> "c"
            "fahrenheit", "f" -> "f"
            "kelvin", "k" -> "k"
            "kilogram", "kilograms", "kgs" -> "kg"
            "gram", "grams" -> "g"
            "pound", "pounds" -> "lbs"
            "ounce", "ounces" -> "oz"
            else -> unit.lowercase().trim()
        }
    }

    private fun isTemperature(u: String) = u in listOf("c", "f", "k")

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val celsius = when (from) {
            "c" -> value
            "f" -> (value - 32.0) * (5.0 / 9.0)
            "k" -> value - 273.15
            else -> value
        }
        return when (to) {
            "c" -> celsius
            "f" -> (celsius * (9.0 / 5.0)) + 32.0
            "k" -> celsius + 273.15
            else -> celsius
        }
    }
}
