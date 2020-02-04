package com.taboola.io

import spock.lang.Specification

import java.nio.CharBuffer

class OffsetTrackingReaderTest extends Specification {
    Reader mockReader
    OffsetTrackingReader reader
    int startingOffset = 10

    void setup() {
        mockReader = Mock()
        reader = new OffsetTrackingReader(mockReader)
        reader.offset = startingOffset //start offset at something other than 0 to ensure tests accurately describe behavior
    }

    def "Should increment offset and delegate read()"() {
        when: "read is called"
        reader.read()

        then: "the call is passed to the delegate"
        1 * mockReader.read() >> 0

        and: "the offset is incremented"
        reader.offset == 1 + startingOffset
    }

    def "Should increment offset and delegate read(CharBuffer)"() {
        given: "a CharBuffer"
        int size = 10
        CharBuffer charBuffer = CharBuffer.allocate(size)

        when: "read is called"
        int amountRead = reader.read(charBuffer)

        then: "the call is passed to the delegate"
        1 * mockReader.read(charBuffer) >> size

        and: "the offset is incremented by the amount read by the delegate"
        reader.offset == size + startingOffset

        and: "the amount read is returned"
        amountRead == size
    }

    def "Should increment offset and delegate read(char[])"() {
        given: "a char[]"
        int size = 10
        char[] arrayOfChars = new char[size]

        when: "read is called"
        int amountRead = reader.read(arrayOfChars)

        then: "the call is passed to the delegate"
        1 * mockReader.read(arrayOfChars) >> size

        and: "the offset is incremented by the amount read by the delegate"
        reader.offset == size + startingOffset

        and: "the amount read is returned"
        amountRead == size
    }

    def "Should increment offset and delegate read(char[],offset,length)"() {
        given: "a char[], offset, and length"
        char[] arrayOfChars = new char[10]
        int offset = 1
        int length = 2

        when: "read is called"
        int amountRead = reader.read(arrayOfChars, offset, length)

        then: "the call is passed to the delegate"
        1 * mockReader.read(arrayOfChars, offset, length) >> length

        and: "the offset is incremented by the amount read by the delegate"
        reader.offset == length + startingOffset

        and: "the amount read is returned"
        amountRead == length
    }

    def "Should increment offset by skip and delegate skip"() {
        given: "a skip increment"
        int skip = 10

        when: "skip is called"
        long amountSkipped = reader.skip(skip)

        then: "the call is passed to the delegate"
        1 * mockReader.skip(skip) >> skip

        and: "the offset is incremented"
        reader.offset == skip + startingOffset

        and: "the amount skipped is returned"
        amountSkipped == skip
    }

    def "Should delegate ready"() {
        when: "ready is called"
        boolean result = reader.ready()

        then: "the call is passed to the delegate"
        1 * mockReader.ready() >> true

        and: "the ready result is returned"
        result
    }

    def "Should delegate markSupported"() {
        when: "markSupported is called"
        boolean result = reader.markSupported()

        then: "the call is passed to the delegate"
        1 * mockReader.markSupported() >> true

        and: "the markSupported result is returned"
        result
    }

    def "Should mark current offset and delegate mark"() {
        given: "a read ahead limit value"
        int mark = 5

        when: "mark is called"
        reader.mark(mark)

        then: "the call is passed to the delegate"
        1 * mockReader.mark(mark)

        and: "the markedOffset matches the offset"
        reader.markedOffset == reader.offset
    }

    def "Should reset to marked offset and delegate reset"() {
        when: "reset is called"
        reader.reset()

        then: "the call is passed to the delegate"
        1 * mockReader.reset()

        and: "the offset is set to the markedOffset"
        reader.markedOffset == reader.offset
    }

    def "Should delegate close"(){
        when: "close is called"
        reader.close()

        then: "the call is passed to the delegate"
        1 * mockReader.close()
    }
}
