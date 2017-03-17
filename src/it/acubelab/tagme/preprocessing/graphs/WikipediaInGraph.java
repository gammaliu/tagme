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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import it.acubelab.Chars;
import it.acubelab.ExternalSort;
import it.acubelab.FastBufferedWriter;
import it.acubelab.PLogger;
import it.acubelab.tagme.config.Config.IndexType;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.Indexer;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.BVGraph;


public class WikipediaInGraph extends Indexer {

	static Logger log=Logger.getLogger(WikipediaInGraph.class);
	final static String NAME="in_graph";	 


	public WikipediaInGraph() {

	}

	@Override
	public void makeIndex(String lang, File workingDir) throws IOException {

		log.info("Perform a in graph build for language "+lang);
		log.info("Loading edges...");

		File edges=new WikipediaEdges(lang).getFile();

		log.info("Storing optimized graph");
		FastBufferedReader in = new FastBufferedReader(new InputStreamReader(new FileInputStream(edges)));
		File tmp1=Dataset.createTmpFile();
		FastBufferedWriter out = new FastBufferedWriter(tmp1);
		PLogger plog= new PLogger(log).start("Reversing edges...");

		MutableString buffer = new MutableString(256);
		while(in.readLine(buffer)!=null){
			CharSequence[] nodes=Chars.split(buffer,'\t');
			out.append(nodes[1]);
			out.append('\t');
			out.append(nodes[0]);
			out.append('\n');
			plog.update();
		}
		plog.stop();
		in.close();
		out.close();
		log.info("Sorting...");
		File tmp2=Dataset.createTmpFile();

		ExternalSort sorter = new ExternalSort();
		sorter.setInFile(tmp1.getAbsolutePath());
		sorter.setOutFile(tmp2.getAbsolutePath());
		sorter.setNumeric(true);
		sorter.setColumns(new int[]{0,1});
		sorter.run();

		log.info("Storing optmized graph...");
		ProgressLogger logger;
		ArcListASCIIGraph tmp_graph = ArcListASCIIGraph.loadOnce(new FileInputStream(tmp2.getAbsolutePath()));
		logger=new ProgressLogger(log,ProgressLogger.TEN_SECONDS);
		BVGraph.store(tmp_graph, new File(workingDir,NAME).getAbsolutePath(),logger);

		log.info("Graph built");
		tmp1.delete();
		tmp2.delete();

	}

	@Override
	public File getIndexDir(String lang) {
		return new File(RepositoryDirs.IN_GRAPH.getPath(lang));
	}
	public static String getGraphBaseName(String lang){
		File graphDir = getDir(RepositoryDirs.GRAPH, lang);
		File path = new File(graphDir, NAME);
		return path.getAbsolutePath();
	}

}
