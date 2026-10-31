package com.pharmatrade.feature.notification.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationDtoTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = true; isLenient = true }

    private fun parse(raw: String) = json.decodeFromString<NotificationDto>(raw).toDomain()

    @Test
    fun flatShapeKeepsItsType() {
        val n = parse(
            """{"id":320,"title":"t","body":"b","type":"drugs_missing_english",
                "notifiable_type":"InventoryUploadLog","notifiable_id":15,"is_read":false}"""
        )
        assertEquals("drugs_missing_english", n.type)
        assertEquals("InventoryUploadLog", n.notifiableType)
        assertEquals("15", n.notifiableId)
    }

    @Test
    fun laravelStoredShapeReadsTheTypeFromData() {
        val n = parse(
            """{"id":"9b1c","type":"App\\Notifications\\DrugsMissingEnglish","read_at":null,
                "data":{"type":"drugs_missing_english","title":"أدوية جديدة","body":"تم إضافة 2 دواء",
                        "notifiable_type":"InventoryUploadLog","notifiable_id":"15"}}"""
        )
        assertEquals("drugs_missing_english", n.type)
        assertEquals("أدوية جديدة", n.title)
        assertEquals("InventoryUploadLog", n.notifiableType)
        assertEquals("15", n.notifiableId)
    }

    @Test
    fun nonObjectDataDoesNotBreakParsing() {
        val n = parse("""{"id":1,"type":"new_order","notifiable_id":7,"data":"[]"}""")
        assertEquals("new_order", n.type)
        assertEquals("7", n.notifiableId)
    }
}
