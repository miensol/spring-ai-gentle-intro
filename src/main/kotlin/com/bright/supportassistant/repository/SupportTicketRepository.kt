package com.bright.supportassistant.repository

import com.bright.supportassistant.model.SupportTicket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SupportTicketRepository : JpaRepository<SupportTicket, Int> {
    // Basic repository methods are inherited from JpaRepository
}
