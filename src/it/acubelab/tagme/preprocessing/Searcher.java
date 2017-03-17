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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public abstract class Searcher {

	protected IndexSearcher searcher;
	protected IndexReader reader;
	protected String lang;
	protected RepositoryDirs type;
	
	protected Searcher(RepositoryDirs dir, String lang) throws IOException
	{
		this.lang = lang;
		this.type = dir;
		File indexDir = getIndexDir();
		this.reader = Indexes.getReader(indexDir.getAbsolutePath());
		this.searcher = Indexes.getSearcher(indexDir.getAbsolutePath());
	}
	
	public Document getDocById(int id) throws IOException
	{
		return searcher.doc(id);
	}
	
	public String getLanguageCode() {
		return lang;
	}
	

	protected File getIndexDir(){
		return type.getDir(lang);
	}
	
	
	
	
}
