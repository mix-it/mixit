package mixit.web

import mixit.MixitProperties
import mixit.model.Role
import mixit.model.User
import mixit.repository.UserRepository
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.CONTENT_LANGUAGE
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream


@Component
class MixitWebFilter(val applicationContext: ApplicationContext, val properties: MixitProperties, val userRepository: UserRepository) : WebFilter {

    private val redirectDoneAttribute = "redirectDone"

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain) = exchange.session.flatMap { session ->
        // We need to know if the user is connected or not
        val email = session.attributes["email"]
        val token = session.attributes["token"]

        if (email != null && token != null) {
            // If session contains an email we load the user
            userRepository.findByEmail(email.toString()).flatMap {
                // We have to see if the token is the good one anf if it is yet valid
                if (it.token.equals(token.toString()) && it.tokenExpiration.isAfter(LocalDateTime.now())) {
                    filter(exchange, chain, it)
                } else {
                    filter(exchange, chain, null)
                }
            }
        } else {
            filter(exchange, chain, null)
        }
    }

    fun filter(exchange: ServerWebExchange, chain: WebFilterChain, user: User?) =
            // People who used the old URL are directly redirected
            if (exchange.request.headers.host?.hostString?.endsWith("mix-it.fr") == true) {
                val response = exchange.response
                response.statusCode = HttpStatus.PERMANENT_REDIRECT
                response.headers.location = URI("${properties.baseUri}${exchange.request.uri.path}")
                Mono.empty()
            }
            // For those who arrive on our home page with another language tha french, we need to load the site in english
            else if (exchange.request.uri.path == "/" &&
                    (exchange.request.headers.acceptLanguageAsLocales.firstOrNull() ?: Locale.FRENCH).language != "fr" &&
                    !isSearchEngineCrawler(exchange)) {
                val response = exchange.response
                exchange.session.flatMap {
                    if (it.attributes[redirectDoneAttribute] == true)
                        chain.filter(exchange.mutate().request(exchange.request.mutate().header(ACCEPT_LANGUAGE, "fr").build()).build())
                    else {
                        response.statusCode = HttpStatus.TEMPORARY_REDIRECT
                        response.headers.location = URI("${properties.baseUri}/en/")
                        it.attributes[redirectDoneAttribute] = true
                        it.save()
                    }
                }
            }
            // In other case we have to see if the page is secured or not
            else {
                // When a user wants to see a page in english uri path starts with 'en'
                val languageEn = exchange.request.uri.path.startsWith("/en/")
                val uriPath = if (languageEn) exchange.request.uri.path.substring(3) else exchange.request.uri.path

                // If url is securized we have to check the credentials information
                if (startWithSecuredUrl(uriPath)) {
                    if (user == null) {
                        redirectForLogin(exchange, "login")
                    } else {
                        // If admin page we see if user is a staff member
                        if (startWithAdminSecuredUrl(uriPath) && user.role != Role.STAFF) {
                            redirectForLogin(exchange, "")
                        } else {
                            val req = exchange.request.mutate().path(uriPath).header(CONTENT_LANGUAGE, if (languageEn) "en" else "fr").build()
                            chain.filter(exchange.mutate().request(req).build())
                        }
                    }
                } else {
                    chain.filter(exchange.mutate().request(exchange.request.mutate()
                            .path(uriPath)
                            .header(CONTENT_LANGUAGE, if (languageEn) "en" else "fr").build())
                            .build())
                }
            }

    private fun redirectForLogin(exchange: ServerWebExchange, uri: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TEMPORARY_REDIRECT
        response.headers.location = URI("${properties.baseUri}/${uri}")
        return Mono.empty()
    }

    private fun startWithSecuredUrl(path: String): Boolean =
            Stream.concat(WebsiteRoutes.securedUrl.stream(), WebsiteRoutes.securedAdminUrl.stream()).anyMatch { path.startsWith(it) }

    private fun startWithAdminSecuredUrl(path: String): Boolean = WebsiteRoutes.securedAdminUrl.stream().anyMatch { path.startsWith(it) }

    private fun isSearchEngineCrawler(exchange: ServerWebExchange): Boolean {
        val userAgent = exchange.request.headers.getFirst(HttpHeaders.USER_AGENT) ?: ""
        val bots = arrayOf("Google", "Bingbot", "Qwant", "Bingbot", "Slurp", "DuckDuckBot", "Baiduspider")
        return bots.any { userAgent.contains(it) }
    }
}

