package io.resrv.timeslot.application.discovery;

import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublicBookableBusinessResolver {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PublicBookableBusinessResolver.class);

    private final BusinessLookupPort businessLookupPort;
    private final BusinessBookingSettingsQueryPort settingsQueryPort;

    public PublicBookableBusinessResolver(
            final BusinessLookupPort businessLookupPort,
            final BusinessBookingSettingsQueryPort settingsQueryPort) {
        this.businessLookupPort = businessLookupPort;
        this.settingsQueryPort = settingsQueryPort;
    }

    public BookableBusiness resolve(final String businessSlug) {
        Objects.requireNonNull(businessSlug, "Business slug must not be null");
        final var activeBusiness = businessLookupPort.findActiveBySlug(businessSlug);
        if (activeBusiness.isEmpty()) {
            final var fact =
                    businessLookupPort.findCurrentSummaryBySlug(businessSlug).isPresent()
                            ? PublicDiscoveryDenialFact.BUSINESS_INACTIVE
                            : PublicDiscoveryDenialFact.BUSINESS_SLUG_NOT_FOUND;
            throw denied(businessSlug, fact);
        }
        final var business = activeBusiness.orElseThrow();
        final var settings = settingsQueryPort.findByBusinessId(business.id());
        if (settings.isEmpty()) {
            throw denied(businessSlug, PublicDiscoveryDenialFact.BUSINESS_SETTINGS_MISSING);
        }
        return new BookableBusiness(business, settings.orElseThrow());
    }

    public PublicDiscoveryNotFoundException denied(
            final String businessSlug, final PublicDiscoveryDenialFact fact) {
        LOGGER.info(
                "Public booking discovery denied: businessSlug={}, fact={}", businessSlug, fact);
        return new PublicDiscoveryNotFoundException();
    }

    public record BookableBusiness(
            BusinessLookupPort.BusinessView business, BusinessBookingSettings settings) {}
}
