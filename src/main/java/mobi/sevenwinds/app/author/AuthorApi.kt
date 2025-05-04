package mobi.sevenwinds.app.author

import com.papsign.ktor.openapigen.annotations.type.string.pattern.RegularExpression
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route

fun NormalOpenAPIRoute.author() {
    route("/author") {
        // Хотя для добавления записей budget используется url /budget/add, лучшие практики использования REST
        // рекомендуют не использовать глаголы в url, так как url определяет ресурс, а оперция с ресурсом определяется
        // http методом. Принял решение сделать url в соответствии с лучшими практиками REST, хотя это и делает api не
        // единообразным.
        post<Unit, Int, AddAuthorRequest>(info("Добавить запись")) { _, body ->
            respond(AuthorService.addRecord(body))
        }
        "".toRegex()
    }
}

data class AddAuthorRequest(
    // На самом деле русское ФИО проверяется более сложной регуляркой, но для простоты считал, что корректное ФИО это
    // просто строка из трех слов, где слово - непустая последовательность непробельных символов
    @RegularExpression("[\\S]+ [\\S]+ [\\S]+") val fullName: String
)
