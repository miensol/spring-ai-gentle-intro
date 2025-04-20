package com.bright.supportassistant.controller

import com.bright.supportassistant.model.SupportTicket
import com.bright.supportassistant.model.TicketStatus
import com.bright.supportassistant.repository.SupportTicketRepository
import com.bright.supportassistant.service.ResponseSuggestionService
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/support")
class SupportAssistantController(
    private val responseSuggestionService: ResponseSuggestionService,
    private val supportTicketRepository: SupportTicketRepository,
    private val embeddingModel: EmbeddingModel
) {

    @PostMapping("/suggest")
    fun suggestResponse(@RequestBody request: SuggestResponseRequest): SuggestResponseResponse {
        val suggestion = responseSuggestionService.suggestResponse(request.customerMessage)
        return SuggestResponseResponse(suggestion)
    }

    @PostMapping("/tickets")
    fun createTicket(@RequestBody request: CreateTicketRequest): SupportTicket {
        val embedding = embeddingModel.embed(
            "${request.title} ${request.customerMessage} ${request.agentResponse}"
        )

        val ticket = SupportTicket(
            title = request.title,
            customerMessage = request.customerMessage,
            agentResponse = request.agentResponse,
            category = request.category,
            status = TicketStatus.valueOf(request.status),
            embedding = embedding
        )

        return supportTicketRepository.save(ticket)
    }

    data class SuggestResponseRequest(
        val customerMessage: String
    )

    data class SuggestResponseResponse(
        val suggestion: String
    )

    data class CreateTicketRequest(
        val title: String,
        val customerMessage: String,
        val agentResponse: String,
        val category: String,
        val status: String
    )
}
