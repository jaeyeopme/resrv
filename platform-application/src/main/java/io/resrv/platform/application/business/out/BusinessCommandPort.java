package io.resrv.platform.application.business.out;

import io.resrv.platform.domain.business.Business;

public interface BusinessCommandPort {

    void save(Business business);
}
