package com.kds.backend.notifications.application;

import com.kds.backend.identity.application.*;
import com.kds.backend.notifications.domain.*;
import com.kds.backend.notifications.repository.NotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class NotificationService {
    private final NotificationRepository notifications; private final MembershipLifecycleService memberships; private final Clock clock;
    public NotificationService(NotificationRepository notifications,MembershipLifecycleService memberships,Clock clock){this.notifications=notifications;this.memberships=memberships;this.clock=clock;}
    public List<NotificationView> feed(UUID actor){UUID member=current(actor);return notifications.feed(member,clock.instant()).stream().map(NotificationService::view).toList();}
    public long unreadCount(UUID actor){return notifications.unreadCount(current(actor),clock.instant());}
    @Transactional public NotificationView markRead(UUID actor,UUID id){UUID member=current(actor);var item=notifications.find(id,member,clock.instant()).orElseThrow(()->new AccessDeniedException("Notification is unavailable in this club."));item.markRead(clock.instant());return view(item);}
    @Transactional public int markAllRead(UUID actor){return notifications.markAllRead(current(actor),clock.instant());}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void create(NotificationType type,String title,String message,String targetPath,String sourceType,UUID sourceId,
                       Instant availableAt,List<NotificationRecipient> recipients){
        UUID club=TenantContext.requireClubId();Instant now=clock.instant();
        for(var recipient:recipients){String key=sourceType+":"+sourceId+":"+type+":"+recipient.membershipId()+":"+availableAt;
            if(!notifications.exists(key))notifications.add(new NotificationEntity(UUID.randomUUID(),club,recipient.membershipId(),recipient.email(),type,title,message,targetPath,sourceType,sourceId,key,availableAt,now));}
        notifications.flush();
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void replaceScheduled(String sourceType,UUID sourceId,List<ScheduledNotification> replacements){
        notifications.deletePending(sourceType,sourceId,clock.instant());
        for(var replacement:replacements)create(replacement.type(),replacement.title(),replacement.message(),replacement.targetPath(),sourceType,sourceId,replacement.availableAt(),replacement.recipients());
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void cancelScheduled(String sourceType,UUID sourceId){notifications.cancelPending(sourceType,sourceId,clock.instant());}
    private UUID current(UUID actor){return memberships.requireCurrentMembership(actor).membershipId();}
    private static NotificationView view(NotificationEntity value){return new NotificationView(value.getId(),value.getType(),value.getTitle(),value.getMessage(),value.getTargetPath(),value.getAvailableAt(),value.getReadAt());}
    public record ScheduledNotification(NotificationType type,String title,String message,String targetPath,Instant availableAt,List<NotificationRecipient> recipients){}
}
