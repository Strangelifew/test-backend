package mobi.sevenwinds.modules

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

class DateTimeSerializer : StdScalarSerializer<DateTime>(
    DateTime::class.java
) {
    override fun serialize(
        dateTime: DateTime,
        jsonGenerator: JsonGenerator,
        provider: SerializerProvider
    ) {
        val dateTimeAsString = ISODateTimeFormat.dateTime().print(dateTime)
        jsonGenerator.writeString(dateTimeAsString)
    }
}

class DateTimeDeserializer : StdScalarDeserializer<DateTime>(
    DateTime::class.java
) {
    override fun deserialize(
        jsonParser: JsonParser,
        deserializationContext: DeserializationContext
    ): DateTime {
        val currentToken = jsonParser.currentToken
        require(currentToken == JsonToken.VALUE_STRING) { "Can't deserialize DateTime from $currentToken" }
        val dateTimeAsString = jsonParser.text.trim { it <= ' ' }
        return ISODateTimeFormat.dateTime().parseDateTime(dateTimeAsString)
    }
}