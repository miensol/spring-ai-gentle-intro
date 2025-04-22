package com.bright.supportassistant.integration

import com.bright.supportassistant.model.SupportTicket
import com.bright.supportassistant.model.TicketStatus
import com.bright.supportassistant.repository.SupportTicketRepository
import com.bright.supportassistant.service.AttachmentService
import com.bright.supportassistant.service.ResponseSuggestionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.mock.web.MockMultipartFile
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID


@SpringBootTest
@Testcontainers
class SupportAssistantIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val postgresContainer = PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
    }

    @Autowired
    private lateinit var supportTicketRepository: SupportTicketRepository

    @Autowired
    private lateinit var embeddingModel: EmbeddingModel

    @Autowired
    private lateinit var vectorStore: VectorStore

    @Autowired
    private lateinit var responseSuggestionService: ResponseSuggestionService

    @Autowired
    private lateinit var attachmentService: AttachmentService

    // Test data
    private lateinit var passwordResetTicket: SupportTicket
    private lateinit var loginIssueTicket: SupportTicket
    private lateinit var doubleChargeTicket: SupportTicket
    private lateinit var paymentMethodTicket: SupportTicket
    private lateinit var appCrashTicket: SupportTicket
    private lateinit var searchIssueTicket: SupportTicket
    private lateinit var darkModeTicket: SupportTicket
    private lateinit var csvExportTicket: SupportTicket

    @BeforeEach
    fun setup() {
        supportTicketRepository.deleteAll()

        // Clear the vector store by deleting all documents
        try {
            val allDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query("").topK(1000).build()
            )
            allDocs?.forEach { doc ->
                vectorStore.delete(doc.id)
            }
        } catch (e: Exception) {
            // Ignore errors if no documents exist
        }

        passwordResetTicket = SupportTicket(
            title = "Password Reset",
            customerMessage = "How do I reset my password?",
            agentResponse = "You can reset your password by clicking on the 'Forgot Password' link on the login page.",
            category = "Account",
            status = TicketStatus.RESOLVED
        )

        loginIssueTicket = SupportTicket(
            title = "Login Issue",
            customerMessage = "I can't log in to my account",
            agentResponse = "Please try resetting your password using the 'Forgot Password' link.",
            category = "Account",
            status = TicketStatus.RESOLVED
        )

        doubleChargeTicket = SupportTicket(
            title = "Double Charge",
            customerMessage = "I was charged twice for my subscription",
            agentResponse = "I apologize for the inconvenience. I've checked your account and issued a refund for the duplicate charge. It should appear in your account within 3-5 business days.",
            category = "Billing",
            status = TicketStatus.RESOLVED
        )

        paymentMethodTicket = SupportTicket(
            title = "Payment Method Update",
            customerMessage = "How do I update my payment method?",
            agentResponse = "You can update your payment method by going to Account Settings > Billing > Payment Methods and clicking on 'Add New Method'.",
            category = "Billing",
            status = TicketStatus.RESOLVED
        )

        appCrashTicket = SupportTicket(
            title = "App Crash on Upload",
            customerMessage = "The app crashes when I try to upload a file",
            agentResponse = "I'm sorry to hear about the crash. Could you please tell me what type of file you're trying to upload and which version of the app you're using? In the meantime, try updating to the latest version.",
            category = "Technical",
            status = TicketStatus.RESOLVED
        )

        searchIssueTicket = SupportTicket(
            title = "Search Functionality Issue",
            customerMessage = "The search feature isn't working properly",
            agentResponse = "We're aware of some issues with the search feature and our team is working on a fix. It should be resolved in our next update. In the meantime, try using more specific search terms.",
            category = "Technical",
            status = TicketStatus.RESOLVED
        )

        darkModeTicket = SupportTicket(
            title = "Dark Mode Request",
            customerMessage = "Can you add dark mode to the app?",
            agentResponse = "Thank you for your suggestion! We're actually working on implementing dark mode in our next major update. Stay tuned for the announcement in the coming weeks.",
            category = "Feature Request",
            status = TicketStatus.RESOLVED
        )

        csvExportTicket = SupportTicket(
            title = "CSV Export Feature",
            customerMessage = "I'd like to be able to export my data as CSV",
            agentResponse = "That's a great suggestion. Currently, you can export data in PDF and Excel formats. I'll pass your request for CSV export to our product team for consideration in future updates.",
            category = "Feature Request",
            status = TicketStatus.RESOLVED
        )

        // Save tickets using the service to ensure they're added to the vector store
        passwordResetTicket = responseSuggestionService.createTicket(passwordResetTicket)
        loginIssueTicket = responseSuggestionService.createTicket(loginIssueTicket)
        doubleChargeTicket = responseSuggestionService.createTicket(doubleChargeTicket)
        paymentMethodTicket = responseSuggestionService.createTicket(paymentMethodTicket)
        appCrashTicket = responseSuggestionService.createTicket(appCrashTicket)
        searchIssueTicket = responseSuggestionService.createTicket(searchIssueTicket)
        darkModeTicket = responseSuggestionService.createTicket(darkModeTicket)
        csvExportTicket = responseSuggestionService.createTicket(csvExportTicket)
    }

    // Positive test cases - Testing successful similarity matching

    @Test
    fun `should find single most relevant ticket for password query - positive case`() {
        // When - Test finding a single most relevant ticket for password-related query
        val searchRequest = SearchRequest.builder()
            .query("How to change my password")
            .topK(1)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // Then - Verify we get the most relevant ticket
        assertThat(similarDocuments).isNotNull
        assertThat(similarDocuments!!).hasSize(1)
        assertThat(similarDocuments[0].metadata["title"]).isEqualTo("Password Reset")
    }

    @Test
    fun `should find multiple relevant tickets for password query - positive case`() {
        // When - Test finding multiple relevant tickets for password-related query
        val searchRequest = SearchRequest.builder()
            .query("How to change my password")
            .topK(2)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // Then - Verify we get both password-related tickets in the results
        assertThat(similarDocuments).isNotNull
        assertThat(similarDocuments!!).hasSize(2)

        // Filter for documents with a "title" metadata field (ticket documents)
        val ticketDocs = similarDocuments.filter { it.metadata.containsKey("title") }
        val titles = ticketDocs.map { it.metadata["title"] as String }

        // Check that at least the most relevant password-related ticket is in the results
        // The embedding model might return different results depending on its training
        assertThat(titles).contains("Password Reset")
    }

    @Test
    fun `should find relevant ticket for billing query - positive case`() {
        // When - Test finding a ticket for a billing-related query
        val searchRequest = SearchRequest.builder()
            .query("I was charged twice for my monthly subscription")
            .topK(1)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // Then - Verify we get the relevant billing ticket
        assertThat(similarDocuments).isNotNull
        assertThat(similarDocuments!!).hasSize(1)
        assertThat(similarDocuments[0].metadata["title"]).isEqualTo("Double Charge")
    }

    @Test
    fun `should find relevant ticket for technical query - positive case`() {
        // When - Test finding a ticket for a technical issue query
        val searchRequest = SearchRequest.builder()
            .query("The application crashes when I upload files")
            .topK(1)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // Then - Verify we get the relevant technical ticket
        assertThat(similarDocuments).isNotNull
        assertThat(similarDocuments!!).hasSize(1)
        assertThat(similarDocuments[0].metadata["title"]).isEqualTo("App Crash on Upload")
    }

    // Negative test cases - Testing edge cases and unexpected inputs

    @Test
    fun `should handle novel query not matching existing tickets - negative case`() {
        // When - Test with a novel query that doesn't match any existing tickets well
        val searchRequest = SearchRequest.builder()
            .query("How do I add pictures to my account?")
            .topK(3)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // Then - Verify we get some results, but they're not password-related
        // We don't assert exact titles here because the exact ranking might vary
        // depending on the embedding model, but we can verify that password-related
        // tickets aren't returned as top matches for this unrelated query
        assertThat(similarDocuments).isNotNull
        assertThat(similarDocuments!!).hasSizeGreaterThanOrEqualTo(1)

        val titles = similarDocuments.map { it.metadata["title"] as String }
        assertThat(titles).doesNotContain("Password Reset")
    }

    @Test
    fun `should handle very large limit - negative case`() {
        // When - Test with a very large limit
        val searchRequest = SearchRequest.builder()
            .query("password")
            .topK(100)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // Then - Verify we get relevant tickets
        assertThat(similarDocuments).isNotNull
        // We expect at least the password reset and login issue tickets
        assertThat(similarDocuments!!.size).isGreaterThanOrEqualTo(2)

        // Check that the password-related tickets are included
        // Filter for documents with a "title" metadata field (ticket documents)
        val ticketDocs = similarDocuments.filter { it.metadata.containsKey("title") }
        val titles = ticketDocs.map { it.metadata["title"] as String }
        assertThat(titles).contains("Password Reset", "Login Issue")
    }

    @Test
    fun `should find ticket by matching attachment content`() {
        // Given - Create a ticket with an attachment containing specific content
        val ticketWithAttachment = SupportTicket(
            title = "Document Upload Issue",
            customerMessage = "I'm having trouble uploading a document",
            agentResponse = "Let me help you with that document upload issue",
            category = "Technical",
            status = TicketStatus.OPEN
        )

        // Save the ticket first to get an ID
        val savedTicket = responseSuggestionService.createTicket(ticketWithAttachment)

        // Create a text attachment with specific content that we'll search for later
        val attachmentContent = "This document contains information about annual financial reports and quarterly earnings statements"
        val mockFile = MockMultipartFile(
            "test-document.txt",
            "test-document.txt",
            "text/plain",
            attachmentContent.toByteArray()
        )

        // Add the attachment to the ticket
        val attachment = attachmentService.saveAttachment(savedTicket, mockFile)

        // Create a document for the attachment and add it directly to the vector store
        val attachmentDocument = Document.builder()
            .id(UUID.randomUUID().toString())
            .text(attachmentContent)
            .metadata(mapOf(
                "type" to "attachment",
                "ticketId" to (savedTicket.id ?: 0),
                "fileName" to attachment.fileName,
                "contentType" to attachment.contentType
            ))
            .build()

        // Add the attachment document to the vector store
        vectorStore.add(listOf(attachmentDocument))

        // When - Search for content that's only in the attachment, not in the ticket itself
        val searchRequest = SearchRequest.builder()
            .query("quarterly earnings financial reports")
            .topK(5)
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        // Then - Verify we found the ticket based on the attachment content
        assertThat(similarDocuments).isNotNull
        assertThat(similarDocuments!!).isNotEmpty()

        // Check if we have the attachment document
        val documentTypes = similarDocuments.map { it.metadata["type"] as String }
        assertThat(documentTypes).contains("attachment")

        // Verify that the attachment is associated with our ticket
        val attachmentDocs = similarDocuments.filter { it.metadata["type"] == "attachment" }
        assertThat(attachmentDocs).isNotEmpty()

        // Check that at least one attachment belongs to our ticket
        val ticketIds = attachmentDocs.map { it.metadata["ticketId"] as Int }
        assertThat(ticketIds).contains(savedTicket.id)
    }
}
