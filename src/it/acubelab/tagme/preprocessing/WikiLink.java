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

import it.acubelab.Chars;
import it.unimi.dsi.lang.MutableString;

import java.util.regex.Pattern;

public class WikiLink
{
	public String anchor;
	public String page;
	public static final WikiLink EMPTY = new WikiLink("","");
	static Pattern multiDot = Pattern.compile("\\.{4,}");
	static Pattern multiSpace = Pattern.compile("\\s{2,}");
	
	
	public WikiLink(MutableString anchor, MutableString page) {
		this(anchor.toString(), page.toString());
	}
	public WikiLink(String anchor, String page) {
		this.anchor = anchor;
		this.page = page;
	}
	public static MutableString cleanAnchor(MutableString a)
	{
		//handle some errors
		a.replace('\t',' ');
		a.replace(" ,",",").replace(" .",".");
		a.replace(" ;",";").replace(" :",":");
		a.replace(",,",",").replace(",.",".");
		a.replace("( ","(").replace(" )",")");
		a.replace("[ ","[").replace(" ]","]");
		a.replace("« ","«").replace(" »","»");
		a = Chars.replace(multiSpace, a, " ");
		a = Chars.replace(multiDot, a, "...");
		
		
		return a;
	}
	public static WikiLink parse(MutableString link)
	{
		if (link.indexOf(':') >= 0) return EMPTY;
		
		link = link.replace('\n', ' ').replace('\t', ' ');
		CharSequence[] tokens = Chars.split(link, '|');
		
		if (tokens.length == 0 || tokens.length > 2) return EMPTY;
		
		MutableString firstToken = new MutableString(tokens[0]).trim();
		if (firstToken.length() == 0) return EMPTY;

		MutableString secondToken;
		MutableString page;
		MutableString anchor;
		if (tokens.length > 1 && (secondToken=new MutableString(tokens[1]).trim()).length() > 0)
		{
			page = firstToken;
			if (page.indexOf('#') >= 0){
				CharSequence[] sections = Chars.split(page,'#');
				if (sections.length==0) return WikiLink.EMPTY; 
				page = new MutableString(sections[0]);
			}
			anchor = secondToken;
		} else {
			page = firstToken;
			anchor = new MutableString(page);
			if (page.indexOf('#') >= 0){
				CharSequence[] sections = Chars.split(page,'#');
				if (sections.length==0) return WikiLink.EMPTY;
				page = new MutableString(sections[0]);
				anchor = new MutableString(sections[sections.length-1]);
			}
		}
		page = WikiTextExtractor.normalizePageName(page);
		anchor = cleanAnchor(anchor);
		if (anchor.length() == 0 || page.length() == 0) return WikiLink.EMPTY;
		return new WikiLink(anchor, page);
	}
	@Override
	public String toString(){
		return anchor +"->"+page;
	}
}
