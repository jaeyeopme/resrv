package io.resrv.bootstrap.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Constitution Art. I enforcement: hexagonal dependency direction and domain purity. */
final class ArchitectureTest {

    private static final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.resrv");

    @Test
    void domain_does_not_depend_on_outer_layers() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.resrv.application..", "io.resrv.adapter..", "io.resrv.bootstrap..")
                .because("Domain must not depend on outer layers (Art. I)")
                .check(classes);
    }

    @Test
    void domain_has_no_framework_dependencies() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
                .because("Domain must have zero framework dependencies (Art. I)")
                .check(classes);
    }

    @Test
    void application_does_not_depend_on_adapters() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.adapter..", "io.resrv.bootstrap..")
                .because("Application must not depend on adapters or bootstrap (Art. I)")
                .check(classes);
    }

    @Test
    void adapters_do_not_depend_on_bootstrap() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.adapter..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.bootstrap..")
                .because("Adapters must not depend on bootstrap (Art. I)")
                .check(classes);
    }

    @Test
    void use_case_interfaces_must_reside_in_in_package() {
        classes()
                .that()
                .areInterfaces()
                .and()
                .resideInAPackage("io.resrv.application..")
                .and()
                .haveSimpleNameEndingWith("UseCase")
                .should()
                .resideInAPackage("..application.*.in..")
                .because("UseCase ports must be in the 'in' sub-package (Art. I)")
                .check(classes);
    }

    @Test
    void out_port_interfaces_must_reside_in_out_package() {
        classes()
                .that()
                .areInterfaces()
                .and()
                .resideInAPackage("io.resrv.application..")
                .and()
                .haveSimpleNameEndingWith("Port")
                .should()
                .resideInAPackage("..application.*.out..")
                .because("Out-ports must be in the 'out' sub-package (Art. I)")
                .check(classes);
    }
}
