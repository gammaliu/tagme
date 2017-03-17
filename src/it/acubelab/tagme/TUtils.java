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
package it.acubelab.tagme;


public class TUtils {

	
	
	public static byte Float2Byte(float fl){
		float f = (float) Math.rint(fl*10000)/10000;
		int i =(int) (f*255);
		byte b;
		if(i>127) b= (byte) (i-255);
		else  b= (byte)i;
		return b;
	}
	
	public static float Byte2Float(byte by){
		int i;
		if(by<0) i=(int)by+255;
		else i=(int) by;
		
		float f=i*(1f/255f);
		f = (float) Math.rint(f*10000)/10000;
		return f;
	}
	
	public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}
	
	public static final int byteArrayToInt(byte [] b) {
			return ( b[0] << 24)
                 + ((b[1] & 0xFF) << 16)
                 + ((b[2] & 0xFF) << 8)
                 + ( b[3] & 0xFF);
        }
	
	
}
