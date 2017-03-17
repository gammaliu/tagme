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

import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class with a variety of multi-purpose static methods.
 * 
 * @author Claudio Corsi
 * @author Paolo Ferragina
 *
 */
public abstract class ExternalSortUtils {
	
	private static ByteBuffer bb = ByteBuffer.allocateDirect(8);
	
	private static double giga = Math.pow(2, 30);
	private static double mega = Math.pow(2, 20);
	private static double kilo = Math.pow(2, 10);
	
	private static int[] offsets = new int[1000000];	// max 1M cols allowed!		  4 MB
	private static char[] chars = new char[100000];	// max 100.000 chars per String!  200 KB
//	private static byte[] buff = new byte[(int)Math.pow(2, 10) * 16];				// 16 KB
	
	private ExternalSortUtils() {}
	
	public static double round(double val, int precision) {
		return (double)Math.round(val * precision) / precision; 
	}
	
	public static String formatSize(long size) {
		if (size >= giga) return round(size/giga, 10) + "GB";
		if (size >= mega) return round(size/mega, 10) + "MB";
		if (size >= kilo) return round(size/kilo, 10) + "KB";
		return size + " bytes";
	}
	
	public static String elapsedTime(long start, long stop) {
		return elapsedTime(stop - start);
	}
	
	public static String elapsedTime(long msecs) {
		int timeInSeconds = (int)(msecs) / 1000;
		int hours, minutes, seconds;
		hours = timeInSeconds / 3600;
		timeInSeconds = timeInSeconds - (hours * 3600);
		minutes = timeInSeconds / 60;
		timeInSeconds = timeInSeconds - (minutes * 60);
		seconds = timeInSeconds;
		return hours + "h " + minutes + "min " + seconds + "sec";
	}
	public static String elapsedTimeMsec(long msecs) {
		int hours, minutes, seconds;
		hours = (int) (msecs / 3600000);
		msecs = msecs - (hours * 3600000);
		minutes = (int) (msecs / 60000);
		msecs = msecs - (minutes * 60000);
		seconds = (int) (msecs / 1000);
		msecs = msecs - (seconds * 1000);
		return hours + "h " + minutes + "min " + seconds + "sec "+ msecs+"ms";
	}
	
	public static byte[] intToBytes(int val) {
		
	    return new byte[] {
                (byte)(val >>> 24),
                (byte)(val >>> 16),
                (byte)(val >>> 8),
                (byte)val};
	    
	}
	
	public static byte[] longToBytes(long val) {
		bb.clear();
		bb.putLong(val);
		bb.flip();
	    byte[] byteData = new byte[8];
	    bb.get(byteData);
	    
	    return byteData;
	}
	
	public static byte[] merge(byte[] b1, byte[] b2) {

		byte[] buff = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, buff, 0, b1.length);
		System.arraycopy(b2, 0, buff, b1.length, b2.length);
		
		return buff;
	}
	
	public static long bytesToLong(byte[] array) {
		bb.clear();
		bb.put(array);
		bb.flip();
	    
	    return bb.getLong();
	}
	
	public static int bytesToInt(byte[] array) {
		
		int i = (0xff & array[0]) << 24;
		i |= (0xff & array[1]) << 16;
		i |= (0xff & array[2]) << 8;
		i |= (0xff & array[3]);
		return i;
	}
	
	
	
	/**
	 * A fast way to load in memory the content of a file.
	 * 
	 * @param file the file to load
	 * @return the file content in bytes
	 * @throws IOException
	 */
	public static byte[] loadFromDisk(String file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		FileChannel ch = fis.getChannel();
		ByteBuffer buff = ByteBuffer.allocate((int)ch.size());
		ch.read(buff);
		fis.close();
		ch.close();
		return buff.array();
	}
	
	public static String getSortingCols(String line, int[] fields, Pattern p) {
		
		if (fields == null || fields.length == 0) return line;
		
		String[] a = p.split(line);
		StringBuffer sb = new StringBuffer();
		
		int i = 0;
		for(; i < fields.length - 1; i++) {
			sb.append(a[fields[i]]);
			sb.append(p.pattern());
		}
		sb.append(a[fields[i]]);
			
		return sb.toString();
	}
	
	public static String trimLeftZeros(String str) {
		int i = 0;
		for(; i < str.length(); i++) {
			if (str.charAt(i) != '0') break;
		}
		return str.substring(i);
	}
	
	// WARNING: columns size must be less or equals to 100.000 chars!!
	public static String getKey(String line, int[] cols, char sep, boolean numeric) {
		
		if (cols.length == 0) {			
			return (numeric) ? pad(line) : line;
		}
		
		int j = 1;
		offsets[0] = -1;
		for(int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == sep) {
				offsets[j] = i;
				j++;
			}
		}
		offsets[j] = line.length();
		
		int n = 0;
		for(int i = 0; i < cols.length; i++) {
			
			if (cols[i] > j) continue;	// ignore columns out of range
			
			int start = offsets[cols[i]] + 1;
			int stop = cols[i] + 1 > j ? -1 : offsets[cols[i] + 1];
			
			if (numeric) {
				int dim = stop - start;
				if (dim < 20) {	// padding for 20 chars (signed long)
					int padding = 20 - dim;
					int limit = n + padding;
					for(; n < limit; n++) chars[n] = '0'; 
				}
			}
			
			for(int k = start; k < stop; k++) {
				chars[n] = line.charAt(k);
				n++;
			}
			chars[n] = sep;
			n++;
		}
		
		if (n == 0) return "";
		
		//System.out.println(n + " line["+line+"]");
		
		return new String(chars, 0, n-1);
	}
	
	public static String pad(String str) {
		int n = 0;
		for(; n < 20 - str.length(); n++) chars[n] = '0';
		int limit = n + str.length();
		int i = 0;
		for(; n < limit; n++) chars[n] = str.charAt(i++);
		
		return new String(chars, 0, n);
	}
	
	public static long getLong(byte[] buff, int pos) {
		return (
				((long)(buff[pos] & 0xff) << 56) |
				((buff[pos+1] & 0xff) << 48) |
				((buff[pos+2] & 0xff) << 40) |
				((buff[pos+3] & 0xff) << 32) |
				((buff[pos+4] & 0xff) << 24) |
				((buff[pos+5] & 0xff) << 16) |
				((buff[pos+6] & 0xff) <<  8) |
				((buff[pos+7] & 0xff) <<  0));
	}
	
	
	public static long wcl (File f) throws IOException
	{
//		char[] buf = new char[524288];
//		InputStreamReader is = new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8"));
//		int res=-1;
//		int lines = 0;
//		while((res=is.read(buf))>=0){
//			for(int i=0;i<res;i++)
//				if (buf[i]=='\n') lines++;
//		}
//		is.close();
//		return lines;
		
		FastBufferedReader r = null;
		try {
			r = new FastBufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8")));
			MutableString line = new MutableString(2048);
			long sum = 0;
			while(r.readLine(line)!=null)
			{
				if (line.length() == 0) continue;
				sum++;
			}
			r.close();
			return sum;
		} finally {
			try{r.close();}
			catch (Exception e) {}
		}
	}

	public static String memSize(boolean doGc){
		if (doGc){
			System.runFinalization();
			System.gc();
		}
		return String.format(Locale.US, "%d Mb", (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1000000);
	}
	
	public static CharSequence formatFloatArray(float[] data, int precision){
		MutableString buf = new MutableString(data.length*2+1);
		buf.append('[');
		for(int i=0;i<data.length; i++){
			if (i>0) buf.append(',');
			buf.append(String.format("%."+precision+"f", data[i]));
		}
		buf.append(']');
		return buf;
	}
	public static CharSequence formatDoubleArray(double[] data, int precision){
		MutableString buf = new MutableString(data.length*2+1);
		buf.append('[');
		for(int i=0;i<data.length; i++){
			if (i>0) buf.append(',');
			buf.append(String.format("%."+precision+"f", data[i]));
		}
		buf.append(']');
		return buf;
	}
}
