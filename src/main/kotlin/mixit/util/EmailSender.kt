package mixit.util

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.User
import mixit.web.generateModelForExernalCall
import org.commonmark.internal.util.Escaping
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import java.util.*
import javax.mail.MessagingException

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 15/10/17.
 */
@Component
class EmailSender(private val mustacheCompiler: Mustache.Compiler,
                  private val resourceLoader: ResourceLoader,
                  private val javaMailSender: JavaMailSender,
                  private val properties: MixitProperties,
                  private val messageSource: MessageSource,
                  private val cryptographer: Cryptographer) {

    private val logger = LoggerFactory.getLogger(this.javaClass)


    fun sendUserTokenEmail(user: User, locale: Locale) {
        sendEmail("email-token",
                messageSource.getMessage("email-token-subject", null, locale),
                user,
                locale)
    }

    fun sendEmail(templateName: String, subject: String, user: User, locale: Locale) {
        try {
            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            val context = generateModelForExernalCall(properties.baseUri!!, locale, messageSource)
            val email = cryptographer.decrypt(user.email!!)

            context.put("user", user)
            context.put("encodedemail", Escaping.escapeHtml(email, true))

            message.setContent(openTemplate(templateName, context), "text/html")
            helper.setTo(email!!)
            helper.setSubject(subject)

            javaMailSender.send(message)

        } catch (e: MessagingException) {
            logger.error(String.format("Not possible to send email [%s] to %s", subject, user.email), e)
            throw RuntimeException("Error when system send the mail " + subject, e)
        }
    }

    fun openTemplate(templateName: String, context: Map<String, Any>): String {
        val resource = resourceLoader.getResource("classpath:templates/$templateName.mustache").inputStream
        val template = mustacheCompiler.compile(InputStreamReader(resource))

        return template.execute(context)
    }

}