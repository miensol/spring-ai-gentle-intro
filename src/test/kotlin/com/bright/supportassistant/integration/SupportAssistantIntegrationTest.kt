package com.bright.supportassistant.integration

import com.bright.supportassistant.model.SupportTicket
import com.bright.supportassistant.model.TicketStatus
import com.bright.supportassistant.repository.SupportTicketRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


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

        passwordResetTicket = SupportTicket(
            title = "Password Reset",
            customerMessage = "How do I reset my password?",
            agentResponse = "You can reset your password by clicking on the 'Forgot Password' link on the login page.",
            category = "Account",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("How do I reset my password?")
        )

        loginIssueTicket = SupportTicket(
            title = "Login Issue",
            customerMessage = "I can't log in to my account",
            agentResponse = "Please try resetting your password using the 'Forgot Password' link.",
            category = "Account",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("I can't log in to my account")
        )

        doubleChargeTicket = SupportTicket(
            title = "Double Charge",
            customerMessage = "I was charged twice for my subscription",
            agentResponse = "I apologize for the inconvenience. I've checked your account and issued a refund for the duplicate charge. It should appear in your account within 3-5 business days.",
            category = "Billing",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("I was charged twice for my subscription")
        )

        paymentMethodTicket = SupportTicket(
            title = "Payment Method Update",
            customerMessage = "How do I update my payment method?",
            agentResponse = "You can update your payment method by going to Account Settings > Billing > Payment Methods and clicking on 'Add New Method'.",
            category = "Billing",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("How do I update my payment method?")
        )

        appCrashTicket = SupportTicket(
            title = "App Crash on Upload",
            customerMessage = "The app crashes when I try to upload a file",
            agentResponse = "I'm sorry to hear about the crash. Could you please tell me what type of file you're trying to upload and which version of the app you're using? In the meantime, try updating to the latest version.",
            category = "Technical",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("The app crashes when I try to upload a file")
        )

        searchIssueTicket = SupportTicket(
            title = "Search Functionality Issue",
            customerMessage = "The search feature isn't working properly",
            agentResponse = "We're aware of some issues with the search feature and our team is working on a fix. It should be resolved in our next update. In the meantime, try using more specific search terms.",
            category = "Technical",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("The search feature isn't working properly")
        )

        darkModeTicket = SupportTicket(
            title = "Dark Mode Request",
            customerMessage = "Can you add dark mode to the app?",
            agentResponse = "Thank you for your suggestion! We're actually working on implementing dark mode in our next major update. Stay tuned for the announcement in the coming weeks.",
            category = "Feature Request",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("Can you add dark mode to the app?")
        )

        csvExportTicket = SupportTicket(
            title = "CSV Export Feature",
            customerMessage = "I'd like to be able to export my data as CSV",
            agentResponse = "That's a great suggestion. Currently, you can export data in PDF and Excel formats. I'll pass your request for CSV export to our product team for consideration in future updates.",
            category = "Feature Request",
            status = TicketStatus.RESOLVED,
            embedding = embeddingModel.embed("I'd like to be able to export my data as CSV")
        )

        supportTicketRepository.saveAll(
            listOf(
                passwordResetTicket,
                loginIssueTicket,
                doubleChargeTicket,
                paymentMethodTicket,
                appCrashTicket,
                searchIssueTicket,
                darkModeTicket,
                csvExportTicket
            )
        )
    }

    // Positive test cases - Testing successful similarity matching

    @Test
    fun `should find single most relevant ticket for password query - positive case`() {
        // When - Test finding a single most relevant ticket for password-related query
        val queryEmbedding = embeddingModel.embed("How to change my password")
        val similarTickets = supportTicketRepository.findSimilarTickets(queryEmbedding, 1)

        // Then - Verify we get the most relevant ticket
        assertThat(similarTickets).hasSize(1)
        assertThat(similarTickets[0].title).isEqualTo("Password Reset")
    }

    @Test
    fun `should find multiple relevant tickets for password query - positive case`() {
        // When - Test finding multiple relevant tickets for password-related query
        val queryEmbedding = embeddingModel.embed("How to change my password")
        val multipleTickets = supportTicketRepository.findSimilarTickets(queryEmbedding, 2)

        // Then - Verify we get both password-related tickets at the top
        assertThat(multipleTickets).hasSize(2)
        assertThat(multipleTickets[0].title).isEqualTo("Password Reset")
        assertThat(multipleTickets[1].title).isEqualTo("Login Issue")
    }

    @Test
    fun `should find relevant ticket for billing query - positive case`() {
        // When - Test finding a ticket for a billing-related query
        val billingQueryEmbedding = embeddingModel.embed("I was charged twice for my monthly subscription")
        val billingTickets = supportTicketRepository.findSimilarTickets(billingQueryEmbedding, 1)

        // Then - Verify we get the relevant billing ticket
        assertThat(billingTickets).hasSize(1)
        assertThat(billingTickets[0].title).isEqualTo("Double Charge")
    }

    @Test
    fun `should find relevant ticket for technical query - positive case`() {
        // When - Test finding a ticket for a technical issue query
        val technicalQueryEmbedding = embeddingModel.embed("The application crashes when I upload files")
        val technicalTickets = supportTicketRepository.findSimilarTickets(technicalQueryEmbedding, 1)

        // Then - Verify we get the relevant technical ticket
        assertThat(technicalTickets).hasSize(1)
        assertThat(technicalTickets[0].title).isEqualTo("App Crash on Upload")
    }

    // Negative test cases - Testing edge cases and unexpected inputs

    @Test
    fun `should handle novel query not matching existing tickets - negative case`() {
        // When - Test with a novel query that doesn't match any existing tickets well
        val novelQueryEmbedding = embeddingModel.embed("How do I add pictures to my account?")
        val novelQueryTickets = supportTicketRepository.findSimilarTickets(novelQueryEmbedding, 3)

        // Then - Verify we get some results, but they're not password-related
        // We don't assert exact titles here because the exact ranking might vary
        // depending on the embedding model, but we can verify that password-related
        // tickets aren't returned as top matches for this unrelated query
        assertThat(novelQueryTickets).hasSizeGreaterThanOrEqualTo(1)
        assertThat(novelQueryTickets.map { it.title }).doesNotContain("Password Reset")
    }

    @Test
    fun `should handle very large limit - negative case`() {
        // When - Test with a very large limit
        val queryEmbedding = embeddingModel.embed("password")
        val largeResultTickets = supportTicketRepository.findSimilarTickets(queryEmbedding, 100)

        // Then - Verify we get relevant tickets
        assertThat(largeResultTickets).hasSize(2)
    }
}
