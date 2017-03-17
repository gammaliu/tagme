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

import it.unimi.dsi.io.FileLinesCollection;
import it.unimi.dsi.io.FileLinesCollection.FileLinesIterator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public final class WordTrie {
	
	private static final int
		INIT_CAPACITY = 8,
		GROW_FACTOR = 2;

	
	private static final class Node
	{
		char[] chars = new char[INIT_CAPACITY];
		Node[] nexts = new Node[INIT_CAPACITY];
		boolean end = false;
		
		void trim()
		{
			int size = computeCurrentSize();
			
			if (size == 0){
				chars = new char[0];
				nexts = new Node[0];
			} if (size<chars.length) {
				
				char[] newChars = new char[size];
				Node[] newNexts = new Node[size];
				System.arraycopy(chars, 0, newChars, 0, size);
				System.arraycopy(nexts, 0, newNexts, 0, size);
				
				this.chars = newChars;
				this.nexts = newNexts;
			}
			
			for(int i=0;i<nexts.length; i++){
				if (nexts[i] == null) break;
				else nexts[i].trim();
			}
		}
		
		private int computeCurrentSize(){
			int size = 0;
			while(size<nexts.length && nexts[size] != null) size++;
			return size;
		}
		
		boolean add(CharSequence string, int pos)
		{
			int size = computeCurrentSize();
			if (pos == string.length()){
				if (end) return false;
				else {
					end = true;
					return true;
				}
			} else {
				int idx = size==0? -1 : Arrays.binarySearch(chars, 0, size, string.charAt(pos));
				if (idx < 0)
				{
					int insert = idx*(-1) - 1;
					if (size == chars.length){
						char[] newChars = new char[(chars.length+1)*GROW_FACTOR];
						Node[] newNexts = new Node[(nexts.length+1)*GROW_FACTOR];
						System.arraycopy(chars, 0, newChars, 0, chars.length);
						System.arraycopy(nexts, 0, newNexts, 0, chars.length);
						chars = newChars;
						nexts = newNexts;
					}
					System.arraycopy(chars, insert, chars, insert+1, size-insert);
					System.arraycopy(nexts, insert, nexts, insert+1, size-insert);
					chars[insert] = string.charAt(pos);
					nexts[insert] = new Node();
						
					return nexts[insert].add(string, pos+1);
				} else {
					return nexts[idx].add(string, pos+1);
				}
			}
		}

	}
	
	protected Node head;
	int size;
	public WordTrie(Iterable<? extends CharSequence> values){
		head = new Node();
		size = 0;
		for(CharSequence v : values)
			if(add(v))
				size++;
		trim();
	}
	public WordTrie(CharSequence... words){
		head = new Node();
		size = 0;
		for(CharSequence v : words)
			if(add(v))
				size++;
		trim();
	}
	WordTrie()
	{
		head = new Node();
		size = 0;
	}
	
	public static WordTrie read(String file) throws IOException
	{
		return read(file, "UTF-8");
	}
	public static WordTrie read(String file, String encoding) throws IOException
	{
		if (new File(file).exists()) {
			FileLinesIterator iter = new FileLinesCollection(file, encoding).iterator();
			WordTrie wt = new WordTrie();
			while (iter.hasNext())
			{
				if (wt.add(iter.next().trim()))
					wt.size++;
			}
			iter.close();
			wt.trim();
			
			return wt;
		} else {
			return null;
		}
		
	}
	
	boolean add(CharSequence string){
		return head.add(string, 0);
	}
	void trim(){
		head.trim();
	}
	public int size(){
		return size;
	}
	public boolean contains(CharSequence word){
		Node current = head;
		for (int i=0;i<word.length();i++){
			int idxNext = Arrays.binarySearch(current.chars, word.charAt(i));
			if (idxNext < 0) return false;
			else current = current.nexts[idxNext];
		}
		return current.end;
	}
	public boolean contains(char[] array, int offset, int len)
	{
		Node current = head;
		for (int i=offset;i<offset+len;i++){
			int idxNext = Arrays.binarySearch(current.chars, array[i]);
			if (idxNext < 0) return false;
			else current = current.nexts[idxNext];
		}
		return current.end;
	}
	
	public CharByCharSearcher getSearcher(){
		return new CharByCharSearcher();
	}
	
	public final class CharByCharSearcher
	{
		Node current = head;
		
		public boolean next(char c){
			if (current == null) return false;
			int idxNext = Arrays.binarySearch(current.chars, c);
			if (idxNext < 0) {
				current = null;
				return false;
			} else {
				current = current.nexts[idxNext];
				return true;
			}
		}
		
		public boolean end(){
			return current != null && current.end;
		}
		
		public void reset(){
			current = head;
		}
		
	}
	
}
