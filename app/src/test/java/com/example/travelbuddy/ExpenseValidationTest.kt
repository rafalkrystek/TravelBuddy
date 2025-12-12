package com.example.travelbuddy

import org.junit.Assert.*
import org.junit.Test

class ExpenseValidationTest {
    @Test
    fun `test empty name validation`() {
        val name = ""
        val amountText = "100"
        val hasNameError = name.isEmpty()
        val amount = amountText.toIntOrNull()
        val hasAmountError = amount == null || amount <= 0
        assertTrue("Nazwa powinna być błędna", hasNameError)
        assertFalse("Kwota powinna być poprawna", hasAmountError)
    }

    @Test
    fun `test empty amount validation`() {
        val name = "Test expense"
        val amountText = ""
        val hasNameError = name.isEmpty()
        val amount = amountText.toIntOrNull()
        val hasAmountError = amount == null || amount <= 0
        assertFalse("Nazwa powinna być poprawna", hasNameError)
        assertTrue("Kwota powinna być błędna", hasAmountError)
    }

    @Test
    fun `test invalid amount validation`() {
        val name = "Test expense"
        val amountText = "abc"
        val hasNameError = name.isEmpty()
        val amount = amountText.toIntOrNull()
        val hasAmountError = amount == null || amount <= 0
        assertFalse("Nazwa powinna być poprawna", hasNameError)
        assertTrue("Kwota powinna być błędna", hasAmountError)
    }

    @Test
    fun `test zero amount validation`() {
        val name = "Test expense"
        val amountText = "0"
        val hasNameError = name.isEmpty()
        val amount = amountText.toIntOrNull()
        val hasAmountError = amount == null || amount <= 0
        assertFalse("Nazwa powinna być poprawna", hasNameError)
        assertTrue("Kwota powinna być błędna (zero)", hasAmountError)
    }

    @Test
    fun `test negative amount validation`() {
        val name = "Test expense"
        val amountText = "-100"
        val hasNameError = name.isEmpty()
        val amount = amountText.toIntOrNull()
        val hasAmountError = amount == null || amount <= 0
        assertFalse("Nazwa powinna być poprawna", hasNameError)
        assertTrue("Kwota powinna być błędna (ujemna)", hasAmountError)
    }

    @Test
    fun `test valid expense`() {
        val name = "Test expense"
        val amountText = "100"
        val hasNameError = name.isEmpty()
        val amount = amountText.toIntOrNull()
        val hasAmountError = amount == null || amount <= 0
        val hasError = hasNameError || hasAmountError
        assertFalse("Nazwa powinna być poprawna", hasNameError)
        assertFalse("Kwota powinna być poprawna", hasAmountError)
        assertFalse("Nie powinno być błędów", hasError)
        assertNotNull("Kwota powinna być parsowalna", amount)
        assertEquals("Kwota powinna być 100", 100, amount)
    }

    @Test
    fun `test expense creation`() {
        val expense = TripBudgetCalculatorActivity.Expense(
            id = "",
            category = "Hotel",
            name = "Test Hotel",
            amount = 500
        )
        assertEquals("ID powinno być puste", "", expense.id)
        assertEquals("Kategoria powinna być Hotel", "Hotel", expense.category)
        assertEquals("Nazwa powinna być Test Hotel", "Test Hotel", expense.name)
        assertEquals("Kwota powinna być 500", 500, expense.amount)
    }
}
