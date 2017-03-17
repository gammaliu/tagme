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

import it.acubelab.tagme.Anchor;
import it.acubelab.tagme.TagmeParser.TokenizedCharSequence;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.preprocessing.Indexes;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

public class LuceneAnchorSearcher extends AnchorSearcher {

	IndexSearcher searcher;
	
	public LuceneAnchorSearcher(String lang) throws IOException{
		super(lang);
		searcher = Indexes.getSearcher(RepositoryDirs.ANCHORS.getPath(lang));
		
	}

	
	
	
	@Override
	public int search(TokenizedCharSequence input)
	{
		int found = 0;
		int tokens = 0;
		
		for(int i=0; i<input.length(); i++)
		{
			if (input.isEndToken(i))
			{
				CharSequence subseq = input.subSequence(0, i+1);
				//System.out.println("Searching for: "+subseq);
				Anchor a = search(subseq);
				if (a != null) {
					input.setAnchorAt(tokens, a);
					//System.out.println("Found");
					found++;
				}
				tokens++;
			}
		}
		return found;
	}
	
	public int exactSearch(CharSequence input){
		return 0;
	}
	
	@Override
	public Anchor search(CharSequence input){
		try{
			TermQuery query = new TermQuery(new Term(AnchorIndexer.FIELD_TEXT, input.toString()));
			TopDocs td = searcher.search(query, 1);
			if (td.totalHits == 0) return null;
			else return Anchor.deserialize(searcher.doc(td.scoreDocs[0].doc).get(AnchorIndexer.FIELD_OBJECT));
		} catch (IOException ioe){
			throw new RuntimeException("Unable to search in the anchor index!", ioe);
		}
	}
	public int size(){
		return searcher.maxDoc();
	}

	
	
	public String[] getOriginals(int id){
		TermQuery q = new TermQuery(new Term(AnchorIndexer.FIELD_ID, ""+id));
		return getOriginals(q);
	}
	public String[] getOriginals(String text){
		TermQuery q = new TermQuery(new Term(AnchorIndexer.FIELD_TEXT, text));
		return getOriginals(q);
	}
	public String[] getOriginals(Query q)
	{
		try {
			TopDocs td = searcher.search(q, 1);
			if (td.totalHits == 0) return null;
			else {
				Document doc = searcher.doc(td.scoreDocs[0].doc);
				return doc.getValues(AnchorIndexer.FIELD_ORIGINAL);
			}
			
		} catch(IOException ioe){
			throw new RuntimeException("Unable to search in the anchor index!", ioe);
		}
		
		
	}


	public int getMaxDocs(){
		return searcher.maxDoc();
	}

}
