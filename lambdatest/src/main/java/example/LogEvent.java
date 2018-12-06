package example;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.model.*;


import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

    static final String FROM = "ruiqing@csye6225-fall2018-jiangruiq.me";
    static final String SUBJECT = "Reset your password";
    // The email body for recipients with non-HTML email clients.
    static final String TEXTBODY = "This email was sent through Amazon SES "
            + "using the AWS SDK for Java.";

    public Object handleRequest(SNSEvent request, Context context) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation started: " + timeStamp);
        String emailtoreset = request.getRecords().get(0).getSNS().getMessage();
        String uuid=UUID.randomUUID().toString();
        context.getLogger().log(emailtoreset);
        if(insertIntoDb(emailtoreset,uuid)){
            context.getLogger().log("insert into db "+emailtoreset+"succefully");
            if(sendemail(emailtoreset,uuid))
                context.getLogger().log("send email to "+emailtoreset+"succefully");
        }
        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation completed: " + timeStamp);
        return null;
    }

    public boolean insertIntoDb(String email,String uuid) {
//        BasicAWSCredentials creds = new BasicAWSCredentials(accesskey, secretkey);
        //create a new SNS client and set endpoint
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
//        client.setRegion(Region.getRegion(REGION));
//        AmazonDynamoDBClient client = new AmazonDynamoDBClient(creds);
        client.setRegion(Region.getRegion(Regions.US_EAST_1));

        DynamoDB dynamoDB = new DynamoDB(client);

        Table table = dynamoDB.getTable("csye6225");
        try {
            Item item1 = table.getItem("id", email);
            if (item1 == null) {
                //sent email and save

                Item item = new Item().withPrimaryKey("id", email).with("uuid",uuid).with("ttl",System.currentTimeMillis() / 1000L+1200);
                table.putItem(item);
                //save uuid
                System.out.println("Printing item after retrieving it....");
                return true;
            } else {
               return false; //do nothing
            }

        } catch (Exception e) {
            System.err.println("Create items failed.");
            System.err.println(e.getMessage());
            return false;
        }

    }


    public boolean sendemail(String email, String uuid) {
//        BasicAWSCredentials creds = new BasicAWSCredentials(accesskey, secretkey);
        String HTMLBODY = "<h1>Reset your password</h1>"
                + "<p>Click <a href='http://example.com/reset?email=" +
                email + "&token=" + uuid + "'> this link</a> to reset your email</p>";

        try {
            AmazonSimpleEmailService client = new AmazonSimpleEmailServiceAsyncClient();
//            AmazonDynamoDBClient client = new AmazonDynamoDBClient();
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(email))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset("UTF-8").withData(HTMLBODY))
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(TEXTBODY)))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(SUBJECT)))
                    .withSource(FROM);
            client.sendEmail(request);
            System.out.println("Email sent!");
            return true;
        } catch (Exception ex) {
            System.out.println("The email was not sent. Error message: "
                    + ex.getMessage());
            return false;
        }
    }

}
 