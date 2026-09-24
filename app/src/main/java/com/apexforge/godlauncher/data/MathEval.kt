package com.apexforge.godlauncher.data

/**
 * Safe arithmetic evaluator for universal search's calculator row.
 * Tiny recursive-descent parser over + - * / ( ) with unary minus —
 * deliberately NOT a script engine (no eval(), no ScriptEngine).
 * Returns null for anything that isn't a valid math expression.
 */
object MathEval {

    private val VALID = Regex("^[0-9+\\-*/(). ]+$")

    /** Returns the result, or null when the query isn't evaluable math. */
    fun tryEvaluate(input: String): Double? {
        val q = input.trim()
        // Needs at least one digit and one operator to be a "calculation".
        if (q.isEmpty() || !VALID.matches(q)) return null
        if (!q.any { it.isDigit() }) return null
        if (!q.any { it == '+' || it == '-' || it == '*' || it == '/' }) return null
        return try {
            Parser(q).parse().takeIf { it.isFinite() }
        } catch (_: Exception) {
            null
        }
    }

    /** Display formatting: drop ".0" for whole numbers, trim noise otherwise. */
    fun format(result: Double): String =
        if (result == kotlin.math.floor(result) && kotlin.math.abs(result) < 1e15) {
            result.toLong().toString()
        } else {
            "%.10f".format(result).trimEnd('0').trimEnd('.')
        }

    private class Parser(private val s: String) {
        private var i = 0

        fun parse(): Double {
            val v = expr()
            skipSpaces()
            if (i != s.length) throw IllegalArgumentException("trailing input")
            return v
        }

        // expr := term (('+'|'-') term)*
        private fun expr(): Double {
            var v = term()
            while (true) {
                skipSpaces()
                v = when {
                    match('+') -> v + term()
                    match('-') -> v - term()
                    else -> return v
                }
            }
        }

        // term := factor (('*'|'/') factor)*
        private fun term(): Double {
            var v = factor()
            while (true) {
                skipSpaces()
                v = when {
                    match('*') -> v * factor()
                    match('/') -> v / factor()
                    else -> return v
                }
            }
        }

        // factor := '-' factor | '(' expr ')' | number
        private fun factor(): Double {
            skipSpaces()
            return when {
                match('-') -> -factor()
                match('+') -> factor()
                match('(') -> {
                    val v = expr()
                    skipSpaces()
                    if (!match(')')) throw IllegalArgumentException("missing )")
                    v
                }
                else -> number()
            }
        }

        private fun number(): Double {
            skipSpaces()
            val start = i
            var dotSeen = false
            while (i < s.length && (s[i].isDigit() || (s[i] == '.' && !dotSeen))) {
                if (s[i] == '.') dotSeen = true
                i++
            }
            if (start == i) throw IllegalArgumentException("expected number at $i")
            return s.substring(start, i).toDouble()
        }

        private fun match(c: Char): Boolean {
            skipSpaces()
            if (i < s.length && s[i] == c) {
                i++
                return true
            }
            return false
        }

        private fun skipSpaces() {
            while (i < s.length && s[i] == ' ') i++
        }
    }
}
