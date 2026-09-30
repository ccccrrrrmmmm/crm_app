package ru.greenland.crm.ui.broadcast

/**
 * Чистая логика рассылки без зависимостей от Android/Room — специально вынесена отдельно,
 * чтобы покрывалась обычными JVM-юнит-тестами.
 */

/**
 * Подставляет имя клиента в шаблон: `{имя}` → первое слово из ФИО, `{фио}` → полное имя.
 * Регистр плейсхолдера не важен.
 */
fun renderBroadcast(template: String, fullName: String): String {
    val trimmed = fullName.trim()
    val firstName = trimmed.substringBefore(' ')
    return template
        .replace("{имя}", firstName, ignoreCase = true)
        .replace("{фио}", trimmed, ignoreCase = true)
}

/**
 * Приводит телефон к формату для SMS: международный вид с ведущим «+».
 * Российские номера, записанные как 8XXX…, переводит в +7XXX…; 10 цифр считает российскими.
 * Возвращает null, если из строки не выходит правдоподобный номер.
 */
fun normalizePhoneForSms(phone: String): String? {
    val digits = phone.filter { it.isDigit() }
    val normalized = when {
        digits.length == 11 && digits.startsWith("8") -> "7" + digits.drop(1)
        digits.length == 10 -> "7$digits"
        digits.length in 11..15 -> digits
        else -> return null
    }
    return "+$normalized"
}
