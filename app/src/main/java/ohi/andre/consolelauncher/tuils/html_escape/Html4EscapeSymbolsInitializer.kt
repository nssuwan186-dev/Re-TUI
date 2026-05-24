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

import ohi.andre.consolelauncher.tuils.html_escape.HtmlEscapeSymbols.References
import java.util.Arrays

/**
 * 
 * 
 * This class initializes the [org.unbescape.html.HtmlEscapeSymbols.HTML4_SYMBOLS] structure.
 * 
 * 
 * @author Daniel Fernndez
 * 
 * @since 1.0.0
 */
internal object Html4EscapeSymbolsInitializer {
    fun initializeHtml4(): HtmlEscapeSymbols {
        val html4References = References()

        /*
         * -----------------------------------------------------------------
         *   HTML4 NAMED CHARACTER REFERENCES (CHARACTER ENTITY REFERENCES)
         *   See: http://www.w3.org/TR/html4/sgml/entities.html
         * -----------------------------------------------------------------
         */

        /* HTML NCRs FOR MARKUP-SIGNIFICANT CHARACTERS */
        // (Note HTML 4 does not include &apos; as a valid NCR)
        html4References.addReference('"'.code, "&quot;")
        html4References.addReference('&'.code, "&amp;")
        html4References.addReference('<'.code, "&lt;")
        html4References.addReference('>'.code, "&gt;")
        /* HTML NCRs FOR ISO-8859-1 CHARACTERS */
        html4References.addReference('\u00A0'.code, "&nbsp;")
        html4References.addReference('\u00A1'.code, "&iexcl;")
        html4References.addReference('\u00A2'.code, "&cent;")
        html4References.addReference('\u00A3'.code, "&pound;")
        html4References.addReference('\u00A4'.code, "&curren;")
        html4References.addReference('\u00A5'.code, "&yen;")
        html4References.addReference('\u00A6'.code, "&brvbar;")
        html4References.addReference('\u00A7'.code, "&sect;")
        html4References.addReference('\u00A8'.code, "&uml;")
        html4References.addReference('\u00A9'.code, "&copy;")
        html4References.addReference('\u00AA'.code, "&ordf;")
        html4References.addReference('\u00AB'.code, "&laquo;")
        html4References.addReference('\u00AC'.code, "&not;")
        html4References.addReference('\u00AD'.code, "&shy;")
        html4References.addReference('\u00AE'.code, "&reg;")
        html4References.addReference('\u00AF'.code, "&macr;")
        html4References.addReference('\u00B0'.code, "&deg;")
        html4References.addReference('\u00B1'.code, "&plusmn;")
        html4References.addReference('\u00B2'.code, "&sup2;")
        html4References.addReference('\u00B3'.code, "&sup3;")
        html4References.addReference('\u00B4'.code, "&acute;")
        html4References.addReference('\u00B5'.code, "&micro;")
        html4References.addReference('\u00B6'.code, "&para;")
        html4References.addReference('\u00B7'.code, "&middot;")
        html4References.addReference('\u00B8'.code, "&cedil;")
        html4References.addReference('\u00B9'.code, "&sup1;")
        html4References.addReference('\u00BA'.code, "&ordm;")
        html4References.addReference('\u00BB'.code, "&raquo;")
        html4References.addReference('\u00BC'.code, "&frac14;")
        html4References.addReference('\u00BD'.code, "&frac12;")
        html4References.addReference('\u00BE'.code, "&frac34;")
        html4References.addReference('\u00BF'.code, "&iquest;")
        html4References.addReference('\u00C0'.code, "&Agrave;")
        html4References.addReference('\u00C1'.code, "&Aacute;")
        html4References.addReference('\u00C2'.code, "&Acirc;")
        html4References.addReference('\u00C3'.code, "&Atilde;")
        html4References.addReference('\u00C4'.code, "&Auml;")
        html4References.addReference('\u00C5'.code, "&Aring;")
        html4References.addReference('\u00C6'.code, "&AElig;")
        html4References.addReference('\u00C7'.code, "&Ccedil;")
        html4References.addReference('\u00C8'.code, "&Egrave;")
        html4References.addReference('\u00C9'.code, "&Eacute;")
        html4References.addReference('\u00CA'.code, "&Ecirc;")
        html4References.addReference('\u00CB'.code, "&Euml;")
        html4References.addReference('\u00CC'.code, "&Igrave;")
        html4References.addReference('\u00CD'.code, "&Iacute;")
        html4References.addReference('\u00CE'.code, "&Icirc;")
        html4References.addReference('\u00CF'.code, "&Iuml;")
        html4References.addReference('\u00D0'.code, "&ETH;")
        html4References.addReference('\u00D1'.code, "&Ntilde;")
        html4References.addReference('\u00D2'.code, "&Ograve;")
        html4References.addReference('\u00D3'.code, "&Oacute;")
        html4References.addReference('\u00D4'.code, "&Ocirc;")
        html4References.addReference('\u00D5'.code, "&Otilde;")
        html4References.addReference('\u00D6'.code, "&Ouml;")
        html4References.addReference('\u00D7'.code, "&times;")
        html4References.addReference('\u00D8'.code, "&Oslash;")
        html4References.addReference('\u00D9'.code, "&Ugrave;")
        html4References.addReference('\u00DA'.code, "&Uacute;")
        html4References.addReference('\u00DB'.code, "&Ucirc;")
        html4References.addReference('\u00DC'.code, "&Uuml;")
        html4References.addReference('\u00DD'.code, "&Yacute;")
        html4References.addReference('\u00DE'.code, "&THORN;")
        html4References.addReference('\u00DF'.code, "&szlig;")
        html4References.addReference('\u00E0'.code, "&agrave;")
        html4References.addReference('\u00E1'.code, "&aacute;")
        html4References.addReference('\u00E2'.code, "&acirc;")
        html4References.addReference('\u00E3'.code, "&atilde;")
        html4References.addReference('\u00E4'.code, "&auml;")
        html4References.addReference('\u00E5'.code, "&aring;")
        html4References.addReference('\u00E6'.code, "&aelig;")
        html4References.addReference('\u00E7'.code, "&ccedil;")
        html4References.addReference('\u00E8'.code, "&egrave;")
        html4References.addReference('\u00E9'.code, "&eacute;")
        html4References.addReference('\u00EA'.code, "&ecirc;")
        html4References.addReference('\u00EB'.code, "&euml;")
        html4References.addReference('\u00EC'.code, "&igrave;")
        html4References.addReference('\u00ED'.code, "&iacute;")
        html4References.addReference('\u00EE'.code, "&icirc;")
        html4References.addReference('\u00EF'.code, "&iuml;")
        html4References.addReference('\u00F0'.code, "&eth;")
        html4References.addReference('\u00F1'.code, "&ntilde;")
        html4References.addReference('\u00F2'.code, "&ograve;")
        html4References.addReference('\u00F3'.code, "&oacute;")
        html4References.addReference('\u00F4'.code, "&ocirc;")
        html4References.addReference('\u00F5'.code, "&otilde;")
        html4References.addReference('\u00F6'.code, "&ouml;")
        html4References.addReference('\u00F7'.code, "&divide;")
        html4References.addReference('\u00F8'.code, "&oslash;")
        html4References.addReference('\u00F9'.code, "&ugrave;")
        html4References.addReference('\u00FA'.code, "&uacute;")
        html4References.addReference('\u00FB'.code, "&ucirc;")
        html4References.addReference('\u00FC'.code, "&uuml;")
        html4References.addReference('\u00FD'.code, "&yacute;")
        html4References.addReference('\u00FE'.code, "&thorn;")
        html4References.addReference('\u00FF'.code, "&yuml;")
        /* HTML NCRs FOR SYMBOLS, MATHEMATICAL SYMBOLS AND GREEK LETTERS */
        /* - Greek */
        html4References.addReference('\u0192'.code, "&fnof;")
        html4References.addReference('\u0391'.code, "&Alpha;")
        html4References.addReference('\u0392'.code, "&Beta;")
        html4References.addReference('\u0393'.code, "&Gamma;")
        html4References.addReference('\u0394'.code, "&Delta;")
        html4References.addReference('\u0395'.code, "&Epsilon;")
        html4References.addReference('\u0396'.code, "&Zeta;")
        html4References.addReference('\u0397'.code, "&Eta;")
        html4References.addReference('\u0398'.code, "&Theta;")
        html4References.addReference('\u0399'.code, "&Iota;")
        html4References.addReference('\u039A'.code, "&Kappa;")
        html4References.addReference('\u039B'.code, "&Lambda;")
        html4References.addReference('\u039C'.code, "&Mu;")
        html4References.addReference('\u039D'.code, "&Nu;")
        html4References.addReference('\u039E'.code, "&Xi;")
        html4References.addReference('\u039F'.code, "&Omicron;")
        html4References.addReference('\u03A0'.code, "&Pi;")
        html4References.addReference('\u03A1'.code, "&Rho;")
        html4References.addReference('\u03A3'.code, "&Sigma;")
        html4References.addReference('\u03A4'.code, "&Tau;")
        html4References.addReference('\u03A5'.code, "&Upsilon;")
        html4References.addReference('\u03A6'.code, "&Phi;")
        html4References.addReference('\u03A7'.code, "&Chi;")
        html4References.addReference('\u03A8'.code, "&Psi;")
        html4References.addReference('\u03A9'.code, "&Omega;")
        html4References.addReference('\u03B1'.code, "&alpha;")
        html4References.addReference('\u03B2'.code, "&beta;")
        html4References.addReference('\u03B3'.code, "&gamma;")
        html4References.addReference('\u03B4'.code, "&delta;")
        html4References.addReference('\u03B5'.code, "&epsilon;")
        html4References.addReference('\u03B6'.code, "&zeta;")
        html4References.addReference('\u03B7'.code, "&eta;")
        html4References.addReference('\u03B8'.code, "&theta;")
        html4References.addReference('\u03B9'.code, "&iota;")
        html4References.addReference('\u03BA'.code, "&kappa;")
        html4References.addReference('\u03BB'.code, "&lambda;")
        html4References.addReference('\u03BC'.code, "&mu;")
        html4References.addReference('\u03BD'.code, "&nu;")
        html4References.addReference('\u03BE'.code, "&xi;")
        html4References.addReference('\u03BF'.code, "&omicron;")
        html4References.addReference('\u03C0'.code, "&pi;")
        html4References.addReference('\u03C1'.code, "&rho;")
        html4References.addReference('\u03C2'.code, "&sigmaf;")
        html4References.addReference('\u03C3'.code, "&sigma;")
        html4References.addReference('\u03C4'.code, "&tau;")
        html4References.addReference('\u03C5'.code, "&upsilon;")
        html4References.addReference('\u03C6'.code, "&phi;")
        html4References.addReference('\u03C7'.code, "&chi;")
        html4References.addReference('\u03C8'.code, "&psi;")
        html4References.addReference('\u03C9'.code, "&omega;")
        html4References.addReference('\u03D1'.code, "&thetasym;")
        html4References.addReference('\u03D2'.code, "&upsih;")
        html4References.addReference('\u03D6'.code, "&piv;")
        /* - General punctuation */
        html4References.addReference('\u2022'.code, "&bull;")
        html4References.addReference('\u2026'.code, "&hellip;")
        html4References.addReference('\u2032'.code, "&prime;")
        html4References.addReference('\u2033'.code, "&Prime;")
        html4References.addReference('\u203E'.code, "&oline;")
        html4References.addReference('\u2044'.code, "&frasl;")
        /* - Letter-like symbols */
        html4References.addReference('\u2118'.code, "&weierp;")
        html4References.addReference('\u2111'.code, "&image;")
        html4References.addReference('\u211C'.code, "&real;")
        html4References.addReference('\u2122'.code, "&trade;")
        html4References.addReference('\u2135'.code, "&alefsym;")
        /* - Arrows */
        html4References.addReference('\u2190'.code, "&larr;")
        html4References.addReference('\u2191'.code, "&uarr;")
        html4References.addReference('\u2192'.code, "&rarr;")
        html4References.addReference('\u2193'.code, "&darr;")
        html4References.addReference('\u2194'.code, "&harr;")
        html4References.addReference('\u21B5'.code, "&crarr;")
        html4References.addReference('\u21D0'.code, "&lArr;")
        html4References.addReference('\u21D1'.code, "&uArr;")
        html4References.addReference('\u21D2'.code, "&rArr;")
        html4References.addReference('\u21D3'.code, "&dArr;")
        html4References.addReference('\u21D4'.code, "&hArr;")
        /* - Mathematical operators */
        html4References.addReference('\u2200'.code, "&forall;")
        html4References.addReference('\u2202'.code, "&part;")
        html4References.addReference('\u2203'.code, "&exist;")
        html4References.addReference('\u2205'.code, "&empty;")
        html4References.addReference('\u2207'.code, "&nabla;")
        html4References.addReference('\u2208'.code, "&isin;")
        html4References.addReference('\u2209'.code, "&notin;")
        html4References.addReference('\u220B'.code, "&ni;")
        html4References.addReference('\u220F'.code, "&prod;")
        html4References.addReference('\u2211'.code, "&sum;")
        html4References.addReference('\u2212'.code, "&minus;")
        html4References.addReference('\u2217'.code, "&lowast;")
        html4References.addReference('\u221A'.code, "&radic;")
        html4References.addReference('\u221D'.code, "&prop;")
        html4References.addReference('\u221E'.code, "&infin;")
        html4References.addReference('\u2220'.code, "&ang;")
        html4References.addReference('\u2227'.code, "&and;")
        html4References.addReference('\u2228'.code, "&or;")
        html4References.addReference('\u2229'.code, "&cap;")
        html4References.addReference('\u222A'.code, "&cup;")
        html4References.addReference('\u222B'.code, "&int;")
        html4References.addReference('\u2234'.code, "&there4;")
        html4References.addReference('\u223C'.code, "&sim;")
        html4References.addReference('\u2245'.code, "&cong;")
        html4References.addReference('\u2248'.code, "&asymp;")
        html4References.addReference('\u2260'.code, "&ne;")
        html4References.addReference('\u2261'.code, "&equiv;")
        html4References.addReference('\u2264'.code, "&le;")
        html4References.addReference('\u2265'.code, "&ge;")
        html4References.addReference('\u2282'.code, "&sub;")
        html4References.addReference('\u2283'.code, "&sup;")
        html4References.addReference('\u2284'.code, "&nsub;")
        html4References.addReference('\u2286'.code, "&sube;")
        html4References.addReference('\u2287'.code, "&supe;")
        html4References.addReference('\u2295'.code, "&oplus;")
        html4References.addReference('\u2297'.code, "&otimes;")
        html4References.addReference('\u22A5'.code, "&perp;")
        html4References.addReference('\u22C5'.code, "&sdot;")
        /* - Miscellaneous technical */
        html4References.addReference('\u2308'.code, "&lceil;")
        html4References.addReference('\u2309'.code, "&rceil;")
        html4References.addReference('\u230A'.code, "&lfloor;")
        html4References.addReference('\u230B'.code, "&rfloor;")
        html4References.addReference('\u2329'.code, "&lang;")
        html4References.addReference('\u232A'.code, "&rang;")
        /* - Geometric shapes */
        html4References.addReference('\u25CA'.code, "&loz;")
        html4References.addReference('\u2660'.code, "&spades;")
        html4References.addReference('\u2663'.code, "&clubs;")
        html4References.addReference('\u2665'.code, "&hearts;")
        html4References.addReference('\u2666'.code, "&diams;")
        /* HTML NCRs FOR INTERNATIONALIZATION CHARACTERS */
        /* - Latin Extended-A */
        html4References.addReference('\u0152'.code, "&OElig;")
        html4References.addReference('\u0153'.code, "&oelig;")
        html4References.addReference('\u0160'.code, "&Scaron;")
        html4References.addReference('\u0161'.code, "&scaron;")
        html4References.addReference('\u0178'.code, "&Yuml;")
        /* - Spacing modifier letters */
        html4References.addReference('\u02C6'.code, "&circ;")
        html4References.addReference('\u02DC'.code, "&tilde;")
        /* - General punctuation */
        html4References.addReference('\u2002'.code, "&ensp;")
        html4References.addReference('\u2003'.code, "&emsp;")
        html4References.addReference('\u2009'.code, "&thinsp;")
        html4References.addReference('\u200C'.code, "&zwnj;")
        html4References.addReference('\u200D'.code, "&zwj;")
        html4References.addReference('\u200E'.code, "&lrm;")
        html4References.addReference('\u200F'.code, "&rlm;")
        html4References.addReference('\u2013'.code, "&ndash;")
        html4References.addReference('\u2014'.code, "&mdash;")
        html4References.addReference('\u2018'.code, "&lsquo;")
        html4References.addReference('\u2019'.code, "&rsquo;")
        html4References.addReference('\u201A'.code, "&sbquo;")
        html4References.addReference('\u201C'.code, "&ldquo;")
        html4References.addReference('\u201D'.code, "&rdquo;")
        html4References.addReference('\u201E'.code, "&bdquo;")
        html4References.addReference('\u2020'.code, "&dagger;")
        html4References.addReference('\u2021'.code, "&Dagger;")
        html4References.addReference('\u2030'.code, "&permil;")
        html4References.addReference('\u2039'.code, "&lsaquo;")
        html4References.addReference('\u203A'.code, "&rsaquo;")
        html4References.addReference('\u20AC'.code, "&euro;")


        /*
         * Initialization of escape levels.
         * Defined levels :
         *
         *    - Level 0 : Only markup-significant characters except the apostrophe (')
         *    - Level 1 : Only markup-significant characters (including the apostrophe)
         *    - Level 2 : Markup-significant characters plus all non-ASCII
         *    - Level 3 : All non-alphanumeric characters
         *    - Level 4 : All characters
         */
        val escapeLevels = ByteArray(0x7f + 2)
        Arrays.fill(escapeLevels, 3.toByte())
        run {
            var c = 'A'
            while (c <= 'Z') {
                escapeLevels[c.code] = 4
                c++
            }
        }
        run {
            var c = 'a'
            while (c <= 'z') {
                escapeLevels[c.code] = 4
                c++
            }
        }
        var c = '0'
        while (c <= '9') {
            escapeLevels[c.code] = 4
            c++
        }
        escapeLevels['\''.code] = 1
        escapeLevels['"'.code] = 0
        escapeLevels['<'.code] = 0
        escapeLevels['>'.code] = 0
        escapeLevels['&'.code] = 0
        escapeLevels[0x7f + 1] = 2


        return HtmlEscapeSymbols(html4References, escapeLevels)
    }
}

