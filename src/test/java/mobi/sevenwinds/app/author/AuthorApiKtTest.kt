package mobi.sevenwinds.app.author

import io.ktor.http.*
import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthorApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { AuthorTable.deleteAll() }
    }

    @Test
    fun testAddRecordSuccess() {
        val fullName = "Харитонова Ульяна Яковлевна"
        RestAssured.given()
            .jsonBody(AddAuthorRequest(fullName))
            .post("/author")
            .then()
            .statusCode(HttpStatusCode.OK.value)
        val authors = transaction { AuthorTable.selectAll().map { AuthorEntity.wrapRow(it) } }
        assertEquals(1, authors.size)
        assertEquals(fullName, authors.first().fullName)
    }

    @Test
    fun testInvalidFullName() {
        val invalidFullName = "Харитонова Ульяна"
        RestAssured.given()
            .jsonBody(AddAuthorRequest(invalidFullName))
            .post("/author")
            .then()
            .statusCode(HttpStatusCode.BadRequest.value)
    }
}