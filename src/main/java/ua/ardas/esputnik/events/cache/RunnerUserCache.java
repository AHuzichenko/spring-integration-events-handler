package ua.ardas.esputnik.events.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.cache.TestAwareCacheBuilder;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.service.organisation.api.internal.UserApi;
import ua.ardas.esputnik.service.organisation.api.model.UserLockStatus;
import ua.ardas.esputnik.service.organisation.api.view.user.DetailUserView;
import ua.ardas.esputnik.services.clients.organisation.OrganisationServiceClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class RunnerUserCache {

	private static final int MAX_SIZE = 50_000;
	private static final int CAMPAIGN_REQUIRED_PERMISSIONS = 601;
	private static final Gson GSON = new Gson();

    @Value("${events.cache.expiration.time.sec:600}")
    private long cacheExpireSeconds;
    private LoadingCache<Pair<Integer, Integer>, Integer> cache;

    private final UserApi userApiClient;
    private final OrganisationServiceClient organisationServiceClient;

    @PostConstruct
    private void postConstruct() {
        cache = TestAwareCacheBuilder.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(cacheExpireSeconds, TimeUnit.SECONDS)
                .build(new CampaignRunnerLoader());
    }

    public int getRunner(EventAction action) throws ExecutionException {
        int userId = null != action.getRunnerUserId() ? action.getRunnerUserId() : action.getUserId();
        return cache.get(Pair.of(userId, action.getOrganisationId()));
    }

	private class CampaignRunnerLoader extends CacheLoader<Pair<Integer, Integer>, Integer> {

	    @Override
        @ParametersAreNonnullByDefault
		public Integer load(Pair<Integer, Integer> key) {
            int userId = key.getFirst();
            int orgId = key.getSecond();
            log.info("Load campaign runner user: {}", userId);

            return isUserHasAccessToOrganisation(userId, orgId)
                    ? userId
                    : findUser(orgId, getRequiredPermissions(orgId));
        }

        private Map<String, Set<String>> getRequiredPermissions(int organisationId) {
            String config = organisationServiceClient.getConfigValue(organisationId, CAMPAIGN_REQUIRED_PERMISSIONS);
            return GSON.fromJson(config, RequiredPermissionsDto.class).getPermissions();
        }

        private boolean isUserHasAccessToOrganisation(int userId, int orgId) {
            boolean hasAccess = userApiClient.hasAccess(orgId, userId) && !isUserBlocked(userId);
            if (!hasAccess) {
                log.info("User {} is blocked for organisation: id = {}", userId, orgId);
            }
            return hasAccess;
        }

        private boolean isUserBlocked(int userId) {
            DetailUserView user = userApiClient.getById(userId);
            return isUserBlocked(user);
        }

        private boolean isUserBlocked(DetailUserView user){
            return user.isDeleted() || UserLockStatus.LOCKED.equals(user.getLockStatus());
        }

        private int findUser(int organisationId, Map<String, Set<String>> requiredPermissions) {
            log.info("Looking for user in organization {} with required permissions {}", organisationId, requiredPermissions);
            List<DetailUserView> users = userApiClient.getUsersByOrg(organisationId);
            return users.stream()
                    .filter(user -> hasPermissions(requiredPermissions, user))
                    .filter(user -> !isUserBlocked(user))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User with required required permissions wasn't found."))
                    .getId();
        }

		private boolean hasPermissions(Map<String, Set<String>> requiredPermissions, DetailUserView user) {
			for (Map.Entry<String, Set<String>> entry : requiredPermissions.entrySet()) {
				Set<String> operations = user.getPermissions().get(entry.getKey());
                if (operations == null || !operations.containsAll(entry.getValue())) {
                    return false;
                }
			}
			return true;
		}
	}

	@Data
	private static class RequiredPermissionsDto {
		private Map<String, Set<String>> permissions;
	}

}