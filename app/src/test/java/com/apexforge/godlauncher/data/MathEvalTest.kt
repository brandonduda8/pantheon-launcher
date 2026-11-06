package com.apexforge.godlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure JVM tests for the universal-search calculator row. */
class MathEvalTest {

    private fun eval(q: String): Double? = MathEval.tryEvaluate(q)

    @Test
    fun basicArithmetic() {
        assertEquals(4.0, eval("2+2")!!, 0.0)
        assertEquals(-1.0, eval("2-3")!!, 0.0)
        assertEquals(12.0, eval("3*4")!!, 0.0)
        assertEquals(2.5, eval("5/2")!!, 0.0)
    }

    @Test
    fun operatorPrecedenceAndParentheses() {
        assertEquals(14.0, eval("2+3*4")!!, 0.0)
        assertEquals(20.0, eval("(2+3)*4")!!, 0.0)
        assertEquals(1.0, eval("10/5/2")!!, 0.0) // left-associative
        assertEquals(5.0, eval("10-3-2")!!, 0.0) // left-associative
        assertEquals(9.0, eval("((1+2))*(3)")!!, 0.0)
    }

    @Test
    fun unaryOperatorsDecimalsAndSpaces() {
        assertEquals(-2.0, eval("-5+3")!!, 0.0)
        assertEquals(6.0, eval("-2*-3")!!, 0.0)
        assertEquals(-3.0, eval("-(1+2)")!!, 0.0)
        assertEquals(0.3, eval("0.1+0.2")!!, 1e-12)
        assertEquals(7.0, eval("  1 + 2 * 3  ")!!, 0.0)
    }

    @Test
    fun nonMathQueriesReturnNull() {
        assertNull(eval(""))
        assertNull(eval("   "))
        assertNull(eval("camera"))
        assertNull(eval("42"))        // a bare number is not a calculation
        assertNull(eval("+-*/"))      // operators with no digits
        assertNull(eval("2^3"))       // unsupported operator
        assertNull(eval("2+x"))
    }

    @Test
    fun malformedExpressionsReturnNull() {
        assertNull(eval("2+"))
        assertNull(eval("(2+3"))
        assertNull(eval("2+3)"))
        assertNull(eval("1.2.3+1"))
        assertNull(eval("()+1"))
    }

    @Test
    fun nonFiniteResultsReturnNull() {
        assertNull(eval("1/0"))
        assertNull(eval("-1/0"))
        assertNull(eval("0/0"))
    }

    @Test
    fun formatDropsTrailingZeros() {
        assertEquals("4", MathEval.format(4.0))
        assertEquals("-12", MathEval.format(-12.0))
        assertEquals("2.5", MathEval.format(2.5))
        assertEquals("0.3333333333", MathEval.format(1.0 / 3.0))
        assertEquals("0.3", MathEval.format(0.1 + 0.2))
    }
}
