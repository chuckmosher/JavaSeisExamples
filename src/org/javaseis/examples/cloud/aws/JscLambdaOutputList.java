package org.javaseis.examples.cloud.aws;

import java.util.ArrayList;
import java.util.List;

import org.javaseis.util.JsonUtil;

public class JscLambdaOutputList {
  JscLambdaInput input;
  List<JscLambdaOutput> outputList;
  
  public JscLambdaOutputList( JscLambdaInput input ) {
    this.input = input;
    outputList = new ArrayList<JscLambdaOutput>();
  }
  
  public void add( JscLambdaOutput output ) {
    outputList.add(output);
  }
  
  public void add( JscLambdaOutputList addList ) {
    outputList.addAll(addList.outputList);
  }
  
  public void addFromInput( JscLambdaInput input, String status ) {
    for (int ivol = input.vol0; ivol <= input.voln; ivol += input.voli) {
      for (int ifrm = input.frm0; ifrm <= input.frmn; ifrm += input.frmi) {
        this.add(new JscLambdaOutput(ifrm, ivol, status, input));
      }
    }
  }
  
  public String toString() {
    return JsonUtil.toJsonString(this);
  }
  public double getIoTime() {
    double iotime = 0;
    for (JscLambdaOutput output : outputList) {
      iotime += output.iotime;
    }
    return iotime;
  }
  public long getIoCount() {
    long iocount = 0;
    for (JscLambdaOutput output : outputList) {
      iocount += output.iobytes;
    }
    return iocount;
  }
  public double getIoRate() {
    double iotime = 0;
    double iocount = 0;
    for (JscLambdaOutput output : outputList) {
      iotime += output.iotime;
      iocount += output.iobytes;
    }
    if (iotime == 0) return 0;
    return iocount/iotime;
  }
}
