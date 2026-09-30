package ru.greenland.crm.ui.broadcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BroadcastTextTest {

    // ---- renderBroadcast ----

    @Test
    fun `подставляет имя и фио`() {
        val result = renderBroadcast("Здравствуйте, {имя}! Это {фио}.", "Иван Петров")
        assertEquals("Здравствуйте, Иван! Это Иван Петров.", result)
    }

    @Test
    fun `имя из одного слова`() {
        assertEquals("Привет, Иван", renderBroadcast("Привет, {имя}", "Иван"))
    }

    @Test
    fun `плейсхолдер регистронезависим`() {
        assertEquals("Иван", renderBroadcast("{ИмЯ}", "Иван Петров"))
    }

    @Test
    fun `лишние пробелы в фио обрезаются`() {
        assertEquals("Иван / Иван Петров", renderBroadcast("{имя} / {фио}", "  Иван Петров  "))
    }

    @Test
    fun `текст без плейсхолдеров не меняется`() {
        assertEquals("Просто текст", renderBroadcast("Просто текст", "Иван Петров"))
    }

    // ---- normalizePhoneForSms ----

    @Test
    fun `формат плюс7 с пробелами и скобками`() {
        assertEquals("+79001234567", normalizePhoneForSms("+7 (900) 123-45-67"))
    }

    @Test
    fun `восьмёрка превращается в семёрку`() {
        assertEquals("+79001234567", normalizePhoneForSms("8 900 123 45 67"))
    }

    @Test
    fun `десять цифр считаются российскими`() {
        assertEquals("+79001234567", normalizePhoneForSms("9001234567"))
    }

    @Test
    fun `слишком короткий номер отклоняется`() {
        assertNull(normalizePhoneForSms("12345"))
    }

    @Test
    fun `пустая строка отклоняется`() {
        assertNull(normalizePhoneForSms(""))
    }

    @Test
    fun `буквы без цифр отклоняются`() {
        assertNull(normalizePhoneForSms("нет телефона"))
    }
}
