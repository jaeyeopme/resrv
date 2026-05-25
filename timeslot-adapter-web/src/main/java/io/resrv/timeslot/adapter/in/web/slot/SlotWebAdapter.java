package io.resrv.timeslot.adapter.in.web.slot;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.slot.in.ListSlotsQuery;
import io.resrv.timeslot.application.slot.in.ListSlotsUseCase;
import io.resrv.timeslot.application.slot.in.SlotResult;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/resources/{resourceId}/slots")
class SlotWebAdapter {

    private final ListSlotsUseCase useCase;

    SlotWebAdapter(final ListSlotsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<SlotResponse> list(
            @PathVariable final UUID businessId,
            @PathVariable final UUID resourceId,
            @RequestParam final LocalDate date) {
        return useCase
                .listSlots(
                        new ListSlotsQuery(
                                BusinessId.of(businessId), ResourceId.of(resourceId), date))
                .stream()
                .map(SlotResponse::from)
                .toList();
    }

    record SlotResponse(String slotId, OffsetDateTime startAt, OffsetDateTime endAt) {

        static SlotResponse from(final SlotResult result) {
            return new SlotResponse(
                    result.id(), result.startAtBusinessTime(), result.endAtBusinessTime());
        }
    }
}
