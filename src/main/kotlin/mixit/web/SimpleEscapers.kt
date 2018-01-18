package mixit.web

import com.samskivert.mustache.Mustache
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.Sanitizers

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 16/01/18.
 */
class SimpleEscapers {

    val policy = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.BLOCKS)
            .and(HtmlPolicyBuilder()
                    .allowUrlProtocols("http", "https")
                    .allowElements("img", "picture", "source")
                    .allowAttributes("alt", "src", "srcset", "type", "class")
                    .onElements("img", "picture", "source")
                    .toFactory())
            .and(HtmlPolicyBuilder().allowAttributes("class").globally().toFactory())

    val HTML: Mustache.Escaper =  Mustache.Escaper { text -> policy.sanitize(text) }
}