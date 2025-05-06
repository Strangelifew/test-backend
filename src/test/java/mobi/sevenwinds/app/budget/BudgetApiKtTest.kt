package mobi.sevenwinds.app.budget

import io.ktor.http.*
import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AddAuthorRequest
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val fullName1 = "Харитонова Ульяна Яковлевна"
private const val fullName2 = "Харлампиев Устиний Ильич"

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { BudgetTable.deleteAll() }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRequest(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRequest(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                assertEquals(5, response.total)
                assertEquals(3, response.items.size)
                assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRequest(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                assertEquals(30, response.items[0].amount)
                assertEquals(5, response.items[1].amount)
                assertEquals(400, response.items[2].amount)
                assertEquals(100, response.items[3].amount)
                assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRequest(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRequest(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    @Test
    fun testAddAuthoredBudgetSuccess() {
        val authorId = RestAssured.given()
            .jsonBody(AddAuthorRequest(fullName1))
            .post("/author")
            .toResponse<Int>()
        val request = BudgetRequest(2020, 5, 100, BudgetType.Приход, authorId)
        addRecord(request)

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                assertEquals(1, response.items.size)
                val budget = response.items.first()
                assertEquals(request, budget.toRequest(authorId))
                assertNotNull(budget.author)
                assertEquals(fullName1, budget.author?.fullName)
            }
    }

    @Test
    fun testAddAuthoredBudgetInvalidId() {
        val request = BudgetRequest(2020, 5, 100, BudgetType.Приход, 0)
        RestAssured.given()
            .jsonBody(request)
            .post("/budget/add")
            .then().statusCode(HttpStatusCode.NotFound.value)

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                assertEquals(0, response.items.size)
            }
    }

    @Test
    fun testGetBudgetStatsWithFilterAuthor_TwoAuthorsFit() {
        prepareAuthorsStatsPlayground()

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&fullName=Хар")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                assertEquals(6, response.total)
                assertEquals(6, response.items.size)
                assertEquals(160, response.totalByType[BudgetType.Приход.name])
                assertEquals(170, response.totalByType[BudgetType.Расход.name])
            }
    }

    @Test
    fun testGetBudgetStatsWithFilterAuthor_OneAuthorFit() {
        prepareAuthorsStatsPlayground()

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&fullName= Улья")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                assertEquals(3, response.total)
                assertEquals(3, response.items.size)
                assertEquals(110, response.totalByType[BudgetType.Приход.name])
                assertEquals(50, response.totalByType[BudgetType.Расход.name])
            }
    }

    @Test
    fun testGetBudgetStatsWithFilterAuthor_NoOneAuthorFit() {
        prepareAuthorsStatsPlayground()

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&fullName= Улья ")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                assertEquals(0, response.total)
                assertEquals(0, response.items.size)
            }
    }

    private fun addRecord(request: BudgetRequest) {
        RestAssured.given()
            .jsonBody(request)
            .post("/budget/add")
            .toResponse<BudgetResponse>().let { response ->
                assertEquals(request, response.toRequest(request.authorId))
            }
    }

    private fun prepareAuthorsStatsPlayground() {
        fun addAuthor(fullName: String) = RestAssured.given()
            .jsonBody(AddAuthorRequest(fullName))
            .post("/author")
            .toResponse<Int>()

        val authorId1 = addAuthor(fullName1)
        val authorId2 = addAuthor(fullName2)

        addRecord(BudgetRequest(2020, 5, 10, BudgetType.Приход, authorId1))
        addRecord(BudgetRequest(2020, 5, 100, BudgetType.Приход, authorId1))
        addRecord(BudgetRequest(2020, 5, 50, BudgetType.Расход, authorId1))

        addRecord(BudgetRequest(2020, 5, 100, BudgetType.Расход, authorId2))
        addRecord(BudgetRequest(2020, 5, 50, BudgetType.Приход, authorId2))
        addRecord(BudgetRequest(2020, 5, 20, BudgetType.Расход, authorId2))

        addRecord(BudgetRequest(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRequest(2020, 5, 20, BudgetType.Приход))
    }

    private fun BudgetResponse.toRequest(authorId: Int?) = BudgetRequest(
        year = year,
        month = month,
        amount = amount,
        type = type,
        authorId = authorId
    )
}