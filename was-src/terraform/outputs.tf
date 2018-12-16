output "rule_arn" {
  value = "${aws_cloudwatch_event_rule.ping.arn}"
}

output "lambda_arn" {
  value = "${aws_lambda_function.dbtestlambda.arn}"
}
