package ua.ardas.esputnik.events.repository;

import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import ua.ardas.esputnik.commons.dao.procedures.base.BaseStoredProcedure;
import ua.ardas.esputnik.commons.dao.queries.base.SingleResultSetOperation;
import ua.ardas.esputnik.commons.dao.utils.RSUtils;
import ua.ardas.esputnik.events.domain.Campaign;

@Repository
public class CampaignRepository extends SingleResultSetOperation<Campaign> {

	private static final String QUERY =
			"[TotalCount] = 1, c.CampaignID, c.Name, c.UpdatedDate, C.InvalidJsonFigures, c.SchemaId, c.Params, c.Metadata, c.Description, c.OrganisationID FROM dbo.Campaigns c";

	private static final String SELECT_TEMPLATE =
			"SELECT " + QUERY + " WHERE [CampaignID] = ?";

	public Campaign getCampaign(int campaignId) {
		List<Campaign> campaigns = getJdbcTemplate().query(SELECT_TEMPLATE, (rs, rowNum) ->
						Campaign.builder()
								.campaignId(rs.getInt("CampaignID"))
								.name(rs.getString("Name"))
								.organisationId(rs.getInt("OrganisationID"))
								.invalidJsonFigures(rs.getBoolean("InvalidJsonFigures"))
								.schemaId(rs.getString("SchemaId"))
								.params(rs.getString("Params"))
								.metadata(rs.getString("Metadata"))
								.description(rs.getString("Description"))
								.build(),
				campaignId);
		return campaigns.isEmpty() ? null : campaigns.get(0);
	}

	@Override
	protected RowMapper<Campaign> getRowMapper() {
		return (rs, rowNum) -> Campaign.builder()
				.campaignId(rs.getInt("CampaignID"))
				.organisationId(rs.getInt("OrganisationID"))
				.name(rs.getString("Name"))
				.updatedDate(BaseStoredProcedure.getUTCDateFromResultSet("UpdatedDate", rs))
				.invalidJsonFigures(rs.getBoolean("InvalidJsonFigures"))
				.schemaId(rs.getString("SchemaId"))
				.params(rs.getString("Params"))
				.metadata(rs.getString("Metadata"))
				.description(rs.getString("Description"))
				.totalCount(rs.getInt("TotalCount"))
				.eventTypeId(RSUtils.getIntegerValue(rs, "EventTypeID"))
				.eventTypeName(rs.getString("EventTypeName"))
				.strategy(rs.getString("Strategy"))
				.interval(rs.getInt("Interval"))
				.groupId(RSUtils.getIntegerValue(rs, "GroupID"))
				.contactFieldId(RSUtils.getIntegerValue(rs, "ProfileInputID"))
				.paused(rs.getBoolean("Paused"))
				.build();
	}
}