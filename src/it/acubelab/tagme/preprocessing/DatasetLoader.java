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


import it.acubelab.ExternalSortUtils;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class DatasetLoader {

	private static ConcurrentHashMap<Dataset<? extends Object>, Object> cache = new ConcurrentHashMap<Dataset<?>, Object>(); 
	static Logger log = Logger.getLogger(Dataset.class);
	
	//to force loading needed sets in application's initialization
	//specify this class on config init methods 
//	public static synchronized void init()
//	{
//		for(String lang : TagmeConfig.get().getLangs())
//		{
//			get(new ArticleIdNode(code));
//			get(new GraphDump(code));
//			//get(new ArticleNodeId(code));
//		}
//	}
	
	
	
	@SuppressWarnings("unchecked")
	public static <T extends Object> T get(Dataset<T> type)
	{
		T set = (T)cache.get(type);
		if (set == null)
		{
			synchronized(DatasetLoader.class)
			{
				if ((set=(T)cache.get(type)) == null)
				{
					try {
						set = type.restoreDataset();
						if (set == null)
							throw new RestoringException(type.toString()+" Dataset not found!");
						cache.put(type, set);
						log.info(type.toString()+" Restored. Memory: "+ExternalSortUtils.memSize(false));
					} catch (IOException ioe){
						 throw new RestoringException(type.getLang()+":"+type.getFile(), ioe);
					}
				}
			}
		}
		
		
		
		return set;
	}

}
