/*******************************************************************************
 * Copyright 2014 A3 lab (Dipartimento di Informatica, Università di Pisa)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.acubelab.tagme.preprocessing;

import it.acubelab.Chars;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;


public class WikiTextExtractor {

	public static final int
		MIN_ABSTRACT_CHARS = 100,
		MAX_ABSTRACT_CHARS = 300;
	
	static final String[] GARBAGE_TAGS = 
		{"ref", "gallery", "timeline", "noinclude", "pre", "table", "source",
		"tr", "td", "ul", "li", "ol", "dl", "dt", "dd", "menu", "dir"};
	
	static final String[] WRAPPER_TAGS = 
		{"nowiki", "cite", "hiero", "div", "font", "span", "strong",
        "strike", "blockquote", "tt", "var", "sup", "sub", "big", "small",
        "center", "h1", "h2", "h3", "em", "b", "i", "u", "a", "s", "p"};
	
	static final String[] SINGLE_TAGS = 
		{"references", "ref", "img", "br", "hr", "li", "dt", "dd"};
	
	static final String[] PLACEHOLDER_TAGS = {"code", "math"};
	
	public static final String[][] HTML_ENTITIES = {
	{"&nbsp;"," "}, {"&iexcl;","\u00A1"}, {"&cent;","\u00A2"},
    {"&pound;","\u00A3"}, {"&curren;","\u00A4"}, {"&yen;","\u00A5"},
    {"&brvbar;","\u00A6"}, {"&sect;","\u00A7"}, {"&uml;","\u00A8"},
    {"&copy;","\u00A9"}, {"&ordf;","\u00AA"}, {"&laquo;","\u00AB"},
    {"&not;","\u00AC"}, {"&shy;","\u00AD"}, {"&reg;","\u00AE"},
    {"&macr;","\u00AF"}, {"&deg;","\u00B0"}, {"&plusmn;","\u00B1"},
    {"&sup2;","\u00B2"}, {"&sup3;","\u00B3"}, {"&acute;","\u00B4"},
    {"&micro;","\u00B5"}, {"&para;","\u00B6"}, {"&middot;","\u00B7"},
    {"&cedil;","\u00B8"}, {"&sup1;","\u00B9"}, {"&ordm;","\u00BA"},
    {"&raquo;","\u00BB"}, {"&frac14;","\u00BC"}, {"&frac12;","\u00BD"},
    {"&frac34;","\u00BE"}, {"&iquest;","\u00BF"}, {"&Agrave;","\u00C0"},
    {"&Aacute;","\u00C1"}, {"&Acirc;","\u00C2"}, {"&Atilde;","\u00C3"},
    {"&Auml;","\u00C4"}, {"&Aring;","\u00C5"}, {"&AElig;","\u00C6"},
    {"&Ccedil;","\u00C7"}, {"&Egrave;","\u00C8"}, {"&Eacute;","\u00C9"},
    {"&Ecirc;","\u00CA"}, {"&Euml;","\u00CB"}, {"&Igrave;","\u00CC"},
    {"&Iacute;","\u00CD"}, {"&Icirc;","\u00CE"}, {"&Iuml;","\u00CF"},
    {"&ETH;","\u00D0"}, {"&Ntilde;","\u00D1"}, {"&Ograve;","\u00D2"},
    {"&Oacute;","\u00D3"}, {"&Ocirc;","\u00D4"}, {"&Otilde;","\u00D5"},
    {"&Ouml;","\u00D6"}, {"&times;","\u00D7"}, {"&Oslash;","\u00D8"},
    {"&Ugrave;","\u00D9"}, {"&Uacute;","\u00DA"}, {"&Ucirc;","\u00DB"},
    {"&Uuml;","\u00DC"}, {"&Yacute;","\u00DD"}, {"&THORN;","\u00DE"},
    {"&szlig;","\u00DF"}, {"&agrave;","\u00E0"}, {"&aacute;","\u00E1"},
    {"&acirc;","\u00E2"}, {"&atilde;","\u00E3"}, {"&auml;","\u00E4"},
    {"&aring;","\u00E5"}, {"&aelig;","\u00E6"}, {"&ccedil;","\u00E7"},
    {"&egrave;","\u00E8"}, {"&eacute;","\u00E9"}, {"&ecirc;","\u00EA"},
    {"&euml;","\u00EB"}, {"&igrave;","\u00EC"}, {"&iacute;","\u00ED"},
    {"&icirc;","\u00EE"}, {"&iuml;","\u00EF"}, {"&eth;","\u00F0"},
    {"&ntilde;","\u00F1"}, {"&ograve;","\u00F2"}, {"&oacute;","\u00F3"},
    {"&ocirc;","\u00F4"}, {"&otilde;","\u00F5"}, {"&ouml;","\u00F6"},
    {"&divide;","\u00F7"}, {"&oslash;","\u00F8"}, {"&ugrave;","\u00F9"},
    {"&uacute;","\u00FA"}, {"&ucirc;","\u00FB"}, {"&uuml;","\u00FC"},
    {"&yacute;","\u00FD"}, {"&thorn;","\u00FE"}, {"&yuml;","\u00FF"},
    {"&fnof;","\u0192"}, {"&Alpha;","\u0391"}, {"&Beta;","\u0392"},
    {"&Gamma;","\u0393"}, {"&Delta;","\u0394"}, {"&Epsilon;","\u0395"},
    {"&Zeta;","\u0396"}, {"&Eta;","\u0397"}, {"&Theta;","\u0398"},
    {"&Iota;","\u0399"}, {"&Kappa;","\u039A"}, {"&Lambda;","\u039B"},
    {"&Mu;","\u039C"}, {"&Nu;","\u039D"}, {"&Xi;","\u039E"},
    {"&Omicron;","\u039F"}, {"&Pi;","\u03A0"}, {"&Rho;","\u03A1"},
    {"&Sigma;","\u03A3"}, {"&Tau;","\u03A4"}, {"&Upsilon;","\u03A5"},
    {"&Phi;","\u03A6"}, {"&Chi;","\u03A7"}, {"&Psi;","\u03A8"},
    {"&Omega;","\u03A9"}, {"&alpha;","\u03B1"}, {"&beta;","\u03B2"},
    {"&gamma;","\u03B3"}, {"&delta;","\u03B4"}, {"&epsilon;","\u03B5"},
    {"&zeta;","\u03B6"}, {"&eta;","\u03B7"}, {"&theta;","\u03B8"},
    {"&iota;","\u03B9"}, {"&kappa;","\u03BA"}, {"&lambda;","\u03BB"},
    {"&mu;","\u03BC"}, {"&nu;","\u03BD"}, {"&xi;","\u03BE"},
    {"&omicron;","\u03BF"}, {"&pi;","\u03C0"}, {"&rho;","\u03C1"},
    {"&sigmaf;","\u03C2"}, {"&sigma;","\u03C3"}, {"&tau;","\u03C4"},
    {"&upsilon;","\u03C5"}, {"&phi;","\u03C6"}, {"&chi;","\u03C7"},
    {"&psi;","\u03C8"}, {"&omega;","\u03C9"}, {"&thetasym;","\u03D1"},
    {"&upsih;","\u03D2"}, {"&piv;","\u03D6"}, {"&bull;","\u2022"},
    {"&hellip;","\u2026"}, {"&prime;","\u2032"}, {"&Prime;","\u2033"},
    {"&oline;","\u203E"}, {"&frasl;","\u2044"}, {"&weierp;","\u2118"},
    {"&image;","\u2111"}, {"&real;","\u211C"}, {"&trade;","\u2122"},
    {"&alefsym;","\u2135"}, {"&larr;","\u2190"}, {"&uarr;","\u2191"},
    {"&rarr;","\u2192"}, {"&darr;","\u2193"}, {"&harr;","\u2194"},
    {"&crarr;","\u21B5"}, {"&lArr;","\u21D0"}, {"&uArr;","\u21D1"},
    {"&rArr;","\u21D2"}, {"&dArr;","\u21D3"}, {"&hArr;","\u21D4"},
    {"&forall;","\u2200"}, {"&part;","\u2202"}, {"&exist;","\u2203"},
    {"&empty;","\u2205"}, {"&nabla;","\u2207"}, {"&isin;","\u2208"},
    {"&notin;","\u2209"}, {"&ni;","\u220B"}, {"&prod;","\u220F"},
    {"&sum;","\u2211"}, {"&minus;","\u2212"}, {"&lowast;","\u2217"},
    {"&radic;","\u221A"}, {"&prop;","\u221D"}, {"&infin;","\u221E"},
    {"&ang;","\u2220"}, {"&and;","\u2227"}, {"&or;","\u2228"},
    {"&cap;","\u2229"}, {"&cup;","\u222A"}, {"&int;","\u222B"},
    {"&there4;","\u2234"}, {"&sim;","\u223C"}, {"&cong;","\u2245"},
    {"&asymp;","\u2248"}, {"&ne;","\u2260"}, {"&equiv;","\u2261"},
    {"&le;","\u2264"}, {"&ge;","\u2265"}, {"&sub;","\u2282"},
    {"&sup;","\u2283"}, {"&nsub;","\u2284"}, {"&sube;","\u2286"},
    {"&supe;","\u2287"}, {"&oplus;","\u2295"}, {"&otimes;","\u2297"},
    {"&perp;","\u22A5"}, {"&sdot;","\u22C5"}, {"&lceil;","\u2308"},
    {"&rceil;","\u2309"}, {"&lfloor;","\u230A"}, {"&rfloor;","\u230B"},
    {"&lang;","\u2329"}, {"&rang;","\u232A"}, {"&loz;","\u25CA"},
    {"&spades;","\u2660"}, {"&clubs;","\u2663"}, {"&hearts;","\u2665"},
    {"&diams;","\u2666"}, {"&quot;","\""}, {"&lt;","\u003C"},
    {"&gt;","\u003E"}, {"&OElig;","\u0152"}, {"&oelig;","\u0153"},
    {"&Scaron;","\u0160"}, {"&scaron;","\u0161"}, {"&Yuml;","\u0178"},
    {"&circ;","\u02C6"}, {"&tilde;","\u02DC"}, {"&ensp;","\u2002"},
    {"&emsp;","\u2003"}, {"&thinsp;","\u2009"}, {"&zwnj;","\u200C"},
    {"&zwj;","\u200D"}, {"&lrm;","\u200E"}, {"&rlm;","\u200F"},
    {"&ndash;","\u2013"}, {"&mdash;","\u2014"}, {"&lsquo;","\u2018"},
    {"&rsquo;","\u2019"}, {"&sbquo;","\u201A"}, {"&ldquo;","\u201C"},
    {"&rdquo;","\u201D"}, {"&bdquo;","\u201E"}, {"&dagger;","\u2020"},
    {"&Dagger;","\u2021"}, {"&permil;","\u2030"}, {"&lsaquo;","\u2039"},
    {"&rsaquo;","\u203A"}, {"&euro;","\u20AC"}};
	
	Pattern patHtmlComments;
	Vector<Pattern> patsGarbageTags = new Vector<Pattern>();
	Vector<Pattern> patsWrapperTags = new Vector<Pattern>();
	Vector<Pattern> patsSingleTags = new Vector<Pattern>();
	Vector<Pattern> patsPlaceHolderTags = new Vector<Pattern>();
	Pattern patTable;
	Pattern patGoodWikiLink;
	Pattern patBadLeftWikiLink;
	Pattern patBadRightWikiLink;
	Pattern patHttpLink;
	Pattern patBadHttpLink;
	Pattern patBold;
	Pattern patItalic;
	Pattern patAposBold;
	Pattern patAposItalic;
	Pattern patNumericHtmlEntities;
	Pattern patMultiSpace;
	Pattern patMultiDot;
	
	
	public WikiTextExtractor()
	{
		patHtmlComments = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
		
		for(String tag : GARBAGE_TAGS)
			patsGarbageTags.add(Pattern.compile("<\\s*"+tag+"(\\s*| [^/]+?)>.*?<\\s*/\\s*"+tag+"\\s*>", Pattern.DOTALL|Pattern.CASE_INSENSITIVE));
		
		for(String tag : WRAPPER_TAGS){
			//left
			patsWrapperTags.add(Pattern.compile("<\\s*"+tag+"(\\s*| [^/]+?)>", Pattern.DOTALL|Pattern.CASE_INSENSITIVE));
			//right
			patsWrapperTags.add(Pattern.compile("<\\s*/\\s*"+tag+"\\s*>", Pattern.DOTALL|Pattern.CASE_INSENSITIVE));
		}
		
		for(String tag : SINGLE_TAGS)
		{
			//good
			patsSingleTags.add(Pattern.compile("<\\s*"+tag+"(\\s*| .+?)/\\s*>", Pattern.DOTALL|Pattern.CASE_INSENSITIVE));
			//bad
			patsSingleTags.add(Pattern.compile("<\\s*(/|\\\\)?\\s*"+tag+"(\\s*| [^/]+?)\\\\?\\s*>", Pattern.DOTALL|Pattern.CASE_INSENSITIVE));
		}
			
		for(String tag : PLACEHOLDER_TAGS)
			patsPlaceHolderTags.add(Pattern.compile("<\\s*"+tag+"(\\s*| [^/]+?)>.*?<\\s*/\\s*"+tag+"\\s*>", Pattern.DOTALL|Pattern.CASE_INSENSITIVE));
		
		patTable = Pattern.compile("\\{[^\\{]*?\\}", Pattern.DOTALL);
		
		patGoodWikiLink = Pattern.compile("\\[\\[[^\\[]*?\\]\\]", Pattern.DOTALL);
		patBadLeftWikiLink = Pattern.compile("\\[[^\\[]*?\\]\\]", Pattern.DOTALL);
		patBadRightWikiLink = Pattern.compile("\\[\\[[^\\[]*?\\]", Pattern.DOTALL);
		
		patHttpLink = Pattern.compile("\\[http.*?\\]", Pattern.DOTALL);
		//some idiot contributors put http links without brakets!!!
		patBadHttpLink = Pattern.compile("(http|https|ftp)\\://([a-zA-Z0-9\\-\\.]+)(:[0-9]*)?/?([a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\+&amp;%\\$#\\=~])*[^\\.\\,\\)\\(\\s]");
		
		patBold = Pattern.compile("(\\w')??('''.+?''')", Pattern.DOTALL);
		patItalic = Pattern.compile("(\\w')??(''.+?'')", Pattern.DOTALL);
		patAposBold = Pattern.compile("\\w'('''.+?''')", Pattern.DOTALL);
		patAposItalic = Pattern.compile("\\w'(''.+?'')", Pattern.DOTALL);
		
		patNumericHtmlEntities = Pattern.compile("&#\\d+?;");
		
		patMultiSpace = Pattern.compile(" {2,}");
		patMultiDot = Pattern.compile("\\.{4,}");
	}
	
	public static interface LinkHandler
	{
		public int handleLink(MutableString input, int start, int end, WikiLink link);
		public boolean removeTrailinBrackets();
	}
	public static final LinkHandler ANCHOR_REPLACER = new LinkHandler() {
		@Override
		public int handleLink(MutableString input, int start, int end, WikiLink link) {
			if (link == WikiLink.EMPTY){
				input.delete(start, end);
				return start;
			} else {
				input.replace(start, end, link.anchor);
				return start+link.anchor.length();
//				return 0;
			}
		}
		@Override
		public boolean removeTrailinBrackets() {
			return true;
		}
	};
	public static final LinkHandler NO_REPLACER = new LinkHandler() {
		@Override
		public int handleLink(MutableString input, int start, int end, WikiLink link) {
			if (link == WikiLink.EMPTY){
				input.delete(start, end);
				return start;
			} else {
				return end;
			}
		}
		@Override
		public boolean removeTrailinBrackets() {
			return false;
		}
	};
	private static class LinkCatcher implements LinkHandler{
		List<WikiLink> links = new ArrayList<WikiLink>();
		@Override
		public boolean removeTrailinBrackets() {
			return ANCHOR_REPLACER.removeTrailinBrackets();
		}
		@Override
		public int handleLink(MutableString input, int start, int end, WikiLink link) {
			if (link != WikiLink.EMPTY) links.add(link);
			return ANCHOR_REPLACER.handleLink(input, start, end, link);
		}
	}

	
	
	public String clean(String input, LinkHandler handler)
	{
		return clean(new MutableString(input), handler).toString();
	}
	
	/**
	 * Remove most of media-wiki syntax, NOT ALL! Use also {@link WikiTextExtractor#removeStructure(MutableString, boolean)} to
	 * get a clean text.
	 * @param input
	 * @param handler
	 * @return
	 */
	public MutableString clean (MutableString input, LinkHandler handler)
	{
		MutableString output = new MutableString(input.length());
		output.append(input);
		output.loose();
		//make tags readable
		output.replace("&lt;", "<").replace("&gt;",">");
		output.replace("<<", "«").replace(">>","»");
		
		//delete html comments
		output = Chars.delete(patHtmlComments, output);
		
		//delete html garbage tags
		for(Pattern p : patsGarbageTags)
			output = Chars.delete(p,output);
		
		//delete html wrapper tags
		for(Pattern p : patsWrapperTags)
			output = Chars.delete(p,output);
		
		//delete single tags
		for(Pattern p : patsSingleTags)
			output = Chars.delete(p,output);
		
		//delete placeholder tags
		for(Pattern p : patsPlaceHolderTags)
			output = Chars.delete(p,output);
		
		//delete templates and tables. max 3 nested table/templates
		output.replace("{{start box}}", "{");
		output.replace("{{end box}}", "}");
		output.replace("{{", "{").replace("}}", "}");
		output.replace("{|", "{").replace("|}", "}");
		output = Chars.delete(patTable, output);
		output = Chars.delete(patTable, output);
		output = Chars.delete(patTable, output);
		
		//delete http link
		output = Chars.delete(patHttpLink, output);
		output = Chars.delete(patBadHttpLink, output);
		output.replace("[]", "");
		
		Matcher m;
		int start;

		//handle bold and italic
		m = patBold.matcher(output);
		start = 0;
		while(m.find(start)){
			String bolded = m.group(2);
			bolded = bolded.substring(3, bolded.length()-3);
			output.replace(m.start(2), m.end(2), bolded);
			start = m.start(2)+bolded.length();
		}
		m = patItalic.matcher(output);
		start = 0;
		while(m.find(start)){
			String ital = m.group(2);
			ital = ital.substring(2, ital.length()-2);
			output.replace(m.start(2), m.end(2), ital);
			start = m.start(2)+ital.length();
		}
		output.replace("''''", "").replace("'''","").replace("''", "");

		//handle special chars
		output.replace("&amp;", "&").replace("&quot;&quot;","&quot;");
		for(String[] ent : HTML_ENTITIES)
			output.replace(ent[0], ent[1]);
		//handle special numeric chars
		m = patNumericHtmlEntities.matcher(output);
		start = 0;
		while(m.find(start))
		{
			String strcode = m.group();
			int code = Integer.parseInt(strcode.substring(2, strcode.length()-1));
			if (code >= 0x10000){
				output.delete(m.start(), m.end());
				start = m.start();
			} else {
				output.replace(m.start(), m.end(), ""+(char)code);
				start = m.start()+1;
			}
		}
		
		//handle some errors
		output.replace('\t',' ');
		output = Chars.replace(patMultiSpace, output, " ");
		output = Chars.replace(patMultiDot, output, "...");
		output.replace(" ,",",").replace(" .",".");
		output.replace(" ;",";").replace(" :",":");
		output.replace(",,",",").replace(",.",".");
		output.replace("( ","(").replace(" )",")");
		output.replace("[ ","[").replace(" ]","]");
		output.replace("« ","«").replace(" »","»");
		
		//handle good wikilink
		start=0;
		m = patGoodWikiLink.matcher(output);
		while(m.find(start)){
			MutableString link = output.substring(m.start()+2, m.end()-2);
			WikiLink l = WikiLink.parse(link);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		//often there are nested links (it is not allowed by Wiki syntax but we found them)
		start=0;
		m = patGoodWikiLink.matcher(output);
		while(m.find(start)){
			MutableString link = output.substring(m.start()+2, m.end()-2);
			WikiLink l = WikiLink.parse(link);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		
		//handle bad left wikilink
		m = patBadLeftWikiLink.matcher(output);
		start = 0;
		while(m.find(start)){
			MutableString link = output.substring(m.start()+1, m.end()-2);
			WikiLink l = WikiLink.parse(link);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		//handle bad right wikilink
		m = patBadRightWikiLink.matcher(output);
		start = 0;
		while(m.find(start)){
			MutableString link = output.substring(m.start()+2, m.end()-1);
			WikiLink l = WikiLink.parse(link);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		//delete trailing brackets
		if (handler.removeTrailinBrackets())
			output.replace("[[", "").replace("]]","");

		
		return output;
	}
	
	public MutableString removeStructure(MutableString input, boolean onlyAbstract) 
	{
		
		MutableString buffer = new MutableString(1024);
		FastBufferedReader tokenizer = new FastBufferedReader(input);
		
		MutableString text = new MutableString(2048);
		String punts = ":.;,-";
		
		try {
			while(tokenizer.readLine(buffer) != null)
			{
				if (text.length() > MIN_ABSTRACT_CHARS && onlyAbstract){
					text.deleteCharAt(text.length()-1);
					return text;					
				}
				
//				MutableString linestr = new MutableString(buffer.trim());
				MutableString linestr = buffer.trim();
				if (linestr.length() == 0) continue;
				
				int start;
				int end;
				String chars;
				char[] line = linestr.array();
				int line_len = linestr.length();
				
				char first = linestr.charAt(0);
				switch (first)
				{
				case '=':{
					chars = " =";
					for(start=0; start <line_len && chars.indexOf(line[start])>=0; start++);
					for(end=line_len-1; end >= 0  && chars.indexOf(line[end])>=0; end--);
					
					if (start < end){
						text.append(linestr.subSequence(start, end+1));
						text.append(". ");
					}
					break;
				}
					
				case '*':
				case '#':
				case ':':
				case ';':{
					
					chars = "*#:; ";
					for(start=0; start<line_len && chars.indexOf(line[start])>=0 ; start++);
					
					if (start < line_len-1){
						text.append(linestr.subSequence(start, linestr.length()));
						if (punts.indexOf(text.lastChar())<0)
							text.append('.');
						text.append(' ');
					}
					
					break;
				}
				case '{':
				case '|':
					break;
				case '.':
				case '-':{
					linestr.delete(new char[]{'.','-'});
					if (linestr.length() > 0){
						text.append(linestr);
						if (punts.indexOf(text.lastChar())<0)
							text.append('.');
						text.append(' ');
					}
					break;
				}
				default:{
					if (linestr.lastChar() == '}')
						break;
					text.append(linestr);
					if (punts.indexOf(text.lastChar())<0)
						text.append('.');
					text.append(' ');
				}
				}
			}
		} catch (IOException e) {}
		if (text.length()>0) text.deleteCharAt(text.length()-1);
		return text;
	}
	
	public List<WikiLink> extractDisambiguationLinks(MutableString cleanText)
	{
		FastBufferedReader tokenizer = new FastBufferedReader(cleanText);
		MutableString buffer = new MutableString(1024);
		List<WikiLink> links = new ArrayList<WikiLink>();
		
		try {
			while(tokenizer.readLine(buffer) != null)
			{
				buffer.trim();
				if (buffer.length() == 0) continue;
				
				if (buffer.charAt(0) == '*')
				{
					int start = 1;
					for(; start<buffer.length() && buffer.charAt(start)=='*' ; start++);
					buffer.delete(start, buffer.length()).trim();
					
					if (buffer.length() == 0) continue;
//					if (!buffer.startsWith("[[")) continue;
					
					List<WikiLink> lineLinks = extractLinkFromCleanedLine(buffer);
					if (lineLinks.size()>0) links.add(lineLinks.get(0));
				}
			}
		} catch (IOException ioe){}
		
		return links;
		
	}
	
	

	
	
	public MutableString extractAbstract(MutableString input)
	{
		MutableString cleaned = clean(input, ANCHOR_REPLACER);
		return removeStructure(cleaned, true);
	}
	public MutableString extractPage(MutableString input)
	{
		MutableString cleaned = clean(input, ANCHOR_REPLACER);
		return removeStructure(cleaned, false);
	}
	public MutableString extractPageAndLink(MutableString input, List<WikiLink> links)
	{
		LinkCatcher catchingLink = new LinkCatcher();
		MutableString cleaned = clean(input, catchingLink);
		links.addAll(catchingLink.links);
		return removeStructure(cleaned, false);
	}
	public List<WikiLink> extractLinkFromPage(String input)
	{
		return extractLinkFromPage(new MutableString(input));
	}
	public List<WikiLink> extractLinkFromPage(MutableString input)
	{
		LinkCatcher catchingLink = new LinkCatcher();
		clean(input, catchingLink);
		return catchingLink.links;
	}
	
	public List<WikiLink> extractLinkFromCleanedLine(String input)
	{
		return extractLinkFromCleanedLine(new MutableString(input));
	}
	public List<WikiLink> extractLinkFromCleanedLine(MutableString input)
	{
		MutableString output = new MutableString(input);
		output.loose();
		LinkHandler handler = ANCHOR_REPLACER;
		ArrayList<WikiLink> ll = new ArrayList<WikiLink>();
		
		Matcher m;
		int start;
		m = patGoodWikiLink.matcher(output);
		start = 0;
		while(m.find(start))
		{
			WikiLink l = WikiLink.parse(output.substring(m.start()+2, m.end()-2));
			if (l != WikiLink.EMPTY) ll.add(l);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		m = patGoodWikiLink.matcher(output);
		start = 0;
		while(m.find(start))
		{
			WikiLink l = WikiLink.parse(output.substring(m.start()+2, m.end()-2));
			if (l != WikiLink.EMPTY) ll.add(l);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		
		//handle bad left wikilink
		m = patBadLeftWikiLink.matcher(output);
		start = 0;
		while(m.find(start)){
			MutableString link = output.substring(m.start()+1, m.end()-2);
			WikiLink l = WikiLink.parse(link);
			if (l != WikiLink.EMPTY) ll.add(l);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		//handle bad right wikilink
		m = patBadRightWikiLink.matcher(output);
		while(m.find()){
			MutableString link = output.substring(m.start()+2, m.end()-1);
			WikiLink l = WikiLink.parse(link);
			if (l != WikiLink.EMPTY) ll.add(l);
			start = handler.handleLink(output, m.start(), m.end(), l);
		}
		return ll;
	}
	
	static URLCodec cc = new URLCodec();
	static Pattern isEncoded = Pattern.compile("%\\d\\d");
	static Pattern multiSpace = Pattern.compile("\\s{2,}");
	public static MutableString normalizePageName(MutableString page)
	{
		if (page.length() == 0) return page;
		page = page.replace('_', ' ');
		if (page.length() == 0) return page;
		page = Chars.replace(multiSpace, page, " ");
		if (page.length() == 0) return page;
		page.setCharAt(0, Character.toUpperCase(page.firstChar()));
		
		if (page.length() == 0) return page;
		if (isEncoded.matcher(page).find()) {
			try {
				page = new MutableString(cc.decode(page.toString()));
			} catch (DecoderException de) {}
		}
		return page;
	}
}
