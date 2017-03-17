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
package it.acubelab.tagme.preprocessing.anchors;

import it.acubelab.Chars;
import it.acubelab.ExternalSort;
import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.Anchor;
import it.acubelab.tagme.config.Config.WikipediaFiles;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.TextDataset;
import it.acubelab.tagme.preprocessing.WikiArticle;
import it.acubelab.tagme.preprocessing.WikiLink;
import it.acubelab.tagme.preprocessing.WikiPatterns;
import it.acubelab.tagme.preprocessing.WikiPatterns.Type;
import it.acubelab.tagme.preprocessing.WikiTextExtractor;
import it.acubelab.tagme.preprocessing.WikipediaArticleParser;
import it.acubelab.tagme.preprocessing.WordTrie;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.acubelab.tagme.preprocessing.support.DisambiguationWIDs;
import it.acubelab.tagme.preprocessing.support.IgnoreWIDs;
import it.acubelab.tagme.preprocessing.support.ListPageWIDs;
import it.acubelab.tagme.preprocessing.support.PeopleWIDs;
import it.acubelab.tagme.preprocessing.support.RedirectMap;
import it.acubelab.tagme.preprocessing.support.TitlesToWIDMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

//import sun.text.normalizer.NormalizerBase;
//import sun.text.normalizer.NormalizerImpl;

public class WikipediaAnchorParser extends TextDataset
{

	static Logger log = Logger.getLogger(WikipediaAnchorParser.class);
	public static final int
	BOOST_TITLE = Anchor.MIN_LINKS,
	MIN_ANCHOR_LEN = 2;

	public static final Pattern
	P_FINAL_BRACKETS = Pattern.compile(" \\([^\\)]+\\)$"),
	P_NUMBER_OR_SPACES = Pattern.compile("^[\\d\\s]*$"),
	P_NON_ASCII = Pattern.compile("[^\\p{ASCII}]+");

	private static final String SPECIAL_PUNCTS = "-/\\";
	private static final char[] SPECIAL_PUNCTS_CHARS = SPECIAL_PUNCTS.toCharArray();
	private static final char[] SPECIAL_PUNCTS_CHAR_MAP = new char[SPECIAL_PUNCTS_CHARS.length];
	static{
		for(int i=0;i<SPECIAL_PUNCTS_CHAR_MAP.length; i++)
			SPECIAL_PUNCTS_CHAR_MAP[i] = ' ';
	}


	public static boolean REMOVE_STOPWORDS_FROM_ANCHOR = true;
	public static boolean REMOVE_NUMBERS_ANCHOR = true;

	public WikipediaAnchorParser(String lang) {
		super(lang);
	}


	@Override
	protected void parseFile(File file) throws IOException
	{

		log.info("Loading support datasets...");

		final Int2IntMap redirects = new RedirectMap(lang).getDataset();
		final IntSet disambiguations = new DisambiguationWIDs(lang).getDataset();
		final IntSet listpages = new ListPageWIDs(lang).getDataset();
		final IntSet peoples = new PeopleWIDs(lang).getDataset();
		final IntSet ignores = new IgnoreWIDs(lang).getDataset();
		final IntSet WIDs = new AllWIDs(lang).getDataset();
		final Object2IntMap<String> titles = new TitlesToWIDMap(lang).getDataset();

		log.info("All datasets loaded");

		final Pattern anchorStart = WikiPatterns.getPattern(lang, Type.ANCHOR_START);
		final WordTrie stopwords = WikiPatterns.getWordSet(lang, Type.ANCHOR_STOPWORDS);


		File tmp = Dataset.createTmpFile();
		final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(tmp), Charset.forName("UTF-8"));


		WikipediaArticleParser parser = new WikipediaArticleParser(){
			WikiTextExtractor wikier = new WikiTextExtractor();
			PLogger plog = new PLogger(log, Step.TEN_MINUTES, "pages", "parsed", "links", "anchors");
			int pages=0, parsed=1, links=2, anchors=3;
			@Override
			protected void start() throws IOException {
				super.start();
				plog.setEnd(parsed, WIDs.size());
				plog.start("Now parsing anchors...");
			}
			@Override
			public void processArticle(WikiArticle a) throws IOException
			{
				plog.update(pages);
				if (WIDs.contains(a.id()))
				{
					plog.update(parsed);

					//MANAGE THE TITLE OF THE PAGE
					String titleNoBrackets = a.title().replaceAll(P_FINAL_BRACKETS.pattern(), "").replaceAll("\\&quot;", "\"");
					
					if (!ignores.contains(a.id()))
					{
						
						//Insert an anchor equal to the title of the page, or equal to the redirect title
						//if (redirects.containsKey(a.id)) target = redirects.get(target);
						
						int target = a.id();
						if (redirects.containsKey(target)) target = redirects.get(target);
						if (!disambiguations.contains(target) &&
								!listpages.contains(target) &&
								!peoples.contains(target)
								)
						{
							//int target = a.id();
							if (redirects.containsKey(target)) target = redirects.get(target);
							CharSequence[] anchors = parseAnchor(a.title(), anchorStart);
							for(int i=0;i<anchors.length;i++)
								append(anchors[i], titleNoBrackets, target, BOOST_TITLE);

						} else if (!redirects.containsKey(a.id())) {

							//Insert name and only surname if he is a person
							if (peoples.contains(a.id()))
							{

								CharSequence[] names = parseNames(a.title());
								for(int i=0;i<names.length;i++)
									append(names[i], titleNoBrackets, a.id(), BOOST_TITLE);

								//Insert all disambiguations links
								//Necessario perchè in inglese c'e' la categoria All_set_index_Articles che include disambiguazioni e liste
							} else if (disambiguations.contains(a.id()) && !listpages.contains(a.id())) {

								CharSequence[] anchors = parseAnchor(a.title(), anchorStart);

								if (anchors.length > 0)
								{
									MutableString cleanedLines = wikier.clean(a.body(), WikiTextExtractor.NO_REPLACER);
									List<WikiLink> links = wikier.extractDisambiguationLinks(cleanedLines);
									for(WikiLink l : links)
									{
										int idTarget = titles.getInt(l.page);
										if (redirects.containsKey(idTarget)) idTarget = redirects.get(idTarget);

										if (idTarget < 0 ||
												disambiguations.contains(idTarget) ||
												listpages.contains(idTarget) ||
												ignores.contains(idTarget)) continue;

										if (redirects.containsKey(idTarget)) idTarget = redirects.get(idTarget);

										append(anchors[0], titleNoBrackets, idTarget, BOOST_TITLE);
									}
								}
							}
						}
					}

					//ALL LINKS IN THE PAGE

					List<WikiLink> ll = wikier.extractLinkFromPage(a.body());
					for(WikiLink l : ll)
					{

						int idTarget = titles.getInt(l.page);

						if (redirects.containsKey(idTarget)) idTarget = redirects.get(idTarget);

						if (idTarget < 0 ||
								disambiguations.contains(idTarget) ||
								listpages.contains(idTarget) ||
								ignores.contains(idTarget)) continue;

						if (redirects.containsKey(idTarget)) idTarget = redirects.get(idTarget);

						plog.update(links);
						CharSequence[] anchors = parseAnchor(l.anchor, anchorStart);
						for(int i=0;i<anchors.length; i++)
							append(anchors[i], l.anchor, idTarget);
					}


				}
			}
			@Override
			protected void stop() throws IOException {
				super.stop();
				plog.stop();
			}

			public void append(CharSequence anchor, CharSequence original, int target, int times) throws IOException
			{
				String original_lowercase = original.toString().toLowerCase();
				for(int i=0;i<times; i++){

					CharSequence removed = WikipediaAnchorParser.removeStopwords(anchor, stopwords);
					if (removed == null || removed.length() > MIN_ANCHOR_LEN)
					{
						if (REMOVE_NUMBERS_ANCHOR && P_NUMBER_OR_SPACES.matcher(anchor).matches())
							continue;
						out.append(anchor);
						out.append(TextDataset.SEP_CHAR);
						out.append(original_lowercase);
						out.append(TextDataset.SEP_CHAR);
						out.append(""+target);
						out.append('\n');
						plog.update(anchors);

						if (removed != null && REMOVE_STOPWORDS_FROM_ANCHOR){
							if (REMOVE_NUMBERS_ANCHOR && P_NUMBER_OR_SPACES.matcher(removed).matches())
								continue;
							out.append(removed);
							out.append(TextDataset.SEP_CHAR);
							out.append(original_lowercase);
							out.append(TextDataset.SEP_CHAR);
							out.append(""+target);
							out.append('\n');
							plog.update(anchors);
						}
					}
				}

			}
			public void append(CharSequence anchor, CharSequence original, int target) throws IOException
			{
				append(anchor, original, target, 1);
			}
		};


		File input = WikipediaFiles.ARTICLES.getSourceFile(lang);
		parser.parse(input);
		out.flush();
		out.close();

		log.info("Now sorting...");
		ExternalSort sorter = new ExternalSort();
		sorter.setColumns(new int[]{0,1,2});
		//		sorter.setNumeric(true);
		sorter.setInFile(tmp.getAbsolutePath());
		sorter.setOutFile(file.getAbsolutePath());
		sorter.run();
		log.info("Sorted. Done.");

	}

	/**
	 * It normalizes the anchor text:
	 * 1. ascii normalization
	 * 2. delete brackets at the end (i.e. in titles)
	 * 3. delete a pattern at the beginning of the text, see anchorStart
	 * 4. delete all dots '.'
	 * 5. replace all punctuations with whitespaces, except for {@link WikipediaAnchorParser#SPECIAL_PUNCTS}
	 * 6. if the original contained any of {@link WikipediaAnchorParser#SPECIAL_PUNCTS},
	 * 	  it returns 2 anchors (with those puncts replaced by whitespace, and with those puncts deleted)
	 *    otherwise, the normalization at 5.
	 * If the normalization process doesn't produce a valid anchor, an empty array is returned
	 * @param original
	 * @param anchorStart A pattern that identifies some common articles or preposition to be deleted if they occur at the beginning of the anchor
	 * @return
	 */
	public static CharSequence[] parseAnchor(CharSequence original, Pattern anchorStart)
	{

		MutableString anchor = Chars.toNormalizedASCII(original);
		anchor.squeezeSpace();
		anchor.trim();

		if (anchor.length() < MIN_ANCHOR_LEN || !contaisText(anchor)) return Chars.EMPTY_STRINGS;

		anchor.loose();
		anchor.toLowerCase();

		Matcher m = P_FINAL_BRACKETS.matcher(anchor);
		if (m.find()) anchor.delete(m.start(), m.end());
		anchor.trim();
		if (anchor.length() < MIN_ANCHOR_LEN) return Chars.EMPTY_STRINGS;

		if (anchorStart != null)
		{
			Matcher m2 = anchorStart.matcher(anchor);
			if (m2.find()) anchor.delete(m2.start(), m2.end());
			anchor.trim();
			if (anchor.length() < MIN_ANCHOR_LEN) return Chars.EMPTY_STRINGS;
		}

		anchor = removeDots(anchor);
		if (anchor.length() < MIN_ANCHOR_LEN) return Chars.EMPTY_STRINGS;

		anchor = removePunctuations(anchor, SPECIAL_PUNCTS, false);
		if (anchor.length() < MIN_ANCHOR_LEN) return Chars.EMPTY_STRINGS;

		if (anchor.indexOfAnyOf(SPECIAL_PUNCTS_CHARS, 0)<0){
			if (!contaisText(anchor)) return Chars.EMPTY_STRINGS;
			else return new CharSequence[]{anchor};
		} else {
			MutableString anchorNoPuncts = new MutableString(anchor);
			anchor.replace(SPECIAL_PUNCTS_CHARS, SPECIAL_PUNCTS_CHAR_MAP);
			//			Chars.trimMultispace(anchor);
			anchor.squeezeSpace().trim();
			if (anchor.length() < MIN_ANCHOR_LEN || !contaisText(anchor)) return Chars.EMPTY_STRINGS;

			anchorNoPuncts.delete(SPECIAL_PUNCTS_CHARS);
			anchorNoPuncts.squeezeSpace().trim();
			if (anchorNoPuncts.length() < MIN_ANCHOR_LEN || !contaisText(anchorNoPuncts)) return new CharSequence[]{anchor};
			else return new CharSequence[]{anchor, anchorNoPuncts};
		}
	}


	public static CharSequence[] parseNames(String original)
	{
		CharSequence[] anchors = parseAnchor(original, null);

		if (anchors.length == 0) return anchors;

		int lastToken = -1;
		for(int i=0; i<anchors[0].length(); i++)
			if (anchors[0].charAt(i) == ' ') lastToken = i;
		if (lastToken < 0) return anchors;

		CharSequence surname = anchors[0].subSequence(lastToken+1, anchors[0].length());
		if (surname.length() < MIN_ANCHOR_LEN || !contaisText(surname)) return anchors;
		else {
			if (anchors.length == 1) return new CharSequence[]{anchors[0], surname};
			else return new CharSequence[]{anchors[0], anchors[1], surname};
		}
	}

	/**
	 * Remove the stopwords from the original charsequence.
	 * @param original
	 * @param stopwords
	 * @return A new charsequence if any stopword is found (possibly empty), otherwise null
	 */
	public static MutableString removeStopwords(CharSequence original, WordTrie stopwords)
	{
		MutableString anchor = null;

		int last = 0;
		for(int i=0; i<original.length(); i++)
		{
			char c = original.charAt(i);
			if (c==' ' || i==original.length()-1)
			{
				if (i==original.length()-1) i++;
				CharSequence word = original.subSequence(last==0?0:last+1, i);
				if (stopwords.contains(word))
				{
					if (anchor == null){
						anchor = new MutableString(original.length());
						if (last > 0) anchor.append(original.subSequence(0, last));
						//						else i++;
					}					
				} else if (anchor != null){
					anchor.append(original.subSequence(anchor.length()>0?last:last+1, i));
				}
				last = i;
			}
		}

		return anchor;
	}

	/**
	 * Remove all punctuations for an anchor, i.e. remove all but letters, digits and whitespaces
	 * but maintains sequences of same puncts
	 * @param input
	 */
	public static MutableString removePunctuations(MutableString input)
	{
		return removePunctuations(input, null, true);
	}

	/**
	 * Remove all punctuations for an anchor, i.e. remove all but letters, digits and whitespaces
	 *
	 * @param input
	 * @param ignoreChars A set of character (no digits, no letters) that are ignored when removing
	 * @param ignoreSequences if true, it does not remove sequences of the same characters i.e. '!!!'
	 * @return A new MutableString
	 */
	public static MutableString removePunctuations(MutableString input, String ignoreChars, boolean ignoreSequences)
	{
		int len = input.length();
		char[] array = input.array();
		MutableString norm = new MutableString(len);

		int i=0, last=0;
		while(i<len)
		{
			while(i<len && (
					Character.isLetter(array[i]) ||
					Character.isDigit(array[i]) ||
					(ignoreChars!=null && ignoreChars.indexOf(array[i])>=0) ||
					(ignoreSequences && !Character.isWhitespace(array[i]) && (i>0 && array[i-1]==array[i] || i<len-1 && array[i+1]==array[i]) )
					))
				i++;

			if (i>last) {
				if (norm.length() > 0) norm.append(' ');
				norm.append(array, last, i-last);
			}

			while(i<len && !(
					Character.isLetter(array[i]) ||
					Character.isDigit(array[i]) ||
					(ignoreChars!=null && ignoreChars.indexOf(array[i])>=0) ||
					(ignoreSequences && !Character.isWhitespace(array[i]) && (i>0 && array[i-1]==array[i] || i<len-1 && array[i+1]==array[i]) )
					)){
				i++;
				last=i;
			}
		}
		return norm;
	}

	/**
	 * Manage dots, removing them if they are part of an abbreviation, or replacing them with
	 * withespaces if they are the last char of a word
	 * @param input
	 * @return
	 */
	public static MutableString removeDots(MutableString input)
	{
		int len = input.length();
		char[] array = input.array();
		MutableString res = new MutableString(len);

		boolean isLastDot = false;
		int i=0, last=0;
		while(i<len)
		{
			while(i<len && array[i]!='.' && !Character.isWhitespace(array[i]))
				i++;

			if (i>last) {
				if (isLastDot && res.length() > 0) res.append(' ');
				res.append(array, last, i-last);
			}

			isLastDot = false;
			while(i<len && (array[i]=='.' || Character.isWhitespace(array[i])))
			{
				if (Character.isWhitespace(array[i]) ||
						(i<len-2 && array[i+2]!='.' && !Character.isWhitespace(array[i+2])) ||
						(i==len-2 && i>1 && array[i-2]!='.' && !Character.isWhitespace(array[i-2]))
						)
					isLastDot = true;
				i++;
				last=i;
			}
		}
		return res;


	}

	public static boolean contaisText(CharSequence input)
	{
		for(int i=0;i<input.length(); i++)
			if (Character.isLetter(input.charAt(i))) return true; //there's a letter so it is not all-dirty or number
		return false;
	}


}
