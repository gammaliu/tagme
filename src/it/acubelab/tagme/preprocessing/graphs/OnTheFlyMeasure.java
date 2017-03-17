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

import java.io.IOException;

import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.IndexType;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.acubelab.tagme.preprocessing.support.DisambiguationWIDs;
import it.acubelab.tagme.preprocessing.support.IgnoreWIDs;
import it.acubelab.tagme.preprocessing.support.ListPageWIDs;
import it.acubelab.tagme.preprocessing.support.PeopleWIDs;
import it.acubelab.tagme.preprocessing.support.RedirectMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

public class OnTheFlyMeasure extends RelatednessMeasure{

	ImmutableGraph graph=null;
	float logW;


	public OnTheFlyMeasure(String lang){
		super(lang);
		graph=WikiGraphs.get(lang,IndexType.IN_GRAPH);
		logW=(float)Math.log(TagmeConfig.get().getDegree(lang));

	}

	protected int[] edges(int node, ImmutableGraph graph){
		int[] edges = new int[graph.outdegree(node)];
		LazyIntIterator iter = graph.successors(node);
		for(int i=0;i<edges.length;i++) edges[i] = iter.nextInt();
		return edges;
	}

	public float rel(int a,int b)
	{
		if (a==b) return 1f;
		float rel;
		int[] edgesA = edges(a,graph);
		int[] edgesB = edges(b,graph);
		if (edgesA.length==0 || edgesB.length==0) rel = 0;
		else {
			int intersection=0;
			int ia=0, ib =0;
			int nextA=edgesA[0], nextB=edgesB[0];
			while(true){
				if (ia>=edgesA.length-1 || ib>=edgesB.length-1)
					break;
				else if (nextA<nextB){
					nextA=edgesA[++ia];
				} else if (nextB<nextA){
					nextB=edgesB[++ib];
				} else {
					intersection++; //intersection
					nextA=edgesA[++ia];
					nextB=edgesB[++ib];
				}
			}
			if (intersection < MIN_INTERSECTION) rel = 0;
			else {
				//WITTEN STANDARD
				rel =(float)
						((Math.log(Math.max(edgesA.length,edgesB.length)) - Math.log(intersection))
								/
								(logW - Math.log(Math.min(edgesA.length,edgesB.length))));
				rel = rel > THRESHOLD ? 0 : (THRESHOLD-rel);
			}
		}


		return rel;

	}

}
