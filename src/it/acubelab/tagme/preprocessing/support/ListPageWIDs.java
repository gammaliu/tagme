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
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.SQLWikiParser;
import it.acubelab.tagme.preprocessing.WikiPatterns;
import it.acubelab.tagme.preprocessing.WikiPatterns.Type;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

public class ListPageWIDs extends Dataset<IntSet> {

	public ListPageWIDs(String lang) {
		super(lang);
	}

	@Override
	protected IntSet parseSet() throws IOException 
	{
		 
		log.info("Searching for list-categories...");
		final HashSet<String> listCategories = new HashSet<String>();
		final Pattern pattern = WikiPatterns.getPattern(lang, Type.LIST_CAT);
		
		SQLWikiParser parser = new SQLWikiParser(log) {
			
			@Override
			public boolean compute(ArrayList<String> values) throws IOException 
			{
				int ns = Integer.parseInt(values.get(SQLWikiParser.PAGE_NS));
				if (ns == SQLWikiParser.NS_CATEGORY){
					
					String name = cleanPageName(values.get(SQLWikiParser.PAGE_TITLE));
					String lowercaseName = name.toLowerCase(); 
					if (pattern.matcher(lowercaseName).matches()){
						listCategories.add(name);
						return true;
					}
					
				}return false;
			}
		}; 
		
		File inputFile = WikipediaFiles.TITLES.getSourceFile(lang);
		InputStreamReader reader = new InputStreamReader(new FileInputStream(inputFile), Charset.forName("UTF-8"));
		parser.compute(reader);
		reader.close();
		log.info(listCategories.size()+" list-categories identified.");
		
		final IntOpenHashSet ids = new IntOpenHashSet();
		SQLWikiParser parserCatLinks = new SQLWikiParser(log)
		{
			@Override
			public boolean compute(ArrayList<String> values) throws IOException 
			{
				String cat = cleanPageName(values.get(SQLWikiParser.CATLINKS_TITLE_TO));
				if (listCategories.contains(cat))
				{
					ids.add(Integer.parseInt(values.get(SQLWikiParser.CATLINKS_ID_FROM)));
					return true;
				}
				return false;
			}
		};
		File catLinks = WikipediaFiles.CAT_LINKS.getSourceFile(lang);
		InputStreamReader inCatLinks = new InputStreamReader(new FileInputStream(catLinks), Charset.forName("UTF-8"));
		parserCatLinks.compute(inCatLinks);
		inCatLinks.close();

		ids.trim();
		
		return ids;
	}

}
