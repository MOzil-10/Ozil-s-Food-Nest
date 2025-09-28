package com.food.Nest.messaging;

import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;

import java.net.URI;

@Configuration
public class MessagingConfig {

    private static final Logger logger = LoggerFactory.getLogger(MessagingConfig.class);

    @Value("${aws.sqs.order-processing-queue}")
    private String orderProcessingQueue;

    @Value("${aws.sqs.notification-queue}")
    private String notificationQueue;

    @Value("${aws.sns.notification-topic}")
    private String notificationTopic;

    @Value("${cloud.aws.sqs.endpoint}")
    private String sqsEndpoint;

    @Value("${cloud.aws.sns.endpoint}")
    private String snsEndpoint;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(sqsEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(sqsEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .endpointOverride(URI.create(snsEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .build();
    }

    @Bean
    public SnsAsyncClient snsAsyncClient() {
        return SnsAsyncClient.builder()
                .endpointOverride(URI.create(snsEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .build();
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.newTemplate(sqsAsyncClient);
    }

    @Bean
    public SnsTemplate snsTemplate(SnsClient snsClient) {
        return new SnsTemplate(snsClient);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeMessagingResources() {
        createSqsQueue(orderProcessingQueue);
        createSqsQueue(notificationQueue);
        createSnsTopic(notificationTopic);
    }

    private void createSqsQueue(String queueName) {
        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            sqsClient().createQueue(request);
            logger.info("Created SQS queue: {}", queueName);
        } catch (Exception e) {
            logger.warn("Failed to create SQS queue: {}. It may already exist.", queueName, e);
        }
    }

    private void createSnsTopic(String topicName) {
        try {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();
            snsClient().createTopic(request);
            logger.info("Created SNS topic: {}", topicName);
        } catch (Exception e) {
            logger.warn("Failed to create SNS topic: {}. It may already exist.", topicName, e);
        }
    }
}
