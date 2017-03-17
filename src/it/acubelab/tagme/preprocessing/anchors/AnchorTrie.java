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
package it.acubelab.tagme.preprocessing.anchors;

import it.acubelab.UTF16toASCII;
import it.acubelab.tagme.Anchor;
import it.acubelab.tagme.TagmeParser.TokenizedCharSequence;
import it.acubelab.tagme.preprocessing.DatasetLoader;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * WARNING: After inserctions you HAVE to trim the trie in order to perform searches.
 */
public final class AnchorTrie implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final int 
		INIT_CAPACITY = 8,
		GROW_FACTOR = 2;
	public static final String ENCODING = "US-ASCII";
	
	private static final class Node implements Serializable
	{
		private static final long serialVersionUID = 1L;
		
		byte[] chars = new byte[INIT_CAPACITY];
		Node[] nexts = new Node[INIT_CAPACITY];
		
		Anchor anchor = null;
		
		void trim()
		{
			int size = computeCurrentSize();
			
			if (size == 0) {
				chars = new byte[0];
				nexts = new Node[0];
			} else if (size<chars.length){
				
				byte[] newChars = new byte[size];
				Node[] newNexts = new Node[size];
				System.arraycopy(chars, 0, newChars, 0, size);
				System.arraycopy(nexts, 0, newNexts, 0, size);
				
				this.chars = newChars;
				this.nexts = newNexts;
			}
			
			for(int i=0;i<nexts.length; i++){
				nexts[i].trim();
			}
		}
		
		private int computeCurrentSize(){
			int size = 0;
			while(size<nexts.length && nexts[size] != null ) size++;
			return size;
		}
		
		boolean add(byte[] string, int pos, Anchor obj)
		{
			if (pos == string.length){
				if (anchor != null) return false;
				else {
					anchor = obj;
					return true;
				}
			} else {
				int size = computeCurrentSize();
				int idx = size==0? -1 : Arrays.binarySearch(chars, 0, size, string[pos]);
				if (idx < 0)
				{
					int insert = -idx - 1;
					if (size == chars.length){
						byte[] newChars = new byte[chars.length*GROW_FACTOR];
						Node[] newNexts = new Node[nexts.length*GROW_FACTOR];
						if (insert > 0){
							System.arraycopy(chars, 0, newChars, 0, insert);
							System.arraycopy(nexts, 0, newNexts, 0, insert);
						}
						if (insert < size){
							System.arraycopy(chars, insert, newChars, insert+1, size-insert);
							System.arraycopy(nexts, insert, newNexts, insert+1, size-insert);
						}
						this.chars = newChars;
						this.nexts = newNexts;
					} else {
						if (insert < size) {
							System.arraycopy(chars, insert, chars, insert+1, size-insert);
							System.arraycopy(nexts, insert, nexts, insert+1, size-insert);
						}
					}
					chars[insert] = string[pos];
					nexts[insert] = new Node();
					return nexts[insert].add(string, pos+1, obj);
					
				} else {
					return nexts[idx].add(string, pos+1, obj);
				}
			}
		}
		
		int count(){
			int c = 0;
			for(int i=0;i<nexts.length; i++)
				c+= nexts[i].count();
			
			if (anchor != null) c++;
			return c;
		}

	}
	
	Node head = null;
	int size;
	public AnchorTrie(){
		head = new Node();
		size = 0;
	}
	
	public boolean add(String anchor, Anchor obj) throws UnsupportedEncodingException
	{
		byte[] string = anchor.getBytes(ENCODING);
		boolean added = head.add(string, 0, obj);
		if (added) size++;
		return added;
	}
	public void trim(){
		head.trim();
	}
	public int size(){
		return size;
	}
	
	public Anchor search(String anchor) throws UnsupportedEncodingException
	{
		byte[] string = anchor.getBytes(ENCODING);
		return search(string);
	}
	public Anchor search(byte[] string){
		
		Node current = head;
		for(int i=0; i<string.length; i++)
		{
			if (current.chars.length == 0) return null;
			int idx = Arrays.binarySearch(current.chars, string[i]);
			if (idx < 0) return null;
			
			current = current.nexts[idx];
		}
		
		return current.anchor;
	}
	
	public List<Anchor> searchAll(String anchor)throws UnsupportedEncodingException
	{
		byte[] string = anchor.getBytes(ENCODING);
		return searchAll(string);
	}
	public List<Anchor> searchAll(byte[] string)
	{
		ArrayList<Anchor> occurrences = new ArrayList<Anchor>();
		Node current = head;
		for(int i=0; i<string.length; i++)
		{
			if (current.chars.length == 0) break;
			int idx = Arrays.binarySearch(current.chars, string[i]);
			if (idx < 0) break;
			
			if (current.anchor != null) occurrences.add(current.anchor);
			current = current.nexts[idx];
		}
		if (current.anchor != null) occurrences.add(current.anchor);
		
		return occurrences;
	}
	
	public static final class Searcher extends AnchorSearcher
	{
		AnchorTrie trie;
		public Searcher(String lang) {
			super(lang);
			trie = DatasetLoader.get(new AnchorTrieDump(lang));
		}

		@Override
		public int search(TokenizedCharSequence input) 
		{
			Node current = trie.head;
			int found = 0, token = 0;
			
			for (int i=0; i<input.length(); i++)
			{
				char c = input.charAt(i);
				if (c > UTF16toASCII.MAX_ASCII) throw new IllegalArgumentException("Invalid char in the anchor '"+c+"' at post "+i);
				byte b = (byte)c;
				int idx = Arrays.binarySearch(current.chars, b);
				if (idx < 0) return found;
				current = current.nexts[idx];
				
				if (input.isEndToken(i))
				{
					if (current.anchor != null) {
						input.setAnchorAt(token, current.anchor);
						found++;
					}
					token++;
				}
			}
			return found;
		}

		@Override
		public Anchor search(CharSequence input) {
			byte[] string = new byte[input.length()];
			for(int i=0;i<string.length; i++){
				char c = input.charAt(i);
				if (c > UTF16toASCII.MAX_ASCII) throw new IllegalArgumentException("Invalid char in the anchor '"+c+"' at pos "+i);
				string[i] = (byte)c;
			}
			return trie.search(string);
		}
		public int size(){
			return trie.size;
		}
		
		public int count(){
			return trie.head.count();
		}



	}

	
	

}
