package de.codecentric.pact

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junit5.MessageTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import de.codecentric.pact.checkout.CheckoutService
import de.codecentric.pact.checkout.Item
import de.codecentric.pact.checkout.Order
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
@Provider("checkout-service")
@PactFolder("pacts")
class CheckoutServiceProviderVerificationTest {

    companion object {
        @JvmStatic
        @Container
        val localstack: LocalStackContainer = LocalStackContainer(). // deprecated, but ok as this might get the real queue
        withServices(LocalStackContainer.Service.SQS)

        val sqsContainer by lazy { SQSHelper.setupSQSTestcontainer(localstack) }
        val sqsClient by lazy { sqsContainer.first }
        val queueUrl by lazy { sqsContainer.second }
        val objectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun testTemplate(pact: Pact<*>, interaction: Interaction, context: PactVerificationContext) {
        println("testTemplate called: " + pact.provider.name + ", " + interaction)
        context.verifyInteraction()
    }

    @BeforeEach
    fun before(context: PactVerificationContext) {
        context.target = MessageTestTarget(Collections.emptyList())
    }

    @State("customer exists") // Must match the state description in the pact file
    fun someProviderState() {
        println("***********************")
    }

    @PactVerifyProvider("an order to export")
    fun anOrderToExport(): String? {
        val checkoutService = CheckoutService(sqsClient, queueUrl, objectMapper)
        val order = Order(
            listOf(
                Item("A secret machine", 1559),
                Item("A riddle", 9990),
                Item("A hidden room", 3330)
            ), "customerId"
            , "referralPartner"
        )
        return checkoutService.createSqsMessage(order).messageBody
    }
}

