package io.resrv.ticketing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

final class TicketingArchitectureTest {

    private static final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.resrv.ticketing");

    @Test
    void domain_does_not_depend_on_application_or_adapters() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.ticketing.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.resrv.ticketing.application..",
                        "io.resrv.ticketing.adapter..",
                        "io.resrv.ticketing.api..")
                .check(classes);
    }

    @Test
    void domain_has_no_framework_dependencies() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.ticketing.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
                .check(classes);
    }

    @Test
    void application_does_not_depend_on_adapters_or_api() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.ticketing.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.ticketing.adapter..", "io.resrv.ticketing.api..")
                .check(classes);
    }

    @Test
    void ticketing_does_not_depend_on_platform_internals() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.ticketing..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.resrv.platform.domain..",
                        "io.resrv.platform.adapter..",
                        "io.resrv.platform.contract..",
                        "io.resrv.platform.api..")
                .check(classes);
    }

    @Test
    void only_platform_outbound_adapter_uses_platform_exchange() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.ticketing..")
                .and()
                .resideOutsideOfPackage("io.resrv.ticketing.adapter.out.platform..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("io.resrv.platform.exchange..")
                .check(classes);
    }

    @Test
    void direct_database_access_stays_in_outbound_adapters() {
        noClasses()
                .that()
                .resideOutsideOfPackage("io.resrv.ticketing.adapter.out..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..", "javax.sql..", "org.springframework.jdbc..")
                .check(classes);
    }

    @Test
    void seat_and_purchase_domain_stay_inside_ticketing_domain() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "io.resrv.ticketing.domain.seat..", "io.resrv.ticketing.domain.purchase..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.platform..", "org.springframework..", "jakarta..")
                .check(classes);
    }
}
