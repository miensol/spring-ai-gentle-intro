package com.bright.supportassistant.service

import com.bright.supportassistant.model.SupportTicket
import com.bright.supportassistant.repository.SupportTicketRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.uuid.Uuid

@Service
class ResponseSuggestionService(
    private val supportTicketRepository: SupportTicketRepository,
    private val vectorStore: VectorStore,
    private val attachmentService: AttachmentService,
    chatClientBuilder: ChatClient.Builder
) {

    private val chatClient = chatClientBuilder.build()

    fun suggestResponse(customerMessage: String, limit: Int = 5): String {
        // Search for similar documents in the vector store
        val searchRequest = SearchRequest.builder()
            .query(customerMessage)
            .topK(limit)
            .similarityThreshold(0.5)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // If no similar documents found, return a default message
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "I don't have enough information to provide a specific answer. Could you please provide more details?"
        }

        // Generate context from similar documents
        val context = similarDocuments.joinToString("\n\n") { document ->
            val metadata = document.metadata
            if (metadata["type"] == "ticket") {
                """
                Customer: ${metadata["customerMessage"]}
                Agent: ${metadata["agentResponse"]}
                """
            } else {
                """
                Attachment: ${metadata["fileName"]}
                Content: ${document.text ?: "No content available"}
                """
            }
        }

        // Generate response using LLM
        return generateLLMResponse(customerMessage, context)
    }

    @Transactional
    fun createTicket(ticket: SupportTicket): SupportTicket {
        // Save the ticket to the database
        val savedTicket = supportTicketRepository.save(ticket)

        // Create a document for the ticket
        val ticketDocument = Document.builder()
            .id(UUID.randomUUID().toString())
            .text("${savedTicket.title} ${savedTicket.customerMessage} ${savedTicket.agentResponse}")
            .metadata(mapOf(
                "type" to "ticket",
                "ticketId" to (savedTicket.id ?: 0),
                "title" to savedTicket.title,
                "customerMessage" to savedTicket.customerMessage,
                "agentResponse" to savedTicket.agentResponse,
                "category" to savedTicket.category,
                "status" to savedTicket.status.name
            ))
            .build()

        // Add the ticket document to the vector store
        vectorStore.add(listOf(ticketDocument))

        // Process attachments if any
        if (savedTicket.attachments.isNotEmpty()) {
            val attachmentDocuments = savedTicket.attachments.map { 
                attachmentService.attachmentToDocument(it)
            }

            // Add attachment documents to the vector store
            if (attachmentDocuments.isNotEmpty()) {
                vectorStore.add(attachmentDocuments)
            }
        }

        return savedTicket
    }

    /**
     * Get a ticket by ID
     */
    fun getTicket(ticketId: Int): SupportTicket? {
        return supportTicketRepository.findById(ticketId).orElse(null)
    }

    /**
     * Update a ticket in the vector store
     */
    @Transactional
    fun updateTicketInVectorStore(ticket: SupportTicket) {
        // Delete existing documents for this ticket
        vectorStore.delete("ticket-${ticket.id}")

        // Create a new document for the ticket
        val ticketDocument = Document.builder()
            .id("ticket-${ticket.id}")
            .text("${ticket.title} ${ticket.customerMessage} ${ticket.agentResponse}")
            .metadata(mapOf(
                "type" to "ticket",
                "ticketId" to (ticket.id ?: 0),
                "title" to ticket.title,
                "customerMessage" to ticket.customerMessage,
                "agentResponse" to ticket.agentResponse,
                "category" to ticket.category,
                "status" to ticket.status.name
            ))
            .build()

        // Add the ticket document to the vector store
        vectorStore.add(listOf(ticketDocument))

        // Process attachments
        if (ticket.attachments.isNotEmpty()) {
            // Delete existing attachment documents
            ticket.attachments.forEach { attachment ->
                if (attachment.id != null) {
                    vectorStore.delete("attachment-${attachment.id}")
                }
            }

            // Add new attachment documents
            val attachmentDocuments = ticket.attachments.map { 
                attachmentService.attachmentToDocument(it)
            }

            if (attachmentDocuments.isNotEmpty()) {
                vectorStore.add(attachmentDocuments)
            }
        }
    }

    private fun generateLLMResponse(customerMessage: String, context: String): String {
        val systemMessage = """
            You are a helpful customer support assistant. Use the following previous support conversations to help answer the customer's question.
            If you can't find a relevant answer in the examples, politely say you don't have enough information.

            Previous support conversations:
            $context
        """.trimIndent()

        val prompt = Prompt(
            listOf(
                SystemMessage(systemMessage),
                UserMessage(customerMessage)
            )
        )

        val response = chatClient.prompt(prompt).call()

        return response.content()!!
    }
}
