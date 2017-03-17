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

import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

public class WikipediaArticleParser {

	static Logger log = Logger.getLogger(WikipediaArticleParser.class);
	
	private enum State {
		IDLE,
		PAGE,
		TEXT
	}
	
	public static final String
		PAGE_START = "<page>",
		PAGE_END = "</page>",
		ID_START = "<id>",
		ID_END = "</id>",
		TITLE_START = "<title>",
		TITLE_END = "</title>",
		TEXT_START = "<text xml:space=\"preserve\">",
		TEXT_END = "</text>";
	
	
	File input;
	FastBufferedReader reader;
	WikiArticle current;
	State state;
	
	public void parse(File f) throws IOException
	{
		this.input = f;
		start();
		
		MutableString line = new MutableString(1024);
		
		while(reader.readLine(line) != null)
		{
			line.trim();
			switch(state)
			{
			case IDLE: 
				if (line.startsWith(PAGE_START)){
					state = State.PAGE;
					current = new WikiArticle();
				}
				break;
			case PAGE:
				if (line.startsWith(TITLE_START) && line.endsWith(TITLE_END) && current.title().length() == 0){
					String title = line.substring(TITLE_START.length(), line.length()-TITLE_END.length()).toString();
					current.setTitle(title);
				} else if (line.startsWith(ID_START) && line.endsWith(ID_END) && current.id() < 0){
					String id = line.substring(ID_START.length(), line.length()-ID_END.length()).toString();
					current.setId(Integer.parseInt(id));
				} else if (line.startsWith(TEXT_START)) {
					if (line.endsWith(TEXT_END)){
						current.setBody(line.substring(TEXT_START.length(), line.length()-TEXT_END.length()));
					} else {
						current.setBody(new MutableString(2048));
						current.body().append(line.substring(TEXT_START.length()));
						current.body().append('\n');
						state = State.TEXT;
					}
				} else if (line.startsWith(PAGE_END)){
					processArticle(current);
					state = State.IDLE;
				}
				break;
			case TEXT:
				if (line.endsWith(TEXT_END)){
					state = State.PAGE;
					current.body().append(line.substring(0, line.length()-TEXT_END.length()));
				} else {
					current.body().append(line);
					current.body().append('\n');
				}
				break;
			}
		}
		
		stop();
		
	}
	
	protected void start() throws IOException
	{
		reader = new FastBufferedReader(new InputStreamReader(new FileInputStream(input), Charset.forName("UTF-8")));
		state = State.IDLE;
	}
	protected void stop() throws IOException
	{
		reader.close();
	}
	public void processArticle(WikiArticle a) throws IOException
	{}
	

	
}
