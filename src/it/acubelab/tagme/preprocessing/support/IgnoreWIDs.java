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
import java.util.regex.Pattern;

import it.acubelab.PLogger;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.WikiPatterns;
import it.acubelab.tagme.preprocessing.WikiPatterns.Type;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public class IgnoreWIDs extends Dataset<IntSet> {

	public IgnoreWIDs(String lang) {
		super(lang);
	}

	@Override
	protected IntSet parseSet() throws IOException 
	{
		log.info("Loading data...");
		Object2IntMap<String> titles = new TitlesToWIDMap(lang).getDataset();
		IntOpenHashSet ids = new IntOpenHashSet(titles.size());
		
		Pattern p_date = WikiPatterns.getPattern(lang, Type.PAGE_DATE);
		Pattern p_other = WikiPatterns.getPattern(lang, Type.PAGE_IGNORE);
		
		PLogger plog = new PLogger(log,"titles","dates","others").setEnd(0, titles.size()).start("Parsing ignore-pages...");
		for(String title : titles.keySet())
		{
			plog.update(0);
			if (p_date.matcher(title).find()) {
				plog.update(1);
				ids.add(titles.get(title));
			}
			else if (p_other.matcher(title).find()) {
				plog.update(2);
				ids.add(titles.get(title));
			}
		}
		plog.stop();
		
		ids.trim();
		return ids;
	}

}
