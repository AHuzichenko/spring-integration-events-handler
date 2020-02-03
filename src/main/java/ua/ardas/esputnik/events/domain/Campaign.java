package ua.ardas.esputnik.events.domain;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Campaign {

	private Integer campaignId;
	private String schemaId;
	private String params;

	private int organisationId;
	private String name;
	private String description;
	private boolean invalidJsonFigures;
	private String metadata;
	private Date updatedDate;
	private Integer totalCount;
	private Integer eventTypeId;
	private String eventTypeName;
	private String strategy;
	private int interval;
	private Integer groupId;
	private String groupName;
	private Integer contactFieldId;
	private String contactFieldName;
	private boolean paused;

	private Date firstExecDate;
	private Date lastExecDate;
	private Integer execCount;
	private Integer execCountWeek;
}