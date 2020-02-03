package ua.ardas.esputnik.events.cache;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.service.organisation.api.internal.UserApi;
import ua.ardas.esputnik.service.organisation.api.model.UserLockStatus;
import ua.ardas.esputnik.service.organisation.api.view.user.DetailUserView;
import ua.ardas.esputnik.services.clients.organisation.OrganisationServiceClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RunnerUserCacheTest {

    @InjectMocks
    private RunnerUserCache testObj;
    @Mock
    private UserApi userApi;
    @Mock
    private OrganisationServiceClient organisationServiceClient;

    @Before
    public void initCache() {
        ReflectionTestUtils.invokeMethod(testObj, "postConstruct");
    }

	@Test
	public void testGetRunnerByCreator() throws ExecutionException {
        EventAction action = EventAction.builder()
                .userId(1001)
                .organisationId(1)
                .build();

        DetailUserView user = new DetailUserView();
        user.setDeleted(false);

        when(userApi.hasAccess(1, 1001)).thenReturn(true);
        when(userApi.getById(1001)).thenReturn(user);

        Assert.assertEquals(1001, testObj.getRunner(action));

        verify(userApi, times(1)).hasAccess(1, 1001);
        verify(userApi, times(1)).getById(1001);
        verifyNoMoreInteractions(userApi);
    }

    @Test
    public void testGetRunnerByCurrentRunner() throws ExecutionException {
        EventAction action = EventAction.builder()
            .userId(1001)
            .runnerUserId(1002)
            .organisationId(1)
            .build();

        DetailUserView user = new DetailUserView();
        user.setDeleted(false);

        when(userApi.hasAccess(1, 1002)).thenReturn(true);
        when(userApi.getById(1002)).thenReturn(user);

        Assert.assertEquals(1002, testObj.getRunner(action));

        verify(userApi, times(1)).hasAccess(1, 1002);
        verify(userApi, times(1)).getById(1002);
        verifyNoMoreInteractions(userApi);
    }


    @Test
    public void testGetRunnerFromOrganisationUsers() throws ExecutionException {
        EventAction action = EventAction.builder()
                .userId(1001)
                .runnerUserId(1002)
                .organisationId(1)
                .build();

        DetailUserView user = new DetailUserView();
        user.setLockStatus(UserLockStatus.LOCKED);

        DetailUserView userWithoutPermissions = new DetailUserView();
        userWithoutPermissions.setId(1003);
        userWithoutPermissions.setPermissions(
            ImmutableMap.of(
            "Dynamic.BalanceTopUp", newHashSet("Enable"),
            "Dynamic.ChangeDivision", newHashSet("Enable")));
        DetailUserView userWithReqPermissions = new DetailUserView();

        userWithReqPermissions.setId(1004);
        userWithReqPermissions.setPermissions(
            ImmutableMap.of(
            "Dynamic.BalanceTopUp", newHashSet("Enable"),
            "Dynamic.ChangeDivision", newHashSet("Enable"),
            "SmartEmail", newHashSet("Enable"),
            "Campaigns", newHashSet("Execute")));

        when(userApi.hasAccess(1, 1002)).thenReturn(true);
        when(userApi.getById(1002)).thenReturn(user);
        when(userApi.getUsersByOrg(1)).thenReturn(
            Lists.newArrayList(userWithoutPermissions, userWithReqPermissions));

        when(organisationServiceClient.getConfigValue(1, 601)).thenReturn(
            "{permissions:{'Dynamic.BalanceTopUp':['Enable'],'Dynamic.ChangeDivision':['Enable'],'SmartEmail':['Enable'],'Campaigns':['Execute']}}");

        Assert.assertEquals(1004, testObj.getRunner(action));

        verify(userApi, times(1)).hasAccess(1, 1002);
        verify(userApi, times(1)).getById(1002);
        verify(userApi, times(1)).getUsersByOrg(1);
        verify(organisationServiceClient, times(1)).getConfigValue(1, 601);
        verifyNoMoreInteractions(userApi);

    }


    @Test(expected = UncheckedExecutionException.class)
    public void testGetRunnerWithNoSuitableUsers() throws ExecutionException {
        EventAction action = EventAction.builder()
                .userId(1001)
                .runnerUserId(1002)
                .organisationId(1)
                .build();

        DetailUserView user = new DetailUserView();
        user.setDeleted(true);

        DetailUserView userWithoutPermissions = new DetailUserView();
        userWithoutPermissions.setId(1003);
        userWithoutPermissions.setPermissions(
                ImmutableMap.of(
                        "Dynamic.BalanceTopUp", newHashSet("Enable"),
                        "Dynamic.ChangeDivision", newHashSet("Enable")));

        DetailUserView blockedUserWithReqPermissions = new DetailUserView();
        blockedUserWithReqPermissions.setId(1004);
        blockedUserWithReqPermissions.setDeleted(true);
        blockedUserWithReqPermissions.setPermissions(
                ImmutableMap.of(
                        "Dynamic.BalanceTopUp", newHashSet("Enable"),
                        "Dynamic.ChangeDivision", newHashSet("Enable"),
                        "SmartEmail", newHashSet("Enable"),
                        "Campaigns", newHashSet("Execute")));

        when(organisationServiceClient.getConfigValue(1, 601)).thenReturn(
                "{permissions:{'Dynamic.BalanceTopUp':['Enable'],'Dynamic.ChangeDivision':['Enable'],'SmartEmail':['Enable'],'Campaigns':['Execute']}}");
        when(userApi.hasAccess(1, 1002)).thenReturn(true);
        when(userApi.getById(1002)).thenReturn(user);
        when(userApi.getUsersByOrg(1)).thenReturn(Arrays.asList(
                userWithoutPermissions, blockedUserWithReqPermissions));

        testObj.getRunner(action);
    }

    @Test
    public void testRunnerIsNotBlocked() throws ExecutionException {
        EventAction action = EventAction.builder()
                .userId(1001)
                .runnerUserId(1002)
                .organisationId(1)
                .build();

        DetailUserView user = new DetailUserView();
        user.setDeleted(false);
        user.setLockStatus(UserLockStatus.UNLOCKED);

        when(userApi.hasAccess(1, 1002)).thenReturn(true);
        when(userApi.getById(1002)).thenReturn(user);

        Assert.assertEquals(1002, testObj.getRunner(action));

        verify(userApi, times(1)).hasAccess(1, 1002);
        verify(userApi, times(1)).getById(1002);
        verifyNoMoreInteractions(userApi);

    }

}