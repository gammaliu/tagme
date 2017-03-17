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
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

public class TopicDocs extends Dataset<int[]> {

	public TopicDocs(String lang) {
		super(lang);
	}

	@Override
	protected int[] parseSet() throws IOException 
	{
		IntSet WIDs = new AllWIDs(lang).getDataset();
		int max_wid = 0;
		for(int wid: WIDs)
			if (wid > max_wid)
				max_wid = wid;
		
		IndexReader topics = Indexes.getReader(RepositoryDirs.TOPICS.getPath(lang));
		int max = topics.maxDoc();
		
		int[] map = new int[max_wid+1];
		for(int i=0;i<map.length; i++) map[i]=-1;
		
		PLogger plog = new PLogger(log, Step.MINUTE)
			.setEnd(max)
			.start();
		for(int i=0;i<max;i++) {
			map[Integer.parseInt(topics.document(i).get(TopicIndexer.FIELD_WID))] = i;
			plog.update();
		}
		plog.stop();
		
		return map;
	}

	
}
