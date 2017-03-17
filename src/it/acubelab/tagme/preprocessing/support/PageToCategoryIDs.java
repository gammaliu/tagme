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

import it.acubelab.PLogger;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.WikipediaFiles;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.acubelab.tagme.preprocessing.SQLWikiParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PageToCategoryIDs extends Dataset<int[][]>{

	public PageToCategoryIDs(String lang) {
		super(lang);
		}

	@Override
	protected int[][] parseSet() throws IOException {
		final Int2ObjectMap<IntSet> map = new Int2ObjectOpenHashMap<IntSet>(3000000);
		final IntSet hidden= DatasetLoader.get(new HiddenCategoriesWIDs(lang));
		File input = WikipediaFiles.CAT_LINKS.getSourceFile(lang);
		final Object2IntMap<String> categories=DatasetLoader.get(new CategoriesToWIDMap(lang));
		
		SQLWikiParser parser = new SQLWikiParser(log) {
			@Override
			public boolean compute(ArrayList<String> values) throws IOException {
				String c_title=cleanPageName(values.get(SQLWikiParser.CATLINKS_TITLE_TO));
				int id=Integer.parseInt(values.get(SQLWikiParser.CATLINKS_ID_FROM));
				if(categories.containsKey(c_title) && !hidden.contains(categories.get(c_title).intValue())){
					if(map.containsKey(id)){
						map.get(id).add(categories.get(c_title).intValue());
					}else{
						IntSet set = new IntOpenHashSet();
						set.add(categories.get(c_title).intValue());
						map.put(id, set);
					}
					return true;
				} else return false;
			}
			
		};
		InputStreamReader reader = new InputStreamReader(new FileInputStream(input), Charset.forName("UTF-8"));
		parser.compute(reader);
		reader.close();
		return createDump(map);
	}
	
	
	private int[][] createDump(Int2ObjectMap<IntSet> map){
		
		int max_wid=0;
		for(int id: map.keySet()){
			if(id>max_wid) max_wid=id;
		}
		
		int[][] dump=new int[max_wid+1][];
		
		for(int id: map.keySet()){
				dump[id]=map.get(id).toIntArray();
			}
		
		
		
		return  dump;
	}
	
	
}
