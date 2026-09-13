package com.kds.backend.notifications.infrastructure;

import com.kds.backend.identity.application.BackgroundClubService;
import com.kds.backend.notifications.application.NotificationEmailDispatcher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.*;

@Component
public class NotificationDeliveryScheduler {
    private static final Logger LOGGER=LoggerFactory.getLogger(NotificationDeliveryScheduler.class);
    private final BackgroundClubService clubs;private final NotificationEmailDispatcher dispatcher;
    public NotificationDeliveryScheduler(BackgroundClubService clubs,NotificationEmailDispatcher dispatcher){this.clubs=clubs;this.dispatcher=dispatcher;}
    @Scheduled(fixedDelayString="${app.notifications.delivery-interval:PT1M}") public void deliver(){for(var club:clubs.allClubIds())try{dispatcher.dispatch(club);}catch(RuntimeException failure){LOGGER.warn("notification_delivery_failed clubId={}",club,failure);}}
}
