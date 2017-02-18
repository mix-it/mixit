package mixit.controller

import mixit.model.EventSponsoring
import mixit.model.Logo
import mixit.model.SponsorshipLevel.*
import mixit.repository.EventRepository
import mixit.support.LazyRouterFunction
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Controller
class GlobalController(val repository: EventRepository) : LazyRouterFunction() {

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: Routes.() -> Unit = {
        accept(TEXT_HTML).route {
            GET("/", this@GlobalController::homeView)
            GET("/archives/") { ok().render("archives") }
        }
        resources("/**", ClassPathResource("static/"))
    }

    fun homeView(req: ServerRequest) = repository.findOne("mixit17")    // TODO This could benefit from an in-memory cache with like 1H retention (data never change)
            .then { events ->
                val sponsors = events.sponsors.groupBy { it.level }
                ok().render("home", mapOf(
                        Pair("sponsors-gold", EventSponsoring.shuffle(sponsors[GOLD])!!.map { eventSponsoring -> SponsorDto.toDto(eventSponsoring) }),
                        Pair("sponsors-silver", EventSponsoring.shuffle(sponsors[SILVER])!!.map { eventSponsoring -> SponsorDto.toDto(eventSponsoring) }),
                        Pair("sponsors-hosting", EventSponsoring.shuffle(sponsors[HOSTING])!!.map { eventSponsoring -> SponsorDto.toDto(eventSponsoring) }),
                        Pair("sponsors-lanyard", EventSponsoring.shuffle(sponsors[LANYARD])!!.map { eventSponsoring -> SponsorDto.toDto(eventSponsoring) }),
                        Pair("sponsors-party", EventSponsoring.shuffle(sponsors[PARTY])!!.map { eventSponsoring -> SponsorDto.toDto(eventSponsoring) })
                ))
            }

    class SponsorDto(
        val login: String,
        val company: String,
        val logoUrl: String,
        val logoType: String,
        val logoWebpUrl: String? = null
    ){
        companion object {
            fun toDto(eventSponsoring: EventSponsoring):SponsorDto = SponsorDto(
                    eventSponsoring.sponsor.login,
                    eventSponsoring.sponsor.company!!,
                    eventSponsoring.sponsor.logoUrl!!,
                    Logo.logoType(eventSponsoring.sponsor.logoUrl!!),
                    Logo.logoWebPUrl(eventSponsoring.sponsor.logoUrl!!)
            )
        }
    }

}
