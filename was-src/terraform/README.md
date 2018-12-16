# CloudWatch Event sent to a lambda function

This sets up a lambda function for the alexa skill and creates a cloudwatch event to ping it every 10 minutes and keep alive.

## How to run the example

```
terraform apply \
	-var=aws_region=us-west-2
```
