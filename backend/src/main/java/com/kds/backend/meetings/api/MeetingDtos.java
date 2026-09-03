package com.kds.backend.meetings.api;
import com.kds.backend.meetings.application.MeetingCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
public final class MeetingDtos {
    private MeetingDtos() {}
    public record AgendaItemRequest(@NotBlank @Size(max=200) String title,@Size(max=2000) String description){MeetingCommand.AgendaItem command(){return new MeetingCommand.AgendaItem(title,description);}}
    public record MeetingRequest(@PositiveOrZero long version,@NotBlank @Size(max=160) String title,@Size(max=4000) String description,
        @NotNull OffsetDateTime startsAt,@Min(15) @Max(1440) int durationMinutes,@Size(max=240) String location,
        @Size(max=500) @Pattern(regexp="^$|^https?://.+",message="Meeting link must start with http:// or https://") String meetingUrl,
        @NotNull @Size(min=1,max=50) List<@Valid AgendaItemRequest> agendaItems){
        MeetingCommand command(){return new MeetingCommand(version,title,description,startsAt,durationMinutes,location,meetingUrl,agendaItems.stream().map(AgendaItemRequest::command).toList());}
    }
}
