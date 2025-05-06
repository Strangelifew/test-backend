package mobi.sevenwinds.app.budget

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import mobi.sevenwinds.modules.DateTimeDeserializer
import mobi.sevenwinds.modules.DateTimeSerializer
import org.joda.time.DateTime

fun NormalOpenAPIRoute.budget() {
    route("/budget") {
        route("/add").post<Unit, BudgetResponse, BudgetRequest>(info("Добавить запись")) { param, body ->
            respond(BudgetService.addRecord(body))
        }

        route("/year/{year}/stats") {
            get<BudgetYearParam, BudgetYearStatsResponse>(info("Получить статистику за год")) { param ->
                respond(BudgetService.getYearStats(param))
            }
        }
    }
}

data class BudgetRequest(
    @Min(1900) val year: Int,
    @Min(1) @Max(12) val month: Int,
    @Min(1) val amount: Int,
    val type: BudgetType,
    val authorId: Int? = null
)

data class BudgetResponse(
    val year: Int,
    val month: Int,
    val amount: Int,
    val type: BudgetType,
    val author: AuthorResponse?
)

data class BudgetYearParam(
    @PathParam("Год") val year: Int,
    @QueryParam("Лимит пагинации") val limit: Int,
    @QueryParam("Смещение пагинации") val offset: Int,
    @QueryParam("Часть ФИО автора") val namePattern: String?
)

class BudgetYearStatsResponse(
    val total: Int,
    val totalByType: Map<String, Int>,
    val items: List<BudgetResponse>
)

data class AuthorResponse(
    val fullName: String,
    @JsonSerialize(using = DateTimeSerializer::class)
    @JsonDeserialize(using = DateTimeDeserializer::class)
    val creationDate: DateTime
)

enum class BudgetType {
    Приход, Расход
}