package org.javaseis.examples.cloud.aws;

import java.util.Map;

public class JscLambdaInput {
  public String bucket;
  public String prefix;
  public int vol0, voln, voli;
  public int frm0, frmn, frmi;
  public int nsamp;
  public int ntrace;
  public int nhdr;
  public int batchSize;
  
  public JscLambdaInput() {
    this.bucket = "bucket";
    this.prefix = "prefix";
  }

  public JscLambdaInput(Map<String, String> eventMap) {
    if (eventMap.containsKey("bucket"))
    bucket = eventMap.get("bucket");
    if (eventMap.containsKey("prefix"))
    prefix = eventMap.get("prefix");
    if (eventMap.containsKey("vol0"))
    vol0 = Integer.parseInt(eventMap.get("vol0"));
    if (eventMap.containsKey("voln"))
    voln = Integer.parseInt(eventMap.get("voln"));
    if (eventMap.containsKey("voli"))
    voli = Integer.parseInt(eventMap.get("voli"));
    if (eventMap.containsKey("frm0"))
    frm0 = Integer.parseInt(eventMap.get("frm0"));
    if (eventMap.containsKey("frmn"))
    frmn = Integer.parseInt(eventMap.get("frmn"));
    if (eventMap.containsKey("frmi"))
    frmi = Integer.parseInt(eventMap.get("frmi"));
    if (eventMap.containsKey("nsamp"))
    nsamp = Integer.parseInt(eventMap.get("nsamp"));
    if (eventMap.containsKey("ntrace"))
    ntrace = Integer.parseInt(eventMap.get("ntrace"));
    if (eventMap.containsKey("nhdr"))
    nhdr = Integer.parseInt(eventMap.get("nhdr"));
    if (eventMap.containsKey("batchSize"))
    batchSize = Integer.parseInt(eventMap.get("batchSize"));
  }
  
  public JscLambdaInput( JscLambdaInput in ) {
    bucket = new String(in.bucket);
    prefix = new String(in.prefix);
    vol0 = in.vol0;
    voln = in.voln;
    voli = in.voli;
    frm0 = in.frm0;
    frmn = in.frmn;
    frmi = in.frmi;
    nsamp = in.nsamp;
    ntrace = in.ntrace;
    nhdr = in.nhdr;
    batchSize = in.batchSize;
  }
  
  public void setRange( int frm0, int frmn, int frmi, int vol0, int voln, int voli ) {
    this.vol0 = vol0;
    this.voln = voln;
    this.voli = voli;
    this.frm0 = frm0;
    this.frmn = frmn;
    this.frmi = frmi;
  }
  
  public void setRange( int frame, int volume ) {
    setRange( frame, frame, 1, volume, volume, 1 );
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public int getVol0() {
    return vol0;
  }

  public void setVol0(int vol0) {
    this.vol0 = vol0;
  }

  public int getVoln() {
    return voln;
  }

  public void setVoln(int voln) {
    this.voln = voln;
  }

  public int getVoli() {
    return voli;
  }

  public void setVoli(int voli) {
    this.voli = voli;
  }

  public int getFrm0() {
    return frm0;
  }

  public void setFrm0(int frm0) {
    this.frm0 = frm0;
  }

  public int getFrmn() {
    return frmn;
  }

  public void setFrmn(int frmn) {
    this.frmn = frmn;
  }

  public int getFrmi() {
    return frmi;
  }

  public void setFrmi(int frmi) {
    this.frmi = frmi;
  }

  public int getNsamp() {
    return nsamp;
  }

  public void setNsamp(int nsamp) {
    this.nsamp = nsamp;
  }

  public int getNtrace() {
    return ntrace;
  }

  public void setNtrace(int ntrace) {
    this.ntrace = ntrace;
  }

  public int getNhdr() {
    return nhdr;
  }

  public void setNhdr(int nhdr) {
    this.nhdr = nhdr;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

}
