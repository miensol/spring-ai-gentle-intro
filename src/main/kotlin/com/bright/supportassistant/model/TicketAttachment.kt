package com.bright.supportassistant.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ticket_attachments")
data class TicketAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    val ticket: SupportTicket,

    @Column(name = "file_name")
    val fileName: String,

    @Column(name = "content_type")
    val contentType: String,

    @Column(columnDefinition = "TEXT")
    val content: String? = null,

    @Column(name = "binary_content")
    val binaryContent: ByteArray? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TicketAttachment

        if (id != other.id) return false
        if (ticket.id != other.ticket.id) return false
        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (content != other.content) return false
        if (binaryContent != null) {
            if (other.binaryContent == null) return false
            if (!binaryContent.contentEquals(other.binaryContent)) return false
        } else if (other.binaryContent != null) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (ticket.id ?: 0)
        result = 31 * result + fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + (binaryContent?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "TicketAttachment(id=$id, ticketId=${ticket.id}, fileName='$fileName', contentType='$contentType', createdAt=$createdAt)"
    }
}