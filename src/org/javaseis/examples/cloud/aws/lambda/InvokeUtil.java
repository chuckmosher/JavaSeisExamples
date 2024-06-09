package org.javaseis.examples.cloud.aws.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class InvokeUtil {

  public static InvokeResult waitForResult(Future<InvokeResult> future) {
    while (!(future.isDone() || future.isCancelled())) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
      }
    }
    try {
      InvokeResult result = future.get(100, TimeUnit.MILLISECONDS);
      return result;
    } catch (Exception e) {
      throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
    }
  }

  public static InvokeResult waitForResult(Future<InvokeResult> future, LambdaLogger logger) {
    logger.log("Waiting for " + future.toString() + " ...");
    while (!(future.isDone() || future.isCancelled())) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
      }
    }
    try {
      logger.log(" ... retrieve results");
      InvokeResult result = future.get(100, TimeUnit.MILLISECONDS);
      logger.log("Return result: " + result.getExecutedVersion());
      return result;
    } catch (Exception e) {
      throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
    }
  }

  public static List<InvokeResult> waitForResults(List<Future<InvokeResult>> futureList, LambdaLogger logger) {
    List<InvokeResult> results = new ArrayList<InvokeResult>();
    boolean notDone;
    logger.log("Waiting on List<Future<InvokeResult>> ...");
    do {
      notDone = false;
      for (Future<InvokeResult> f : futureList) {
        if (!f.isDone() && !f.isCancelled())
          notDone = true;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
      }
    } while (notDone);
    logger.log("... All tasks completed");
    for (Future<InvokeResult> f : futureList) {
      try {
        logger.log("Retrieve results for index: " + futureList.indexOf(f));
        InvokeResult result = f.get(100, TimeUnit.MILLISECONDS);
        results.add(result);
      } catch (Exception e) {
        throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
      }
    }
    logger.log("Return List<InvokeResult>");
    return results;
  }
  
  public static List<InvokeResult> waitForResults(List<Future<InvokeResult>> futureList) {
    List<InvokeResult> results = new ArrayList<InvokeResult>();
    boolean notDone;
    do {
      notDone = false;
      for (Future<InvokeResult> f : futureList) {
        if (!f.isDone() && !f.isCancelled())
          notDone = true;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
      }
    } while (notDone);
    for (Future<InvokeResult> f : futureList) {
      try {
        InvokeResult result = f.get(100, TimeUnit.MILLISECONDS);
        results.add(result);
      } catch (Exception e) {
        throw new IllegalStateException("Could not see the Future: " + e.getMessage(), e.getCause());
      }
    }
    return results;
  }
}
