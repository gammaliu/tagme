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
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.io.FileLinesCollection;
import it.unimi.dsi.io.FileLinesCollection.FileLinesIterator;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public final class AnchorTernaryTrie implements Serializable{

	public static final String ENCODING = "US-ASCII";
	private static final long serialVersionUID = 1L;

	private static final class Node implements Serializable
	{
		private static final long serialVersionUID = 1L;

		final byte split;
		Node lo = null;
		Node eq = null;
		Node hi = null;
		Anchor obj = null;

		public Node(byte letter){
			this.split = letter;
		}

		boolean add(byte[] string, int pos, Anchor obj)
		{
			if (string[pos] == split)
			{
				if (pos == string.length-1)
				{
					if (this.obj != null) return false;
					else {
						this.obj = obj;
						return true;
					}
				}
				if (eq == null) eq = new Node(string[pos+1]);
				return eq.add(string, pos+1, obj);
			} else if (string[pos] < split){
				if (lo == null) lo = new Node(string[pos]);
				return lo.add(string, pos, obj);
			} else { //if (string[pos] > split){
				if (hi == null) hi = new Node(string[pos]);
				return hi.add(string, pos, obj);
			}
		}
		int count(){
			int c = 0;
			if (lo != null) c+= lo.count();
			if (eq != null) c+= eq.count();
			if (hi != null) c+= hi.count();
			if (obj != null) c++;
			return c;
		}
	}

	Node head = null;
	int size = 0;
	public AnchorTernaryTrie(){
	}

	public boolean add(String anchor, Anchor obj) throws UnsupportedEncodingException 
	{
		byte[] string = anchor.getBytes(ENCODING);
		if (head == null) head = new Node(string[0]);
		boolean added = head.add(string, 0, obj);
		if (added) size++;
		return added;
	}
	public int size(){
		return size;
	}

	public Anchor search(String anchor) throws UnsupportedEncodingException
	{
		byte[] string = anchor.getBytes(ENCODING);
		return search(string);
	}
	public Anchor search(byte[] string)
	{
		Node current = head;
		for(int i=0; i<string.length; i++)
		{
			while(current!= null && current.split != string[i])
				current = string[i] < current.split  ? current.lo : current.hi;

			if (current == null) return null;
			else if (i < string.length-1) current = current.eq; 
		}

		return current.obj;
	}
	public List<Anchor> searchAll(String anchor) throws UnsupportedEncodingException
	{
		byte[] string = anchor.getBytes(ENCODING);
		ArrayList<Anchor> results = new ArrayList<Anchor>(16);
		Node current = head;
		for(int i=0; i<string.length; i++)
		{
			while(current!= null && current.split != string[i])
				current = current.split < string[i] ? current.hi : current.lo;

			if (current == null) return null;
			if (current.obj != null) results.add(current.obj);
			if (i < string.length-1) current = current.eq;

		}

		return results;
	}

	public static class Searcher extends AnchorSearcher
	{
		AnchorTernaryTrie trie;
		public Searcher(String lang) {
			super(lang);
			trie = DatasetLoader.get(new AnchorTernaryTrieDump(lang));
		}

		@Override
		public int search(TokenizedCharSequence input) 
		{
			Node current = trie.head;
			int found = 0, token = 0;

			for (int i=0; i<input.length(); i++)
			{
				char c = input.charAt(i);
				if (c > UTF16toASCII.MAX_ASCII) throw new IllegalArgumentException("Invalid char in the anchor '"+c+"' at pos "+i);

				byte b = (byte)c;
				while(current!= null && current.split != b)
					current = current.split < b ? current.hi : current.lo;

				if (current == null) return found;

				if (input.isEndToken(i))
				{
					if (current.obj != null){
						input.setAnchorAt(token, current.obj);
						found++;
					}
					token++;
				}
				current = current.eq;
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







	public static HashSet<String> teststrings(String file) throws IOException{
		HashSet<String> strings = new HashSet<String>();
		FileLinesIterator iter = new FileLinesCollection(file, "UTF-8").iterator();
		while(iter.hasNext()) strings.add(iter.next().toString());
		iter.close();
		return strings;
	}
	public static Anchor fake(String w){
		Int2IntMap links = new Int2IntArrayMap();
		links.put(0, w.length());
		return Anchor.build(0, links, w.length(), new IntArraySet());
	}



}
