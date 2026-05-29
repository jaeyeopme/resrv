package io.resrv.platform.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

final class PlatformArchitectureTest {

    private static final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.resrv.platform");

    @Test
    void domain_does_not_depend_on_application_or_adapters() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.platform.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.resrv.platform.application..",
                        "io.resrv.platform.adapter..",
                        "io.resrv.platform.api..")
                .check(classes);
    }

    @Test
    void domain_has_no_framework_dependencies() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.platform.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
                .check(classes);
    }

    @Test
    void application_does_not_depend_on_adapters_or_api() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.platform.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.platform.adapter..", "io.resrv.platform.api..")
                .check(classes);
    }

    @Test
    void direct_database_access_stays_in_outbound_adapters() {
        noClasses()
                .that()
                .resideOutsideOfPackage("io.resrv.platform.adapter.out..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..", "javax.sql..", "org.springframework.jdbc..")
                .check(classes);
    }

    @Test
    void inbound_web_adapters_do_not_depend_on_persistence_adapters() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.platform.adapter.in.web..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.platform.adapter.out.persistence..")
                .check(classes);
    }

    @Test
    void only_api_runtime_assembles_timeslot() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.platform..")
                .and()
                .resideOutsideOfPackage("io.resrv.platform.api..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("io.resrv.timeslot..")
                .check(classes);
    }
}
