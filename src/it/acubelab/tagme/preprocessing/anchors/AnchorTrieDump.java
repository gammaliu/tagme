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

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

public class AnchorTrieDump extends Dataset<AnchorTrie> 
{
	public AnchorTrieDump(String lang) {
		super(lang);
	}

	@Override
	protected AnchorTrie parseSet() throws IOException 
	{
		IndexReader anchors = Indexes.getReader(RepositoryDirs.ANCHORS.getPath(lang));
		AnchorTrie trie = new AnchorTrie();
		int maxdoc = anchors.maxDoc();
		
		PLogger plog = new PLogger(log, Step.TEN_MINUTES, "anchors", "skipped", "duplicates");
		plog.setEnd(0, maxdoc);
		plog.start("Inserting in to trie...");
		for(int i=0; i<maxdoc; i++)
		{
			plog.update(0);
			Document doc = anchors.document(i);
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
			
			boolean done = trie.add(anchorText, anchorObj);
			
			if (!done) plog.update(2);
		}
		plog.stop();
		
		log.info("Now trimming...");
		trie.trim();
		log.info("Done.");
		
		return trie;
		
	}

}
