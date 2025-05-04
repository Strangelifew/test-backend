package mobi.sevenwinds.app.budget

import io.ktor.features.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRequest): BudgetResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                year = body.year
                month = body.month
                amount = body.amount
                type = body.type
                author = body
                    .authorId
                    ?.let {
                        AuthorTable
                            .select { AuthorTable.id eq it }
                            .firstOrNull()
                            ?: throw NotFoundException("Author with id=$it is not found")
                    }
                    ?.let { AuthorEntity.wrapRow(it) }
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            var total = 0
            val sumByType = mutableMapOf<String, Int>()

            BudgetTable
                .slice(BudgetTable.amount.count(), BudgetTable.amount.sum(), BudgetTable.type)
                .select { BudgetTable.year eq param.year }
                .groupBy(BudgetTable.type)
                .forEach {
                    total += it[BudgetTable.amount.count()]
                    val key = it[BudgetTable.type].name
                    sumByType[key] = (sumByType[key] ?: 0) + (it[BudgetTable.amount.sum()] ?: 0)
                }

            val query = BudgetTable
                .select { BudgetTable.year eq param.year }
                .orderBy(BudgetTable.month to ASC, BudgetTable.amount to DESC)
                .limit(param.limit, param.offset)

            val data = BudgetEntity.wrapRows(query).map { it.toResponse() }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}