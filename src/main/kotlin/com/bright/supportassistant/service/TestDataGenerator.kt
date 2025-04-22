package com.bright.supportassistant.service

import com.bright.supportassistant.model.SupportTicket
import com.bright.supportassistant.model.TicketStatus
import com.bright.supportassistant.repository.SupportTicketRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.Random

@Component
class TestDataGenerator(
    private val supportTicketRepository: SupportTicketRepository,
    private val responseSuggestionService: ResponseSuggestionService
) {

//    @EventListener(ApplicationReadyEvent::class)
    fun generateTestData() {
        // Check if data already exists
        if (supportTicketRepository.count() > 0) {
            return
        }

        val categories = listOf("Account", "Billing", "Technical", "Product", "General")
        val statuses = listOf(TicketStatus.RESOLVED, TicketStatus.CLOSED)

        val customerQuestions = listOf(
            "How do I reset my password?",
            "I can't log in to my account",
            "My subscription was charged twice",
            "How do I cancel my subscription?",
            "The app is crashing on startup",
            "I'm getting an error message when trying to save",
            "How do I export my data?",
            "Is there a way to change my username?",
            "The search feature isn't working",
            "How do I connect my social media accounts?"
        )

        val agentResponses = listOf(
            "You can reset your password by clicking on the 'Forgot Password' link on the login page.",
            "Please try clearing your browser cache and cookies, then try logging in again.",
            "I've checked your account and issued a refund for the duplicate charge. It should appear in 3-5 business days.",
            "You can cancel your subscription by going to Account Settings > Subscription > Cancel.",
            "Please try updating the app to the latest version. If the issue persists, try uninstalling and reinstalling.",
            "This error typically occurs when there's a connection issue. Please check your internet connection and try again.",
            "To export your data, go to Account Settings > Privacy > Download My Data.",
            "Yes, you can change your username once every 30 days in Account Settings > Profile.",
            "We're aware of the search issue and our team is working on a fix. It should be resolved within 24 hours.",
            "You can connect your social media accounts in Account Settings > Connected Accounts."
        )

        val random = Random()
        val tickets = mutableListOf<SupportTicket>()

        // Generate 1000 tickets with variations
        for (i in 1..1000) {
            val baseQuestionIndex = random.nextInt(customerQuestions.size)
            val baseQuestion = customerQuestions[baseQuestionIndex]
            val baseResponse = agentResponses[baseQuestionIndex]

            // Add some variation to make the data more realistic
            val questionVariation = if (random.nextBoolean()) {
                baseQuestion
            } else {
                // Add some random words or phrases to the question
                val variations = listOf(
                    "Hi, $baseQuestion",
                    "$baseQuestion Please help!",
                    "I'm having trouble with this. $baseQuestion",
                    "$baseQuestion I've tried multiple times.",
                    "Urgent: $baseQuestion"
                )
                variations[random.nextInt(variations.size)]
            }

            val responseVariation = if (random.nextBoolean()) {
                baseResponse
            } else {
                // Add some random words or phrases to the response
                val variations = listOf(
                    "Hi there! $baseResponse",
                    "$baseResponse Let me know if you need anything else!",
                    "$baseResponse If you have any other questions, feel free to ask.",
                    "Thank you for contacting us. $baseResponse",
                    "$baseResponse Have a great day!"
                )
                variations[random.nextInt(variations.size)]
            }

            val category = categories[random.nextInt(categories.size)]
            val status = statuses[random.nextInt(statuses.size)]
            val title = "Support Request #${i + 1000}"

            val ticket = SupportTicket(
                title = title,
                customerMessage = questionVariation,
                agentResponse = responseVariation,
                category = category,
                status = status
            )

            // Use ResponseSuggestionService to create the ticket and add it to the vector store
            responseSuggestionService.createTicket(ticket)

            // Save in batches to avoid memory issues
            if (tickets.size >= 100) {
                tickets.clear()
            }
        }

        println("Generated 1000 test support tickets")
    }
}