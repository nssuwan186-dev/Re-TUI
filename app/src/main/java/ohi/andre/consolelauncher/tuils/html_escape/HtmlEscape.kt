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
import kotlin.math.min

/**
 * 
 * 
 * Utility class for performing HTML escape/unescape operations.
 * 
 * 
 * **<u>Configuration of escape/unescape operations</u>**
 * 
 * 
 * 
 * **Escape** operations can be (optionally) configured by means of:
 * 
 * 
 *  * *Level*, which defines how deep the escape operation must be (what
 * chars are to be considered eligible for escaping, depending on the specific
 * needs of the scenario). Its values are defined by the [org.unbescape.html.HtmlEscapeLevel]
 * enum.
 *  * *Type*, which defines whether escaping should be performed by means of NCRs
 * (Named Character References), by means of decimal/hexadecimal numerical references,
 * using the HTML5 or the HTML 4 NCR set, etc. Its values are defined by the
 * [org.unbescape.html.HtmlEscapeType] enum.
 * 
 * 
 * 
 * **Unescape** operations need no configuration parameters. Unescape operations
 * will always perform *complete* unescape of NCRs (whole HTML5 set supported), decimal
 * and hexadecimal references.
 * 
 * 
 * **<u>Features</u>**
 * 
 * 
 * 
 * Specific features of the HTML escape/unescape operations performed by means of this class:
 * 
 * 
 *  * Whole HTML5 NCR (Named Character Reference) set supported, if required:
 * <tt>&amp;rsqb;</tt>,<tt>&amp;NewLine;</tt>, etc. (HTML 4 set available too).
 *  * Mixed named and numerical (decimal or hexa) character references supported.
 *  * Ability to default to numerical (decimal or hexa) references when an applicable NCR does not exist
 * (depending on the selected operation *level*).
 *  * Support for the whole Unicode character set: <tt>&#92;u0000</tt> to <tt>&#92;u10FFFF</tt>, including
 * characters not representable by only one <tt>char</tt> in Java (<tt>&gt;&#92;uFFFF</tt>).
 *  * Support for unescape of double-char NCRs in HTML5: <tt>'&amp;fjlig;'</tt>  <tt>'fj'</tt>.
 *  * Support for a set of HTML5 unescape *tweaks* included in the HTML5 specification:
 * 
 *  * Unescape of numerical character references not ending in semi-colon
 * (e.g. <tt>'&amp;#x23ac'</tt>).
 *  * Unescape of specific NCRs not ending in semi-colon (e.g. <tt>'&amp;aacute'</tt>).
 *  * Unescape of specific numerical character references wrongly specified by their Windows-1252
 * codepage code instead of the Unicode one (e.g. <tt>'&amp;#x80;'</tt> for ''
 * (<tt>'&amp;euro;'</tt>) instead of <tt>'&amp;#x20ac;'</tt>).
 * 
 * 
 * 
 * 
 * **<u>Input/Output</u>**
 * 
 * 
 * 
 * There are four different input/output modes that can be used in escape/unescape operations:
 * 
 * 
 *  * *<tt>String</tt> input, <tt>String</tt> output*: Input is specified as a <tt>String</tt> object
 * and output is returned as another. In order to improve memory performance, all escape and unescape
 * operations <u>will return the exact same input object as output if no escape/unescape modifications
 * are required</u>.
 *  * *<tt>String</tt> input, <tt>java.io.Writer</tt> output*: Input will be read from a String
 * and output will be written into the specified <tt>java.io.Writer</tt>.
 *  * *<tt>java.io.Reader</tt> input, <tt>java.io.Writer</tt> output*: Input will be read from a Reader
 * and output will be written into the specified <tt>java.io.Writer</tt>.
 *  * *<tt>char[]</tt> input, <tt>java.io.Writer</tt> output*: Input will be read from a char array
 * (<tt>char[]</tt>) and output will be written into the specified <tt>java.io.Writer</tt>.
 * Two <tt>int</tt> arguments called <tt>offset</tt> and <tt>len</tt> will be
 * used for specifying the part of the <tt>char[]</tt> that should be escaped/unescaped. These methods
 * should be called with <tt>offset = 0</tt> and <tt>len = text.length</tt> in order to process
 * the whole <tt>char[]</tt>.
 * 
 * 
 * **<u>Glossary</u>**
 * 
 * <dl>
 * <dt>NCR</dt>
 * <dd>Named Character Reference or *Character Entity Reference*: textual
 * representation of an Unicode codepoint: <tt>&amp;aacute;</tt></dd>
 * <dt>DCR</dt>
 * <dd>Decimal Character Reference: base-10 numerical representation of an Unicode codepoint:
 * <tt>&amp;#225;</tt></dd>
 * <dt>HCR</dt>
 * <dd>Hexadecimal Character Reference: hexadecimal numerical representation of an Unicode codepoint:
 * <tt>&amp;#xE1;</tt></dd>
 * <dt>Unicode Codepoint</dt>
 * <dd>Each of the <tt>int</tt> values conforming the Unicode code space.
 * Normally corresponding to a Java <tt>char</tt> primitive value (codepoint &lt;= <tt>&#92;uFFFF</tt>),
 * but might be two <tt>char</tt>s for codepoints <tt>&#92;u10000</tt> to <tt>&#92;u10FFFF</tt> if the
 * first <tt>char</tt> is a high surrogate (<tt>&#92;uD800</tt> to <tt>&#92;uDBFF</tt>) and the
 * second is a low surrogate (<tt>&#92;uDC00</tt> to <tt>&#92;uDFFF</tt>).</dd>
</dl> * 
 * 
 * **<u>References</u>**
 * 
 * 
 * 
 * The following references apply:
 * 
 * 
 *  * [Using character escapes in
 * markup and CSS](http://www.w3.org/International/questions/qa-escapes) [w3.org]
 *  * [Named Character References (or
 * *Character entity references*) in HTML 4](http://www.w3.org/TR/html4/sgml/entities.html) [w3.org]
 *  * [Named Character
 * References (or *Character entity references*) in HTML5](http://www.w3.org/TR/html5/syntax.html#named-character-references) [w3.org]
 *  * [Named Character
 * References (or *Character entity references*) in HTML 5.1](http://www.w3.org/TR/html51/syntax.html#named-character-references) [w3.org]
 *  * [How to consume a
 * character reference (HTML5 specification)](http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference) [w3.org]
 *  * [OWASP XSS (Cross Site Scripting) Prevention Cheat Sheet](https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet) [owasp.org]
 *  * [Supplementary characters in the Java Platform](http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html) [oracle.com]
 * 
 * 
 * 
 * @author Daniel Fernndez
 * 
 * @since 1.0.0
 */
object HtmlEscape {
    /**
     * 
     * 
     * Perform an HTML5 level 2 (result is ASCII) **escape** operation on a <tt>String</tt> input.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    fun escapeHtml5(text: String?): String? {
        return escapeHtml(
            text, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML5 level 1 (XML-style) **escape** operation on a <tt>String</tt> input.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as [.escapeHtml4Xml] because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    fun escapeHtml5Xml(text: String?): String? {
        return escapeHtml(
            text, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 2 (result is ASCII) **escape** operation on a <tt>String</tt> input.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    fun escapeHtml4(text: String?): String? {
        return escapeHtml(
            text, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 1 (XML-style) **escape** operation on a <tt>String</tt> input.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as [.escapeHtml5Xml] because
     * it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     * such character (<tt>&amp;apos;</tt>).
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    fun escapeHtml4Xml(text: String?): String? {
        return escapeHtml(
            text, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform a (configurable) HTML **escape** operation on a <tt>String</tt> input.
     * 
     * 
     * 
     * This method will perform an escape operation according to the specified
     * [org.unbescape.html.HtmlEscapeType] and [org.unbescape.html.HtmlEscapeLevel]
     * argument values.
     * 
     * 
     * 
     * All other <tt>String</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @param type the type of escape operation to be performed, see [org.unbescape.html.HtmlEscapeType].
     * @param level the escape level to be applied, see [org.unbescape.html.HtmlEscapeLevel].
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    fun escapeHtml(text: String?, type: HtmlEscapeType, level: HtmlEscapeLevel): String? {
        requireNotNull(type) { "The 'type' argument cannot be null" }

        requireNotNull(level) { "The 'level' argument cannot be null" }

        return HtmlEscapeUtil.escape(text, type, level)
    }


    /**
     * 
     * 
     * Perform an HTML5 level 2 (result is ASCII) **escape** operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml5(text: String, writer: Writer) {
        escapeHtml(
            text, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML5 level 1 (XML-style) **escape** operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as [.escapeHtml4Xml] because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml5Xml(text: String, writer: Writer) {
        escapeHtml(
            text, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 2 (result is ASCII) **escape** operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml4(text: String, writer: Writer) {
        escapeHtml(
            text, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 1 (XML-style) **escape** operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as [.escapeHtml5Xml] because
     * it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     * such character (<tt>&amp;apos;</tt>).
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml4Xml(text: String, writer: Writer) {
        escapeHtml(
            text, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform a (configurable) HTML **escape** operation on a <tt>String</tt> input, writing
     * results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * This method will perform an escape operation according to the specified
     * [org.unbescape.html.HtmlEscapeType] and [org.unbescape.html.HtmlEscapeLevel]
     * argument values.
     * 
     * 
     * 
     * All other <tt>String</tt>/<tt>Writer</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @param type the type of escape operation to be performed, see [org.unbescape.html.HtmlEscapeType].
     * @param level the escape level to be applied, see [org.unbescape.html.HtmlEscapeLevel].
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml(text: String, writer: Writer, type: HtmlEscapeType, level: HtmlEscapeLevel) {
        requireNotNull(writer) { "Argument 'writer' cannot be null" }

        requireNotNull(type) { "The 'type' argument cannot be null" }

        requireNotNull(level) { "The 'level' argument cannot be null" }

        HtmlEscapeUtil.escape(InternalStringReader(text), writer, type, level)
    }


    /**
     * 
     * 
     * Perform an HTML5 level 2 (result is ASCII) **escape** operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml5(reader: Reader?, writer: Writer) {
        escapeHtml(
            reader, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML5 level 1 (XML-style) **escape** operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as [.escapeHtml4Xml] because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml5Xml(reader: Reader?, writer: Writer) {
        escapeHtml(
            reader, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 2 (result is ASCII) **escape** operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml4(reader: Reader?, writer: Writer) {
        escapeHtml(
            reader, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 1 (XML-style) **escape** operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as [.escapeHtml5Xml] because
     * it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     * such character (<tt>&amp;apos;</tt>).
     * 
     * 
     * 
     * This method calls [.escapeHtml] with the following
     * preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml4Xml(reader: Reader?, writer: Writer) {
        escapeHtml(
            reader, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform a (configurable) HTML **escape** operation on a <tt>Reader</tt> input, writing
     * results to a <tt>Writer</tt>.
     * 
     * 
     * 
     * This method will perform an escape operation according to the specified
     * [org.unbescape.html.HtmlEscapeType] and [org.unbescape.html.HtmlEscapeLevel]
     * argument values.
     * 
     * 
     * 
     * All other <tt>Reader</tt>/<tt>Writer</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @param type the type of escape operation to be performed, see [org.unbescape.html.HtmlEscapeType].
     * @param level the escape level to be applied, see [org.unbescape.html.HtmlEscapeLevel].
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun escapeHtml(reader: Reader?, writer: Writer, type: HtmlEscapeType, level: HtmlEscapeLevel) {
        requireNotNull(writer) { "Argument 'writer' cannot be null" }

        requireNotNull(type) { "The 'type' argument cannot be null" }

        requireNotNull(level) { "The 'level' argument cannot be null" }

        HtmlEscapeUtil.escape(reader, writer, type, level)
    }


    /**
     * 
     * 
     * Perform an HTML5 level 2 (result is ASCII) **escape** operation on a <tt>char[]</tt> input.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml]
     * with the following preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    @Throws(IOException::class)
    fun escapeHtml5(text: CharArray?, offset: Int, len: Int, writer: Writer) {
        escapeHtml(
            text, offset, len, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML5 level 1 (XML-style) **escape** operation on a <tt>char[]</tt> input.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as
     * [.escapeHtml4Xml] because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * 
     * 
     * 
     * This method calls [.escapeHtml]
     * with the following preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    @Throws(IOException::class)
    fun escapeHtml5Xml(text: CharArray?, offset: Int, len: Int, writer: Writer) {
        escapeHtml(
            text, offset, len, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 2 (result is ASCII) **escape** operation on a <tt>char[]</tt> input.
     * 
     * 
     * 
     * *Level 2* means this method will escape:
     * 
     * 
     *  * The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     * <tt>&quot;</tt> and <tt>&#39;</tt>
     *  * All non ASCII characters.
     * 
     * 
     * 
     * This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     * (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     * character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * 
     * 
     * 
     * This method calls [.escapeHtml]
     * with the following preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    @Throws(IOException::class)
    fun escapeHtml4(text: CharArray?, offset: Int, len: Int, writer: Writer) {
        escapeHtml(
            text, offset, len, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform an HTML 4 level 1 (XML-style) **escape** operation on a <tt>char[]</tt> input.
     * 
     * 
     * 
     * *Level 1* means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * *XML-style* in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * 
     * 
     * 
     * Note this method may **not** produce the same results as
     * [.escapeHtml5Xml]  because it will escape the apostrophe as
     * <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for such character (<tt>&amp;apos;</tt>).
     * 
     * 
     * 
     * This method calls [.escapeHtml]
     * with the following preconfigured values:
     * 
     * 
     *  * <tt>type</tt>:
     * [org.unbescape.html.HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL]
     *  * <tt>level</tt>:
     * [org.unbescape.html.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT]
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    @Throws(IOException::class)
    fun escapeHtml4Xml(text: CharArray?, offset: Int, len: Int, writer: Writer) {
        escapeHtml(
            text, offset, len, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT
        )
    }


    /**
     * 
     * 
     * Perform a (configurable) HTML **escape** operation on a <tt>char[]</tt> input.
     * 
     * 
     * 
     * This method will perform an escape operation according to the specified
     * [org.unbescape.html.HtmlEscapeType] and [org.unbescape.html.HtmlEscapeLevel]
     * argument values.
     * 
     * 
     * 
     * All other <tt>char[]</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @param type the type of escape operation to be performed, see [org.unbescape.html.HtmlEscapeType].
     * @param level the escape level to be applied, see [org.unbescape.html.HtmlEscapeLevel].
     * @throws IOException if an input/output exception occurs
     */
    @Throws(IOException::class)
    fun escapeHtml(
        text: CharArray?, offset: Int, len: Int, writer: Writer,
        type: HtmlEscapeType, level: HtmlEscapeLevel
    ) {
        requireNotNull(writer) { "Argument 'writer' cannot be null" }

        requireNotNull(type) { "The 'type' argument cannot be null" }

        requireNotNull(level) { "The 'level' argument cannot be null" }

        val textLen = (if (text == null) 0 else text.size)

        require(!(offset < 0 || offset > textLen)) { "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen }

        require(!(len < 0 || (offset + len) > textLen)) { "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen }

        HtmlEscapeUtil.escape(text, offset, len, writer, type, level)
    }


    /**
     * 
     * 
     * Perform an HTML **unescape** operation on a <tt>String</tt> input.
     * 
     * 
     * 
     * No additional configuration arguments are required. Unescape operations
     * will always perform *complete* unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be unescaped.
     * @return The unescaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no unescaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    fun unescapeHtml(text: String?): String? {
        if (text == null) {
            return null
        }
        if (text.indexOf('&') < 0) {
            // Fail fast, avoid more complex (and less JIT-table) method to execute if not needed
            return text
        }
        return HtmlEscapeUtil.unescape(text)
    }


    /**
     * 
     * 
     * Perform an HTML **unescape** operation on a <tt>String</tt> input, writing results to
     * a <tt>Writer</tt>.
     * 
     * 
     * 
     * No additional configuration arguments are required. Unescape operations
     * will always perform *complete* unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>String</tt> to be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun unescapeHtml(text: String?, writer: Writer) {
        requireNotNull(writer) { "Argument 'writer' cannot be null" }
        if (text == null) {
            return
        }
        if (text.indexOf('&') < 0) {
            // Fail fast, avoid more complex (and less JIT-table) method to execute if not needed
            writer.write(text)
            return
        }

        HtmlEscapeUtil.unescape(InternalStringReader(text), writer)
    }


    /**
     * 
     * 
     * Perform an HTML **unescape** operation on a <tt>Reader</tt> input, writing results to
     * a <tt>Writer</tt>.
     * 
     * 
     * 
     * No additional configuration arguments are required. Unescape operations
     * will always perform *complete* unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param reader the <tt>Reader</tt> reading the text to be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * 
     * @since 1.1.2
     */
    @Throws(IOException::class)
    fun unescapeHtml(reader: Reader?, writer: Writer) {
        requireNotNull(writer) { "Argument 'writer' cannot be null" }

        HtmlEscapeUtil.unescape(reader, writer)
    }


    /**
     * 
     * 
     * Perform an HTML **unescape** operation on a <tt>char[]</tt> input.
     * 
     * 
     * 
     * No additional configuration arguments are required. Unescape operations
     * will always perform *complete* unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * 
     * 
     * 
     * This method is **thread-safe**.
     * 
     * 
     * @param text the <tt>char[]</tt> to be unescaped.
     * @param offset the position in <tt>text</tt> at which the unescape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     * be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    @Throws(IOException::class)
    fun unescapeHtml(text: CharArray?, offset: Int, len: Int, writer: Writer) {
        requireNotNull(writer) { "Argument 'writer' cannot be null" }

        val textLen = (if (text == null) 0 else text.size)

        require(!(offset < 0 || offset > textLen)) { "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen }

        require(!(len < 0 || (offset + len) > textLen)) { "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen }

        HtmlEscapeUtil.unescape(text, offset, len, writer)
    }


    /*
     * This is basically a very simplified, thread-unsafe version of StringReader that should
     * perform better than the original StringReader by removing all synchronization structures.
     *
     * Note the only implemented methods are those that we know are really used from within the
     * stream-based escape/unescape operations.
     */
    private class InternalStringReader(s: String) : Reader() {
        private var str: String?
        private val length: Int
        private var next = 0

        init {
            this.str = s
            this.length = s.length
        }

        @Throws(IOException::class)
        override fun read(): Int {
            if (this.next >= length) {
                return -1
            }
            return this.str!!.get(this.next++).code
        }

        @Throws(IOException::class)
        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            if ((off < 0) || (off > cbuf.size) || (len < 0) ||
                ((off + len) > cbuf.size) || ((off + len) < 0)
            ) {
                throw IndexOutOfBoundsException()
            } else if (len == 0) {
                return 0
            }
            if (this.next >= this.length) {
                return -1
            }
            val n = min(this.length - this.next, len)
            this.str!!.toCharArray(cbuf, off, this.next, this.next + n)
            this.next += n
            return n
        }

        @Throws(IOException::class)
        override fun close() {
            this.str = null // Just set the reference to null, help the GC
        }
    }
}

