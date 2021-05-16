package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.User
import mixit.model.WorkAdventure
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository


@Repository
class WorkedAdventureRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        deleteAll().block()
        val workedAdventureResource = ClassPathResource("data/worked-adventure.json")
        val data: List<WorkAdventure> = objectMapper.readValue(workedAdventureResource.inputStream)
        data.forEach { save(it).block() }
        logger.info("WorkedAdventure Ticket data initialization complete")
    }

    fun count() = template.count<WorkAdventure>()

    fun findAll() = template.findAll<WorkAdventure>()

    fun findOne(ticket: String) = template.findById<WorkAdventure>(ticket)

    fun deleteAll() = template.remove<WorkAdventure>(Query())

    fun save(workedAdventure: WorkAdventure) = template.save(workedAdventure)
}
