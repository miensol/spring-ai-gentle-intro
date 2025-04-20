package com.bright.supportassistant.model

import jakarta.persistence.*
import org.hibernate.annotations.Array
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
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

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding")
    @Array(length = 1024) // must match dimensions of the embedding model
    val embedding: FloatArray? = null
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
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

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
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "SupportTicket(createdAt=$createdAt, status=$status, category='$category', agentResponse='$agentResponse', customerMessage='$customerMessage', title='$title', id=$id)"
    }


}

enum class TicketStatus {
    OPEN, RESOLVED, CLOSED
}
