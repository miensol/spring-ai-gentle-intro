package com.bright.supportassistant.controller

import com.bright.supportassistant.model.SupportTicket
import com.bright.supportassistant.model.TicketStatus
import com.bright.supportassistant.service.AttachmentService
import com.bright.supportassistant.service.ResponseSuggestionService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/support")
class SupportAssistantController(
    private val responseSuggestionService: ResponseSuggestionService,
    private val attachmentService: AttachmentService
) {

    @PostMapping("/suggest")
    fun suggestResponse(@RequestBody request: SuggestResponseRequest): SuggestResponseResponse {
        val suggestion = responseSuggestionService.suggestResponse(request.customerMessage)
        return SuggestResponseResponse(suggestion)
    }

    @PostMapping("/tickets")
    fun createTicket(@RequestBody request: CreateTicketRequest): SupportTicket {
        val ticket = SupportTicket(
            title = request.title,
            customerMessage = request.customerMessage,
            agentResponse = request.agentResponse,
            category = request.category,
            status = TicketStatus.valueOf(request.status)
        )

        return responseSuggestionService.createTicket(ticket)
    }

    @PostMapping("/tickets/{ticketId}/attachments", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addAttachmentToTicket(
        @PathVariable ticketId: Int,
        @RequestParam("file") file: MultipartFile
    ): Map<String, String> {
        val ticket = responseSuggestionService.getTicket(ticketId)
            ?: throw IllegalArgumentException("Ticket not found with ID: $ticketId")

        val attachment = attachmentService.saveAttachment(ticket, file)

        // Update the ticket in the vector store
        responseSuggestionService.updateTicketInVectorStore(ticket)

        return mapOf(
            "message" to "Attachment added successfully",
            "attachmentId" to attachment.id.toString(),
            "fileName" to attachment.fileName
        )
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
