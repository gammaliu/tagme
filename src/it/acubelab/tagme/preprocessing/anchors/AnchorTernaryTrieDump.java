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

import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.Anchor;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.Indexes;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

public class AnchorTernaryTrieDump  extends Dataset<AnchorTernaryTrie> 
{
	public AnchorTernaryTrieDump(String lang) {
		super(lang);
	}

	@Override
	protected AnchorTernaryTrie parseSet() throws IOException 
	{
		
		File indexDir = RepositoryDirs.ANCHORS.getDir(lang);
		long indexSize = FileUtils.sizeOfDirectory(indexDir);
		long maxMemory = Runtime.getRuntime().maxMemory();
		
		IndexReader anchors;
		if (indexSize < maxMemory * 0.8){
			
			log.info("MaxMemory is enough, loading Anchor index...");
			anchors = IndexReader.open(new RAMDirectory(new SimpleFSDirectory(indexDir)), true);
			log.info("Anchor index loaded.");
			
		} else {
			log.info("Not enough memory ["+maxMemory/1000000+"Mb] to load Anchor index (about "+indexSize/1000000+"Mb)");
			anchors = Indexes.getReader(RepositoryDirs.ANCHORS.getPath(lang));
		}

		
		AnchorTernaryTrie trie = new AnchorTernaryTrie();
		
		int maxdoc = anchors.maxDoc();
		
		IntList doclist = new IntArrayList();
		for(int i=0;i<maxdoc;i++) doclist.add(i);
		Random rnd = new Random(System.currentTimeMillis());
		
		PLogger plog = new PLogger(log, Step.TEN_MINUTES, "anchors", "skipped", "duplicates");
		plog.setEnd(0, maxdoc);
		plog.start("Inserting in to trie...");
		while(!doclist.isEmpty())
		{
			int docID = doclist.removeInt(rnd.nextInt(doclist.size()));
			
			plog.update(0);
			Document doc = anchors.document(docID);
			if (doc == null){
				plog.update(1);
				continue;
			}
			
			String anchorText = doc.get(AnchorIndexer.FIELD_TEXT);
			String serial = doc.get(AnchorIndexer.FIELD_OBJECT);
			Anchor anchorObj = Anchor.deserialize(serial);
			
			if (anchorObj == null){
				plog.update(1);
				continue;
			}
			
			boolean added = trie.add(anchorText, anchorObj);
			
			if (!added) plog.update(2);
		}
		plog.stop();
		
		return trie;
	}

}
