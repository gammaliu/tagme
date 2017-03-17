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

import it.acubelab.tagme.config.Config.RepositoryDirs;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

public class TopicSearcher {

	IndexSearcher index;
	int[] topic2doc;
	
	public TopicSearcher(String lang) throws IOException
	{
		index = Indexes.getSearcher(RepositoryDirs.TOPICS.getPath(lang));
		topic2doc = DatasetLoader.get(new TopicDocs(lang));
	}
	
	public String getTitle(int wid) throws IOException
	{
		int doc_id = wid>=0&&wid<topic2doc.length? topic2doc[wid] : -1;
		if (doc_id < 0) return null;
		return index.doc(doc_id).get(TopicIndexer.FIELD_TITLE);
	}
	
	public String getAbstract(int wid) throws IOException
	{
		int doc_id = wid>=0&&wid<topic2doc.length? topic2doc[wid] : -1;
		if (doc_id < 0) return null;
		return index.doc(doc_id).get(TopicIndexer.FIELD_ABSTRACT);
	}

	public String getBestAnchor(int wid) throws IOException
	{
		int doc_id = wid>=0&&wid<topic2doc.length? topic2doc[wid] : -1;
		if (doc_id < 0) return null;
		return index.doc(doc_id).get(TopicIndexer.FIELD_BEST_ANCHOR);
	}
	
	public int getIdByTitle(String title) throws IOException
	{
		TermQuery q = new TermQuery(new Term(TopicIndexer.FIELD_TITLE, title));
		TopDocs td = index.search(q, 1);
		if (td.totalHits == 0) return -1;
		else return Integer.parseInt(index.doc(td.scoreDocs[0].doc).get(TopicIndexer.FIELD_WID));
	}
	
	public boolean contains(int wid)
	{
		return wid>=0 && wid<topic2doc.length && topic2doc[wid] >= 0;
	}
	
	public String[] getCategories(int wid) throws IOException
	{
		int doc_id = wid>=0&&wid<topic2doc.length? topic2doc[wid] : -1;
		if (doc_id < 0) return null;
		return index.doc(doc_id).getValues(TopicIndexer.FIELD_CAT);
		
	}
	
	public int numTopics()
	{
		return index.maxDoc();
	}
}
