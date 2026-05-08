package io.resrv.application.resource.in;

import java.util.List;

public interface ListResourcesUseCase {

    List<ResourceResult> list(final ListResourcesQuery query);
}
