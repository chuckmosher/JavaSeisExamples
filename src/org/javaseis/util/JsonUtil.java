
package org.javaseis.util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.javaseis.util.SeisException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JsonUtil {
  /** JSON for persistence */
  private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

  /**
   * Serialize an object to a json string
   * 
   * @param fileParms - object to be serialized
   * @return - json string
   */
  public static String toJsonString(Object parms) {
    return gson.toJson(parms);
  }
  
  /**
   * Serialize an object to a json string
   * 
   * @param fileParms - object to be serialized
   * @return - json string
   */
  public static String toJsonString(Class<?> objClass, Object parms) {
    return gson.toJson(objClass.cast(parms));
  }
  
  /**
   * Convert a jsonString to a JsonElement
   * @param jsonString input string
   * @return JsonElement, null if operation failed
   */
  public static JsonElement parseString( String jsonString ) {
    return new JsonParser().parse(jsonString);
  }

  /**
   * Create an object from a json string
   * 
   * @param parmClass  - class of object to be created
   * @param jsonString - input json string
   * @return - new object of type parmClass, null if operation failed
   */
  public static Object fromJsonString(Class<?> parmClass, String jsonString) {
    return gson.fromJson(jsonString, parmClass);
  }

  /**
   * Serialize an object to a json file
   * 
   * @param fileParms        - object to be serialized
   * @param jsonFilePath - file path for serialized object in json format
   * @return - true if operation succeeded, false otherwise
   */
  public static boolean toJsonFile(Object parms, String jsonFilePath, boolean append) {
    try {
      //gson.toJson(fileParms, new FileWriter(jsonFilePath, append));
      FileWriter fw = new FileWriter(jsonFilePath,append);
      fw.write(gson.toJson(parms));
      fw.close();
    } catch (JsonIOException | IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * Serialize an object to a json file
   * 
   * @param fileParms        - object to be serialized
   * @param jsonFilePath - file path for serialized object in json format
   * @return - true if operation succeeded, false otherwise
   */
  public static void toJsonFile(Object parms, String jsonFilePath) throws SeisException {
    try {
      FileWriter fw = new FileWriter(jsonFilePath);
      fw.write(gson.toJson(parms));
      fw.close();
    } catch (JsonIOException | IOException e) {
     throw new SeisException("Error writing object to path: " + jsonFilePath,e);
    }
  }

  /**
   * Create parameters from a json file
   * 
   * @param parmClass    - class to be constructed
   * @param jsonFilePath - path to json file containing serialized object
   * @return new object of type parmClass, null if operation failed
   * @throws SeisException 
   *
   */
  public static Object fromJsonFile(Class<?> parmClass, String jsonFilePath) throws SeisException {
    try {
      String content = new String ( Files.readAllBytes( Paths.get(jsonFilePath) ) );
      return gson.fromJson(content, parmClass);
    } catch (IOException | JsonSyntaxException | JsonIOException  e) {
      throw new SeisException("Could not load class from file: " + jsonFilePath,e);
    }
  }

  /**
   * Retrieve a field from a Json string representation of an object
   * 
   * @param jsonString - Json string containing object
   * @param value      - name of the field to be retrieved
   * @return - string with value of the field
   */
  public static String getValue(String jsonString, String value) {
    JsonObject jobj = gson.fromJson(jsonString, JsonObject.class);
    JsonElement jel = jobj.get(value);
    if (jel == null)
      return null;
    return jel.toString();
  }

  /**
   * Retrieve a string field from a Json object
   * 
   * @param jobj - Json string containing object
   * @param name - name of the field to be retrieved
   * @return - string with value of the field
   */
  public static String getString(JsonObject jobj, String name) {
    JsonElement jel = jobj.get(name);
    if (jel == null)
      return "null";
    return jel.toString();
  }

  /**
   * Retrieve a long integer field from a Json object
   * 
   * @param jobj - Json string containing object
   * @param name - name of the field to be retrieved
   * @return - integer value of the field
   */
  public static long getLong(JsonObject jobj, String name) {
    JsonElement jel = jobj.get(name);
    if (jel == null)
      return 0;
    return jel.getAsLong();
  }

  /**
   * Retrieve a double field from a Json object
   * 
   * @param jobj - Json string containing object
   * @param name - name of the field to be retrieved
   * @return - double value of the field
   */
  public static double getDouble(JsonObject jobj, String name) {
    JsonElement jel = jobj.get(name);
    if (jel == null)
      return 0;
    return jel.getAsDouble();
  }
  
  /**
   * Retrieve a boolean field from a Json object
   * 
   * @param jobj - Json string containing object
   * @param name - name of the field to be retrieved
   * @return - double value of the field
   */
  public static boolean getBoolean(JsonObject jobj, String name) {
    JsonElement jel = jobj.get(name);
    if (jel == null)
      return false;
    return jel.getAsBoolean();
  }

}