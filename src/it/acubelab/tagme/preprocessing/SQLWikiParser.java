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

import it.acubelab.PLogger;
import it.acubelab.PLogger.Step;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public abstract class SQLWikiParser 
{
	
	public static final int
		NS_ARTICLE = 0,
		NS_CATEGORY = 14,
		PAGE_ID = 0,
		PAGE_NS = 1,
		PAGE_TITLE = 2,
		REDIRECT_ID_FROM = 0,
		REDIRECT_TITLE_TO = 2,
		REDIRECT_NS = 1,
		CATLINKS_ID_FROM = 0,
		CATLINKS_TITLE_TO = 1,
		PAGELINKS_ID_FROM = 0,
		PAGELINKS_NS = 1,
		PAGELINKS_TITLE_TO = 2,
		CAT_ID= 0, 
		CAT_TITLE=1,
		CAT_PAGES=2,
		CAT_SUB_CAT=3,
		CAT_HIDDEN=5;
	public static final String
		NS_ARTICLE_STRING = "0",
		NS_CATEGORY_STRING = "14";
	
   static int readEndLine(InputStreamReader r) throws IOException
    {
        int c = r.read();
        while (c != '\n' && c != -1)
        {
            c = r.read();
        }
        return c;
    }
    static String readToken(InputStreamReader r) throws IOException
    {
        MutableString b = new MutableString();
        int c = r.read();
        while (c != ' ' && c != '\n' && c != -1)
        {
            b.append((char)c);
            c = r.read();
        }
        return b.toString();
    }	
    
	static final int IDLE=0, OPEN=1, STRING=2, ESCAPE=3;
	
    static ArrayList<String> readValues(InputStreamReader r) throws IOException
    {
        int state = IDLE;
        ArrayList<String> values = new ArrayList<String>(10);
        MutableString b = new MutableString();
        int i = r.read();
        while (i != -1 && i != '\n')
        {
            char c = (char)i;
            switch (state)
            {
                case IDLE:
                    if (c == '(') state = OPEN;
                    break;
                case OPEN:
                	if (c == '\'') state = STRING;
                	else if (c == ',') 
                	{
                		values.add(b.toString());
                		b = new MutableString();
                	}
                	else if (c == ')'){
                		values.add(b.toString());
                		return values;
                	}
                	else b.append(c);
                	break;
                case STRING:
                	if (c == '\'') state = OPEN;
                	else if (c == '\\') state = ESCAPE;
                	else b.append(c);
                	break;
                case ESCAPE:
                	b.append(c);
                	state = STRING;
                	break;
            }
            i = r.read();
        }
        return null;
    }
	    

	private PLogger log;
	
	protected SQLWikiParser(Logger log){
		this(log, Step.TEN_MINUTES, new String[0]);
	}
	protected SQLWikiParser(Logger log, String... moreItems){
		this(log, Step.TEN_MINUTES, moreItems);
	}
	protected SQLWikiParser(Logger log, Step step){
		this(log, step, new String[0]);
	}
	protected SQLWikiParser(Logger log, Step step, String... moreItems){
		String[] items = new String[moreItems.length+2];
		items[0] = "scanned"; items[1]="processed";
		for(int i=0;i<moreItems.length; i++) items[i+2] = moreItems[i];
		this.log = new PLogger(log, step, items);
	}
	protected SQLWikiParser(){
		this.log = null;
	}
	
	public int getTotal(){
		return (int)log.counts(0);
	}
	public int getProcessed(){
		return (int)log.counts(1);
	}
	public int getItemCount(int index){
		return (int)log.counts(index+2);
	}
	protected void updateItem(int index){
		log.update(index+2);
	}
	public void compute(InputStreamReader in) throws IOException
	{
		if (log != null) log.start();
		int i = in.read();
		while(i > 0)
		{
			String firstToken = readToken(in);
			if (firstToken.equals("INSERT"))
			{
				ArrayList<String> values = null;
				while ((values=readValues(in)) != null)
				{
					if (log != null) log.update(0);
					boolean proc = compute(values);
					if (log != null && proc) log.update(1);
				}
			}
			else
				i = readEndLine(in);
		}
		finish();
		
	}
	public void compute(File file) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
		compute(reader);
		reader.close();
	}
	public abstract boolean compute(ArrayList<String> values) throws IOException;
	public void finish() {
		if (log != null) log.stop();
	}

}
