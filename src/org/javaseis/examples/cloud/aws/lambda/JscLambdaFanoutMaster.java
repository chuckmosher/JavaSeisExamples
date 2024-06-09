package org.javaseis.examples.cloud.aws.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.javaseis.util.JsonUtil;

import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;

public class JscLambdaFanoutMaster {
  
  public static void main(String[] args) {
    JscLambdaInput inputFull = new JscLambdaInput();
    inputFull.setBucket("momacmos3");
    inputFull.setPrefix("momacmo/meagerdas/1432_aws_output_filt_5_50_despike");
    inputFull.setRange(1,8640,1,11,11,1);
    inputFull.setNsamp(1250);
    inputFull.setNtrace(380);
    inputFull.setBatchSize(8);

    AWSLambdaAsync lambda = AWSLambdaAsyncClientBuilder.defaultClient();
    List<Future<InvokeResult>> futureList = new ArrayList<Future<InvokeResult>>();

    long t0 = System.currentTimeMillis();
    for (int i=0; i<40; i++) {
      JscLambdaInput input = new JscLambdaInput(inputFull);
      input.setRange((216*i)+1, 216*(i+1), 1, 11, 11, 1 );
      InvokeRequest invokeRequest = new InvokeRequest()
          .withFunctionName("JscLambdaFanoutDriver")
          .withPayload(JsonUtil.toJsonString(input));
      System.out.println("Submit " + i);
      try {
        futureList.add(lambda.invokeAsync(invokeRequest,new JscLambdaAsyncResponse()));
      } catch (ServiceException e) {
        System.out.println(e);
        System.exit(500);
      }
    }
    InvokeUtil.waitForResults(futureList);
    long t1 = System.currentTimeMillis();
    double iorate = 969760.0 * 8640.0 / (t1-t0) / 1024.0;
    System.out.println("Processed 1 day of DAS data in " + 0.001*(t1-t0) + " sec");
    System.out.println("Throughput Rate: " + iorate + " MiB/s");
    System.exit(0);
  }
 
}
