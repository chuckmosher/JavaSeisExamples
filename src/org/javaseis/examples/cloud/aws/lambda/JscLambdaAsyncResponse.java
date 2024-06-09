package org.javaseis.examples.cloud.aws.lambda;

import java.nio.charset.StandardCharsets;

import org.javaseis.util.JsonUtil;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

public class JscLambdaAsyncResponse implements AsyncHandler<InvokeRequest, InvokeResult>
{
  public void onSuccess(InvokeRequest req, InvokeResult invokeResult) {
    String result = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8).replaceAll("\\\\n","").replaceAll("\\\\","");
    JscLambdaOutputList outputList = (JscLambdaOutputList) JsonUtil.fromJsonString(JscLambdaOutputList.class, result.substring(1,result.length()-1));
    //System.out.println(JsonUtil.toJsonString(outputList));
    double time = 0;
    long count = 0;
    for (JscLambdaOutput out : outputList.outputList) {
      time += out.iotime;
      count += out.iobytes;
    }
    System.out.println("Success\n" + "Frame Range: " + outputList.input.frm0 + "-" + outputList.input.frmn + "I/O Rate: " + count/time/1024 + " KiB/s");
  }

  public void onError(Exception e) {
      System.out.println("Failure: " + e.getMessage());
  }

}
