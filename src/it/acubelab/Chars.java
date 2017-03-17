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
package it.acubelab;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.lang.MutableString;

public class Chars {

	/**
	 * @param c a sequence of chars. Must be in the form "nnn" or "-nnn".
	 * @return the numerical value associated to the char sequence, considered in base 10.
	 * @throws NumberFormatException if the char sequence is not well formed.
	 */
	public static int parseInt(CharSequence c){
		int res = 0;
		int pow = 1;
		boolean negative = false;
		for(int i=c.length()-1; i>=0; i--)
		{
			char n = c.charAt(i);
			int nn=0;
			switch(n){
			case '0': nn=0; break; 
			case '1': nn=1; break;
			case '2': nn=2; break;
			case '3': nn=3; break;
			case '4': nn=4; break;
			case '5': nn=5; break;
			case '6': nn=6; break;
			case '7': nn=7; break;
			case '8': nn=8; break;
			case '9': nn=9; break;
			case '-':
				if (i!=0) throw new NumberFormatException(c.toString());
				negative = true;
				break;
			default: throw new NumberFormatException(c.toString());
			}
			res += pow*nn;
			pow*=10;
		}
		
		return negative? -res : res;
	}

	/**
	 * @param c a sequence of characters.
	 * @return the sequence of chars with leading and trailing spaces removed.
	 */
	public static CharSequence trim(CharSequence c){
		int first = 0;
		int last = c.length()-1;
		while (first<c.length() && Character.isWhitespace(c.charAt(first)))
			first++;
		while (last>=0 && Character.isWhitespace(c.charAt(last)))
				last--;
		last++;
		if (last<first) return c.subSequence(0, 0);
		else return c.subSequence(first, last);
		
	}
	
	/**Splits the input char sequence in fields using char c as delimiter (c is discarded).
	 * @param input
	 * @param c
	 * @param fields
	 */
	public static void fields(CharSequence input, char c, CharSequence[] fields)
	{
		int last = 0, idx = 0;
		for(int i=0; i<input.length(); i++)
		{
			if (input.charAt(i)==c){
				fields[idx] = input.subSequence(last, i);
				last = i+1;
				idx++;
			}
		}
		if (last < input.length())
			fields[idx] = input.subSequence(last, input.length());
	}

	/** Splits the input string in char sequences using char c as delimiter (c is discarded).
	 * @param input
	 * @param c
	 * @return the splitted sub-strings.
	 */
	public static CharSequence[] split (MutableString input, char c){
		
		if (input.indexOf(c)<0) return new CharSequence[]{input};
		
		ObjectArrayList<CharSequence> tokens = new ObjectArrayList<CharSequence>();
		int pos=-1, last=0;
		while((pos=input.indexOf(c, last))>=0)
		{
			if (last<pos)
				tokens.add(input.subSequence(last, pos));
			else 
				tokens.add(new MutableString(""));
			last = pos+1;
		}
		if (last < input.length()) tokens.add(input.subSequence(last, input.length()));
		return tokens.toArray(Chars.EMPTY_STRINGS);
	}

	public static final CharSequence[] EMPTY_STRINGS = new CharSequence[0];

	/**
	 * @param word
	 * @return word with the first character untouched and the others turned to lowercase.
	 */
	public static String capitalizeOnlyFirst(String word)
	{
		MutableString s = new MutableString(word.length());
		s.append(word.charAt(0));
		for(int i=1; i<word.length(); i++)
		{
			char c = word.charAt(i);
			s.append(Character.toLowerCase(c));
		}
		return s.toString();
	}
	
	/** Replace the occurrences of pattern in the input string with the replacement string.
	 * @param pattern
	 * @param input
	 * @param replacement
	 * @return the input string with replaced parts.
	 */
	public static MutableString replace(Pattern pattern, MutableString input, String replacement)
	{
		MutableString output = null;
		Matcher m = pattern.matcher(input);
		int lastEnd = 0;
		while(m.find(lastEnd))
		{
			if (output == null) output = new MutableString(input.length());
//			input.replace(m.start(), m.end(), replacement);
			output.append(input.array(), lastEnd, m.start()-lastEnd);
			output.append(replacement);
			
			lastEnd = m.end();
		}
		if (output != null){
			if (lastEnd < input.length()-1)
				output.append(input.array(), lastEnd, input.length()-lastEnd);
			return output;
		} else return input;
	}

	
	/**
	 * @param str
	 * @return iff the first char of str is uppercase
	 */
	public boolean IsFirstUpperCase(MutableString str){
		return Character.isUpperCase(str.charAt(0));
	}
	
	/** Deletes all occurrences of pattern in the input string.
	 * @param pattern
	 * @param input
	 * @return the input string without the removed parts.
	 */
	public static MutableString delete(Pattern pattern, MutableString input)
	{
		MutableString output = null;
		Matcher m = pattern.matcher(input);
		int lastEnd = 0;
		while(m.find(lastEnd))
		{
			if (output == null) output = new MutableString(input.length());
			//input.delete(m.start(), m.end());
			output.append(input.array(), lastEnd, m.start()-lastEnd);
			lastEnd = m.end();
		}
		if (output != null){
			if (lastEnd < input.length()-1)
				output.append(input.array(), lastEnd, input.length()-lastEnd);
			return output;
		} else return input;
	}
	
	/** Convert a string from UTF-16 to ASCII.
	 * @param input the string to convert.
	 */
	public static void normalizeASCII(MutableString input)
	{
		char[] chars = input.array();
		int len = input.length();
		for(int i=0; i<len; i++)
			chars[i] = UTF16toASCII.MAP[(int)chars[i]];
	}
	/**Convert sequence of characters from UTF-16 to ASCII.
	 * @param input the sequence to convert.
	 * @return the string converted to ASCII
	 */
	public static MutableString toNormalizedASCII(CharSequence input)
	{
		int len = input.length();
		MutableString s = new MutableString(len+1);
		for(int i=0; i<len; i++)
			s.append(UTF16toASCII.MAP[(int)input.charAt(i)]);
		return s;
	}

	//NON SERVE ora c'è il metedo MutableString.squeezeSpaces
//	public static final Pattern P_MULTI_SPACE = Pattern.compile("\\s+");
//	public static MutableString trimMultispace(MutableString input)
//	{
//		return Chars.replace(P_MULTI_SPACE, input, " ").trim();
//	}
	
}
