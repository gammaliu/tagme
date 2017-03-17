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
package it.acubelab.tagme.preprocessing.support;

import java.io.IOException;

import it.acubelab.tagme.config.Config;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public class AllWIDs extends Dataset<IntSet> {

	public AllWIDs(String lang) {
		super(lang);
	}

	@Override
	protected IntSet parseSet() throws IOException {
		
		Object2IntMap<String> title2wid = new TitlesToWIDMap(lang).getDataset();
		
		IntOpenHashSet wids = new IntOpenHashSet(title2wid.size()*2);
		wids.addAll(title2wid.values());
		wids.trim();
		
		return wids;
	}

}
 
