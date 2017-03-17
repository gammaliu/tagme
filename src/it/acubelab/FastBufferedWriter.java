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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public class FastBufferedWriter extends Writer {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	public static final int DEFAULT_BUFFER_SIZE = 16*1024; //16KB
	
	OutputStreamWriter out;
	char[] buffer;
	int size;
	
	
	public void appendFloat(float number) throws IOException
	{
		write(Float.toString(number));
	}
	public FastBufferedWriter(String filename) throws IOException {
		this(new File(filename));
	}
	public FastBufferedWriter(String filename, int bufferSize) throws IOException {
		this(new File(filename), bufferSize);
	}
	public FastBufferedWriter(File output) throws IOException {
		this(new OutputStreamWriter(new FileOutputStream(output), DEFAULT_CHARSET), DEFAULT_BUFFER_SIZE);
	}
	public FastBufferedWriter(File output, Charset charset) throws IOException {
		this(new OutputStreamWriter(new FileOutputStream(output), charset), DEFAULT_BUFFER_SIZE);
	}
	public FastBufferedWriter(File output, int bufferSize, Charset charset) throws IOException {
		this(new OutputStreamWriter(new FileOutputStream(output), charset), bufferSize);
	}
	public FastBufferedWriter(File output, int bufferSize) throws IOException {
		this(new OutputStreamWriter(new FileOutputStream(output), DEFAULT_CHARSET), bufferSize);
	}
	public FastBufferedWriter(OutputStream output) throws IOException {
		this(new OutputStreamWriter(output, DEFAULT_CHARSET), DEFAULT_BUFFER_SIZE);
	}
	public FastBufferedWriter(OutputStream output, int bufferSize) throws IOException {
		this(new OutputStreamWriter(output, DEFAULT_CHARSET), bufferSize);
	}
	public FastBufferedWriter(OutputStream output, int bufferSize, Charset charset) throws IOException {
		this(new OutputStreamWriter(output, charset), bufferSize);
	}

	public FastBufferedWriter(OutputStreamWriter output) {
		this(output, DEFAULT_BUFFER_SIZE);
	}
	public FastBufferedWriter(OutputStreamWriter output, int bufferSize) {
		out = output;
		buffer = new char[bufferSize];
		size = 0;
	}

	@Override
	public void close() throws IOException {
		flush();
		out.close();
	}

	@Override
	public void flush() throws IOException
	{
		if (size > 0){
			out.write(buffer, 0, size);
			out.flush();
			size = 0;
		}
	}

	public void appendInt(int number) throws IOException
	{
		write(Integer.toString(number));
	}
	public void writeInt(int number) throws IOException
	{
		write(Integer.toString(number));
	}
	@Override
	public void write(char[] cc, int start, int length) throws IOException 
	{
		if (length > buffer.length - size){
			out.write(buffer, 0, size);
			out.flush();
			size = 0;
		}
		
		if (length >= buffer.length){
			out.write(cc, start, length);
			out.flush();
		} else {
			System.arraycopy(cc, start, buffer, size, length);
			size += length;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

}
