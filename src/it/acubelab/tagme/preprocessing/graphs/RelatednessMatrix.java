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

import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.acubelab.tagme.TUtils;
import it.acubelab.tagme.preprocessing.Dataset;
import it.acubelab.tagme.preprocessing.support.AllWIDs;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;


public class RelatednessMatrix extends Dataset<byte[][]>{

	private final float MIN_RELATEDNESS=0f;

	static final Logger log=Logger.getLogger(RelatednessMatrix.class);


	public RelatednessMatrix(String lang) {
		super(lang);
	}

	@Override
	protected byte[][] parseSet() throws IOException {


		IntSet wid=new AllWIDs(lang).getDataset();
		int max_id=-1;
		for(int id:wid){
			if(id>max_id) max_id=id;
		}



		byte[][] matrix = new byte[max_id+1][];
		File values=new RelatednessValues(lang).getFile();
		log.info("Creating Relatedness Matrix");
		//FastBufferedReader reader = new FastBufferedReader(values.getAbsolutePath());
		BufferedReader reader = new BufferedReader(new FileReader(values.getAbsolutePath()));

		//		System.out.println(values.getAbsolutePath());
		//MutableString entry= new MutableString(128);
		int current_node=-1;
		ArrayList<MatrixElement> row=new ArrayList<MatrixElement>();
		String entry=reader.readLine();
		String[] seq=entry.split("\t");
		current_node=Integer.parseInt(seq[0]);

		PLogger plog=new PLogger(log, Step.TEN_MINUTES).printMemoryStatus(true).start();

		while(entry!=null)
		{
			String[] sequence=entry.split("\t");
			int new_node=Integer.parseInt(sequence[0]);
			if(current_node==new_node){
				float rel=Float.parseFloat(sequence[2]);
				if(rel>MIN_RELATEDNESS)
					row.add(new MatrixElement(Integer.parseInt(sequence[1]),rel));
			}else{
				if(row.size()>0)
					matrix[current_node]=toByteArray(row);
				row.clear();
				float rel=Float.parseFloat(sequence[2]);
				if(rel>MIN_RELATEDNESS)
					row.add(new MatrixElement(Integer.parseInt(sequence[1]),rel));
				//row.add(new MatrixElement(Integer.parseInt(sequence[1]),Float.parseFloat(sequence[2])));
				current_node=new_node;
				plog.update();
			}
			entry=reader.readLine();

		}
		plog.stop();
		log.info("Matrix built! Done.");
		return matrix;
	}


	private static byte[] toByteArray(ArrayList<MatrixElement> array){

		Collections.sort(array);

		byte[] b_array=new byte[array.size()*5];
		for(int i=0;i<array.size();i++){
			byte[] b_int=TUtils.intToByteArray(array.get(i).id);
			float rel=array.get(i).rel;
			b_array[i*5]   = b_int[0];
			b_array[i*5+1] = b_int[1];
			b_array[i*5+2] = b_int[2];
			b_array[i*5+3] = b_int[3];
			b_array[i*5+4] = TUtils.Float2Byte(rel);

		}
		return b_array;
	}

	private class MatrixElement implements Comparable<MatrixElement>{
		public int id;
		public float rel;

		public MatrixElement(int id,float rel){
			this.id=id;
			this.rel=rel;
		}

		@Override
		public int compareTo(MatrixElement o) {
			return this.id - o.id;
		}

	}


}
