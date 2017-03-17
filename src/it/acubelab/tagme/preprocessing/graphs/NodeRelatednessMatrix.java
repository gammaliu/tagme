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

import it.acubelab.Chars;
import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.graphs.RelatednessValues.SimpleSet;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.io.FileLinesCollection;
import it.unimi.dsi.io.FileLinesCollection.FileLinesIterator;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public final class NodeRelatednessMatrix extends Dataset<NodeMatrix>
{
	public static final float MIN_REL = 0.01f;
	public static final int EL_SIZE = 4;
	
	
	public NodeRelatednessMatrix(String lang) {
		super(lang);
	}

	@Override
	protected NodeMatrix parseSet() throws IOException 
	{
		log.info("Creating mapping ID->node ...");
		
		File values = new RelatednessValues(lang).getFile();

		IntSet allWIDs = new AllWIDs(lang).getDataset();
		int max_id=-1;
		for(int id : allWIDs){
			if(id>max_id) max_id=id;
		}
		
		NodeMatrix data = new NodeMatrix();
		data.map = new int[max_id+1];
		for (int i=0; i<data.map.length; i++) data.map[i] = -1;
		
		PLogger plog = new PLogger(log, Step.MINUTE, "values").start();
		FileLinesIterator iter = new FileLinesCollection(values.getAbsolutePath(), "UTF-8").iterator();
		SimpleSet nodes = new SimpleSet(max_id);
		CharSequence[] fields = new CharSequence[3];
		while (iter.hasNext()){
//			CharSequence[] fields = Chars.split(iter.next(), '\t');
			Chars.fields(iter.next(), '\t', fields);
			int a = Chars.parseInt(fields[0]);
			int b = Chars.parseInt(fields[1]);
			nodes.add(a);
			nodes.add(b);
			plog.update();
		}
		plog.stop();
		iter.close();

		for(int n=0; n<nodes.size; n++)
			data.map[nodes.elements[n]]= n;
		
		data.matrix = new byte[nodes.size][];
		
		log.info("Mapping created, "+nodes.size+" nodes.");
		
		
		
		ArrayList<MatrixElement> row = new ArrayList<MatrixElement>();
		
		plog=new PLogger(log, Step.MINUTE, "nodes", "empty")
			.printMemoryStatus(true)
			.setEnd(0, nodes.size)
			.start("Creating Node Relatedness Matrix");
		
		iter = new FileLinesCollection(values.getAbsolutePath(), "UTF-8").iterator();
		
		int curr_wid=-1;
		while(iter.hasNext())
		{
//			CharSequence[] fields = Chars.split(iter.next(), '\t');
			Chars.fields(iter.next(), '\t', fields);
			int wid = Chars.parseInt(fields[0]);
			if (curr_wid >= 0 && curr_wid != wid)
			{
				if (row.size() > 0) {
					int node = data.map[curr_wid];
					if (node < 0) throw new IllegalStateException("WID wid not found! "+node);
					data.matrix[node] = toByteArray(row);
					row.clear();
				} else {
					plog.update(1);
				}
				plog.update(0);
			}
			curr_wid = wid;
			int targetNode = data.map[Chars.parseInt(fields[1])];
			if (targetNode < 0) throw new IllegalStateException("WID wid not found! "+targetNode);
			float rel = Float.parseFloat(fields[2].toString());
			if (rel > MIN_REL) row.add(new MatrixElement(targetNode, rel));
		}
		if (row.size() > 0) {
			int node = data.map[curr_wid];
			if (node < 0) throw new IllegalStateException("WID wid not found! "+node);
			data.matrix[node] = toByteArray(row);
		}
		else plog.update(1);
		plog.update(0);

		iter.close();
				
		plog.stop();
		log.info("Matrix built! Done.");

		
		return data;
	}

	
	private class MatrixElement implements Comparable<MatrixElement>{
		public int node;
		public float rel;

		public MatrixElement(int id,float rel){
			this.node=id;
			this.rel=rel;
		}

		@Override
		public int compareTo(MatrixElement o) {
			return this.node - o.node;
		}
	}
	private static byte[] toByteArray(ArrayList<MatrixElement> array)
	{
		Collections.sort(array);

		byte[] b_array=new byte[array.size()*EL_SIZE];
		for(int i=0;i<array.size();i++)
		{
			MatrixElement a = array.get(i);
			
			byte[] b_int = new byte[] {
	                (byte)(a.node >>> 16),
	                (byte)(a.node >>> 8),
	                (byte) a.node};
			
			b_array[i*EL_SIZE]   = b_int[0];
			b_array[i*EL_SIZE+1] = b_int[1];
			b_array[i*EL_SIZE+2] = b_int[2];
//			b_array[i*EL_SIZE+3] = TUtils.Float2Byte(a.rel);
			float normRel = (a.rel - MIN_REL) / (1f-MIN_REL);
			int intRel =(int) (normRel*255);
			b_array[i*EL_SIZE+3] = intRel>127 ? (byte)(intRel-255) : (byte)intRel;

		}
		return b_array;
	}
	

}
