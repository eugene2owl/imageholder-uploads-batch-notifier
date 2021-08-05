# imageholder-uploads-batch-notifier
AWS SAM project which stores several definitions of the Lambda functions (AWS Java SDK) and SAM deployment template file for them.

### Manually invoked Lambda
Lambda function defined in the "ManualUploadsNotifierFunction" module should be invoked "manually", e.g.
* Via AWS Console
* Via attached AWS API Gateway
* Via web application which uses AWS SDK to build Lambda client
* Via AWS CloudWatch scheduled event
<img src="https://raw.githubusercontent.com/eugene2owl/imageholder-uploads-batch-notifier/ManualUploadsNotifierFunction/src/main/assets/manually-triggered-lambda.png" alt="drawing" width="800"/>

### Automatically triggered Lambda
Lambda function defined in the "TriggeredUploadsNotifierFunction" module should be triggered by attached AWS SQS queue.
<img src="https://raw.githubusercontent.com/eugene2owl/imageholder-uploads-batch-notifier/assets/auto-triggered-lambda.png" alt="drawing" width="800"/>

### Update code of the remote Lambda function
Update code of the remote Lambda function according local code of the Handler class right from the IntelliJ IDE.
Without knowing of all the details about packaging and deployment of the local Java code.
<img src="https://raw.githubusercontent.com/eugene2owl/imageholder-uploads-batch-notifier/assets/update-function-code.png" alt="drawing" width="800"/>

### Modify configuration of the local Lambda function
Update code of the local Lambda function for local debugging and development right in the IntelliJ IDE.
<img src="https://raw.githubusercontent.com/eugene2owl/imageholder-uploads-batch-notifier/assets/modify-run-configuration.png" alt="drawing" width="800"/>
Function configuration includes memory, timeout, input data, runtime, etc.
<img src="https://raw.githubusercontent.com/eugene2owl/imageholder-uploads-batch-notifier/assets/run-configuration-details.png" alt="drawing" width="800"/>

### Deploy Lambda functions using SAM template
Deploy several Lambda functions with particular configurations as a CloudFormation stack using SAM.
<img src="https://raw.githubusercontent.com/eugene2owl/imageholder-uploads-batch-notifier/assets/deploy-sam.png" alt="drawing" width="800"/>
