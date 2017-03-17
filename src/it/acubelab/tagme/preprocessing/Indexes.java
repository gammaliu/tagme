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

import it.acubelab.ExternalSortUtils;
import it.acubelab.tagme.config.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.SimpleFSDirectory;

public class Indexes {

	
	private static ConcurrentHashMap<String, IndexSearcher> searcherMap = new ConcurrentHashMap<String, IndexSearcher>();
	private static ConcurrentHashMap<String, IndexReader> readerMap = new ConcurrentHashMap<String, IndexReader>();
	
	static Logger log = Logger.getLogger(Indexes.class);
	
	public static IndexSearcher getSearcher(String path) throws IOException
	{
		if (!searcherMap.containsKey(path))
		{
			synchronized(Indexes.class)
			{
				if (!searcherMap.containsKey(path))
				{
					IndexSearcher s = new IndexSearcher(getReader(path));
					searcherMap.put(path, s);
				}
			}
		}
		
		return searcherMap.get(path);
	}
	public static IndexReader getReader(String path) throws IOException
	{
		if (!readerMap.containsKey(path))
		{
			synchronized(Indexes.class)
			{
				if (!readerMap.containsKey(path))
				{
					log.info("["+path+"] Opening...");
					if (! new File(path).exists())
						throw new ConfigurationException("Unable to find index in "+path);

					IndexReader r = IndexReader.open(new SimpleFSDirectory(new File(path)), true);
					readerMap.put(path, r);
					log.info("["+path+"] Opened. Memory: "+ExternalSortUtils.memSize(false));
				}
			}
		}
		
		return readerMap.get(path);
	}
	
	
	
}
