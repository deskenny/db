provider "aws" {
  region = "${var.aws_region}"
}

resource "aws_cloudwatch_event_rule" "ping" {
  name = "${var.rule_name}"
  description = "${var.rule_description}"
  schedule_expression = "rate(10 minutes)"
}

resource "aws_cloudwatch_event_target" "pingtarget" {
  rule      = "${aws_cloudwatch_event_rule.ping.name}"
  target_id = "${var.target_name}"
  arn       = "${aws_lambda_function.dbtestlambda.arn}"
  input = 
	<<PATTERN
		{   
		"session": 
			{     "sessionId": "SessionId.faca6bc2-e9e3-441d-a02a-3830f86e5c0d",     "application": 
				{       
				"applicationId": "amzn1.ask.skill.3df127cf-5065-4c7d-b0a4-4dac8be345cd"     },
				"attributes": {},
				"user": {  
					"userId": "amzn1.ask.account.AFBFFHVY2BQ6UM2Z6BZT3ZHZBRYROAGHER7ZRFK6LMFFW75AD6ZV6FD3ALIG7DNSM56SKBZRCR23WKH2NJ62JFYWAXYDIV46NS3SXBFH2AINWAQMABEO4GP6QRBSFF54LL3W46KA77GTT7MXBDJJZ5QAW7R2S5RRJ4EVYPMIOG76NPZFMSHWYMO6DTZR4KPN3QXXYKKAE5T2UYY"     },
					"new": true   },
					"request": 
					{     
						"type": "IntentRequest",     "requestId": "EdwRequestId.85694398-0db5-4771-9435-57e60ce2085a",     "locale": "en-GB",     "timestamp": "2017-04-13T15:40:27Z",     "intent": 
						{       
							"name": "NextBusIntent",       "slots": {}     
					}   
				},
				"version": "1.0" 
			}
	PATTERN
}

resource "aws_lambda_function" "dbtestlambda" {
    filename = "../../target/dbtest-4.0.0-jar-with-dependencies.jar"
    description = "Irish Dublin Bus backend"
    function_name = "TestDB"
    role = "${var.lambda_role}"
    handler = "db.DBRequestStreamHandler"
    runtime = "java8"
}
