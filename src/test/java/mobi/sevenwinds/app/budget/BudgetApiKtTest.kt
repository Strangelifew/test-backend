package mobi.sevenwinds.app.budget

import io.ktor.http.*
import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AddAuthorRequest
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
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

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
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
        val fullName = "Харитонова Ульяна Яковлевна"
        val authorId = RestAssured.given()
            .jsonBody(AddAuthorRequest(fullName))
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
                assertEquals(fullName, budget.author?.fullName)
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

    private fun addRecord(request: BudgetRequest) {
        RestAssured.given()
            .jsonBody(request)
            .post("/budget/add")
            .toResponse<BudgetResponse>().let { response ->
                Assert.assertEquals(request, response.toRequest(request.authorId))
            }
    }

    private fun BudgetResponse.toRequest(authorId: Int?) = BudgetRequest(
        year = year,
        month = month,
        amount = amount,
        type = type,
        authorId = authorId
    )
}