package io.resrv.application.admin.out;

import io.resrv.domain.admin.Admin;

public interface AdminCommandPort {

    void save(final Admin admin);
}
