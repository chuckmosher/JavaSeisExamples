package org.javaseis.examples.cloud.aws.s3;

import org.momacmo.javaseis.properties.PropertiesTreeImpl;

import com.amazonaws.services.s3.AmazonS3;

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
  // Parms container
  //JscFileProperties jscFileProperties;
  //JavaSeisContext jscContext;
  public static String FILE_PROPERTIES_JSC = "JscFileProperties.json";
  public static String S3_PARMS = "jscAwsS3Parms";
  PropertiesTreeImpl propertiesTree;
  // AWS bucket, prefix, and client
  String awsProfile, awsRegion, awsBucket, awsPrefix;
  AmazonS3 s3;
  public JscAwsS3Example() {
    
  }
}
