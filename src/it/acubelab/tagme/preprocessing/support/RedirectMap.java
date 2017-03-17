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
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class RedirectMap extends Dataset<Int2IntMap> {

	public RedirectMap(String lang) {
		super(lang);
	}

	@Override
	protected Int2IntMap parseSet() throws IOException 
	{
		final Object2IntMap<String> titles = new TitlesToWIDMap(lang).getDataset();
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap(3000000);
		SQLWikiParser parser = new SQLWikiParser(log, "Titles NF") {
			@Override
			public boolean compute(ArrayList<String> values) throws IOException 
			{
				int ns = Integer.parseInt(values.get(SQLWikiParser.REDIRECT_NS));
				if (ns == SQLWikiParser.NS_ARTICLE)
				{
					int idFrom = Integer.parseInt(values.get(SQLWikiParser.REDIRECT_ID_FROM));
					int idTo = titles.getInt(cleanPageName(values.get(SQLWikiParser.REDIRECT_TITLE_TO)));
					if (idTo >= 0)
						map.put(idFrom, idTo);
					else this.updateItem(0);
					
					return true;
				} else return false;
			}
		};

		File input = WikipediaFiles.REDIRECTS.getSourceFile(lang);
		InputStreamReader in = new InputStreamReader(new FileInputStream(input), Charset.forName("UTF-8"));
		parser.compute(in);
		in.close();
		
		map.defaultReturnValue(-1);
		map.trim();
		
		return map;

	}

}
