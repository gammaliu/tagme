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
import it.acubelab.ExternalSortUtils;
import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.Anchor;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.Indexer;
import it.acubelab.tagme.preprocessing.Indexes;
import it.acubelab.tagme.preprocessing.TextDataset;
import it.acubelab.tagme.preprocessing.WikipediaIndexer;
import it.acubelab.tagme.preprocessing.support.PeopleWIDs;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class AnchorIndexer extends Indexer {

	Logger log = Logger.getLogger(AnchorIndexer.class);
	
	public static final String
		FIELD_ID = "id",
		FIELD_TEXT = "text",
		FIELD_ORIGINAL = "original",
		FIELD_OBJECT = "obj",
		FIELD_WID = "wid";
	
	public AnchorIndexer() {
	}

	//per ottimizzare le ricerche, se sono su un server ben messo posso
	//caricare in memoria tutto l'indice!
	private static double GAP_FACTOR = 0.8;
	private IndexSearcher openWikipediaIndex(String lang) throws IOException{
		
		
		File indexDir = RepositoryDirs.WIKIPEDIA.getDir(lang);
		long indexSize = FileUtils.sizeOfDirectory(indexDir);
		
		long maxMemory = Runtime.getRuntime().maxMemory();
		
		if (indexSize < maxMemory * GAP_FACTOR){
			
			log.info("MaxMemory is enough, loading Wikipedia index...");
			IndexReader r = IndexReader.open(new RAMDirectory(FSDirectory.open(indexDir)), true);
			log.info("WikipediaIndex loaded.");
			return new IndexSearcher(r);
			
		} else {
			log.info("Not enough memory ["+maxMemory/1000000+"Mb] to load WikipediaIndex (about "+indexSize/1000000+"Mb)");
			return Indexes.getSearcher(RepositoryDirs.WIKIPEDIA.getPath(lang));
		}
	}
	
	@Override
	public void makeIndex(String lang, File workingDir) throws IOException
	{
		log.info("Loading support datasets...");
		
		File all_anchors = new WikipediaAnchorParser(lang).getFile();
		long numAnchors = ExternalSortUtils.wcl(all_anchors);
		AnchorIterator iterator = new AnchorIterator(all_anchors);
		
		IntSet people = new PeopleWIDs(lang).getDataset();
		
//		IndexSearcher articles = Indexes.getSearcher(RepositoryDirs.WIKIPEDIA.getPath(lang));
		IndexSearcher articles = openWikipediaIndex(lang);
		//QueryParser queryParser = new QueryParser(Version.LUCENE_34, WikipediaIndexer.FIELD_BODY, new WhitespaceAnalyzer(Version.LUCENE_34));
		QueryParser queryParser = new QueryParser(Version.LUCENE_34, WikipediaIndexer.FIELD_BODY, new StandardAnalyzer(Version.LUCENE_34, new HashSet<String>()));
		
		IndexWriter index = new IndexWriter(FSDirectory.open(workingDir.getAbsoluteFile()), new IndexWriterConfig(Version.LUCENE_34, new KeywordAnalyzer()));
		Document doc = new Document();
		Field fId = new Field(FIELD_ID, "", Store.YES, Index.NOT_ANALYZED);
		Field fText = new Field(FIELD_TEXT, "", Store.YES, Index.NOT_ANALYZED);
		Field fObject = new Field(FIELD_OBJECT, "", Store.YES, Index.NO);
		
		doc.add(fId);
		doc.add(fText);
		doc.add(fObject);
		
//		Field fOriginal = new Field(FIELD_ORIGINAL, "", Store.YES, Index.ANALYZED);
//		Field fWID = new Field(FIELD_WID, "", Store.NO, Index.ANALYZED);
		
		PLogger plog = new PLogger(log, Step.TEN_MINUTES, "lines", "anchors", "searches", "indexed", "0-freq","dropped");
		plog.setEnd(0, numAnchors);
		plog.start("Support datasets loaded, now parsing...");
		int id=0;
		while(iterator.next())
		{
			plog.update(0, iterator.scroll);
			plog.update(1);
			String anchorText = iterator.anchor;
			
			int freq = freq(iterator.originals, articles, queryParser);
			plog.update(2, iterator.originals.size());
			if (freq == 0) plog.update(4);
			
			Anchor anchorObj = Anchor.build(id, iterator.links, freq, people);
			if (anchorObj == null){
				plog.update(5);
				continue;
			}
			
			String anchorSerial = Anchor.serialize(anchorObj);
			fId.setValue(Integer.toString(++id));
			fText.setValue(anchorText);
			fObject.setValue(anchorSerial);
			
			for(int page : anchorObj){
				Field fWID = new Field(FIELD_WID, Integer.toString(page), Store.YES, Index.NOT_ANALYZED);
//				fWID.setBoost(iterator.links.get(page));
				doc.add(fWID);
			}
			for(String original : iterator.originals) {
				doc.add(new Field(FIELD_ORIGINAL, original, Store.YES, Index.NOT_ANALYZED));
			}
			
			index.addDocument(doc);
			plog.update(3);
			
			doc.removeFields(FIELD_ORIGINAL);
			doc.removeFields(FIELD_WID);
		}
		plog.stop();
		iterator.close();
		
		log.info("Now optimizing...");
		index.optimize();
		
		index.close();
		log.info("Done.");
	}
	
	static final String QUERY_PATTERN = "\"%s\"";
	static int freq(Set<String> anchors, IndexSearcher index, QueryParser queryParser) throws IOException
	{
		//int sum = 0;
		BitSet bits = new BitSet(index.maxDoc());
		for(String a : anchors)
		{
			try {
				Query q = queryParser.parse(String.format(QUERY_PATTERN, QueryParser.escape(a)));
				
				TotalHitCountCollectorSet results = new TotalHitCountCollectorSet(bits);
				
				index.search(q, results);
			
				//sum += results.getTotalHits();
			
			} catch (ParseException e) {
				
			}
		}
		return bits.cardinality();
	}
	
	

	@Override
	public File getIndexDir(String lang) {
		return RepositoryDirs.ANCHORS.getDir(lang);
	}



	private static class TotalHitCountCollectorSet extends TotalHitCountCollector{

		private BitSet set;
		private int docBase;
		
		public TotalHitCountCollectorSet(BitSet set) {
			super();
			this.set =set;
		}

		public boolean acceptsDocsOutOfOrder(){
			return true;
		}
		
		public void setNextReader(IndexReader reader,int docBase){
			this.docBase=docBase;
		}
		
		
		public void collect(int doc){
			super.collect(doc);
			set.set(doc+docBase);
			
		}

		
	}
	
	
	/**
	 * Anchor Iterator
	 * */
	
	private static class AnchorIterator
	{
		String anchor;
		String lastAnchor;
		Int2IntMap links;
		Set<String> originals;
		FastBufferedReader in;
		MutableString line;
		int scroll;
		boolean end;
		
		public AnchorIterator(File inputFile) throws IOException
		{
			anchor = null;
			links = new Int2IntOpenHashMap(1024);
			links.defaultReturnValue(0);
			originals = new HashSet<String>(32);
			in = new FastBufferedReader(new InputStreamReader(new FileInputStream(inputFile), Charset.forName("UTF-8")));
			line = new MutableString(1024);
			in.readLine(line);
			lastAnchor = Chars.split(line, TextDataset.SEP_CHAR)[0].toString();
			scroll = 1;
			end = false;
		}
		
		public boolean next() throws IOException
		{
			//Verifichiamo se siamo giunti alla fine del file.
			if (end) return false;
			
			links.clear();
			originals.clear();
			scroll = 0;
			
			while(true)
			{
				CharSequence[] fields = Chars.split(line, TextDataset.SEP_CHAR);
				
				if (fields[0].equals(lastAnchor))
				{
					//Aggiungiamo un nuovo original.
					originals.add(fields[1].toString());
					//Prendiamo la target page
					int targetpage = Chars.parseInt(fields[2]);
					//Aumentiamo di uno il numero di link alla target page (Da notare che se non esiste nella map links.get(targetpage) restiuisce 0.
					links.put(targetpage, links.get(targetpage)+1);
					
				}else{
					//Nuova ancora
					anchor = lastAnchor;
					lastAnchor = fields[0].toString();
					break;
				}
				
				scroll++;
				if (in.readLine(line)==null){
					end = true;
					break;
				}
				
			};
			
			return true;
		}
		
		void close() throws IOException {
			in.close();
		}
		
	}
	/**
	 * End of AnchorIterator
	 * */
	

}
