package com.kuocai.cdn.service.domain.operation;

import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiCdnDomainServiceImplTest {

    private final MultiCdnDomainServiceImpl service =
            new MultiCdnDomainServiceImpl(null, null, null);

    @Test
    void allOnlineAggregatesToOnline() {
        assertEquals(DomainStatus.ONLINE,
                service.aggregateDomainStatus(Arrays.asList(DomainStatus.ONLINE, DomainStatus.ONLINE)));
    }

    @Test
    void mixedOnlineAndOfflineRemainsConfiguring() {
        assertEquals(DomainStatus.CONFIGURING,
                service.aggregateDomainStatus(Arrays.asList(DomainStatus.ONLINE, DomainStatus.OFFLINE)));
    }

    @Test
    void anyExplicitFailureAggregatesToFailure() {
        assertEquals(DomainStatus.CONFIGURE_FAILED,
                service.aggregateDomainStatus(Arrays.asList(DomainStatus.ONLINE, DomainStatus.CHECK_FAILED)));
    }

    @Test
    void missingStatusesRemainConfiguring() {
        assertEquals(DomainStatus.CONFIGURING,
                service.aggregateDomainStatus(Collections.emptyList()));
    }
}
