package com.bright.supportassistant.repository

import com.bright.supportassistant.model.TicketAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketAttachmentRepository : JpaRepository<TicketAttachment, Int> {
    fun findByTicketId(ticketId: Int): List<TicketAttachment>
}