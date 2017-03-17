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

import it.acubelab.FastBufferedWriter;
import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.config.Config.IndexType;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.acubelab.tagme.preprocessing.TextDataset;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.acubelab.tagme.preprocessing.support.DisambiguationWIDs;
import it.acubelab.tagme.preprocessing.support.IgnoreWIDs;
import it.acubelab.tagme.preprocessing.support.ListPageWIDs;
import it.acubelab.tagme.preprocessing.support.RedirectMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

public class RelatednessValues extends TextDataset {


	Logger log=Logger.getLogger(RelatednessValues.class);

	public RelatednessValues(String lang) {
		super(lang);
	}

	@Override
	protected void parseFile(File mapFile) throws IOException 
	{

		log.info("Loading datasets...");

		ImmutableGraph graph = WikiGraphs.get(lang,IndexType.GRAPH);
		ImmutableGraph in_graph=WikiGraphs.get(lang,IndexType.IN_GRAPH );

		IntSet ignores = new IgnoreWIDs(lang).getDataset();
		IntSet listpages = new ListPageWIDs(lang).getDataset();
		IntSet ids= new AllWIDs(lang).getDataset();
		IntSet disamb= new DisambiguationWIDs(lang).getDataset();
		SimpleSet candidates= new SimpleSet(graph.numNodes());
		int[] intersections=new int[graph.numNodes()];
		for(int i=0;i<intersections.length;i++) intersections[i]=0;


		float logW=(float)Math.log(TagmeConfig.get().getDegree(lang));

		FastBufferedWriter out= new FastBufferedWriter(mapFile);

		PLogger plog=new PLogger(log, Step.MINUTE, "nodes","arcs")
		.setEnd(0, graph.numNodes())
		.start("Computing relatedness values...");
/*
		RelatednessMeasure relate= OnTheFlyArrayMeasure.create(lang);

		for(int i=graph.numNodes()-1;i>=0;i--){
			if(ids.contains(i) && !ignores.contains(i) && !disamb.contains(i) && !listpages.contains(i)){
				for(int j=i;j>=0;j--){
					if(ids.contains(j) && !ignores.contains(j) && !disamb.contains(j) && !listpages.contains(j)){
						float rel=0;
						if((rel=relate.rel(i,j))>0.0f){
							out.appendInt(i);
							out.append(SEP_CHAR);
							out.appendInt(j);
							out.append(SEP_CHAR);
							out.appendFloat(rel);
							out.append("\n");
							plog.update(1);
						}
					}		
				}

			}
		plog.update(0);
		}*/
		int max=0;
		for(int i:ids){
			if(i>max) max=i;
		}
		//System.out.println(max);
		//System.out.println(graph.numNodes());
		
		for(int node=graph.numNodes()-1;node>=0;node--)
		{
			if(!ids.contains(node)) continue;
			if (ignores.contains(node) || listpages.contains(node)) continue;

			LazyIntIterator in_star=in_graph.successors(node);

			int middle_node;
			while((middle_node=in_star.nextInt())!=-1)
			{
				LazyIntIterator out_star=graph.successors(middle_node);
				int n2;
				while((n2=out_star.nextInt())!=-1){
					if(n2<node)
					{
						intersections[n2]++;
						candidates.add(n2);
					}
				}
			}


			float node_size=in_graph.outdegree(node);

			for(int j=0;j<candidates.size;j++)
			{
				int node2=candidates.get(j);
				if(intersections[node2]>=RelatednessMeasure.MIN_INTERSECTION && 
						!ignores.contains(node2) && 
						!listpages.contains(node2)) 
				{
					int node2_size=in_graph.outdegree(node2);

					
					
					float rel =	
					(float) (
					(Math.log(Math.max(node_size,node2_size)) - Math.log(intersections[node2])) 
					/
					(logW - Math.log(Math.min(node_size, node2_size))));

					rel = rel > RelatednessMeasure.THRESHOLD ? 0 : (RelatednessMeasure.THRESHOLD-rel);

					if(rel>0.0f){
					//	System.out.println(node);
						
						out.appendInt(node);
						out.append(SEP_CHAR);
						out.appendInt(node2);
						out.append(SEP_CHAR);
						out.appendFloat(rel);
						out.append("\n");
						plog.update(1);
					}

				}
				intersections[node2]=0;
			}
			plog.update(0);
			candidates.removeAll();
		}
		 


		//		for(int node=graph.numNodes()-1;node>=0;node--){
		//
		//			LazyIntIterator in_star=in_graph.successors(node);
		//
		//
		//			LazyIntIterator out_star=graph.successors(node);
		//			
		//			int n0=-1;
		//			while( (n0=out_star.nextInt())>=0){
		//				intersections[n0]++;
		//				candidates.add(n0);
		//			}
		//			 
		//
		//			int n2=-1;
		//			while( (n2=in_star.nextInt())>=0){
		//			
		//				if(n2<node){
		//					intersections[n2]++;
		//					candidates.add(n2);
		//				}
		//				LazyIntIterator out_star2 = graph.successors(n2);
		//				int n3=-1;
		//				while((n3=out_star2.nextInt())>=0)
		//				{
		//					if (n3<){
		//						intersections[n3]++;
		//						candidates.add(n3);
		//
		//					}
		//				}
		//			}
		//			float node_size=in_graph.outdegree(node);
		//	
		//			for(int j=0;j<candidates.size;j++){
		//				int node2=candidates.get(j);
		//				if(intersections[node2]>=2){
		//					float node2_size=in_graph.outdegree(node2);
		//					float rel = (float)
		//					((Math.log(Math.max(node_size,node2_size)) - Math.log(intersections[node2])) 
		//							/ 
		//					(logW - Math.log(Math.min(node_size, node2_size))));
		//					
		//					rel = rel > THRESHOLD ? 0 : (THRESHOLD-rel)/THRESHOLD;
		//					
		//					if(rel>0.0f){
		//						out.appendInt(node);
		//						out.append(SEP_CHAR);
		//						out.appendInt(node2);
		//						out.append(SEP_CHAR);
		//						out.appendFloat(rel);
		//						out.append("\n");
		//					}
		//					
		//				}
		//				intersections[node2]=0;
		//			}
		//			plog.update();
		//			
		//		candidates.removeAll();
		//		}

		out.flush();
		out.close();
		plog.stop();
		//		log.info("Sorting Relatedness...");
		//
		//		ExternalSort sorter=new ExternalSort();
		//		sorter.setNumeric(true);
		//		sorter.setColumns(new int[]{0,1});
		//		sorter.setInFile(tmp1.getAbsolutePath());
		//		sorter.setOutFile(mapFile.getAbsolutePath());
		//		sorter.run();
		//		tmp1.delete();
		//		
		//		log.info("Sorted. Done.");
		log.info("Done.");
	}


	public static class SimpleSet
	{
		int N;
		boolean[] bitmap;
		int[] elements;
		int size;
		public SimpleSet(int maxSize){
			this.N = maxSize+1;
			this.bitmap = new boolean[N];
			this.elements = new int[N];
			this.size = 0;
			for(int i=0;i<N;i++) bitmap[i] = false;
			//for(int i=0;i<N;i++) elements[i]=-1;
		}
		public boolean add(int i){
			if (bitmap[i]) return false;
			else {
				bitmap[i] = true;
				elements[size]=i;
				size++;
				return true;
			}
		}
		public boolean contains(int i){
			return bitmap[i];
		}
		public int get(int index){
			return elements[index];
		}
		public void removeAll(){
			for(int i=0;i<size;i++)
				bitmap[elements[i]]=false;	
			this.size = 0;
		}



	}

	public static void main(String[] args) throws IOException{
		TagmeConfig.init();
		//File tmp = new RelatednessValues("it").getFile();
		File tmp = new RelatednessValues("en").getFile();
	}
}
