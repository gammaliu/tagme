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
package it.acubelab.tagme.preprocessing.graphs;

import it.acubelab.ExternalSort;
import it.acubelab.tagme.config.Config.WikipediaFiles;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.acubelab.tagme.preprocessing.SQLWikiParser;
import it.acubelab.tagme.preprocessing.TextDataset;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.acubelab.tagme.preprocessing.support.DisambiguationWIDs;
import it.acubelab.tagme.preprocessing.support.IgnoreWIDs;
import it.acubelab.tagme.preprocessing.support.ListPageWIDs;
import it.acubelab.tagme.preprocessing.support.PeopleWIDs;
import it.acubelab.tagme.preprocessing.support.RedirectMap;
import it.acubelab.tagme.preprocessing.support.TitlesToWIDMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class WikipediaEdges extends TextDataset {

	static Logger log = Logger.getLogger(WikipediaEdges.class);

	public WikipediaEdges(String lang) {
		super(lang);
	}

	@Override
	protected void parseFile(File file) throws IOException 
	{

		final Int2IntMap redirects = DatasetLoader.get(new RedirectMap(lang));
		final IntSet disambiguations = DatasetLoader.get(new DisambiguationWIDs(lang));
		final IntSet listpages = DatasetLoader.get(new ListPageWIDs(lang));
		final IntSet ignores = DatasetLoader.get(new IgnoreWIDs(lang));
		final IntSet valids = new AllWIDs(lang).getDataset();//DatasetLoader.get(new AllWIDs(lang));
		valids.removeAll(redirects.keySet());
		//valids.removeAll(disambiguations);
		//valids.removeAll(listpages);
		valids.removeAll(ignores);
		final Object2IntMap<String> titles = DatasetLoader.get(new TitlesToWIDMap(lang));


		File tmp = Dataset.createTmpFile();
		final BufferedWriter out = new BufferedWriter(new FileWriter(tmp));
		SQLWikiParser parser = new 	SQLWikiParser(log) {
			@Override
			public boolean compute(ArrayList<String> values) throws IOException 
			{
				int idFrom = Integer.parseInt(values.get(SQLWikiParser.PAGELINKS_ID_FROM));
				if (redirects.containsKey(idFrom)) idFrom = redirects.get(idFrom);
				
				int ns = Integer.parseInt(values.get(SQLWikiParser.PAGELINKS_NS));

				
				if (ns == SQLWikiParser.NS_ARTICLE && !redirects.containsKey(idFrom) && !ignores.contains(idFrom) && 
						//questo e' necessario perchè alcune pagine che sono delle liste, in inglese finiscono 
						//tra le pagine di disambiguazione (per via della categoria All_set_index_articles)
						(listpages.contains(idFrom) || !disambiguations.contains(idFrom))
						//!listpages.contains(idFrom) && !disambiguations.contains(idFrom) 
						&& valids.contains(idFrom)
				
				/**/ )
				{

					String titleTo = Dataset.cleanPageName(values.get(SQLWikiParser.PAGELINKS_TITLE_TO));

					int idTo = titles.getInt(titleTo);
					
					if (redirects.containsKey(idTo)) idTo = redirects.get(idTo);
					if (idTo >= 0 && !ignores.contains(idTo) && (listpages.contains(idFrom) || !disambiguations.contains(idFrom)) && valids.contains(idTo))
					{
						out.append(Integer.toString(idFrom));
						out.append(SEP_CHAR);
						out.append(Integer.toString(idTo));
						out.append('\n');
						return true;
					}
				}
				return false;
			}
		};

		File input = WikipediaFiles.PAGE_LINKS.getSourceFile(lang);
		parser.compute(input);
		out.close();

		log.info("Now sorting edges...");

		ExternalSort sorter = new ExternalSort();
		sorter.setUniq(true);
		sorter.setNumeric(true);
		sorter.setColumns(new int[]{0,1});
		sorter.setInFile(tmp.getAbsolutePath());
		sorter.setOutFile(file.getAbsolutePath());
		sorter.run();

		tmp.delete();

		log.info("Sorted. Done.");

	}

}
