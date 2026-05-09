package io.resrv.application.customer.in;

public interface RegisterCustomerUseCase {

    CustomerResult register(RegisterCustomerCommand command);
}
