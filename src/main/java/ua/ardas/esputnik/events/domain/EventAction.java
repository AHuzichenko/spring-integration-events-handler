package ua.ardas.esputnik.events.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventAction {
    private int eventTypeId;
    private int campaignId;
    private String paramsMapping;
    private int organisationId;
    private int userId;
    private Integer runnerUserId;
    private String strategy;
    private int interval;
    private boolean campaignPaused;
    private boolean campaignActive;
}
