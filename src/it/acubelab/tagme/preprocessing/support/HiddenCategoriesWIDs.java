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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import it.acubelab.tagme.config.Config.WikipediaFiles;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.SQLWikiParser;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class HiddenCategoriesWIDs extends Dataset<IntSet>{

	public HiddenCategoriesWIDs(String lang) {
		super(lang);
	}

	@Override
	protected IntSet parseSet() throws IOException {
		final IntSet set = new IntOpenHashSet();
		File input = WikipediaFiles.CAT_LINKS.getSourceFile(lang);

		SQLWikiParser parser = new SQLWikiParser(log) {
			@Override
			public boolean compute(ArrayList<String> values) throws IOException {
				if(cleanPageName(values.get(SQLWikiParser.CAT_TITLE)).equals("Hidden categories")){
					set.add(Integer.parseInt(values.get(SQLWikiParser.CATLINKS_ID_FROM)));
					return true;
				}else return false;
			}

		};
		InputStreamReader reader = new InputStreamReader(new FileInputStream(input), Charset.forName("UTF-8"));
		parser.compute(reader);
		reader.close();

		return set;
	}


}
