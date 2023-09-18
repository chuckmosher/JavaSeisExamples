package org.javaseis.examples.cloud.aws;

public class JscLambdaOutput {
  public int frame;
  public int volume;
  public int traceCount;
  public float iotime;
  public int iobytes;
  public String status;
  
  public JscLambdaOutput( int frame, int volume ) {
    this.frame = frame;
    this.volume = volume;
    status = "None";
  }
  
  public JscLambdaOutput( int frmIndex, int volIndex, String status, JscLambdaInput input ) {
    this.frame = input.frm0 + frmIndex*input.frmi;
    this.volume = input.vol0 + volIndex*input.voli;
    this.status = status;
  }

  public int getVolume() {
    return volume;
  }

  public void setVolume(int volume) {
    this.volume = volume;
  }

  public int getFrmame() {
    return frame;
  }

  public void setFrame(int frame) {
    this.frame = frame;
  }

  public float getIotime() {
    return iotime;
  }

  public void setIotime(float iotime) {
    this.iotime = iotime;
  }

  public int getIobytes() {
    return iobytes;
  }

  public void setIobytes(int iobytes) {
    this.iobytes = iobytes;
  }

  public int getTraceCount() {
    return traceCount;
  }

  public void setTraceCount(int traceCount) {
    this.traceCount = traceCount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
