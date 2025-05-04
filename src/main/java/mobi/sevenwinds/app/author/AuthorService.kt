package mobi.sevenwinds.app.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object AuthorService {
    suspend fun addRecord(body: AddAuthorRequest): Int = withContext(Dispatchers.IO) {
        transaction {
            val entity = AuthorEntity.new {
                fullName = body.fullName
                creationDate = DateTime.now()
            }
            entity
        }.id.value
    }
}