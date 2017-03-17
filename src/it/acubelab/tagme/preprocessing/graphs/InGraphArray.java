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

import it.acubelab.tagme.config.Config.IndexType;
import it.acubelab.tagme.preprocessing.Dataset;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

import java.io.IOException;

public class InGraphArray extends Dataset<int[][]> {

	public InGraphArray(String lang) {
		super(lang);
	}

	@Override
	protected int[][] parseSet() throws IOException 
	{
		
		ImmutableGraph in_graph = WikiGraphs.get(lang, IndexType.IN_GRAPH);
		int[][] arrayGraph = new int[in_graph.numNodes()][];
		for(int i=0; i<arrayGraph.length; i++)
		{
			int i_deg = in_graph.outdegree(i);
			if (i_deg > 0){
				arrayGraph[i] = new int[i_deg];
				LazyIntIterator iter = in_graph.successors(i);
				for (int j=0; j<arrayGraph[i].length; j++)
					arrayGraph[i][j] = iter.nextInt();
			}
		}

		return arrayGraph;
	}

}
