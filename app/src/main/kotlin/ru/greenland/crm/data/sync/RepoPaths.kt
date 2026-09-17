package ru.greenland.crm.data.sync

/** Схема файлов в GitHub-репозитории: одна запись — один JSON-файл, легко мержить по отдельности. */
object RepoPaths {
    fun client(id: String) = "data/clients/$id.json"
    fun master(id: String) = "data/masters/$id.json"
    fun order(id: String) = "data/orders/$id.json"
    fun message(orderId: String, id: String) = "data/messages/$orderId/$id.json"
    fun photoMeta(orderId: String, id: String) = "data/photos/$orderId/$id.json"
    fun photoBinary(orderId: String, id: String) = "photos/$orderId/$id.jpg"

    // Человекочитаемые версии — их удобно открыть прямо на github.com.
    fun clientDoc(id: String) = "docs/clients/$id.md"
    fun masterDoc(id: String) = "docs/masters/$id.md"
    fun orderDoc(id: String) = "docs/orders/$id.md"
}
