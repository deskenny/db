package db;

public class UploadObjectSingleOperation {
//	private static final Logger log = LoggerFactory.getLogger(UploadObjectSingleOperation.class);
//	//private static String bucketName     = "db_test_bucket";
//	//private static String keyName        = "*** Provide key ***";
//	//private static String uploadFileName = "*** Provide file name ***";
//	
//	public void uploadFile(String bucketName, String uploadFileName, String keyName) throws IOException {
//		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
//                .withRegion(Regions.US_EAST_1)
//                .build();
//        try {
//            log.info("Uploading a new object to S3 from a file\n");
//            File file = new File(uploadFileName);
//            s3Client.putObject(new PutObjectRequest(
//            		                 bucketName, keyName, file));
//
//         } catch (AmazonServiceException ase) {
//            log.info("Caught an AmazonServiceException, which " +
//            		"means your request made it " +
//                    "to Amazon S3, but was rejected with an error response" +
//                    " for some reason.");
//            log.info("Error Message:    " + ase.getMessage());
//            log.info("HTTP Status Code: " + ase.getStatusCode());
//            log.info("AWS Error Code:   " + ase.getErrorCode());
//            log.info("Error Type:       " + ase.getErrorType());
//            log.info("Request ID:       " + ase.getRequestId());
//        } catch (AmazonClientException ace) {
//            log.error("Caught an AmazonClientException, which " +
//            		"means the client encountered " +
//                    "an internal error while trying to " +
//                    "communicate with S3, " +
//                    "such as not being able to access the network.");
//            log.error("Error Message: " + ace.getMessage());
//        }
//    }
}