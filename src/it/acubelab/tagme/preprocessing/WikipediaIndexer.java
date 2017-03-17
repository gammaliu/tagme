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

import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.config.Config.WikipediaFiles;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.acubelab.tagme.preprocessing.support.DisambiguationWIDs;
import it.acubelab.tagme.preprocessing.support.IgnoreWIDs;
import it.acubelab.tagme.preprocessing.support.ListPageWIDs;
import it.acubelab.tagme.preprocessing.support.PeopleWIDs;
import it.acubelab.tagme.preprocessing.support.RedirectMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class WikipediaIndexer extends Indexer {

	public WikipediaIndexer() {
	}
	
	public static final String
		FIELD_WID = "wid",
		FIELD_TITLE = "title",
		FIELD_BODY = "body",
		FIELD_ABSTRACT = "abstract",
		FIELD_TYPE = "type",
		FIELD_REDIRECT_TARGET = "redirect",
		FIELD_CAT = "cat";
	
	
	public static enum PageType {
		TOPIC,
		IGNORE,
		LIST,
		DISAMBIGUATION,
		REDIRECT
	}

	@Override
	public void makeIndex(String lang, File workingDir) throws IOException 
	{
		final Int2IntMap redirects = new RedirectMap(lang).getDataset();
		final IntSet disambiguations = new DisambiguationWIDs(lang).getDataset();
		final IntSet listpages = new ListPageWIDs(lang).getDataset();
		final IntSet people = new PeopleWIDs(lang).getDataset();
		final IntSet ignores = new IgnoreWIDs(lang).getDataset();
		final IntSet WIDs = new AllWIDs(lang).getDataset();
		
		final HashMap<String, List<String>> categories = parseDBPediaCategories(lang);
		
		log.info("All datasets loaded");
		
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer());
		analyzer.addAnalyzer(FIELD_BODY, new StandardAnalyzer(Version.LUCENE_34, new HashSet<String>()));
		final IndexWriter writer = new IndexWriter(FSDirectory.open(workingDir), new IndexWriterConfig(Version.LUCENE_34, analyzer));
		
		final PLogger plog = new PLogger(log, Step.TEN_MINUTES, "Pages", "Articles", "Redirects", "Disambiguations", "ListPages", "Ignored", "People", "NoCats");
		plog.setEnd(1, WIDs.size());
		WikipediaArticleParser parser = new WikipediaArticleParser()
		{
			Field fWID = new Field(FIELD_WID, "", Store.YES, Index.NOT_ANALYZED);
			Field fTitle = new Field(FIELD_TITLE, "", Store.YES, Index.NOT_ANALYZED);
			Field fAbstract = new Field(FIELD_ABSTRACT, "", Store.YES, Index.NO);
			Field fType = new Field(FIELD_TYPE, "", Store.YES, Index.NOT_ANALYZED);
			Field fRedirect = new Field(FIELD_REDIRECT_TARGET, "", Store.YES, Index.NOT_ANALYZED);
			Field fBody = new Field(FIELD_BODY, "", Store.NO, Index.ANALYZED);
			
			Document theDoc = new Document(); 
			
			WikiTextExtractor wikier = new WikiTextExtractor();
			
			@Override
			protected void start() throws IOException {
				super.start();
				theDoc.add(fWID);
				theDoc.add(fTitle);
				theDoc.add(fAbstract);
				theDoc.add(fType);
				theDoc.add(fRedirect);
				theDoc.add(fBody);
				plog.start();
			}
			@Override
			public void processArticle(WikiArticle a) throws IOException 
			{
				plog.update(0);
				if (WIDs.contains(a.id()))
				{
					plog.update(1);
					
					fWID.setValue(Integer.toString(a.id()));
					fTitle.setValue(a.title());
					
					boolean isPeople = false;
					PageType type = PageType.TOPIC;
					if (listpages.contains(a.id())) {
						type=PageType.LIST;
						plog.update(4);
					} else if (disambiguations.contains(a.id())) {
						type=PageType.DISAMBIGUATION;
						plog.update(3);
					} else if (redirects.containsKey(a.id())) {
						type=PageType.REDIRECT;
						plog.update(2);
					} else if (ignores.contains(a.id())) {
						type=PageType.IGNORE;
						plog.update(5);
					} else if (people.contains(a.id())){
						plog.update(6);
						isPeople = true;
					}
					fType.setValue(type.name());
					
					switch(type){
					case DISAMBIGUATION:
					case LIST:
					case IGNORE:
						fAbstract.setValue("");
						fBody.setValue("");
						fRedirect.setValue("");
						break;
					case REDIRECT:
						fAbstract.setValue("");
						fBody.setValue("");
						fRedirect.setValue(Integer.toString(redirects.get(a.id())));
						break;
					case TOPIC:
						MutableString cleanText = wikier.clean(a.body, WikiTextExtractor.ANCHOR_REPLACER);
						MutableString text = wikier.removeStructure(cleanText, false);
						MutableString snippet  = wikier.removeStructure(cleanText, true);
						text.insert(0, a.title()+".\n");
						fAbstract.setValue(snippet.toString());
						fBody.setValue(text.toString());
						fRedirect.setValue(isPeople ? "PEOPLE" : "");
						break;
					}
					
					if (categories.containsKey(a.title()))
					{
						for (String c : categories.get(a.title()))
							theDoc.add(new Field(FIELD_CAT, c, Store.YES, Index.NOT_ANALYZED));
					}
					else
					{
						plog.update(7);
					}
					
					writer.addDocument(theDoc);
					
					theDoc.removeFields(FIELD_CAT);
				}
				
			}
			@Override
			protected void stop() throws IOException {
				super.stop();
				plog.stop();
			}
			
		};

		File input = WikipediaFiles.ARTICLES.getSourceFile(lang);
		parser.parse(input);
		
		log.info("Now optimizing...");
		writer.optimize();
		
		writer.close();
		log.info("Done.");
		
	}

	public static HashMap<String, List<String>> parseDBPediaCategories(String lang) throws IOException
	{
		PLogger plog = new PLogger(log, Step.TEN_MINUTES, "Lines", "Articles", "Errors");
		plog.start("Parsing DBPEDIA categories");
		
		HashMap<String, List<String>> cats = new HashMap<String, List<String>>(1600000);

		Pattern patTitle = Pattern.compile("/resource/([^>]*)>");
		Pattern patCat = Pattern.compile("/resource/[^:</]*:([^>]*)>");

		
		File dbpedia_cat = WikipediaFiles.DBPEDIA_CAT.getSourceFile(lang);
		FastBufferedReader fbr = new FastBufferedReader(new InputStreamReader(new FileInputStream(dbpedia_cat), Charset.forName("UTF-8")));
		
		MutableString line = new MutableString(1024);
		while(fbr.readLine(line) != null)
		{
			plog.update(0);
			line.trim();
			if (line.startsWith("#")) continue;
			
			Matcher m = patTitle.matcher(line);
			if (!m.find())
			{
				plog.update(2);
				continue;
			}
			String title = m.group(1).replace('_', ' ');
			
			int lastCharTitle = m.end();
			m = patCat.matcher(line);
			if (!m.find(lastCharTitle))
			{
				plog.update(2);
				continue;
			}
			String cat = m.group(1).replace('_', ' ');
			
			if (!cats.containsKey(title))
			{
				plog.update(1);
				cats.put(title, new ArrayList<String>());
			}
			cats.get(title).add(cat);
		}
		plog.stop();
		
		fbr.close();
		return cats;
	}
	
	@Override
	public File getIndexDir(String lang) {
		return getDir(RepositoryDirs.WIKIPEDIA, lang);
	}

}

