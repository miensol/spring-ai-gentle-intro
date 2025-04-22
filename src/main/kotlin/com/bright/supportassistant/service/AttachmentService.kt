package com.bright.supportassistant.service

import com.bright.supportassistant.model.TicketAttachment
import org.springframework.ai.document.Document
import java.util.UUID

fun attachmentToDocument(attachment: TicketAttachment): Document {
    val documentBuilder = Document.builder()
        .id(UUID.randomUUID().toString())
        .metadata(
            mapOf(
                "type" to "attachment",
                "ticketId" to (attachment.ticket.id!!),
                "fileName" to attachment.fileName,
                "contentType" to attachment.contentType
            )
        )
    return when {
        attachment.content != null -> {
            // Text attachment
            documentBuilder
                .text(attachment.content)
                .build()
        }

        attachment.contentType == "application/pdf" && attachment.binaryContent != null -> {
            // PDF attachment - in a real application, you would use a PDF parser here
            // For simplicity, we're just creating a document with text content
            documentBuilder
                .text("PDF attachment: ${attachment.fileName}")
                .build()
        }

        else -> {
            // Other binary attachment - in a real application, you might use different parsers
            // For simplicity, we're just creating a document with metadata
            documentBuilder
                .text("Binary attachment: ${attachment.fileName}")
                .build()
        }
    }
}
