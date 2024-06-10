package org.javaseis.examples.cloud.aws.s3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.javaseis.cloud.properties.JscPropertyAdapter;
import org.javaseis.util.SeisException;
import org.momacmo.parms.JsonUtil;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.fasterxml.jackson.databind.util.ByteBufferBackedOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AwsS3Utils {

  static byte[] xfrBytes = new byte[16384];
  public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

  /**
   * Store an object as a json string in the current JavaSeis AWS-S3 Bucket/Prefix
   * location
   * 
   * @param objectName - name to append to Bucket/Prefix
   * @param filePath   - Path to file to be stored
   * @throws SeisException - on I/O or AWS errors
   */
  public static void putJsonObject(AmazonS3 awsS3, String bucket, String key, Object obj, boolean overwrite)
      throws SeisException {
    if (awsS3.doesObjectExist(bucket, key) == true && overwrite == false)
      throw new SeisException("JsAwsS3 putObject failed, object already exists: s3://" + bucket + "/" + key);
    try {
      byte[] bytes = JsonUtil.toJsonString(obj).getBytes();
      ObjectMetadata om = new ObjectMetadata();
      om.setContentLength(bytes.length);
      InputStream is = new ByteArrayInputStream(bytes);
      awsS3.putObject(bucket, key, is, om);
    } catch (Exception e) {
      throw new SeisException("JsAwsS3 putObject failed for:  s3://" + bucket + "/" + key, e.getCause());
    }
  }

  /**
   * Store an object as a json string in the current JavaSeis AWS-S3 Bucket/Prefix
   * location
   * 
   * @param objectName - name to append to Bucket/Prefix
   * @param filePath   - Path to file to be stored
   * @throws SeisException - on I/O or AWS errors
   */
  public static void putJscObject(AmazonS3 awsS3, String bucket, String key, Object obj, boolean overwrite)
      throws SeisException {
    if (awsS3.doesObjectExist(bucket, key) == true && overwrite == false)
      throw new SeisException("JsAwsS3 putObject failed, object already exists: s3://" + bucket + "/" + key);
    try {
      byte[] bytes = JscPropertyAdapter.gson.toJson(obj).getBytes();
      ObjectMetadata om = new ObjectMetadata();
      om.setContentLength(bytes.length);
      InputStream is = new ByteArrayInputStream(bytes);
      awsS3.putObject(bucket, key, is, om);
    } catch (Exception e) {
      throw new SeisException("JsAwsS3 putObject failed for:  s3://" + bucket + "/" + key, e.getCause());
    }
  }

  /**
   * Store an object as a json string in the current JavaSeis AWS-S3 Bucket/Prefix
   * location
   * 
   * @param objectName - name to append to Bucket/Prefix
   * @param filePath   - Path to file to be stored
   * @throws SeisException - on I/O or AWS errors
   */
  public static Object getJsonObject(AmazonS3 awsS3, String bucket, String key, Class<?> objClass)
      throws SeisException {
    if (awsS3.doesObjectExist(bucket, key) == false)
      throw new SeisException("JsAwsS3 getJsonObject failed, object does not exist: s3://" + bucket + "/" + key);
    try {
      GetObjectRequest gor = new GetObjectRequest(bucket, key);
      S3Object s3o = awsS3.getObject(gor);
      int maxbytes = (int) s3o.getObjectMetadata().getContentLength();
      byte[] bytes = new byte[maxbytes];
      InputStream is = s3o.getObjectContent();
      int len = 0;
      int count = 0;
      while ((len = is.read(bytes, count, Math.min(16384, maxbytes - count))) > 0) {
        count += len;
      }
      com.amazonaws.util.IOUtils.drainInputStream(is);
      s3o.close();
      return JsonUtil.fromJsonString(objClass, new String(bytes));
    } catch (Exception e) {
      throw new SeisException("JsAwsS3 getObject failed for: s3://" + bucket + "/" + key, e);
    }
  }

  /**
   * Copy a local file to an AWS-S3 Bucket/Prefix location
   */
  public static void putFile(AmazonS3 s3handle, String bucket, String key, String filePath, boolean overwrite)
      throws SeisException {
    if (s3handle.doesObjectExist(bucket, key) == false)
      throw new SeisException("JsAwsS3 putFile failed, object already exists s3://" + bucket + "/" + key);
    try {
      File f = new File(filePath);
      InputStream is = new FileInputStream(f);
      ObjectMetadata om = new ObjectMetadata();
      om.setContentLength(f.length());
      s3handle.putObject(bucket, key, is, om);
    } catch (Exception e) {
      e.printStackTrace();
      throw new SeisException("JsAwsS3 putFile failed for file: " + filePath, e.getCause());
    }
  }
  
  /**
   * Copy an AWS-S3 Bucket/Key object to a local file
   */
  public static void getFile(AmazonS3 s3handle, String bucket, String key, String filePath, boolean overwrite)
      throws SeisException {
    if (s3handle.doesObjectExist(bucket, key) == false)
      throw new SeisException("JsAwsS3 getFile failed, object does not exist - s3://" + bucket + "/" + key);
    try {
      File f = new File(filePath);
      OutputStream os = new FileOutputStream(f);
      GetObjectRequest gor = new GetObjectRequest(bucket, key);
      S3Object s3o = s3handle.getObject(gor);
      InputStream is = s3o.getObjectContent();
      int count = 0;
      int len = 0;
      while ((len = is.read(xfrBytes, 0, 16384)) > 0) {
        os.write(xfrBytes, 0, len);
        count += len;
      }
      os.close();
      s3o.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new SeisException("JsAwsS3 getFile failed for file: " + filePath, e.getCause());
    }
  }

  /**
   * Retrieve a text file from an AWS-S3 Bucket/Prefix location
   * 
   * @param s3 - AmazonS3 handle
   * @param awsBucket - bucket name
   * @param awsPrefix - prefix name
   * @param objectName - name of the object to be retrieved
   * @param filePath   - Path to file where results will be stored
   * @throws SeisException - on I/O or AWS errors
   */
  public static void getTextFile(AmazonS3 s3, String awsBucket, String awsPrefix, String objectName, String filePath) throws SeisException {
    String key = awsPrefix + "/" + objectName;
    if (s3.doesObjectExist(awsBucket, key) == false)
      throw new SeisException("Could not find object " + awsBucket + ":" + awsPrefix + "/" + objectName);
    S3Object fileObj = s3.getObject(new GetObjectRequest(awsBucket, key));
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(fileObj.getObjectContent()));
      BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
      String line = null;
      while ((line = reader.readLine()) != null) {
        writer.append(line + "\n");
      }
      writer.close();
      reader.close();
    } catch (Exception e) {
      throw new SeisException("Error while writing to: " + filePath);
    }
  }
  
  public static void putByteBuffer( AmazonS3 s3, String awsBucket, String key, ByteBuffer buf ) throws SeisException {
    try {
      InputStream is = new ByteArrayInputStream( buf.array() );
      ObjectMetadata om = new ObjectMetadata();
      int length = buf.capacity();
      om.setContentLength(length);
      s3.putObject(awsBucket, key, is, om);
    } catch (Exception e) {
      e.printStackTrace();
      throw new SeisException("JsAwsS3 putByteBuffer failed for: " + awsBucket + ":" + key, e.getCause());
    }
  }
  
  public static int getByteBuffer(AmazonS3 s3, String awsBucket, String key, ByteBuffer buf)
      throws SeisException {
    try {
      GetObjectRequest gor = new GetObjectRequest(awsBucket, key);
      S3Object s3o = s3.getObject(gor);
      int length = (int) s3o.getObjectMetadata().getContentLength();
      InputStream is = s3o.getObjectContent();
      int count = 0;
      int len = 0;
      buf.position(0);
      byte[] bytes = buf.array();
      while ((len = is.read(bytes, count, 16384)) > 0)
        count += len;
      s3o.close();
      buf.position(0);
      return count;
    } catch (Exception e) {
      e.printStackTrace();
      throw new SeisException("JsAwsS3 putByteBuffer failed for: " + awsBucket + ":" + key,
          e.getCause());
    }
  }
}
