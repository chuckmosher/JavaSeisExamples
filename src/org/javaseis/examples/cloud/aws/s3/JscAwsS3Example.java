package org.javaseis.examples.cloud.aws.s3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.javaseis.util.SeisException;
import org.momacmo.aws.s3.javaseis.JscAwsS3Parms;
import org.momacmo.parms.JsonUtil;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import beta.javaseis.util.RandomRange;
import edu.mines.jtk.util.ArrayMath;

/**
 * Example implementation of using AWS S3 Object Storage to hold traces and
 * headers for a JavaSeis Cloud dataset. Existing machinery and SeisSpace
 * code is used to manage metadata, grid and header definitions, and so on.
 * <p>
 * Current implementation supports up to 6 dimensions.
 * <p>
 * In this example we use an AWS Bucket and Prefix as the root directory for
 * metadata, traces, and headers. For example:
 * <p>
 * AWS Bucket = data<br>
 * AWS Prefix = Project/SubProject
 * <p>
 * We create 'folders' and store data under the Bucket/Prefix
 * <p>
 * <code>
 * data/Project/SubProject/FileProperties.xml
 * <br>
 * data/Project/SubProject/Traces/
 * <br>
 * data/Project/SubProject/Headers/
 * </code>
 * <p>
 * Traces and headers are stored frame by frame using a key suffix based on the
 * index values for frame, volume. For example, for Volume 11, Frame 134, the traces
 * and headers would be stored as:
 * <p>
 * <code>
 * data/Project/SubProject/Traces/V11/F134
 * <br>
 * data/Project/SubProject/Headers/V11/F134
 * </code>
 * <p>
 * For higher dimensional datasets (5D and above) the higher dimensions are
 * "flattened" to volume index. For example, for a dataset with a volume dimension
 * of 10 and a hypercube dimension of 5, the frames for Volume 3 Hypercube 2 the
 * volume prefix would be set to VolumesPerHypercube * hypercubeIndex + volumeIndex
 * = 10*2 + 3 = V23
 * <p>
 * <code>
 * data/Project/SubProject/Traces/V23/FXXX
 * </code>
 * 
 * @author Chuck Mosher for MoMacMo.org
 *
 */
public class JscAwsS3Example {
  public JscAwsS3Parms parms;
  boolean overwrite;
  AmazonS3 s3;
  
  public JscAwsS3Example( JscAwsS3Parms parms ) throws SeisException {
    this.parms = parms;
    try {
      AWSCredentials credentials = new ProfileCredentialsProvider(parms.awsProfile).getCredentials();
      if (parms.awsRegion.equals("default")) {
        s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
      } else {
        s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withRegion(parms.awsRegion).build();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new SeisException("Cannot load AWS credentials from the credential profiles file. "
          + "Please make sure that your credentials file is at the correct "
          + "location (~/.aws/credentials), and is in valid format.", e);
    }

    if (s3.doesBucketExistV2(parms.awsBucket) == false) throw new SeisException("Bucket does not exist: " + parms.awsBucket);
  }

  /**
   * Create an AWS S3 JavaSeis dataset
   * @param filePath - local path for storing file properties locally
   * @param fileProperties - input JscFileProperties for the dataset
   * @param overwrite - set to true to overwrite an existing dataset
   * @throws SeisException - on local or remote access errors
   */
  public void create(String fileName, boolean overwrite) throws SeisException {   
    String key = parms.awsPrefix + "/" + fileName;
    boolean exists = s3.doesObjectExist(parms.awsBucket, key);
    if (exists == true && overwrite == false) throw new SeisException("Specified key exists and overwite is false: " + parms.awsBucket + ":" + key);
    this.overwrite = overwrite;
    key = key + "/JscAwsS3Parms.json";
    AwsS3Utils.putJsonObject( s3, parms.awsBucket,key, parms, overwrite );
  }

  /**
   * Open an existing JavaSeis AWS-S3 dataset
   * 
   * @param filename - Dataset fileName, appended to prefix as key (bucket:prefix/fileName)
   * @throws SeisException - on IO or AWS access errors
   */
  public void open(String filename) throws SeisException {
    String key = parms.awsPrefix + "/" + filename + "/" + "JscAwsS3Parms.json";
    if (s3.doesObjectExist(parms.awsBucket, key ) == false)
        throw new SeisException("Object does not exist: " + parms.awsBucket + ":" + key);
    JscAwsS3Parms remoteParms = (JscAwsS3Parms) AwsS3Utils.getJsonObject(s3, parms.awsBucket, key, JscAwsS3Parms.class);
    System.out.println( JsonUtil.toJsonString(remoteParms) );  
  }
  
  public void write( int volume, int frame, int ntrc, int nsamp, float[][] traces, int nbytes, byte[][] headers ) throws SeisException {
    String base = parms.awsPrefix + "/" + parms.fileName;
    String location = "/V" + Integer.toString(volume).trim() + "/F" + Integer.toString(frame).trim();
    ByteBuffer buf = ByteBuffer.allocate(4*ntrc*nsamp);
    FloatBuffer fbuf = buf.asFloatBuffer();
    for (int j=0; j<ntrc; j++) {
      fbuf.put(traces[j],0,nsamp);
    }
    buf.flip();
    AwsS3Utils.putByteBuffer(s3, parms.awsBucket, base + "/Traces" + location, buf);
    buf = ByteBuffer.allocate(ntrc*nbytes);
    for (int j=0; j<ntrc; j++) {
      buf.put(headers[j],0,nbytes);
    }
    buf.flip();
    AwsS3Utils.putByteBuffer(s3, parms.awsBucket, base + "/Headers" + location, buf);
  }
  
  public void read( int volume, int frame, int ntrc, int nsamp, float[][] traces, int nbytes, byte[][] headers ) throws SeisException {
    String base = parms.awsPrefix + "/" + parms.fileName;
    String location = "/V" + Integer.toString(volume).trim() + "/F" + Integer.toString(frame).trim();
    ByteBuffer buf = ByteBuffer.allocate(4*ntrc*nsamp);
    AwsS3Utils.getByteBuffer(s3, parms.awsBucket, base + "/Traces" + location, buf);
    buf.position(0);
    FloatBuffer fbuf = buf.asFloatBuffer();
    for (int j=0; j<ntrc; j++) {
      fbuf.get(traces[j],0,nsamp);
    }
    buf = ByteBuffer.allocate(ntrc*nbytes);
    AwsS3Utils.getByteBuffer(s3, parms.awsBucket, base + "/Headers" + location, buf);
    buf.position(0);
    for (int j=0; j<ntrc; j++) {
      buf.get(headers[j],0,nbytes);
    }
  }
  
  public void close() throws SeisException {
    try {
    s3.shutdown();
    } catch ( Exception e ) {
      throw new SeisException("S3 Shutdown failed",e.getCause());
    }
  }

  public static void main( String[] args ) {
    JscAwsS3Parms s3Parms = new JscAwsS3Parms( args );
    int nsamp = 1000;
    int ntrc = 380;
    int nbytes = 40;
    int volume = 11;
    int frame = 57;
    float[][] trcs = new float[ntrc][nsamp];
    RandomRange rr = new RandomRange(666,-1,1);
    rr.fill(trcs);
    float[][] rtrc = new float[ntrc][nsamp];
    byte[][] hdrs = new byte[ntrc][nbytes];
    byte[][] rhdr = new byte[ntrc][nbytes];
    for (int j=0; j<ntrc; j++) {
      ArrayMath.ramp((byte)1, (byte)j, hdrs[j]);
    }
    try {
      JscAwsS3Example s3Example = new JscAwsS3Example( s3Parms );
      s3Example.create(s3Parms.fileName, true);
      s3Example.open(s3Parms.fileName);
      s3Example.write(volume, frame, ntrc, nsamp, trcs, nbytes, hdrs);
      s3Example.read(volume, frame, ntrc, nsamp, rtrc, nbytes, rhdr);
      s3Example.close();
    } catch (SeisException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    for (int j=0; j<ntrc; j++) {
      for (int i=0; i<nsamp; i++) {
        assert rtrc[j][i] == trcs[j][i] : "Failed";
      }
    }
  }
}
