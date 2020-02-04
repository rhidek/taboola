package com.taboola.json

import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll

class JsonParserTest extends Specification {
    JsonParser jsonParser = new JsonParser()

    void setup() {
    }

    void cleanup() {
    }

    def "Should parse an empty object"() {
        given: "an empty object"
        String empty = "{}"

        when: "it is parsed"
        Map<String, Object> result = jsonParser.parse(empty)

        then: "the result is an empty Map"
        result == [:]
    }

    @PendingFeature
    def "Should parse key-value pairs"() {
        given: "a populated object"
        json

        when: "it is parsed"
        Map<String, Object> result = jsonParser.parse(json)

        then: "the result is a populated Map"
        result == expectedResult

        where:
        useCase           | expectedResult                   | json
        "shallow strings" | [key1: "value 1", key2: "value 2"] | """{ "key1": "value 1", "key2": "value 2" }"""
    }

    def "Should throw an NPE for null String input"() {
        when: "null is parsed"
        jsonParser.parse(null as String)

        then: "an NPE it thrown"
        thrown(NullPointerException)
    }

    def "Should throw an NPE for null Reader input"() {
        when: "null is parsed"
        jsonParser.parse(null as Reader)

        then: "an NPE it thrown"
        thrown(NullPointerException)
    }

    @Unroll
    def "Should throw a ParseException for invalid (#reason) json"() {
        given: "some invalid json"
        invalidJson

        when: "it is parsed"
        jsonParser.parse(invalidJson)

        then: "a ParseException is thrown"
        JsonParseException exception = thrown(JsonParseException)

        and: "the appropriate error message is used"
        exception.message == expectedMessage

        and: "the correct error offset is supplied"
        exception.offset == expectedOffset

        where:
        reason            | expectedMessage                        | expectedOffset | invalidJson
        "empty"           | "JSON is empty"                        | 0              | ""
        "naked array"     | "JSON must begin with an Object"       | 0              | "[]"
        "junk"            | "JSON must begin with an Object"       | 0              | "this is junk"
        "unclosed Object" | "Encountered unexpected end of Object" | 1              | "{"
    }
}
