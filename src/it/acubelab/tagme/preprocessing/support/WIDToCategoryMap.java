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

import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public class WIDToCategoryMap extends Dataset<Int2ObjectMap<String>>{

	public WIDToCategoryMap(String lang) {
		super(lang);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Int2ObjectMap<String> parseSet() throws IOException {
		final Object2IntMap<String> categories=DatasetLoader.get(new CategoriesToWIDMap(lang));
		final Int2ObjectMap<String> map=new Int2ObjectOpenHashMap<String>();
		
		for(String s:categories.keySet()){
			map.put(categories.get(s),s);
		}
		
		return map;
	}

	
	
}
