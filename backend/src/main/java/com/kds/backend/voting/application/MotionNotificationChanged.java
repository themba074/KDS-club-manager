package com.kds.backend.voting.application;

import com.kds.backend.members.application.VotingEligibleMember;
import java.time.Instant;
import java.util.*;
public record MotionNotificationChanged(Action action,UUID clubId,UUID motionId,String title,Instant opensAt,Instant closesAt,List<VotingEligibleMember> recipients){public enum Action{SCHEDULED,CANCELLED}}
