package com.bright.supportassistant.service

import com.bright.supportassistant.repository.SupportTicketRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

@Service
class ResponseSuggestionService(
    private val embeddingModel: EmbeddingModel,
    private val supportTicketRepository: SupportTicketRepository,
    chatClientBuilder: ChatClient.Builder
) {

    private val chatClient = chatClientBuilder.build()

    fun suggestResponse(customerMessage: String, limit: Int = 5): String {
        // Generate embedding for the customer message
        val embedding = embeddingModel.embed(customerMessage)

        // Find similar tickets
        val similarTickets = supportTicketRepository.findSimilarTickets(embedding, limit)

        // If no similar tickets found, return a default message
        if (similarTickets.isEmpty()) {
            return "I don't have enough information to provide a specific answer. Could you please provide more details?"
        }

        // Generate context from similar tickets
        val context = similarTickets.joinToString("\n\n") { ticket ->
            """
            Customer: ${ticket.customerMessage}
            Agent: ${ticket.agentResponse}
            """
        }

        // Generate response using LLM
        return generateLLMResponse(customerMessage, context)
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
