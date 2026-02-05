package com.cloud.community.notice.config;

import com.cloud.community.core.constant.RabbitConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("noticeRabbitConfig")
public class RabbitConfig {

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(RabbitConstants.NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue activitySignupQueue() {
        return new Queue(RabbitConstants.ACTIVITY_SIGNUP_QUEUE);
    }

    @Bean
    public Binding bindingActivitySignup(Queue activitySignupQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(activitySignupQueue).to(notificationExchange).with(RabbitConstants.ACTIVITY_SIGNUP_ROUTING_KEY);
    }

    @Bean
    public Queue commonNotificationQueue() {
        return new Queue(RabbitConstants.COMMON_NOTIFICATION_QUEUE);
    }

    @Bean
    public Binding bindingCommonNotification(Queue commonNotificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(commonNotificationQueue).to(notificationExchange).with(RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Queue clubBroadcastQueue() {
        return new Queue(RabbitConstants.CLUB_BROADCAST_QUEUE);
    }

    @Bean
    public Binding bindingClubBroadcast(Queue clubBroadcastQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(clubBroadcastQueue).to(notificationExchange).with(RabbitConstants.CLUB_BROADCAST_ROUTING_KEY);
    }
}
