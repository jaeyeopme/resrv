package io.resrv.platform.exchange;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

final class PlatformExchangeArchitectureTest {

    private static final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.resrv.platform.exchange");

    @Test
    void exchange_api_has_no_framework_or_platform_implementation_dependencies() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.platform.exchange..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "javax..",
                        "org.hibernate..",
                        "io.resrv.platform.application..",
                        "io.resrv.platform.adapter..",
                        "io.resrv.platform.api..",
                        "io.resrv.platform.domain..")
                .check(classes);
    }

    @Test
    void exchange_api_does_not_define_event_packages_yet() {
        noClasses().should().resideInAPackage("io.resrv.platform.exchange.event..").check(classes);
    }
}
