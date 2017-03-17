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
import it.acubelab.tagme.preprocessing.WikipediaIndexer.PageType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

public class TopicIndexer extends Indexer {

	static Logger log = Logger.getLogger(TopicIndexer.class);
	
	public static final String
		FIELD_WID = "wid",
		FIELD_TITLE = "title",
		FIELD_ABSTRACT = "abstract",
		FIELD_BEST_ANCHOR = "best",
		FIELD_CAT = "cat";
	
	
	@Override
	public void makeIndex(String lang, File workingDir) throws IOException 
	{
		
		IndexReader articles = Indexes.getReader(RepositoryDirs.WIKIPEDIA.getPath(lang));
		Int2ObjectMap<String> bestAnchorMap = new BestAnchors(lang).getDataset();
		
		IndexWriter index = new IndexWriter(new SimpleFSDirectory(workingDir), new IndexWriterConfig(Version.LUCENE_34, new KeywordAnalyzer()));
		Document doc = new Document();
		Field fWID = new Field(FIELD_WID, "", Store.YES, Index.NOT_ANALYZED);
		Field fTitle = new Field(FIELD_TITLE, "", Store.YES, Index.NOT_ANALYZED);
		Field fAbstract = new Field(FIELD_ABSTRACT, "", Store.YES, Index.NO);
		Field fBestAnchor = new Field(FIELD_BEST_ANCHOR, "", Store.YES, Index.NO);
		doc.add(fWID);
		doc.add(fTitle);
		doc.add(fAbstract);
		doc.add(fBestAnchor);
				
		
		int max = articles.maxDoc();
		PLogger plog = new PLogger(log, Step.TEN_MINUTES, "pages", "indexed", "noBest");
		plog.setEnd(max);
		plog.start("Start indexing...");
		
		for(int i=0; i<max; i++)
		{
			plog.update(0);
			Document oldDoc = articles.document(i);
			PageType type = PageType.valueOf(oldDoc.get(WikipediaIndexer.FIELD_TYPE));
			if (type == PageType.TOPIC)
			{
				int wid = Integer.parseInt(oldDoc.get(WikipediaIndexer.FIELD_WID));
				fWID.setValue(oldDoc.get(WikipediaIndexer.FIELD_WID));
				fAbstract.setValue(oldDoc.get(WikipediaIndexer.FIELD_ABSTRACT));
				fTitle.setValue(oldDoc.get(WikipediaIndexer.FIELD_TITLE));
				
				String bestAnchor = bestAnchorMap.get(wid);
				if (bestAnchor == null || bestAnchor.length() == 0) plog.update(2);
				fBestAnchor.setValue(bestAnchor==null?"":bestAnchor);
				
				String[] cats = oldDoc.getValues(WikipediaIndexer.FIELD_CAT);
				if (cats != null) {
					for (int j=0; j<cats.length; j++)
						doc.add(new Field(FIELD_CAT, cats[j], Store.YES, Index.NOT_ANALYZED));
				}
				
				index.addDocument(doc);
				plog.update(1);
				
				doc.removeFields(FIELD_CAT);
			}
		}
		
		plog.stop();
		
		log.info("Now optimizing...");
		index.optimize();
		
		index.close();
		
		//we cannot call this because the index is still in the temporary dir
		//so TopicDocs will be created using old index
//		log.info("Index Done, now creating WID->DOC_ID map");
//		
//		TopicDocs td = new TopicDocs(lang);
//		td.forceParsing();
		
		log.info("Done.");
	}
	

	@Override
	public File getIndexDir(String lang) {
		return RepositoryDirs.TOPICS.getDir(lang);
	}

}
