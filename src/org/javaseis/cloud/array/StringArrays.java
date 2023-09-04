package org.javaseis.cloud.array;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Static methods for converting a string to an array of primitive elements
 */
public class StringArrays {

  static String separator = ",";
  static char startChar = ' ';
  static char endChar = ' ';
  String sep = separator;
  char schar = startChar;
  char echar = endChar;

  /**
   * Default constructor using default character separators.
   */
  public StringArrays() {
  }

  /**
   * Constructor specifying character separators.
   * @param sep separator between array elements in the string, i.e. , ;
   * @param start character used to delimit start of string, i.e. "{('
   * @param end character used to delimit end of string, i.e. "})'
   */
  public StringArrays( String sep, char start, char end ) {
    separator = sep;
    startChar = start;
    endChar = end;
  }

  /**
   * Convert a string to an array of strings using default character separators
   * @param s input string to be converted to an array
   * @return array of strings broken out from the input string
   */
  public static String[] stringToStringArray( String s ) {
  	return stringToArray( s, separator, startChar, endChar );
  }
  
  /**
   * Convert a string to list of strings using default character separators
   * @param s input string to be converted to an array
   * @return list of strings broken out from the input string
   */
  public static ArrayList<String> stringToList( String s ) {
  	String[] buf = stringToArray( s, separator, startChar, endChar );
  	ArrayList <String> al = new ArrayList<String>( buf.length );
  	for (int i=0; i<buf.length; i++) {
  		al.add(buf[i]);
  	}
  	return al;
  }
  
  /**
   * Convert a string to an array of integers using default character separators
   * @param s input string to be converted to an array
   * @return array of integers broken out from the input string
   */
  public static int[] stringToIntArray( String s ) {
    String[] buf = stringToArray( s, separator, startChar, endChar );
    if (buf.length == 0)
      return (int [])null;
    int[] ints = new int[buf.length];
    for (int i=0; i<buf.length; i++) {
      ints[i] = Integer.parseInt(buf[i].trim());
    }
    return ints;
  }
  
  /**
   * Convert a string to an array of long integers using default character separators
   * @param s input string to be converted to an array
   * @return array of longs broken out from the input string
   */
  public static long[] stringToLongArray( String s ) {
    String[] buf = stringToArray( s, separator, startChar, endChar );
    if (buf.length == 0)
      return (long [])null;
    long[] longs = new long[buf.length];
    for (int i=0; i<buf.length; i++) {
      longs[i] = Integer.parseInt(buf[i].trim());
    }
    return longs;
  }

  /**
   * Convert a string to an array of floats using default character separators
   * @param s input string to be converted to an array
   * @return array of floats broken out from the input string
   */
  public static float[] stringToFloatArray( String s ) {
    String[] buf = stringToArray( s, separator, startChar, endChar );
    if (buf.length == 0)
      return (float [])null;
    float[] floats = new float[buf.length];
    for (int i=0; i<buf.length; i++) {
      floats[i] = Float.parseFloat(buf[i].trim());
    }
    return floats;
  }

  /**
   * Convert a string to an array of doubles using default character separators
   * @param s input string to be converted to an array
   * @return array of doubles broken out from the input string
   */
  public static double[] stringToDoubleArray( String s ) {
    String[] buf = stringToArray( s, separator, startChar, endChar );
    if (buf.length == 0)
      return (double [])null;
    double[] doubles = new double[buf.length];
    for (int i=0; i<buf.length; i++) {
      doubles[i] = Double.parseDouble(buf[i].trim());
    }
    return doubles;
  }

  /**
   * Convert a string to an array of strings based on character separators
   * @param s input string to be converted to an array
   * @param sep separator between array elements in the string, i.e. , ;
   * @param start character used to delimit start of string, i.e. "{('
   * @param end character used to delimit end of string, i.e. "})'
   * @return array of strings broken out from the input string
   **/
  public static String[] stringToArray( String s, String sep, char start, char end ) {
    int i1 = 0;
    if ( s.charAt( i1 ) == start ) {
      i1++;
    }
    int i2 = s.length();
    if ( s.charAt( i2 - 1 ) == end ) {
      i2--;
    }
    if ( i2 <= i1 ) {
      return null;
    }
    StringBuffer buf = new StringBuffer( s.substring( i1, i2 ) );
    int arraysize = 1;
    for ( int i = 0; i < buf.length(); i++ ) {
      if ( sep.indexOf( buf.charAt( i ) ) != -1 ) {
        arraysize++;
      }
    }
    String[] elements = new String[arraysize];
    int y, z = 0;
    if ( buf.toString().indexOf( sep ) != -1 ) {
      while ( buf.length() > 0 ) {
        if ( buf.toString().indexOf( sep ) != -1 ) {
          y = buf.toString().indexOf( sep );
          if ( y != buf.toString().lastIndexOf( sep ) ) {
            elements[z] = buf.toString().substring( 0, y );
            z++;
            buf.delete( 0, y + 1 );
          }
          else if ( buf.toString().lastIndexOf( sep ) == y ) {
            elements[z] = buf.toString().substring( 0,
              buf.toString().indexOf( sep ) );
            z++;
            buf.delete( 0, buf.toString().indexOf( sep ) + 1 );
            elements[z] = buf.toString();
            z++;
            buf.delete( 0, buf.length() );
          }
        }
      }
    }
    else {
      elements[0] = buf.toString();
    }
    buf = null;
    return elements;
  }
  
  public static String arrayToString(int[] vals) {
    String s = Arrays.toString(vals);
    return s.substring(1,s.length()-1);
  }
  
  public static String arrayToString(long[] vals) {
    String s = Arrays.toString(vals);
    return s.substring(1,s.length()-1);
  }
  
  public static String arrayToString(float[] vals) {
    String s = Arrays.toString(vals);
    return s.substring(1,s.length()-1);
  }
  
  public static String arrayToString(double[] vals) {
    String s = Arrays.toString(vals);
    return s.substring(1,s.length()-1);
  }
  
}
