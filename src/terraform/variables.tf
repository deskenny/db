variable "aws_region" {
  description = "The AWS region to create resources in."
  default     = "us-east-1"
}

variable "rule_name" {
  description = "The ping DB lambda"
  default     = "ping-db-lambda-terraform"
}

variable "rule_description" {
  description = "The description of the rule"
  default     = "Lambda function to ping the database Ireland"
}

variable "target_name" {
  description = "DB Lambda"
  default     = "db-lambda-target"
}

variable "lambda_role" {
  description = "Lambda role"
  default     = "arn:aws:iam::881856840650:role/AlexaDublinBusLambda"
}



