package mobi.sevenwinds.app.author

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object AuthorTable: IntIdTable("author") {
    val fullName = text("full_name")
    val creationDate = datetime("creation_date")
}

class AuthorEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<AuthorEntity>(AuthorTable)

    var fullName by AuthorTable.fullName
    var creationDate by AuthorTable.creationDate
}