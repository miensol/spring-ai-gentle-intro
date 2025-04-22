package com.bright.supportassistant.model

import jakarta.persistence.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Entity
@Table(name = "support_tickets")
data class SupportTicket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val title: String,

    @Column(columnDefinition = "TEXT")
    val customerMessage: String,

    @Column(columnDefinition = "TEXT")
    val agentResponse: String,

    val category: String,

    @Enumerated(EnumType.STRING)
    val status: TicketStatus,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "ticket", cascade = [CascadeType.ALL], orphanRemoval = true)
    val attachments: MutableList<TicketAttachment> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SupportTicket

        if (id != other.id) return false
        if (title != other.title) return false
        if (customerMessage != other.customerMessage) return false
        if (agentResponse != other.agentResponse) return false
        if (category != other.category) return false
        if (status != other.status) return false
        if (createdAt != other.createdAt) return false
        // Not comparing attachments to avoid circular reference issues

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + customerMessage.hashCode()
        result = 31 * result + agentResponse.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + createdAt.hashCode()
        // Not including attachments in hashCode to avoid circular reference issues
        return result
    }

    override fun toString(): String {
        return "SupportTicket(id=$id, title='$title', category='$category', status=$status, createdAt=$createdAt, attachmentsCount=${attachments.size})"
    }

    fun addAttachment(file: MultipartFile) {
        val attachment = toAttachment(file)
        attachments.add(attachment)
    }

    fun SupportTicket.toAttachment(file: MultipartFile): TicketAttachment {
        val contentType = file.contentType ?: "application/octet-stream"
        val fileName = file.originalFilename ?: "unnamed-file"
        return when {
            contentType == "text/plain" -> {
                // For text files, store as text
                TicketAttachment(
                    ticket = this,
                    fileName = fileName,
                    contentType = contentType,
                    content = String(file.bytes)
                )
            }

            else -> {
                // Default case, store as binary
                TicketAttachment(
                    ticket = this,
                    fileName = fileName,
                    contentType = contentType,
                    binaryContent = file.bytes
                )
            }
        }
    }

    }

    enum class TicketStatus {
        OPEN, RESOLVED, CLOSED
    }
