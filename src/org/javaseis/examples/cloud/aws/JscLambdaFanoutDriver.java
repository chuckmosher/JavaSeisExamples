package org.javaseis.examples.cloud.aws;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.javaseis.util.JsonUtil;

import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * JavaSeis Cloud Driver for lambda "fan-out" pattern
 * This lambda function triggers a "batch" of requests to process data from an
 * existing JavaSeis Cloud dataset. 
 * @author chuck@momacmo.org
 *
 */
public class JscLambdaFanoutDriver implements RequestHandler<Map<String, String>, String> {
  public static final int MAX_BATCH_SIZE = 1000;
  @Override
  public String handleRequest(Map<String, String> event, Context context) {
    // Get logger and input for the request
    LambdaLogger logger = context.getLogger();
    logger.log(JsonUtil.toJsonString(event));    
    JscLambdaInput input = new JscLambdaInput(event);
    // Set batch size to 10 if it is zero or smaller, and restrict to a max value
    int batchSize = Math.min((input.batchSize < 1 ? 10 : input.batchSize),MAX_BATCH_SIZE);
    // Log info about the request
    logger.log(this.getClass().getCanonicalName() + "::handleRequest\n" + Instant.now());
    logger.log("Input request:\n" + JsonUtil.toJsonString(input));
    // Calculate the total number of frameArrays in the request
    int nframe = 1 + (input.frmn - input.frm0) / input.frmi;
    // Set the number of batches
    int nbatch = nframe / batchSize;
    if (batchSize * nbatch != nframe)
      nbatch++;
    int frm0, frmn, frmi;
    frmi = input.frmi;
    logger.log("Total number of frameArrays: " + nframe);
    logger.log("Frames per batch: " + batchSize);
    logger.log("Number of batches: " + nbatch);
    boolean waitForCompletion = true;
    String str = System.getenv("WAIT_FOR_COMPLETION");
    logger.log("Wait for completion: " + str);
    if (str != null && str.equals("false")) waitForCompletion = false;   
    logger.log("Begin submission ... ");
    // Get an async lambda client and create array of futures to track results
    AWSLambdaAsync lambda = AWSLambdaAsyncClientBuilder.defaultClient();
    List<Future<InvokeResult>> futureList = new ArrayList<Future<InvokeResult>>();
    JscLambdaOutputList outputList = new JscLambdaOutputList(input);
    StringBuffer buf = new StringBuffer();
    // Loop over the number of batches
    for (int i = 0; i < nbatch; i++) {
      // Set the frame range for this submission
      frm0 = input.frm0 + batchSize * frmi * i;
      frmn = Math.min(input.frmn, frm0 + (batchSize - 1) * frmi);
      JscLambdaInput tmp = new JscLambdaInput(input);
      tmp.setRange(frm0, frmn, frmi, input.vol0, input.voln, input.voli);
      // Submit the request
      InvokeRequest req = new InvokeRequest().withFunctionName("JscProcessingWorker")
        .withPayload(JsonUtil.toJsonString(tmp));
      futureList.add(lambda.invokeAsync(req));
      if (waitForCompletion == false) outputList.addFromInput(tmp,"Submitted"); 
      String msg = (" Batch " + (i + 1) + " Frame Range: " + frm0 + ", " + frmn + ", " + frmi);
      logger.log("Submitted: " + msg);
      buf.append(msg + "\n");
    }
    // Wait for completion if requested
    
    if (waitForCompletion == false) return(JsonUtil.toJsonString(outputList));
    
    List<InvokeResult> resultList = InvokeUtil.waitForResults(futureList, logger);
    for (InvokeResult result : resultList ) {
      String outputString = new String(result.getPayload().array(), StandardCharsets.UTF_8).replaceAll("\\\\n","").replaceAll("\\\\",""); 
      logger.log("Completed:\n" + outputString);
      JscLambdaOutputList partialList = (JscLambdaOutputList) JsonUtil.fromJsonString(JscLambdaOutputList.class, outputString.substring(1, outputString.length()-1));
      outputList.add(partialList);       
    }
    
    return(JsonUtil.toJsonString(outputList));
  }
  
  public static void main(String[] args) {
    JscLambdaInput input = new JscLambdaInput();
    input.setBucket("momacmos3");
    input.setPrefix("momacmo/meagerdas/1432_aws_output_filt_5_50_despike");
    input.setRange(401,410,1,11,11,1);
    input.setNsamp(1250);
    input.setNtrace(380);
    input.setBatchSize(5);

    InvokeRequest invokeRequest = new InvokeRequest().withFunctionName("JscLambdaFanoutDriver")
        .withPayload(JsonUtil.toJsonString(input));
    
    try {
      AWSLambdaAsync lambda = AWSLambdaAsyncClientBuilder.defaultClient();
      Future<InvokeResult> future = lambda.invokeAsync(invokeRequest);
      InvokeResult result = InvokeUtil.waitForResult( future );
      // write out the return value
      String rawOutput = Charset.defaultCharset().decode(result.getPayload()).toString();     
      System.out.println("Raw output:\n" + rawOutput);
      String outputString = rawOutput.replaceAll("\\\\n","").replaceAll("\\\\","");
      System.out.println("Processed output:\n" + outputString);
      JscLambdaOutputList output = (JscLambdaOutputList) JsonUtil.fromJsonString(JscLambdaOutputList.class, outputString.substring(1,outputString.length()-1));
      System.out.println("JscLambdaOutputList:\n" + JsonUtil.toJsonString(output));
      System.out.println("I/O Rate (KiB/s) " + 1e-3*output.getIoRate());
      System.exit(200);
    } catch (ServiceException e) {
      System.out.println(e);
      System.exit(500);
    }
  }
}