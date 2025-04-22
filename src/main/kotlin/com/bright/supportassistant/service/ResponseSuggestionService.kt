package com.bright.supportassistant.service

import com.bright.supportassistant.model.SupportTicket
import com.bright.supportassistant.model.TicketAttachment
import com.bright.supportassistant.repository.SupportTicketRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class ResponseSuggestionService(
    private val supportTicketRepository: SupportTicketRepository,
    private val vectorStore: VectorStore,
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
            .metadata(
                mapOf(
                    "type" to "ticket",
                    "ticketId" to (savedTicket.id ?: 0),
                    "title" to savedTicket.title,
                    "customerMessage" to savedTicket.customerMessage,
                    "agentResponse" to savedTicket.agentResponse,
                    "category" to savedTicket.category,
                    "status" to savedTicket.status.name
                )
            )
            .build()

        vectorStore.add(listOf(ticketDocument))

        return savedTicket
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

    @Transactional
    fun attachAttachment(ticketId: Int, file: MultipartFile): TicketAttachment {
        val ticket = supportTicketRepository.getReferenceById(ticketId)

        ticket.addAttachment(file)

        supportTicketRepository.save(ticket)

        val attachment = ticket.attachments.last()

        vectorStore.add(listOf(attachmentToDocument(attachment)))

        return attachment
    }
}
