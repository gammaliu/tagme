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

import it.acubelab.tagme.config.Config.WikipediaFiles;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.SQLWikiParser;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class CategoriesToWIDMap extends Dataset<Object2IntMap<String>> {

	public CategoriesToWIDMap(String lang) {
		super(lang);
	}

	@Override
	protected Object2IntMap<String> parseSet() throws IOException 
	{
		final Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<String>(3000000);
		File input = WikipediaFiles.TITLES.getSourceFile(lang);
		
		SQLWikiParser parser = new SQLWikiParser(log) {
			@Override
			public boolean compute(ArrayList<String> values) throws IOException {
				if (values.get(SQLWikiParser.PAGE_NS).equals(SQLWikiParser.NS_CATEGORY_STRING)){
					
					String category = cleanPageName(values.get(SQLWikiParser.PAGE_TITLE));
					map.put(category, Integer.parseInt(values.get(SQLWikiParser.PAGE_ID)));
					return true;
				} else return false;
			}
			
		};
		InputStreamReader reader = new InputStreamReader(new FileInputStream(input), Charset.forName("UTF-8"));
		parser.compute(reader);
		reader.close();
		
		map.defaultReturnValue(-1);
		map.trim();
		
		return map;
	}

	
	
	
}
