package org.javaseis.examples.cloud.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.javaseis.util.JsonUtil;

import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

public class JscIOBenchmark {

  public static void main(String[] args) {
    JscLambdaInput input = new JscLambdaInput();
    input.setBucket("momacmos3");
    input.setPrefix("momacmo/meagerdas/1432_aws_output_filt_5_50_despike");
    input.setRange(1, 1, 1, 11, 11, 1);
    input.setNsamp(1250);
    input.setNtrace(380);

    AWSLambdaAsync lambda = AWSLambdaAsyncClientBuilder.defaultClient();

    List<Future<InvokeResult>> futureList = new ArrayList<Future<InvokeResult>>();
    System.out.println("Begin submission");
    int count = 864;
    int size = 10;
    int frm0,frmn;
    for (int i = 0; i < count; i++) {
      frm0 = 1 + size*i;
      frmn = size*(i+1);
      JscLambdaInput tmp = new JscLambdaInput(input);
      tmp.setRange(frm0,frmn,1,11,11,1);
      InvokeRequest req = new InvokeRequest().withFunctionName("mmm-lambda-java")
          .withPayload(JsonUtil.toJsonString(tmp));
      futureList.add(lambda.invokeAsync(req, new JscLambdaAsyncResponse()));
    }
    System.out.println("Waiting for async callback");
    boolean notDone;
    do {
      notDone = false;
      for (Future<InvokeResult> f : futureList) {
        if (!f.isDone() && !f.isCancelled())
          notDone = true;
      }
      // perform some other tasks...
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        System.err.println("Thread.sleep() was interrupted!");
        System.exit(1);
      }
    } while (notDone);
    System.out.println("\nCompleted");
    System.exit(0);
  }
}