package ua.ardas.esputnik.events.repository;

import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import ua.ardas.esputnik.commons.dao.queries.base.SingleResultSetOperation;
import ua.ardas.esputnik.commons.dao.utils.RSUtils;
import ua.ardas.esputnik.events.domain.EventAction;

@Repository
public class EventActionRepository extends SingleResultSetOperation<EventAction> {

	private static final String QUERY =
			"SELECT\n" +
			"ea.EventActionID," +
			"ea.EventTypeID," +
			"et.OrganisationID," +
			"ea.UserID," +
			"ea.RunnerUserID," +
			"ea.CampaignID," +
			"ea.ParamsMapping," +
			"ea.Strategy," +
			"ea.Interval," +
			"c.Paused," +
			"c.IsActive\n" +
			"FROM dbo.EventActions ea (nolock)\n" +
			"INNER JOIN dbo.EventTypes et (nolock) ON ea.EventTypeID=et.EventTypeID\n" +
			"INNER JOIN dbo.Campaigns c (nolock) ON c.CampaignID=ea.CampaignID\n" +
			"WHERE ea.EventTypeID=?";

	public List<EventAction> find(int eventTypeID) {
		return exec(QUERY, eventTypeID);
	}

	@Override
	protected RowMapper<EventAction> getRowMapper() {
		return (rs, rowNum) -> EventAction.builder()
				.eventTypeId(rs.getInt("EventTypeID"))
				.campaignId(rs.getInt("CampaignID"))
				.paramsMapping(rs.getString("ParamsMapping"))
				.organisationId(rs.getInt("OrganisationID"))
				.userId(rs.getInt("UserID"))
				.runnerUserId(RSUtils.getIntegerValue(rs,"RunnerUserID"))
				.strategy(rs.getString("Strategy"))
				.interval(rs.getInt("Interval"))
				.campaignPaused(rs.getBoolean("Paused"))
				.campaignActive(rs.getBoolean("IsActive"))
				.build();
	}
}