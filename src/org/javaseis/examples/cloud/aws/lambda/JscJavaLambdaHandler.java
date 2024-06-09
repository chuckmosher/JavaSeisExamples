package org.javaseis.examples.cloud.aws.lambda;

import java.io.InputStream;
import java.util.Map;

import org.javaseis.compress.TraceCompressor;
import org.javaseis.properties.DataFormat;
import org.javaseis.util.SeisException;
import org.javaseis.util.JsonUtil;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

// Handler value: example.Handler
public class JscJavaLambdaHandler implements RequestHandler<Map<String, String>, String> {

  @Override
  public String handleRequest(Map<String, String> event, Context context) {
    LambdaLogger logger = context.getLogger();
    JscLambdaInput input = new JscLambdaInput(event);
    logger.log("Lambda Function Inovked: " + this.getClass().getCanonicalName() + "::handleRequest");
    logger.log(JsonUtil.toJsonString(input));
    JscLambdaOutputList outputList = new JscLambdaOutputList(input);
    AmazonS3 s3 = null;
    try {
      s3 = AmazonS3ClientBuilder.standard().build();
    } catch (Exception e) {
      e.printStackTrace();
      logger.log("Could not obtain S3 client");
      return outputList.toString();
    }
    byte[] trcBytes = null;
    try {
      trcBytes = new byte[input.ntrace * TraceCompressor.getRecordLength(DataFormat.COMPRESSED_INT16, input.nsamp)];
    } catch (SeisException e) {
      e.printStackTrace();
      logger.log("Could not allocate trace buffer");
      return outputList.toString();
    }
    getFrameRange( s3, input, outputList, trcBytes );
    logger.log("Lambda Function Completed: " + this.getClass().getCanonicalName() + "::handleRequest");
    logger.log("JscLambdaOutputList:\n" + JsonUtil.toJsonString(outputList));
    return outputList.toString();
  }

  public static JscLambdaOutput getFrameTraces(AmazonS3 s3, JscLambdaInput input, int frame, int volume,
      byte[] trcBytes) {
    JscLambdaOutput output = new JscLambdaOutput(frame, volume);
    String key = input.prefix + "/Traces" + "/V" + volume + "/F" + frame;
    if (s3.doesObjectExist(input.bucket, key) == false) {
      output.setStatus("Failure: Object not found: " + input.bucket + "/" + key);
      return output;
    }
    int traceCount = 0;
    int count = 0;
    int maxbytes = trcBytes.length;
    long tms = System.currentTimeMillis();
    try {
      GetObjectRequest gor = new GetObjectRequest(input.bucket, key);
      S3Object s3o = s3.getObject(gor);
      traceCount = Integer.parseInt(s3o.getObjectMetadata().getUserMetaDataOf("traceCount"));
      System.out.println("TraceCount " + traceCount);
      InputStream is = s3o.getObjectContent();
      int len = 0;
      while ((len = is.read(trcBytes, count, Math.min(16384, maxbytes - count))) > 0) {
        count += len;
      }
      com.amazonaws.util.IOUtils.drainInputStream(is);
      s3o.close();
      output.setStatus("Success");
      tms = System.currentTimeMillis() - tms;
      output.traceCount = traceCount;
      output.iobytes = count;
      output.iotime = 0.001f * tms;
    } catch (Exception e) {
      e.printStackTrace();
      output.setStatus("Failure: " + e.getMessage());
    }
    return output;
  }
  
  public static void getFrameRange(AmazonS3 s3, JscLambdaInput input, JscLambdaOutputList outputList,
      byte[] trcBytes) {
    for (int volume = input.vol0; volume <= input.voln; volume += input.voli) {
      for (int frame = input.frm0; frame <= input.frmn; frame += input.frmi) {
        JscLambdaOutput output = getFrameTraces(s3, input, frame, volume, trcBytes);
        outputList.add(output);
      }
    }
  }
}