/*
 * =============================================================================
 * 
 *   Copyright (c) 2014-2017, The UNBESCAPE team (http://www.unbescape.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package ohi.andre.consolelauncher.tuils.html_escape

import java.io.IOException
import java.io.Reader
import java.io.Writer

/**
 * 
 * 
 * Internal class in charge of performing the real escape/unescape operations.
 * 
 * 
 * @author Daniel Fernndez
 * 
 * @since 1.0.0
 */
internal object HtmlEscapeUtil {
    /*
     * GLOSSARY
     * ------------------------
     *
     *   NCR
     *      Named Character Reference or Character Entity Reference: textual
     *      representation of an Unicode codepoint: &aacute;
     *
     *   DCR
     *      Decimal Character Reference: base-10 numerical representation of an Unicode codepoint: &#225;
     *
     *   HCR
     *      Hexadecimal Character Reference: hexadecimal numerical representation of an Unicode codepoint: &#xE1;
     *
     *   Unicode Codepoint
     *      Each of the int values conforming the Unicode code space.
     *      Normally corresponding to a Java char primitive value (codepoint <= \uFFFF),
     *      but might be two chars for codepoints \u10000 to \u10FFFF if the first char is a high
     *      surrogate (\uD800 to \uDBFF) and the second is a low surrogate (\uDC00 to \uDFFF).
     *      See: http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html
     *
     */
    /*
     * Prefixes and suffix defined for use in decimal/hexa escape and unescape.
     */
    private const val REFERENCE_PREFIX = '&'
    private const val REFERENCE_NUMERIC_PREFIX2 = '#'
    private const val REFERENCE_HEXA_PREFIX3_UPPER = 'X'
    private const val REFERENCE_HEXA_PREFIX3_LOWER = 'x'
    private val REFERENCE_DECIMAL_PREFIX = "&#".toCharArray()
    private val REFERENCE_HEXA_PREFIX = "&#x".toCharArray()
    private const val REFERENCE_SUFFIX = ';'

    /*
     * Small utility char arrays for hexadecimal conversion
     */
    private val HEXA_CHARS_UPPER = "0123456789ABCDEF".toCharArray()
    private val HEXA_CHARS_LOWER = "0123456789abcdef".toCharArray()


    /*
     * Perform an escape operation, based on String, according to the specified level and type.
     */
    fun escape(text: String?, escapeType: HtmlEscapeType, escapeLevel: HtmlEscapeLevel): String? {
        if (text == null) {
            return null
        }

        val level = escapeLevel.getEscapeLevel()
        val useHtml5 = escapeType.getUseHtml5()
        val useNCRs = escapeType.getUseNCRs()
        val useHexa = escapeType.getUseHexa()

        val symbols =
            (if (useHtml5) HtmlEscapeSymbols.HTML5_SYMBOLS else HtmlEscapeSymbols.HTML4_SYMBOLS)

        var strBuilder: StringBuilder? = null

        val offset = 0
        val max = text.length

        var readOffset = offset

        var i = offset
        while (i < max) {
            val c = text.get(i)


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (c <= HtmlEscapeSymbols.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[c.code]) {
                i++
                continue
            }


            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (c > HtmlEscapeSymbols.MAX_ASCII_CHAR
                && level < symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.MAX_ASCII_CHAR.code + 1]
            ) {
                i++
                continue
            }


            /*
             * Compute the codepoint. This will be used instead of the char for the rest of the process.
             */
            val codepoint = Character.codePointAt(text, i)


            /*
             * At this point we know for sure we will need some kind of escape, so we
             * can increase the offset and initialize the string builder if needed, along with
             * copying to it all the contents pending up to this point.
             */
            if (strBuilder == null) {
                strBuilder = StringBuilder(max + 20)
            }

            if (i - readOffset > 0) {
                strBuilder.append(text, readOffset, i)
            }

            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually escaping two char[] positions with a single codepoint.
                i++
            }

            readOffset = i + 1


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Perform the real escape, attending the different combinations of NCR, DCR and HCR needs.
             *
             * -----------------------------------------------------------------------------------------
             */
            if (useNCRs) {
                // We will try to use an NCR

                if (codepoint < HtmlEscapeSymbols.Companion.NCRS_BY_CODEPOINT_LEN) {
                    // codepoint < 0x2fff - all HTML4, most HTML5

                    val ncrIndex = symbols.NCRS_BY_CODEPOINT[codepoint]
                    if (ncrIndex != HtmlEscapeSymbols.Companion.NO_NCR) {
                        // There is an NCR for this codepoint!
                        strBuilder.append(symbols.SORTED_NCRS[ncrIndex.toInt()])
                        i++
                        continue
                    } // else, just let it exit the block and let decimal/hexa escape do its job
                } else if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
                    // codepoint >= 0x2fff. NCR, if exists, will live at the overflow map (if there is one).

                    val ncrIndex = symbols.NCRS_BY_CODEPOINT_OVERFLOW.get(codepoint)
                    if (ncrIndex != null) {
                        strBuilder.append(symbols.SORTED_NCRS[ncrIndex.toInt()])
                        i++
                        continue
                    } // else, just let it exit the block and let decimal/hexa escape do its job
                }
            }

            /*
             * No NCR-escape was possible (or allowed), so we need decimal/hexa escape.
             */
            if (useHexa) {
                strBuilder.append(REFERENCE_HEXA_PREFIX)
                strBuilder.append(Integer.toHexString(codepoint))
            } else {
                strBuilder.append(REFERENCE_DECIMAL_PREFIX)
                strBuilder.append(codepoint.toString())
            }
            strBuilder.append(REFERENCE_SUFFIX)

            i++
        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: return the original String object if no escape was actually needed. Otherwise
         *                 append the remaining unescaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */
        if (strBuilder == null) {
            return text
        }

        if (max - readOffset > 0) {
            strBuilder.append(text, readOffset, max)
        }

        return strBuilder.toString()
    }


    /*
     * Perform an escape operation, based on a Reader, according to the specified level and type and writing the
     * result to a Writer.
     *
     * Note this reader is going to be read char-by-char, so some kind of buffering might be appropriate if this
     * is an inconvenience for the specific Reader implementation.
     */
    @Throws(IOException::class)
    fun escape(
        reader: Reader?, writer: Writer, escapeType: HtmlEscapeType, escapeLevel: HtmlEscapeLevel
    ) {
        if (reader == null) {
            return
        }

        val level = escapeLevel.getEscapeLevel()
        val useHtml5 = escapeType.getUseHtml5()
        val useNCRs = escapeType.getUseNCRs()
        val useHexa = escapeType.getUseHexa()

        val symbols =
            (if (useHtml5) HtmlEscapeSymbols.HTML5_SYMBOLS else HtmlEscapeSymbols.HTML4_SYMBOLS)

        var c1: Int
        var c2: Int // c1: current char, c2: next char

        c2 = reader.read()

        while (c2 >= 0) {
            c1 = c2
            c2 = reader.read()


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (c1 <= HtmlEscapeSymbols.MAX_ASCII_CHAR.code && level < symbols.ESCAPE_LEVELS[c1]) {
                writer.write(c1)
                continue
            }


            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (c1 > HtmlEscapeSymbols.MAX_ASCII_CHAR.code
                && level < symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.MAX_ASCII_CHAR.code + 1]
            ) {
                writer.write(c1)
                continue
            }


            /*
             * Compute the codepoint. This will be used instead of the char for the rest of the process.
             */
            val codepoint = codePointAt(c1.toChar(), c2.toChar())


            /*
             * We know we need to escape, so from here on we will only work with the codepoint -- we can advance
             * the chars.
             */
            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually reading two char positions with a single codepoint.
                c1 = c2
                c2 = reader.read()
            }


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Perform the real escape, attending the different combinations of NCR, DCR and HCR needs.
             *
             * -----------------------------------------------------------------------------------------
             */
            if (useNCRs) {
                // We will try to use an NCR

                if (codepoint < HtmlEscapeSymbols.Companion.NCRS_BY_CODEPOINT_LEN) {
                    // codepoint < 0x2fff - all HTML4, most HTML5

                    val ncrIndex = symbols.NCRS_BY_CODEPOINT[codepoint]
                    if (ncrIndex != HtmlEscapeSymbols.Companion.NO_NCR) {
                        // There is an NCR for this codepoint!
                        writer.write(symbols.SORTED_NCRS[ncrIndex.toInt()])
                        continue
                    } // else, just let it exit the block and let decimal/hexa escape do its job
                } else if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
                    // codepoint >= 0x2fff. NCR, if exists, will live at the overflow map (if there is one).

                    val ncrIndex = symbols.NCRS_BY_CODEPOINT_OVERFLOW.get(codepoint)
                    if (ncrIndex != null) {
                        writer.write(symbols.SORTED_NCRS[ncrIndex.toInt()])
                        continue
                    } // else, just let it exit the block and let decimal/hexa escape do its job
                }
            }

            /*
             * No NCR-escape was possible (or allowed), so we need decimal/hexa escape.
             */
            if (useHexa) {
                writer.write(REFERENCE_HEXA_PREFIX)
                writer.write(Integer.toHexString(codepoint))
            } else {
                writer.write(REFERENCE_DECIMAL_PREFIX)
                writer.write(codepoint.toString())
            }
            writer.write(REFERENCE_SUFFIX.code)
        }
    }


    /*
     * Perform an escape operation, based on char[], according to the specified level and type.
     */
    @Throws(IOException::class)
    fun escape(
        text: CharArray?, offset: Int, len: Int, writer: Writer,
        escapeType: HtmlEscapeType, escapeLevel: HtmlEscapeLevel
    ) {
        if (text == null || text.size == 0) {
            return
        }

        val level = escapeLevel.getEscapeLevel()
        val useHtml5 = escapeType.getUseHtml5()
        val useNCRs = escapeType.getUseNCRs()
        val useHexa = escapeType.getUseHexa()

        val symbols =
            (if (useHtml5) HtmlEscapeSymbols.HTML5_SYMBOLS else HtmlEscapeSymbols.HTML4_SYMBOLS)

        val max = (offset + len)

        var readOffset = offset

        var i = offset
        while (i < max) {
            val c = text[i]


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (c <= HtmlEscapeSymbols.Companion.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[c.code]) {
                i++
                continue
            }


            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (c > HtmlEscapeSymbols.Companion.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.Companion.MAX_ASCII_CHAR.code + 1]) {
                i++
                continue
            }


            /*
             * Compute the codepoint. This will be used instead of the char for the rest of the process.
             */
            val codepoint = Character.codePointAt(text, i)


            /*
             * At this point we know for sure we will need some kind of escape, so we
             * can write all the contents pending up to this point.
             */
            if (i - readOffset > 0) {
                writer.write(text, readOffset, (i - readOffset))
            }

            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually escaping two char[] positions with a single codepoint.
                i++
            }

            readOffset = i + 1


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Perform the real escape, attending the different combinations of NCR, DCR and HCR needs.
             *
             * -----------------------------------------------------------------------------------------
             */
            if (useNCRs) {
                // We will try to use an NCR

                if (codepoint < HtmlEscapeSymbols.Companion.NCRS_BY_CODEPOINT_LEN) {
                    // codepoint < 0x2fff - all HTML4, most HTML5

                    val ncrIndex = symbols.NCRS_BY_CODEPOINT[codepoint]
                    if (ncrIndex != HtmlEscapeSymbols.Companion.NO_NCR) {
                        // There is an NCR for this codepoint!
                        writer.write(symbols.SORTED_NCRS[ncrIndex.toInt()])
                        i++
                        continue
                    } // else, just let it exit the block and let decimal/hexa escape do its job
                } else if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
                    // codepoint >= 0x2fff. NCR, if exists, will live at the overflow map (if there is one).

                    val ncrIndex = symbols.NCRS_BY_CODEPOINT_OVERFLOW.get(codepoint)
                    if (ncrIndex != null) {
                        writer.write(symbols.SORTED_NCRS[ncrIndex.toInt()])
                        i++
                        continue
                    } // else, just let it exit the block and let decimal/hexa escape do its job
                }
            }

            /*
             * No NCR-escape was possible (or allowed), so we need decimal/hexa escape.
             */
            if (useHexa) {
                writer.write(REFERENCE_HEXA_PREFIX)
                writer.write(Integer.toHexString(codepoint))
            } else {
                writer.write(REFERENCE_DECIMAL_PREFIX)
                writer.write(codepoint.toString())
            }
            writer.write(REFERENCE_SUFFIX.code)

            i++
        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: append the remaining unescaped text to the writer and return.
         * -----------------------------------------------------------------------------------------------
         */
        if (max - readOffset > 0) {
            writer.write(text, readOffset, (max - readOffset))
        }
    }


    /*
     * This translation is needed during unescape to support ill-formed escape codes for Windows 1252 codes
     * instead of the correct unicode ones (for example, &#x80; for the euro symbol instead of &#x20aC;). This is
     * something browsers do support, and included in the HTML5 spec for consuming character references.
     * See http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     */
    fun translateIllFormedCodepoint(codepoint: Int): Int {
        when (codepoint) {
            0x00 -> return 0xFFFD
            0x80 -> return 0x20AC
            0x82 -> return 0x201A
            0x83 -> return 0x0192
            0x84 -> return 0x201E
            0x85 -> return 0x2026
            0x86 -> return 0x2020
            0x87 -> return 0x2021
            0x88 -> return 0x02C6
            0x89 -> return 0x2030
            0x8A -> return 0x0160
            0x8B -> return 0x2039
            0x8C -> return 0x0152
            0x8E -> return 0x017D
            0x91 -> return 0x2018
            0x92 -> return 0x2019
            0x93 -> return 0x201C
            0x94 -> return 0x201D
            0x95 -> return 0x2022
            0x96 -> return 0x2013
            0x97 -> return 0x2014
            0x98 -> return 0x02DC
            0x99 -> return 0x2122
            0x9A -> return 0x0161
            0x9B -> return 0x203A
            0x9C -> return 0x0153
            0x9E -> return 0x017E
            0x9F -> return 0x0178
            else -> {}
        }
        if (codepoint >= 0xD800 && codepoint <= 0xDFFF) {
            return 0xFFFD
        } else if (codepoint > 0x10FFFF) {
            return 0xFFFD
        } else {
            return codepoint
        }
    }


    /*
     * This methods (the two versions) are used instead of Integer.parseInt(str,radix) in order to avoid the need
     * to create substrings of the text being unescaped to feed such method.
     * -  No need to check all chars are within the radix limits - reference parsing code will already have done so.
     */
    fun parseIntFromReference(text: String, start: Int, end: Int, radix: Int): Int {
        var result = 0
        for (i in start..<end) {
            val c = text.get(i)
            var n = -1
            for (j in HEXA_CHARS_UPPER.indices) {
                if (c == HEXA_CHARS_UPPER[j] || c == HEXA_CHARS_LOWER[j]) {
                    n = j
                    break
                }
            }
            result *= radix
            if (result < 0) {
                return 0xFFFD
            }
            result += n
            if (result < 0) {
                return 0xFFFD
            }
        }
        return result
    }

    fun parseIntFromReference(text: CharArray, start: Int, end: Int, radix: Int): Int {
        var result = 0
        for (i in start..<end) {
            val c = text[i]
            var n = -1
            for (j in HEXA_CHARS_UPPER.indices) {
                if (c == HEXA_CHARS_UPPER[j] || c == HEXA_CHARS_LOWER[j]) {
                    n = j
                    break
                }
            }
            result *= radix
            if (result < 0) {
                return 0xFFFD
            }
            result += n
            if (result < 0) {
                return 0xFFFD
            }
        }
        return result
    }


    /*
     * Perform an unescape operation based on String. Unescape operations are always based on the HTML5 symbol set.
     * Unescape operations will be performed in the most similar way possible to the process a browser follows for
     * showing HTML5 escaped code. See: http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     */
    fun unescape(text: String?): String? {
        if (text == null) {
            return null
        }

        // Unescape will always cover the full HTML5 spectrum.
        val symbols = HtmlEscapeSymbols.HTML5_SYMBOLS
        var strBuilder: StringBuilder? = null

        val offset = 0
        val max = text.length

        var readOffset = offset
        var referenceOffset = offset

        var i = offset
        while (i < max) {
            val c = text.get(i)

            /*
             * Check the need for an unescape operation at this point
             */
            if (c != REFERENCE_PREFIX || (i + 1) >= max) {
                i++
                continue
            }

            var codepoint = 0

            if (c == REFERENCE_PREFIX) {
                val c1 = text.get(i + 1)

                if (c1 == '\u0020' ||  // SPACE
                    c1 == '\n' ||  // LF
                    c1 == '\u0009' ||  // TAB
                    c1 == '\u000C' ||  // FF
                    c1 == '\u003C' ||  // LES-THAN SIGN
                    c1 == '\u0026'
                ) { // AMPERSAND
                    // Not a character references. No characters are consumed, and nothing is returned.
                    i++
                    continue
                } else if (c1 == REFERENCE_NUMERIC_PREFIX2) {
                    if (i + 2 >= max) {
                        // No reference possible
                        i++
                        continue
                    }

                    val c2 = text.get(i + 2)

                    if ((c2 == REFERENCE_HEXA_PREFIX3_LOWER || c2 == REFERENCE_HEXA_PREFIX3_UPPER) && (i + 3) < max) {
                        // This is a hexadecimal reference

                        var f = i + 3
                        while (f < max) {
                            val cf = text.get(f)
                            if (!((cf >= '0' && cf <= '9') || (cf >= 'A' && cf <= 'F') || (cf >= 'a' && cf <= 'f'))) {
                                break
                            }
                            f++
                        }

                        if ((f - (i + 3)) <= 0) {
                            // We weren't able to consume any hexa chars
                            i++
                            continue
                        }

                        codepoint = parseIntFromReference(text, i + 3, f, 16)
                        referenceOffset = f - 1

                        if ((f < max) && text.get(f) == REFERENCE_SUFFIX) {
                            referenceOffset++
                        }

                        codepoint = translateIllFormedCodepoint(codepoint)

                        // Don't continue here, just let the unescape code below do its job
                    } else if (c2 >= '0' && c2 <= '9') {
                        // This is a decimal reference

                        var f = i + 2
                        while (f < max) {
                            val cf = text.get(f)
                            if (!(cf >= '0' && cf <= '9')) {
                                break
                            }
                            f++
                        }

                        if ((f - (i + 2)) <= 0) {
                            // We weren't able to consume any decimal chars
                            i++
                            continue
                        }

                        codepoint = parseIntFromReference(text, i + 2, f, 10)
                        referenceOffset = f - 1

                        if ((f < max) && text.get(f) == REFERENCE_SUFFIX) {
                            referenceOffset++
                        }

                        codepoint = translateIllFormedCodepoint(codepoint)

                        // Don't continue here, just let the unescape code below do its job
                    } else {
                        // This is not a valid reference, just discard
                        i++
                        continue
                    }
                } else {
                    // This is a named reference, must be comprised only of ALPHABETIC chars

                    var f = i + 1
                    while (f < max) {
                        val cf = text.get(f)
                        if (!((cf >= 'a' && cf <= 'z') || (cf >= 'A' && cf <= 'Z') || (cf >= '0' && cf <= '9'))) {
                            break
                        }
                        f++
                    }

                    if ((f - (i + 1)) <= 0) {
                        // We weren't able to consume any alphanumeric
                        i++
                        continue
                    }

                    if ((f < max) && text.get(f) == REFERENCE_SUFFIX) {
                        f++
                    }

                    val ncrPosition: Int =
                        HtmlEscapeSymbols.binarySearch(symbols.SORTED_NCRS, text, i, f)
                    if (ncrPosition >= 0) {
                        codepoint = symbols.SORTED_CODEPOINTS[ncrPosition]
                    } else if (ncrPosition == Int.Companion.MIN_VALUE) {
                        // Not found! Just ignore our efforts to find a match.
                        i++
                        continue
                    } else if (ncrPosition < -10) {
                        // Found but partial!
                        val partialIndex = (-1) * (ncrPosition + 10)
                        val partialMatch = symbols.SORTED_NCRS[partialIndex]
                        codepoint = symbols.SORTED_CODEPOINTS[partialIndex]
                        f -= ((f - i) - partialMatch!!.size) // un-consume the chars remaining from the partial match
                    } else {
                        // Should never happen!
                        throw RuntimeException("Invalid unescape codepoint after search: " + ncrPosition)
                    }

                    referenceOffset = f - 1
                }
            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * can increase the offset and initialize the string builder if needed, along with
             * copying to it all the contents pending up to this point.
             */
            if (strBuilder == null) {
                strBuilder = StringBuilder(max + 5)
            }

            if (i - readOffset > 0) {
                strBuilder.append(text, readOffset, i)
            }

            i = referenceOffset
            readOffset = i + 1

            /*
             * --------------------------
             *
             * Perform the real unescape
             *
             * --------------------------
             */
            if (codepoint > '\uFFFF'.code) {
                strBuilder.append(Character.toChars(codepoint))
            } else if (codepoint < 0) {
                // This is a double-codepoint unescape operation
                val codepoints = symbols.DOUBLE_CODEPOINTS!![((-1) * codepoint) - 1]
                if (codepoints!![0] > '\uFFFF'.code) {
                    strBuilder.append(Character.toChars(codepoints[0]))
                } else {
                    strBuilder.append(codepoints[0].toChar())
                }
                if (codepoints[1] > '\uFFFF'.code) {
                    strBuilder.append(Character.toChars(codepoints[1]))
                } else {
                    strBuilder.append(codepoints[1].toChar())
                }
            } else {
                strBuilder.append(codepoint.toChar())
            }

            i++
        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: return the original String object if no unescape was actually needed. Otherwise
         *                 append the remaining escaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */
        if (strBuilder == null) {
            return text
        }

        if (max - readOffset > 0) {
            strBuilder.append(text, readOffset, max)
        }

        return strBuilder.toString()
    }


    /*
     * Perform an unescape operation based on a Reader, writing the results to a Writer. Unescape operations are
     * always based on the HTML5 symbol set. Unescape operations will be performed in the most similar way
     * possible to the process a browser follows for showing HTML5 escaped code.
     * See: http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     *
     * Note this reader is going to be read char-by-char, so some kind of buffering might be appropriate if this
     * is an inconvenience for the specific Reader implementation.
     */
    @Throws(IOException::class)
    fun unescape(reader: Reader?, writer: Writer) {
        if (reader == null) {
            return
        }

        // Unescape will always cover the full HTML5 spectrum.
        val symbols = HtmlEscapeSymbols.HTML5_SYMBOLS

        var escapes = CharArray(10)
        var escapei = 0

        var c1: Int
        var c2: Int
        var ce: Int // c1: current char, c2: next char, ce: current escaped char

        c2 = reader.read()

        while (c2 >= 0) {
            c1 = c2
            c2 = reader.read()

            escapei = 0

            /*
             * Check the need for an unescape operation at this point
             */
            if (c1 != REFERENCE_PREFIX.code || c2 < 0) {
                writer.write(c1)
                continue
            }

            var codepoint = 0

            if (c1 == REFERENCE_PREFIX.code) {
                if (c2 == '\u0020'.code ||  // SPACE
                    c2 == '\n'.code ||  // LF
                    c2 == '\u0009'.code ||  // TAB
                    c2 == '\u000C'.code ||  // FF
                    c2 == '\u003C'.code ||  // LES-THAN SIGN
                    c2 == '\u0026'.code
                ) { // AMPERSAND
                    // Not a character references. No characters are consumed, and nothing is returned.
                    writer.write(c1)
                    continue
                } else if (c2 == REFERENCE_NUMERIC_PREFIX2.code) {
                    val c3 = reader.read()

                    if (c3 < 0) {
                        // No reference possible
                        writer.write(c1)
                        writer.write(c2)
                        c1 = c2
                        c2 = c3
                        continue
                    }

                    if ((c3 == REFERENCE_HEXA_PREFIX3_LOWER.code || c3 == REFERENCE_HEXA_PREFIX3_UPPER.code)) {
                        // This is a hexadecimal reference

                        ce = reader.read()
                        while (ce >= 0) {
                            if (!((ce >= '0'.code && ce <= '9'.code) || (ce >= 'A'.code && ce <= 'F'.code) || (ce >= 'a'.code && ce <= 'f'.code))) {
                                break
                            }
                            if (escapei == escapes.size) {
                                // too many escape chars for our array: grow it!
                                val newEscapes = CharArray(escapes.size + 4)
                                System.arraycopy(escapes, 0, newEscapes, 0, escapes.size)
                                escapes = newEscapes
                            }
                            escapes[escapei] = ce.toChar()
                            ce = reader.read()
                            escapei++
                        }

                        if (escapei == 0) {
                            // We weren't able to consume any hexa chars
                            writer.write(c1)
                            writer.write(c2)
                            writer.write(c3)
                            c1 = c3
                            c2 = ce
                            continue
                        }

                        c1 = escapes[escapei - 1].code
                        c2 = ce

                        codepoint = parseIntFromReference(escapes, 0, escapei, 16)

                        if (c2 == REFERENCE_SUFFIX.code) {
                            // If the reference ends in a ';', just consume it
                            c1 = c2
                            c2 = reader.read()
                        }

                        codepoint = translateIllFormedCodepoint(codepoint)

                        escapei = 0

                        // Don't continue here, just let the unescape code below do its job
                    } else if (c3 >= '0'.code && c3 <= '9'.code) {
                        // This is a decimal reference

                        ce = c3
                        while (ce >= 0) {
                            if (!(ce >= '0'.code && ce <= '9'.code)) {
                                break
                            }
                            if (escapei == escapes.size) {
                                // too many escape chars for our array: grow it!
                                val newEscapes = CharArray(escapes.size + 4)
                                System.arraycopy(escapes, 0, newEscapes, 0, escapes.size)
                                escapes = newEscapes
                            }
                            escapes[escapei] = ce.toChar()
                            ce = reader.read()
                            escapei++
                        }

                        if (escapei == 0) {
                            // We weren't able to consume any decimal chars
                            writer.write(c1)
                            writer.write(c2)
                            c1 = c2
                            c2 = c3
                            continue
                        }

                        c1 = escapes[escapei - 1].code
                        c2 = ce

                        codepoint = parseIntFromReference(escapes, 0, escapei, 10)

                        if (c2 == REFERENCE_SUFFIX.code) {
                            // If the reference ends in a ';', just consume it
                            c1 = c2
                            c2 = reader.read()
                        }

                        codepoint = translateIllFormedCodepoint(codepoint)

                        escapei = 0

                        // Don't continue here, just let the unescape code below do its job
                    } else {
                        // This is not a valid reference, just discard
                        writer.write(c1)
                        writer.write(c2)
                        c1 = c2
                        c2 = c3
                        continue
                    }
                } else {
                    // This is a named reference, must be comprised only of ALPHABETIC chars

                    ce = c2
                    while (ce >= 0) {
                        if (!((ce >= '0'.code && ce <= '9'.code) || (ce >= 'A'.code && ce <= 'Z'.code) || (ce >= 'a'.code && ce <= 'z'.code))) {
                            break
                        }
                        if (escapei == escapes.size) {
                            // too many escape chars for our array: grow it!
                            val newEscapes = CharArray(escapes.size + 4)
                            System.arraycopy(escapes, 0, newEscapes, 0, escapes.size)
                            escapes = newEscapes
                        }
                        escapes[escapei] = ce.toChar()
                        ce = reader.read()
                        escapei++
                    }

                    if (escapei == 0) {
                        // We weren't able to consume any decimal chars
                        writer.write(c1)
                        continue
                    }

                    if (escapei + 2 >= escapes.size) {
                        // the entire escape sequence does not fit: grow it!
                        val newEscapes = CharArray(escapes.size + 4)
                        System.arraycopy(escapes, 0, newEscapes, 0, escapes.size)
                        escapes = newEscapes
                    }

                    System.arraycopy(escapes, 0, escapes, 1, escapei)
                    escapes[0] = c1.toChar()
                    escapei++

                    if (ce == REFERENCE_SUFFIX.code) {
                        // If the reference ends in a ';', just consume it
                        escapes[escapei++] = ce.toChar()
                        ce = reader.read()
                    }

                    c1 = escapes[escapei - 1].code
                    c2 = ce

                    val ncrPosition: Int =
                        HtmlEscapeSymbols.binarySearch(symbols.SORTED_NCRS, escapes, 0, escapei)
                    if (ncrPosition >= 0) {
                        codepoint = symbols.SORTED_CODEPOINTS[ncrPosition]
                        escapei = 0
                    } else if (ncrPosition == Int.Companion.MIN_VALUE) {
                        // Not found! Just ignore our efforts to find a match.
                        writer.write(escapes, 0, escapei)
                        continue
                    } else if (ncrPosition < -10) {
                        // Found but partial!
                        val partialIndex = (-1) * (ncrPosition + 10)
                        val partialMatch = symbols.SORTED_NCRS[partialIndex]
                        codepoint = symbols.SORTED_CODEPOINTS[partialIndex]
                        System.arraycopy(
                            escapes,
                            partialMatch!!.size,
                            escapes,
                            0,
                            (escapei - partialMatch.size)
                        )
                        escapei -= partialMatch.size // so that we know we have to output the rest of 'escapes'
                    } else {
                        // Should never happen!
                        throw RuntimeException("Invalid unescape codepoint after search: " + ncrPosition)
                    }
                }
            }

            /*
             * --------------------------
             *
             * Perform the real unescape
             *
             * --------------------------
             */
            if (codepoint > '\uFFFF'.code) {
                writer.write(Character.toChars(codepoint))
            } else if (codepoint < 0) {
                // This is a double-codepoint unescape operation
                val codepoints = symbols.DOUBLE_CODEPOINTS!![((-1) * codepoint) - 1]
                if (codepoints!![0] > '\uFFFF'.code) {
                    writer.write(Character.toChars(codepoints[0]))
                } else {
                    writer.write(codepoints[0].toChar().code)
                }
                if (codepoints[1] > '\uFFFF'.code) {
                    writer.write(Character.toChars(codepoints[1]))
                } else {
                    writer.write(codepoints[1].toChar().code)
                }
            } else {
                writer.write(codepoint.toChar().code)
            }

            /*
             * ----------------------------------------
             * Cleanup, in case we had a partial match
             * ----------------------------------------
             */
            if (escapei > 0) {
                writer.write(escapes, 0, escapei)
                escapei = 0
            }
        }
    }


    /*
     * Perform an unescape operation based on char[]. Unescape operations are always based on the HTML5 symbol set.
     * Unescape operations will be performed in the most similar way possible to the process a browser follows for
     * showing HTML5 escaped code. See: http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     */
    @Throws(IOException::class)
    fun unescape(text: CharArray?, offset: Int, len: Int, writer: Writer) {
        if (text == null) {
            return
        }

        val symbols = HtmlEscapeSymbols.HTML5_SYMBOLS

        val max = (offset + len)

        var readOffset = offset
        var referenceOffset = offset

        var i = offset
        while (i < max) {
            val c = text[i]

            /*
             * Check the need for an unescape operation at this point
             */
            if (c != REFERENCE_PREFIX || (i + 1) >= max) {
                i++
                continue
            }

            var codepoint = 0

            if (c == REFERENCE_PREFIX) {
                val c1 = text[i + 1]

                if (c1 == '\u0020' ||  // SPACE
                    c1 == '\n' ||  // LF
                    c1 == '\u0009' ||  // TAB
                    c1 == '\u000C' ||  // FF
                    c1 == '\u003C' ||  // LES-THAN SIGN
                    c1 == '\u0026'
                ) { // AMPERSAND
                    // Not a character references. No characters are consumed, and nothing is returned.
                    i++
                    continue
                } else if (c1 == REFERENCE_NUMERIC_PREFIX2) {
                    if (i + 2 >= max) {
                        // No reference possible
                        i++
                        continue
                    }

                    val c2 = text[i + 2]

                    if ((c2 == REFERENCE_HEXA_PREFIX3_LOWER || c2 == REFERENCE_HEXA_PREFIX3_UPPER) && (i + 3) < max) {
                        // This is a hexadecimal reference

                        var f = i + 3
                        while (f < max) {
                            val cf = text[f]
                            if (!((cf >= '0' && cf <= '9') || (cf >= 'A' && cf <= 'F') || (cf >= 'a' && cf <= 'f'))) {
                                break
                            }
                            f++
                        }

                        if ((f - (i + 3)) <= 0) {
                            // We weren't able to consume any hexa chars
                            i++
                            continue
                        }

                        codepoint = parseIntFromReference(text, i + 3, f, 16)
                        referenceOffset = f - 1

                        if ((f < max) && text[f] == REFERENCE_SUFFIX) {
                            referenceOffset++
                        }

                        codepoint = translateIllFormedCodepoint(codepoint)

                        // Don't continue here, just let the unescape code below do its job
                    } else if (c2 >= '0' && c2 <= '9') {
                        // This is a decimal reference

                        var f = i + 2
                        while (f < max) {
                            val cf = text[f]
                            if (!(cf >= '0' && cf <= '9')) {
                                break
                            }
                            f++
                        }

                        if ((f - (i + 2)) <= 0) {
                            // We weren't able to consume any decimal chars
                            i++
                            continue
                        }

                        codepoint = parseIntFromReference(text, i + 2, f, 10)
                        referenceOffset = f - 1

                        if ((f < max) && text[f] == REFERENCE_SUFFIX) {
                            referenceOffset++
                        }

                        codepoint = translateIllFormedCodepoint(codepoint)

                        // Don't continue here, just let the unescape code below do its job
                    } else {
                        // This is not a valid reference, just discard
                        i++
                        continue
                    }
                } else {
                    // This is a named reference, must be comprised only of ALPHABETIC chars

                    var f = i + 1
                    while (f < max) {
                        val cf = text[f]
                        if (!((cf >= 'a' && cf <= 'z') || (cf >= 'A' && cf <= 'Z') || (cf >= '0' && cf <= '9'))) {
                            break
                        }
                        f++
                    }

                    if ((f - (i + 1)) <= 0) {
                        // We weren't able to consume any alphanumeric
                        i++
                        continue
                    }

                    if ((f < max) && text[f] == REFERENCE_SUFFIX) {
                        f++
                    }

                    val ncrPosition: Int =
                        HtmlEscapeSymbols.binarySearch(symbols.SORTED_NCRS, text, i, f)
                    if (ncrPosition >= 0) {
                        codepoint = symbols.SORTED_CODEPOINTS[ncrPosition]
                    } else if (ncrPosition == Int.Companion.MIN_VALUE) {
                        // Not found! Just ignore our efforts to find a match.
                        i++
                        continue
                    } else if (ncrPosition < -10) {
                        // Found but partial!
                        val partialIndex = (-1) * (ncrPosition + 10)
                        val partialMatch = symbols.SORTED_NCRS[partialIndex]
                        codepoint = symbols.SORTED_CODEPOINTS[partialIndex]
                        f -= ((f - i) - partialMatch!!.size) // un-consume the chars remaining from the partial match
                    } else {
                        // Should never happen!
                        throw RuntimeException("Invalid unescape codepoint after search: " + ncrPosition)
                    }

                    referenceOffset = f - 1
                }
            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * write all the contents pending up to this point.
             */
            if (i - readOffset > 0) {
                writer.write(text, readOffset, (i - readOffset))
            }

            i = referenceOffset
            readOffset = i + 1

            /*
             * --------------------------
             *
             * Perform the real unescape
             *
             * --------------------------
             */
            if (codepoint > '\uFFFF'.code) {
                writer.write(Character.toChars(codepoint))
            } else if (codepoint < 0) {
                // This is a double-codepoint unescape operation
                val codepoints = symbols.DOUBLE_CODEPOINTS!![((-1) * codepoint) - 1]
                if (codepoints!![0] > '\uFFFF'.code) {
                    writer.write(Character.toChars(codepoints[0]))
                } else {
                    writer.write(codepoints[0].toChar().code)
                }
                if (codepoints[1] > '\uFFFF'.code) {
                    writer.write(Character.toChars(codepoints[1]))
                } else {
                    writer.write(codepoints[1].toChar().code)
                }
            } else {
                writer.write(codepoint.toChar().code)
            }

            i++
        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: writer the remaining escaped text and return.
         * -----------------------------------------------------------------------------------------------
         */
        if (max - readOffset > 0) {
            writer.write(text, readOffset, (max - readOffset))
        }
    }


    private fun codePointAt(c1: Char, c2: Char): Int {
        if (Character.isHighSurrogate(c1)) {
            if (c2.code >= 0) {
                if (Character.isLowSurrogate(c2)) {
                    return Character.toCodePoint(c1, c2)
                }
            }
        }
        return c1.code
    }
}

