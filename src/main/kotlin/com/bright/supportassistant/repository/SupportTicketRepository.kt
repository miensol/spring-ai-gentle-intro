package com.bright.supportassistant.repository

import com.bright.supportassistant.model.SupportTicket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SupportTicketRepository : JpaRepository<SupportTicket, Long> {

    @Query(
        value = """
        SELECT * FROM support_tickets
        WHERE embedding <=> (:embedding)::vector < :threshold
        ORDER BY embedding <=> (:embedding)::vector
        LIMIT :limit
    """,
        nativeQuery = true
    )
    fun findSimilarTickets(
        @Param("embedding") embedding: FloatArray,
        @Param("limit") limit: Int,
        @Param("threshold") threshold: Float = 0.5f, // 0 perfect matches, 1 different concept, 2 opposite
    ): List<SupportTicket>
}
