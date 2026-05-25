package io.resrv.timeslot.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

final class TimeslotArchitectureTest {

    private static final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.resrv.timeslot");

    @Test
    void domain_does_not_depend_on_application_or_adapters() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.timeslot.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.resrv.timeslot.application..",
                        "io.resrv.timeslot.adapter..",
                        "io.resrv.timeslot.api..")
                .check(classes);
    }

    @Test
    void domain_has_no_framework_dependencies() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.timeslot.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
                .check(classes);
    }

    @Test
    void application_does_not_depend_on_adapters_or_api() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.timeslot.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.timeslot.adapter..", "io.resrv.timeslot.api..")
                .check(classes);
    }

    @Test
    void timeslot_does_not_depend_on_platform_domain() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.timeslot..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.platform.domain..", "io.resrv.platform.application..")
                .check(classes);
    }
}
