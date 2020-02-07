package com.taboola.json


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

    @Unroll
    def "Should parse k-v (#useCase) pairs"() {
        given: "a populated object"
        String json = """
            { "k 1": ${text} }
            """

        when: "it is parsed"
        Map<String, Object> result = jsonParser.parse(json)

        then: "the result is a populated Map"
        result == ['k 1': expected]

        where:
        useCase   | text      | expected
        "null"    | 'null'    | null
        "bool"    | 'true'    | true
        "bool2"   | 'false'   | false
        "string"  | '"v 1"'   | 'v 1'
        "float"   | '12.1234' | BigDecimal.valueOf(12.1234)
        "sciNot"  | '6.24E+2' | new BigDecimal("6.24E+2")
        "sciNot2" | '12e-2'   | new BigDecimal("12e-2")
    }

    def "Should parse nested objects"() {
        given: "a populated object"
        String json = """
            { 
                "k": {
                    "k":"v",
                    "k2":"v2" 
                } 
            }
            """

        when: "it is parsed"
        Map<String, Object> result = jsonParser.parse(json)

        then: "the result is a populated Map"
        result == [k: [k: "v", k2: "v2"]]
    }

    def "Should parse nested arrays"() {
        given: "a populated object"
        String json = """
            { 
                "k": [ 
                    "a", 
                    "b", 
                    1, 
                    2 
                ] 
            }
            """

        when: "it is parsed"
        Map<String, Object> result = jsonParser.parse(json)

        then: "the result is a populated Map"
        result == [k: ["a", "b", 1, 2]]
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
        reason             | expectedMessage                                    | expectedOffset | invalidJson
        "empty1"           | "JSON is empty."                                   | 0              | ''
        "empty2"           | "JSON is empty."                                   | 0              | ' '
        "naked array"      | "JSON must begin with an Object."                  | 0              | '[]'
        "junk"             | "JSON must begin with an Object."                  | 0              | 'this is junk'
        "unclosed Object1" | "No closing token for object started at offset 1." | 1              | ' {'
        "unclosed Object2" | "No closing token for object started at offset 0." | 13             | '{"k":{"k":"v"}'
        "incomplete KV1"   | "Encountered unexpected token: '}'."               | 5              | '{"k":}'
        "incomplete KV2"   | "Encountered unexpected token: '}'."               | 14             | '{"k1":"v","k2"}'
        "incomplete KV3"   | "Encountered unexpected token: '}'."               | 15             | '{"k1":"v","k2":}'
        "invalid null"     | "Expected 'null' but got 'nil}'."                  | 5              | '{"k":nil}'
        "invalid null2"    | "Encountered unexpected end of stream."            | 5              | '{"k":n'
        "invalid true"     | "Expected 'true' but got 'tru}'."                  | 5              | '{"k":tru}'
        "invalid true2"    | "Encountered unexpected end of stream."            | 5              | '{"k":t'
        "invalid false"    | "Expected 'false' but got 'falsw'."                | 5              | '{"k":falsw}'
        "invalid false2"   | "Encountered unexpected end of stream."            | 5              | '{"k":f'
    }
}
