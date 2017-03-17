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
//import it.acubelab.tagme.config.Config;
import it.acubelab.tagme.config.Config.IndexType;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.config.TagmeConfig;
import it.unimi.dsi.webgraph.ImmutableGraph;
//import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.acubelab.tagme.preprocessing.RestoringException;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class WikiGraphs {

	static Logger log = Logger.getLogger(WikiGraphs.class);

	static ConcurrentHashMap<String, ImmutableGraph> graphs = new ConcurrentHashMap<String, ImmutableGraph>();

	public static synchronized void init() throws IOException
	{
	}



	private static String buildKey(String lang, IndexType en){
		return lang.toUpperCase()+"/"+en.name();
	}


	public static ImmutableGraph get(String lang, IndexType type)
	{
		String key = buildKey(lang, type);
		ImmutableGraph g = graphs.get(key);
		if (g == null)
		{
			synchronized(WikiGraphs.class)
			{
				if ((g=graphs.get(key)) == null)
				{
					log.debug("["+key+"] Loading ...");
					try {
						switch(type){
						case GRAPH:
							String path = RepositoryDirs.GRAPH.getPath(lang)+"/graph";//"/graph";
							g = ImmutableGraph.load(path);
							break;
						case IN_GRAPH:
							String inpath =RepositoryDirs.IN_GRAPH.getPath(lang)+"/in_graph";
							g = ImmutableGraph.load(inpath);
							break;
						default:
							throw new RestoringException(key+": Unknown graph type!"); 
						}
						log.debug("["+key+"] Graph loaded. Memory: "/*+Utils.memSize(false)*/);
						graphs.put(key, g);
					} catch (IOException ioe){
						throw new RestoringException(key,ioe);
					}
				}
			}
		}
		return g;

	}




}
