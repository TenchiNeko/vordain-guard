package com.vordain.guard.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainNameTest {
    @Test
    fun trimsWhitespace() {
        assertEquals("example.com", DomainName.from("  example.com  ").value)
    }

    @Test
    fun lowercasesAsciiInput() {
        assertEquals("example.com", DomainName.from("Example.COM").value)
    }

    @Test
    fun removesOneTrailingRootDot() {
        assertEquals("example.com", DomainName.from("Example.COM.").value)
    }

    @Test
    fun rejectsBlankInput() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("   ")
        }
    }

    @Test
    fun rejectsEmbeddedSpaces() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("example .com")
        }
    }

    @Test
    fun rejectsUrlScheme() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("https://example.com")
        }
    }

    @Test
    fun rejectsPath() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("example.com/path")
        }
    }

    @Test
    fun rejectsPort() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("example.com:443")
        }
    }

    @Test
    fun rejectsEmptyLabels() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("example..com")
        }
    }

    @Test
    fun rejectsLabelOverSixtyThreeCharacters() {
        val longLabel = "a".repeat(64)

        assertFailsWith<IllegalArgumentException> {
            DomainName.from("$longLabel.example")
        }
    }

    @Test
    fun rejectsDomainOverTwoHundredFiftyThreeCharacters() {
        val label63 = "a".repeat(63)
        val domain = listOf(label63, label63, label63, label63).joinToString(".")

        assertFailsWith<IllegalArgumentException> {
            DomainName.from(domain)
        }
    }

    @Test
    fun rejectsLabelStartingWithHyphen() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("-example.com")
        }
    }

    @Test
    fun rejectsLabelEndingWithHyphen() {
        assertFailsWith<IllegalArgumentException> {
            DomainName.from("example-.com")
        }
    }

    @Test
    fun normalizesSimpleInternationalizedDomainToAscii() {
        assertEquals("xn--bcher-kva.example", DomainName.from("bücher.example").value)
    }

    @Test
    fun preservesAlreadyNormalizedDomain() {
        assertEquals("example.com", DomainName.from("example.com").value)
    }
}
