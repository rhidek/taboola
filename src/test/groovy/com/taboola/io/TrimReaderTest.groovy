package com.taboola.io

import spock.lang.Specification

class TrimReaderTest extends Specification {

    def "Should ignore whitespace during read"() {
        given: "A string with trimmable whitespace"
        String toTrim = "   a   \n  b \t   c  "
        TrimReader reader = new TrimReader(new StringReader(toTrim))

        when: "read calls are made"
        int first = reader.read()
        int second = reader.read()
        int third = reader.read()
        int fourth = reader.read()

        then: "only the non-whitespace characters are returned"
        first == 'a' as char
        second == 'b' as char
        third == 'c' as char
        fourth == -1
    }

    def "Should not trim within quotes"() {
        given: "A string with trimmable whitespace"
        String toTrim = ' " a b " '
        TrimReader reader = new TrimReader(new StringReader(toTrim))

        when: "read calls are made"
        int first = reader.read()
        int second = reader.read()
        int third = reader.read()
        int fourth = reader.read()
        int fifth = reader.read()
        int sixth = reader.read()
        int seventh = reader.read()
        int eighth = reader.read()

        then: "only the non-whitespace characters are returned"
        first == '"' as char
        second == ' ' as char
        third == 'a' as char
        fourth == ' ' as char
        fifth == 'b' as char
        sixth == ' ' as char
        seventh == '"' as char
        eighth == -1
    }
}
