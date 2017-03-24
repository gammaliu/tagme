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
import it.acubelab.ExternalSort;
import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.TagmeParser;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.preprocessing.WikiPatterns.Type;
import it.acubelab.tagme.preprocessing.anchors.WikipediaAnchorParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.io.FileLinesCollection;
import it.unimi.dsi.io.FileLinesCollection.FileLinesIterator;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import snowball.SnowballStemmerFactory;
import snowball.SnowballStemmerFactory.LANG;
import snowball.Stemmer;

public class BestAnchors extends Dataset<Int2ObjectMap<String>> {

	Pattern anchorStart ;
	IndexSearcher articles;
	WordTrie stopwords;
	Stemmer stemmer;

	public static final int MIN_ANCHORS = 2;

	public BestAnchors(String lang) {
		super(lang);
	}

	void init() throws IOException{
		anchorStart = WikiPatterns.getPattern(lang, Type.ANCHOR_START);
		articles = Indexes.getSearcher(RepositoryDirs.WIKIPEDIA.getPath(lang));
		if (lang.equals("de"))
			stemmer = SnowballStemmerFactory.create(LANG.GERMAN);
		else if (lang.equals("en"))
			stemmer = SnowballStemmerFactory.create(LANG.ENGLISH);
		else if (lang.equals("es"))
			stemmer = SnowballStemmerFactory.create(LANG.SPANISH);
		else if (lang.equals("fr"))
			stemmer = SnowballStemmerFactory.create(LANG.FRENCH);
		else if (lang.equals("it"))
			stemmer = SnowballStemmerFactory.create(LANG.ITALIAN);
		stopwords = WikiPatterns.getAnchorStopwords(lang);
	}

	@Override
	protected Int2ObjectMap<String> parseSet() throws IOException
	{
		log.info("Sorting anchors by Wikipedia pages...");

		File originalAnchors = new WikipediaAnchorParser(lang).getFile();
		File sortedAnchors = Dataset.createTmpFile();

		ExternalSort sorter = new ExternalSort();
		sorter.setInFile(originalAnchors.getAbsolutePath());
		sorter.setOutFile(sortedAnchors.getAbsolutePath());
		sorter.setColumns(new int[]{2,1});
		sorter.run();

		log.info("Sorted.");

		init();

		Int2ObjectOpenHashMap<String> bestAnchorMap = new Int2ObjectOpenHashMap<String>(3000000);
		PLogger plog = new PLogger(log, Step.TEN_MINUTES, "lines","pages","no-best")
		.start("Scrolling all anchors...");

		final Object2IntMap<String> anchors = new Object2IntOpenHashMap<String>(32);
		anchors.defaultReturnValue(0);

		FileLinesIterator iter = new FileLinesCollection(sortedAnchors.getAbsolutePath(), "UTF-8").iterator();
		CharSequence[] fields = new CharSequence[3];
		//MutableString line=iter.next();
		//Chars.fields(iter.next(), TextDataset.SEP_CHAR, fields);
		Chars.fields(iter.next(), TextDataset.SEP_CHAR, fields);

		int last_id=Chars.parseInt(fields[2]);
		if(last_id>0)
			anchors.put(fields[1].toString(),1);
		
		while(iter.hasNext())
		{
			Chars.fields(iter.next(), TextDataset.SEP_CHAR, fields);

			plog.update(0);	
			int curr_id = Chars.parseInt(fields[2]);

			if (last_id >= 0 && last_id != curr_id)
			{
				plog.update(1);

				String best = findBest(last_id, anchors);
				if (best == null) plog.update(2);
				else  bestAnchorMap.put(last_id, best);

				anchors.clear();
			}
			last_id = curr_id;
			String anchorString = fields[1].toString();
			anchors.put(anchorString, anchors.getInt(anchorString)+1);

		}
		String best = findBest(last_id, anchors);
		if (best == null) plog.update(2);
		else  bestAnchorMap.put(last_id, best);

		plog.stop();
		iter.close();

		bestAnchorMap.trim();

		log.info("Done.");

		return bestAnchorMap;
	}

	String findBest(int wid, final Object2IntMap<String> anchors) throws IOException
	{
		Query q = new TermQuery(new Term(WikipediaIndexer.FIELD_WID, ""+wid));
		TopDocs td = articles.search(q, 1);
		if (td.totalHits == 0) return null;//throw new IOException("Unable to find title for WID:"+wid);
		String title = articles.doc(td.scoreDocs[0].doc).get(WikipediaIndexer.FIELD_TITLE);
		title = title.replaceAll("\\&quot;", "\"");

		Set<String> titleTerms = terms(title).keySet();

		List<String> bests = new ArrayList<String>(anchors.size());
		bests.addAll(anchors.keySet());
		Collections.sort(bests, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return anchors.getInt(o2)-anchors.getInt(o1);
			}
		});


		for (String a : bests)
		{
			if (anchors.getInt(a)< MIN_ANCHORS) continue;
			Set<String> anchorTerms = terms(a).keySet();
			for(String aw : anchorTerms)
				if (!titleTerms.contains(aw))
					return a;
		}
		return null;
	}

	//	CharSequence[] EMPTY = new CharSequence[0];
	//	CharSequence[] splitTitle (String title)
	//	{
	//		MutableString buffer = Chars.toNormalizedASCII(title);
	//		buffer.squeezeSpace();
	//		buffer.trim();
	//		if (buffer.length() == 0 || !WikipediaAnchorParser.contaisText(buffer)) return EMPTY;
	//		buffer.loose();
	//		buffer.toLowerCase();
	//		
	//		buffer.delete('.').trim();
	//		if (buffer.length() == 0 || !WikipediaAnchorParser.contaisText(buffer)) return EMPTY;
	//		
	//		WikipediaAnchorParser.removePunctuations(buffer);
	//		if (buffer.length() == 0 || !WikipediaAnchorParser.contaisText(buffer)) return EMPTY;
	//		
	//		return Chars.split(buffer, ' ');
	//	}
	//	
	//	CharSequence[] splitAnchor(String anchor)
	//	{
	//		CharSequence[] anchors = WikipediaAnchorParser.parseAnchor(anchor, anchorStart);
	//		if (anchors.length == 0) return EMPTY;
	//		else return Chars.split(new MutableString(anchors[0]), ' ');
	//	}

	HashMap<String,String> terms(String phrase)
	{
		char[] chars = phrase.toCharArray();
		char[] text = new char[chars.length];
		int[] offsets = new int[chars.length];

		int len = TagmeParser.clean(chars, text, offsets);

		HashMap<String, String> terms = new HashMap<String, String>(8);
		int last=0;
		for(int i=0; i<len; i++){
			if (i == len-1 || text[i+1] == ' '){
				String word = new String(text, last, i-last+1);
				last = i+1;
				if (stopwords.contains(word)) continue;
				else terms.put(stemmer.stem(word), word);
			}
		}

		return terms;
	}


}
