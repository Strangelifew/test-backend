package mobi.sevenwinds.app.budget

import io.ktor.features.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.SortOrder.DESC
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

            val query: Query
            val totalsQuery: Query
            if (param.namePattern != null) {
                val filter: SqlExpressionBuilder.() -> Op<Boolean> = {
                    BudgetTable.year eq param.year and
                            (BudgetTable.author eq AuthorTable.id) and
                            (AuthorTable.fullName.lowerCase() match "%${param.namePattern.toLowerCase()}%")
                }
                query = (BudgetTable innerJoin AuthorTable)
                    .select(filter)
                totalsQuery = (BudgetTable innerJoin AuthorTable)
                    .slice(BudgetTable.amount.count(), BudgetTable.amount.sum(), BudgetTable.type)
                    .select(filter)
            } else {
                val filter: SqlExpressionBuilder.() -> Op<Boolean> = { BudgetTable.year eq param.year }
                query = BudgetTable
                    .select(filter)
                totalsQuery = BudgetTable
                    .slice(BudgetTable.amount.count(), BudgetTable.amount.sum(), BudgetTable.type)
                    .select(filter)
            }

            totalsQuery
                .groupBy(BudgetTable.type)
                .forEach {
                    total += it[BudgetTable.amount.count()]
                    val key = it[BudgetTable.type].name
                    sumByType[key] = (sumByType[key] ?: 0) + (it[BudgetTable.amount.sum()] ?: 0)
                }


            val data = BudgetEntity.wrapRows(
                query
                    .orderBy(BudgetTable.month to ASC, BudgetTable.amount to DESC)
                    .limit(param.limit, param.offset)
            ).map { it.toResponse() }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}