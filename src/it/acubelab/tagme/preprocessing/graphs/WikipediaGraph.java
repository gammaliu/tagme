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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import it.acubelab.Chars;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.preprocessing.Indexer;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;

public class WikipediaGraph extends Indexer {

	static Logger log= Logger.getLogger(WikipediaGraph.class);
	final static String NAME="graph";
	
	
	public WikipediaGraph() {
		
	}

	@Override
	public void makeIndex(String lang, File workingDir) throws IOException {
		
		log.info("Perform a graph build for language "+lang);
		
		log.info("Loading egdes...");
		
		File edges=new WikipediaEdges(lang).getFile();
		

		
		log.info("Storing optmized graph...");
		ProgressLogger logger;
		ArcListASCIIGraph tmp_graph= ArcListASCIIGraph.loadOnce(new FileInputStream(edges));
		logger=new ProgressLogger(log,ProgressLogger.TEN_SECONDS);
		BVGraph.store(tmp_graph, new File(workingDir,NAME).getAbsolutePath(),logger);
		log.info("Nodes: "+tmp_graph.numNodes());
		log.info("Graph built");
		
	
	}
	

	@Override
	public File getIndexDir(String lang) {
		return new File(RepositoryDirs.GRAPH.getPath(lang));
	}

}
