# Reservation Platform v1 Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 현재 tenant-local 예약 API를 platform account/business와 timeslot booking bounded context로 재구성한다.

**Architecture:** Gradle 모듈을 bounded context 기준으로 분리한다. `platform-api`가 account, business, membership, JWT 발급을 맡고 `timeslot-booking-api`가 booking settings, resources, schedules, slots, reservations를 맡는다. 두 API는 같은 Postgres를 쓰되 `platform`/`timeslot` schema를 나누고 cross-schema FK 없이 UUID reference와 application port로 연결한다.

**Tech Stack:** Java 25, Spring Boot 4, Spring MVC, Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, PostgreSQL advisory lock, Testcontainers, ArchUnit, Gradle Kotlin DSL, JaCoCo, Spotless.

---

## Scope Check

이 작업은 큰 리팩터링이지만 순서대로 쪼개면 각 task가 독립 검증 가능하다.

1. Module boundary와 shared ID 먼저 만든다.
2. Platform identity를 만든 뒤 JWT를 account 중심으로 전환한다.
3. Timeslot booking settings/resource/schedule을 만든다.
4. Virtual slot과 reservation facts model을 만든다.
5. 기존 tenant/customer/admin/availability/reservation API를 새 API로 대체한다.

성능 검증(`500 sustained RPS`, `2000 burst RPS`)은 이 plan 범위가 아니다. 별도 k6/container 제한 plan에서 다룬다.

## File Structure Map

새 module:

- `shared-kernel`: UUID 기반 ID value object와 공통 time/id helpers.
- `platform-domain`: `Account`, `Business`, `BusinessMembership`.
- `platform-application`: account/business/auth use cases and ports.
- `platform-adapter-persistence`: `platform` schema JPA adapters.
- `platform-adapter-web`: platform REST controllers and request/response DTOs.
- `platform-api`: platform boot app, security config, JWT signing.
- `timeslot-domain`: booking settings, resource, schedule, slot, reservation facts.
- `timeslot-application`: booking use cases and ports.
- `timeslot-adapter-persistence`: `timeslot` schema JPA adapters, advisory lock adapter.
- `timeslot-adapter-web`: timeslot REST controllers and DTOs.
- `timeslot-booking-api`: timeslot boot app, resource-server security config.

Old modules stay until replacement is complete:

- `domain`, `application`, `adapter-web`, `adapter-persistence`, `bootstrap`.

During migration, keep old modules compiling. After new integration tests pass, remove old modules in final cleanup task.

---

### Task 1: Create Bounded Context Gradle Modules

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Create: `shared-kernel/build.gradle.kts`
- Create: `platform-domain/build.gradle.kts`
- Create: `platform-application/build.gradle.kts`
- Create: `platform-adapter-persistence/build.gradle.kts`
- Create: `platform-adapter-web/build.gradle.kts`
- Create: `platform-api/build.gradle.kts`
- Create: `timeslot-domain/build.gradle.kts`
- Create: `timeslot-application/build.gradle.kts`
- Create: `timeslot-adapter-persistence/build.gradle.kts`
- Create: `timeslot-adapter-web/build.gradle.kts`
- Create: `timeslot-booking-api/build.gradle.kts`
- Test: `platform-api/src/test/java/io/resrv/platform/api/architecture/PlatformArchitectureTest.java`
- Test: `timeslot-booking-api/src/test/java/io/resrv/timeslot/api/architecture/TimeslotArchitectureTest.java`

- [ ] **Step 1: Write platform architecture test**

Create `platform-api/src/test/java/io/resrv/platform/api/architecture/PlatformArchitectureTest.java`:

```java
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
    void application_does_not_depend_on_adapters_or_api() {
        noClasses()
                .that()
                .resideInAPackage("io.resrv.platform.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.resrv.platform.adapter..", "io.resrv.platform.api..")
                .check(classes);
    }
}
```

- [ ] **Step 2: Write timeslot architecture test**

Create `timeslot-booking-api/src/test/java/io/resrv/timeslot/api/architecture/TimeslotArchitectureTest.java`:

```java
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
```

- [ ] **Step 3: Run architecture tests and verify module paths fail**

Run:

```bash
./gradlew :platform-api:test :timeslot-booking-api:test
```

Expected: FAIL because modules do not exist in `settings.gradle.kts`.

- [ ] **Step 4: Add modules to Gradle settings**

Modify `settings.gradle.kts` include block to:

```kotlin
include(
    "shared-kernel",
    "platform-domain",
    "platform-application",
    "platform-adapter-persistence",
    "platform-adapter-web",
    "platform-api",
    "timeslot-domain",
    "timeslot-application",
    "timeslot-adapter-persistence",
    "timeslot-adapter-web",
    "timeslot-booking-api",
    "domain",
    "application",
    "adapter-web",
    "adapter-persistence",
    "bootstrap",
)
```

Modify `jacocoLineCoverageMinimums` in `build.gradle.kts`:

```kotlin
val jacocoLineCoverageMinimums =
    mapOf(
        "shared-kernel" to "0.85".toBigDecimal(),
        "platform-domain" to "0.85".toBigDecimal(),
        "platform-application" to "0.90".toBigDecimal(),
        "platform-adapter-web" to "0.90".toBigDecimal(),
        "platform-adapter-persistence" to "0.90".toBigDecimal(),
        "platform-api" to "0.90".toBigDecimal(),
        "timeslot-domain" to "0.85".toBigDecimal(),
        "timeslot-application" to "0.90".toBigDecimal(),
        "timeslot-adapter-web" to "0.90".toBigDecimal(),
        "timeslot-adapter-persistence" to "0.90".toBigDecimal(),
        "timeslot-booking-api" to "0.90".toBigDecimal(),
        "domain" to "0.85".toBigDecimal(),
        "application" to "0.90".toBigDecimal(),
        "adapter-web" to "0.90".toBigDecimal(),
        "adapter-persistence" to "0.90".toBigDecimal(),
        "bootstrap" to "0.90".toBigDecimal(),
    )
```

- [ ] **Step 5: Add module build files**

Create `shared-kernel/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    api(libs.java.uuid.generator)
    testImplementation(libs.spring.boot.starter.test)
}
```

Create `platform-domain/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    api(project(":shared-kernel"))
    testImplementation(libs.spring.boot.starter.test)
}
```

Create `timeslot-domain/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    api(project(":shared-kernel"))
    testImplementation(libs.spring.boot.starter.test)
}
```

Create `platform-application/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":platform-domain"))
    implementation(libs.spring.tx)
    implementation(libs.spring.context)
    testImplementation(libs.spring.boot.starter.test)
}
```

Create `timeslot-application/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":timeslot-domain"))
    implementation(project(":shared-kernel"))
    implementation(libs.spring.tx)
    implementation(libs.spring.context)
    testImplementation(libs.spring.boot.starter.test)
}
```

Create `platform-adapter-persistence/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":platform-application"))
    implementation(project(":platform-domain"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.flyway.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
```

Create `timeslot-adapter-persistence/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":timeslot-application"))
    implementation(project(":timeslot-domain"))
    implementation(project(":shared-kernel"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.flyway.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
```

Create `platform-adapter-web/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":platform-application"))
    implementation(project(":platform-domain"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.security.test)
}
```

Create `timeslot-adapter-web/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    jacoco
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":timeslot-application"))
    implementation(project(":timeslot-domain"))
    implementation(project(":shared-kernel"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.security.test)
}
```

Create `platform-api/build.gradle.kts`:

```kotlin
plugins {
    java
    jacoco
    checkstyle
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":platform-application"))
    implementation(project(":platform-domain"))
    implementation(project(":platform-adapter-web"))
    implementation(project(":platform-adapter-persistence"))
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.java.jwt)
    developmentOnly(libs.spring.boot.docker.compose)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
}
```

Create `timeslot-booking-api/build.gradle.kts`:

```kotlin
plugins {
    java
    jacoco
    checkstyle
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":timeslot-application"))
    implementation(project(":timeslot-domain"))
    implementation(project(":timeslot-adapter-web"))
    implementation(project(":timeslot-adapter-persistence"))
    implementation(project(":shared-kernel"))
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    developmentOnly(libs.spring.boot.docker.compose)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
}
```

- [ ] **Step 6: Run module tests**

Run:

```bash
./gradlew :platform-api:test :timeslot-booking-api:test
```

Expected: PASS. Empty modules plus architecture tests compile.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts build.gradle.kts shared-kernel platform-domain platform-application platform-adapter-persistence platform-adapter-web platform-api timeslot-domain timeslot-application timeslot-adapter-persistence timeslot-adapter-web timeslot-booking-api
git commit -m "build: add bounded context modules"
```

---

### Task 2: Shared Kernel IDs and Time Helpers

**Files:**
- Create: `shared-kernel/src/main/java/io/resrv/shared/kernel/AccountId.java`
- Create: `shared-kernel/src/main/java/io/resrv/shared/kernel/BusinessId.java`
- Create: `shared-kernel/src/main/java/io/resrv/shared/kernel/ResourceId.java`
- Create: `shared-kernel/src/main/java/io/resrv/shared/kernel/ReservationId.java`
- Create: `shared-kernel/src/main/java/io/resrv/shared/kernel/Timezone.java`
- Test: `shared-kernel/src/test/java/io/resrv/shared/kernel/SharedIdTest.java`
- Test: `shared-kernel/src/test/java/io/resrv/shared/kernel/TimezoneTest.java`

- [ ] **Step 1: Write shared kernel tests**

Create `shared-kernel/src/test/java/io/resrv/shared/kernel/SharedIdTest.java`:

```java
package io.resrv.shared.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SharedIdTest {

    @Test
    void accountId_wrapsUuid() {
        final var uuid = UUID.randomUUID();
        assertEquals(uuid, AccountId.of(uuid).value());
    }

    @Test
    void idsWithSameUuidAreEqualInsideSameType() {
        final var uuid = UUID.randomUUID();
        assertEquals(BusinessId.of(uuid), BusinessId.of(uuid));
    }

    @Test
    void differentIdTypesAreNotEqualEvenWithSameUuid() {
        final var uuid = UUID.randomUUID();
        assertNotEquals(AccountId.of(uuid), BusinessId.of(uuid));
    }

    @Test
    void idCannotWrapNull() {
        assertThrows(NullPointerException.class, () -> ResourceId.of(null));
    }
}
```

Create `shared-kernel/src/test/java/io/resrv/shared/kernel/TimezoneTest.java`:

```java
package io.resrv.shared.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

final class TimezoneTest {

    @Test
    void acceptsIanaTimezone() {
        assertEquals(ZoneId.of("Asia/Seoul"), Timezone.of("Asia/Seoul").value());
    }

    @Test
    void rejectsBlankTimezone() {
        assertThrows(IllegalArgumentException.class, () -> Timezone.of(" "));
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :shared-kernel:test --tests "*SharedIdTest" --tests "*TimezoneTest"
```

Expected: FAIL because shared ID classes do not exist.

- [ ] **Step 3: Add ID value objects**

Create `shared-kernel/src/main/java/io/resrv/shared/kernel/AccountId.java`:

```java
package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "Account id must not be null");
    }

    public static AccountId create() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(final UUID value) {
        return new AccountId(value);
    }
}
```

Create `shared-kernel/src/main/java/io/resrv/shared/kernel/BusinessId.java`:

```java
package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record BusinessId(UUID value) {

    public BusinessId {
        Objects.requireNonNull(value, "Business id must not be null");
    }

    public static BusinessId create() {
        return new BusinessId(UUID.randomUUID());
    }

    public static BusinessId of(final UUID value) {
        return new BusinessId(value);
    }
}
```

Create `shared-kernel/src/main/java/io/resrv/shared/kernel/ResourceId.java`:

```java
package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record ResourceId(UUID value) {

    public ResourceId {
        Objects.requireNonNull(value, "Resource id must not be null");
    }

    public static ResourceId create() {
        return new ResourceId(UUID.randomUUID());
    }

    public static ResourceId of(final UUID value) {
        return new ResourceId(value);
    }
}
```

Create `shared-kernel/src/main/java/io/resrv/shared/kernel/ReservationId.java`:

```java
package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "Reservation id must not be null");
    }

    public static ReservationId create() {
        return new ReservationId(UUID.randomUUID());
    }

    public static ReservationId of(final UUID value) {
        return new ReservationId(value);
    }
}
```

Create `shared-kernel/src/main/java/io/resrv/shared/kernel/Timezone.java`:

```java
package io.resrv.shared.kernel;

import java.time.ZoneId;
import java.util.Objects;

public record Timezone(ZoneId value) {

    public Timezone {
        Objects.requireNonNull(value, "Timezone must not be null");
    }

    public static Timezone of(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Timezone must not be blank");
        }
        return new Timezone(ZoneId.of(value));
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
./gradlew :shared-kernel:test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared-kernel
git commit -m "feat: add shared identity primitives"
```

---

### Task 3: Platform Account Domain and Registration

**Files:**
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/account/Account.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountEmail.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountName.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountStatus.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountEmailAlreadyExistsException.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/account/in/RegisterAccountCommand.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/account/in/RegisterAccountResult.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/account/in/RegisterAccountUseCase.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/account/out/AccountCommandPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/account/out/AccountQueryPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/security/out/PasswordHashingPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/account/RegisterAccountService.java`
- Test: `platform-domain/src/test/java/io/resrv/platform/domain/account/AccountTest.java`
- Test: `platform-application/src/test/java/io/resrv/platform/application/account/RegisterAccountServiceTest.java`

- [ ] **Step 1: Write account domain test**

Create `platform-domain/src/test/java/io/resrv/platform/domain/account/AccountTest.java`:

```java
package io.resrv.platform.domain.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

final class AccountTest {

    @Test
    void createAccountStartsActive() {
        final var now = Instant.parse("2026-05-25T00:00:00Z");
        final var account =
                Account.create(
                        new AccountEmail("user@example.com"),
                        new AccountName("User One"),
                        "hashed-password",
                        now);

        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals("user@example.com", account.email().value());
        assertEquals(now, account.createdAt());
    }

    @Test
    void rejectsBlankHashedPassword() {
        final var now = Instant.parse("2026-05-25T00:00:00Z");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Account.create(
                                new AccountEmail("user@example.com"),
                                new AccountName("User One"),
                                " ",
                                now));
    }
}
```

- [ ] **Step 2: Run domain test and verify failure**

Run:

```bash
./gradlew :platform-domain:test --tests "*AccountTest"
```

Expected: FAIL because `Account` does not exist.

- [ ] **Step 3: Add account domain classes**

Create `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountStatus.java`:

```java
package io.resrv.platform.domain.account;

public enum AccountStatus {
    ACTIVE,
    DISABLED
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountEmail.java`:

```java
package io.resrv.platform.domain.account;

import java.util.regex.Pattern;

public record AccountEmail(String value) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public AccountEmail {
        if (value == null || value.isBlank() || !EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Account email must be valid");
        }
        value = value.trim().toLowerCase();
    }
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountName.java`:

```java
package io.resrv.platform.domain.account;

public record AccountName(String value) {

    public AccountName {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new IllegalArgumentException("Account name must be 1-100 characters");
        }
        value = value.trim();
    }
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/account/Account.java`:

```java
package io.resrv.platform.domain.account;

import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import java.util.Objects;

public final class Account {

    private final AccountId id;
    private final AccountEmail email;
    private final AccountName name;
    private final String hashedPassword;
    private final AccountStatus status;
    private final Instant createdAt;

    private Account(
            final AccountId id,
            final AccountEmail email,
            final AccountName name,
            final String hashedPassword,
            final AccountStatus status,
            final Instant createdAt) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Account hashed password must not be blank");
        }
        this.id = Objects.requireNonNull(id, "Account id must not be null");
        this.email = Objects.requireNonNull(email, "Account email must not be null");
        this.name = Objects.requireNonNull(name, "Account name must not be null");
        this.hashedPassword = hashedPassword;
        this.status = Objects.requireNonNull(status, "Account status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Account createdAt must not be null");
    }

    public static Account create(
            final AccountEmail email,
            final AccountName name,
            final String hashedPassword,
            final Instant now) {
        return new Account(AccountId.create(), email, name, hashedPassword, AccountStatus.ACTIVE, now);
    }

    public static Account reconstitute(
            final AccountId id,
            final AccountEmail email,
            final AccountName name,
            final String hashedPassword,
            final AccountStatus status,
            final Instant createdAt) {
        return new Account(id, email, name, hashedPassword, status, createdAt);
    }

    public boolean active() {
        return status == AccountStatus.ACTIVE;
    }

    public AccountId id() {
        return id;
    }

    public AccountEmail email() {
        return email;
    }

    public AccountName name() {
        return name;
    }

    public String hashedPassword() {
        return hashedPassword;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/account/AccountEmailAlreadyExistsException.java`:

```java
package io.resrv.platform.domain.account;

public final class AccountEmailAlreadyExistsException extends RuntimeException {

    public AccountEmailAlreadyExistsException(final AccountEmail email) {
        super("Account email already exists: " + email.value());
    }
}
```

- [ ] **Step 4: Run domain test**

Run:

```bash
./gradlew :platform-domain:test --tests "*AccountTest"
```

Expected: PASS.

- [ ] **Step 5: Write registration service test**

Create `platform-application/src/test/java/io/resrv/platform/application/account/RegisterAccountServiceTest.java`:

```java
package io.resrv.platform.application.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.in.RegisterAccountCommand;
import io.resrv.platform.application.account.out.AccountCommandPort;
import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountEmailAlreadyExistsException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class RegisterAccountServiceTest {

    private AccountCommandPort commandPort;
    private AccountQueryPort queryPort;
    private PasswordHashingPort passwordHashingPort;
    private RegisterAccountService service;

    @BeforeEach
    void setUp() {
        commandPort = mock(AccountCommandPort.class);
        queryPort = mock(AccountQueryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        service =
                new RegisterAccountService(
                        commandPort,
                        queryPort,
                        passwordHashingPort,
                        Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void registersAccountWithHashedPassword() {
        when(queryPort.findByEmail(new AccountEmail("user@example.com"))).thenReturn(Optional.empty());
        when(passwordHashingPort.hash("plain-password")).thenReturn("hashed-password");

        final var result =
                service.register(
                        new RegisterAccountCommand(
                                "user@example.com", "User One", "plain-password"));

        assertEquals("user@example.com", result.email());
        verify(commandPort).save(org.mockito.ArgumentMatchers.any(Account.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        final var email = new AccountEmail("user@example.com");
        when(queryPort.findByEmail(email))
                .thenReturn(
                        Optional.of(
                                Account.create(
                                        email,
                                        new io.resrv.platform.domain.account.AccountName("User One"),
                                        "hashed-password",
                                        Instant.parse("2026-05-25T00:00:00Z"))));

        assertThrows(
                AccountEmailAlreadyExistsException.class,
                () ->
                        service.register(
                                new RegisterAccountCommand(
                                        "user@example.com", "User Two", "plain-password")));
    }
}
```

- [ ] **Step 6: Add registration application classes**

Create `platform-application/src/main/java/io/resrv/platform/application/account/in/RegisterAccountCommand.java`:

```java
package io.resrv.platform.application.account.in;

public record RegisterAccountCommand(String email, String name, String password) {}
```

Create `platform-application/src/main/java/io/resrv/platform/application/account/in/RegisterAccountResult.java`:

```java
package io.resrv.platform.application.account.in;

import io.resrv.platform.domain.account.Account;
import java.util.UUID;

public record RegisterAccountResult(UUID id, String email, String name) {

    public static RegisterAccountResult from(final Account account) {
        return new RegisterAccountResult(
                account.id().value(), account.email().value(), account.name().value());
    }
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/account/in/RegisterAccountUseCase.java`:

```java
package io.resrv.platform.application.account.in;

public interface RegisterAccountUseCase {

    RegisterAccountResult register(RegisterAccountCommand command);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/account/out/AccountCommandPort.java`:

```java
package io.resrv.platform.application.account.out;

import io.resrv.platform.domain.account.Account;

public interface AccountCommandPort {

    void save(Account account);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/account/out/AccountQueryPort.java`:

```java
package io.resrv.platform.application.account.out;

import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.shared.kernel.AccountId;
import java.util.Optional;

public interface AccountQueryPort {

    Optional<Account> findById(AccountId accountId);

    Optional<Account> findByEmail(AccountEmail email);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/security/out/PasswordHashingPort.java`:

```java
package io.resrv.platform.application.security.out;

public interface PasswordHashingPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/account/RegisterAccountService.java`:

```java
package io.resrv.platform.application.account;

import io.resrv.platform.application.account.in.RegisterAccountCommand;
import io.resrv.platform.application.account.in.RegisterAccountResult;
import io.resrv.platform.application.account.in.RegisterAccountUseCase;
import io.resrv.platform.application.account.out.AccountCommandPort;
import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountEmailAlreadyExistsException;
import io.resrv.platform.domain.account.AccountName;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterAccountService implements RegisterAccountUseCase {

    private final AccountCommandPort commandPort;
    private final AccountQueryPort queryPort;
    private final PasswordHashingPort passwordHashingPort;
    private final Clock clock;

    public RegisterAccountService(
            final AccountCommandPort commandPort,
            final AccountQueryPort queryPort,
            final PasswordHashingPort passwordHashingPort,
            final Clock clock) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.passwordHashingPort = passwordHashingPort;
        this.clock = clock;
    }

    @Override
    public RegisterAccountResult register(final RegisterAccountCommand command) {
        final var email = new AccountEmail(command.email());
        if (queryPort.findByEmail(email).isPresent()) {
            throw new AccountEmailAlreadyExistsException(email);
        }
        final var account =
                Account.create(
                        email,
                        new AccountName(command.name()),
                        passwordHashingPort.hash(command.password()),
                        clock.instant());
        commandPort.save(account);
        return RegisterAccountResult.from(account);
    }
}
```

- [ ] **Step 7: Run platform tests**

Run:

```bash
./gradlew :platform-domain:test :platform-application:test --tests "*RegisterAccountServiceTest"
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add platform-domain platform-application
git commit -m "feat(auth): add platform accounts"
```

---

### Task 4: Platform Business and Membership

**Files:**
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/business/Business.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessName.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessSlug.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessStatus.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessSlugAlreadyExistsException.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/membership/BusinessMembership.java`
- Create: `platform-domain/src/main/java/io/resrv/platform/domain/membership/BusinessRole.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/business/in/CreateBusinessCommand.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/business/in/CreateBusinessResult.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/business/in/CreateBusinessUseCase.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/business/out/BusinessCommandPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/business/out/BusinessQueryPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/membership/out/BusinessMembershipCommandPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/membership/out/BusinessMembershipQueryPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/business/CreateBusinessService.java`
- Test: `platform-application/src/test/java/io/resrv/platform/application/business/CreateBusinessServiceTest.java`

- [ ] **Step 1: Write business creation test**

Create `platform-application/src/test/java/io/resrv/platform/application/business/CreateBusinessServiceTest.java`:

```java
package io.resrv.platform.application.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.business.in.CreateBusinessCommand;
import io.resrv.platform.application.business.out.BusinessCommandPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.Timezone;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class CreateBusinessServiceTest {

    private BusinessCommandPort businessCommandPort;
    private BusinessQueryPort businessQueryPort;
    private BusinessMembershipCommandPort membershipCommandPort;
    private CreateBusinessService service;

    @BeforeEach
    void setUp() {
        businessCommandPort = mock(BusinessCommandPort.class);
        businessQueryPort = mock(BusinessQueryPort.class);
        membershipCommandPort = mock(BusinessMembershipCommandPort.class);
        service =
                new CreateBusinessService(
                        businessCommandPort,
                        businessQueryPort,
                        membershipCommandPort,
                        Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsBusinessAndOwnerMembership() {
        final var ownerId = AccountId.create();
        when(businessQueryPort.findBySlug(new BusinessSlug("salon-a"))).thenReturn(Optional.empty());

        final var result =
                service.create(
                        new CreateBusinessCommand(ownerId, "Salon A", "salon-a", "Asia/Seoul"));

        assertEquals("Salon A", result.name());
        assertEquals("salon-a", result.slug());
        verify(businessCommandPort).save(org.mockito.ArgumentMatchers.any(Business.class));
        verify(membershipCommandPort)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                membership ->
                                        membership.accountId().equals(ownerId)
                                                && membership.role() == BusinessRole.OWNER));
    }
}
```

- [ ] **Step 2: Add business domain classes**

Create `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessStatus.java`:

```java
package io.resrv.platform.domain.business;

public enum BusinessStatus {
    ACTIVE,
    INACTIVE
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessName.java`:

```java
package io.resrv.platform.domain.business;

public record BusinessName(String value) {

    public BusinessName {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new IllegalArgumentException("Business name must be 1-100 characters");
        }
        value = value.trim();
    }
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessSlug.java`:

```java
package io.resrv.platform.domain.business;

public record BusinessSlug(String value) {

    public BusinessSlug {
        if (value == null || !value.matches("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$")) {
            throw new IllegalArgumentException("Business slug must be 3-63 lowercase URL characters");
        }
    }
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/business/Business.java`:

```java
package io.resrv.platform.domain.business;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.Objects;

public final class Business {

    private final BusinessId id;
    private final BusinessName name;
    private final BusinessSlug slug;
    private final Timezone timezone;
    private final BusinessStatus status;
    private final Instant createdAt;

    private Business(
            final BusinessId id,
            final BusinessName name,
            final BusinessSlug slug,
            final Timezone timezone,
            final BusinessStatus status,
            final Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Business id must not be null");
        this.name = Objects.requireNonNull(name, "Business name must not be null");
        this.slug = Objects.requireNonNull(slug, "Business slug must not be null");
        this.timezone = Objects.requireNonNull(timezone, "Business timezone must not be null");
        this.status = Objects.requireNonNull(status, "Business status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Business createdAt must not be null");
    }

    public static Business create(
            final BusinessName name,
            final BusinessSlug slug,
            final Timezone timezone,
            final Instant now) {
        return new Business(BusinessId.create(), name, slug, timezone, BusinessStatus.ACTIVE, now);
    }

    public static Business reconstitute(
            final BusinessId id,
            final BusinessName name,
            final BusinessSlug slug,
            final Timezone timezone,
            final BusinessStatus status,
            final Instant createdAt) {
        return new Business(id, name, slug, timezone, status, createdAt);
    }

    public boolean active() {
        return status == BusinessStatus.ACTIVE;
    }

    public BusinessId id() {
        return id;
    }

    public BusinessName name() {
        return name;
    }

    public BusinessSlug slug() {
        return slug;
    }

    public Timezone timezone() {
        return timezone;
    }

    public BusinessStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/business/BusinessSlugAlreadyExistsException.java`:

```java
package io.resrv.platform.domain.business;

public final class BusinessSlugAlreadyExistsException extends RuntimeException {

    public BusinessSlugAlreadyExistsException(final BusinessSlug slug) {
        super("Business slug already exists: " + slug.value());
    }
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/membership/BusinessRole.java`:

```java
package io.resrv.platform.domain.membership;

public enum BusinessRole {
    OWNER,
    STAFF
}
```

Create `platform-domain/src/main/java/io/resrv/platform/domain/membership/BusinessMembership.java`:

```java
package io.resrv.platform.domain.membership;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class BusinessMembership {

    private final UUID id;
    private final AccountId accountId;
    private final BusinessId businessId;
    private final BusinessRole role;
    private final boolean active;
    private final Instant createdAt;

    private BusinessMembership(
            final UUID id,
            final AccountId accountId,
            final BusinessId businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Business membership id must not be null");
        this.accountId = Objects.requireNonNull(accountId, "Membership account id must not be null");
        this.businessId = Objects.requireNonNull(businessId, "Membership business id must not be null");
        this.role = Objects.requireNonNull(role, "Membership role must not be null");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "Membership createdAt must not be null");
    }

    public static BusinessMembership owner(
            final AccountId accountId, final BusinessId businessId, final Instant now) {
        return new BusinessMembership(UUID.randomUUID(), accountId, businessId, BusinessRole.OWNER, true, now);
    }

    public static BusinessMembership reconstitute(
            final UUID id,
            final AccountId accountId,
            final BusinessId businessId,
            final BusinessRole role,
            final boolean active,
            final Instant createdAt) {
        return new BusinessMembership(id, accountId, businessId, role, active, createdAt);
    }

    public UUID id() {
        return id;
    }

    public AccountId accountId() {
        return accountId;
    }

    public BusinessId businessId() {
        return businessId;
    }

    public BusinessRole role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
```

- [ ] **Step 3: Add business application classes**

Create `platform-application/src/main/java/io/resrv/platform/application/business/in/CreateBusinessCommand.java`:

```java
package io.resrv.platform.application.business.in;

import io.resrv.shared.kernel.AccountId;

public record CreateBusinessCommand(AccountId ownerAccountId, String name, String slug, String timezone) {}
```

Create `platform-application/src/main/java/io/resrv/platform/application/business/in/CreateBusinessResult.java`:

```java
package io.resrv.platform.application.business.in;

import io.resrv.platform.domain.business.Business;
import java.util.UUID;

public record CreateBusinessResult(UUID id, String name, String slug, String timezone) {

    public static CreateBusinessResult from(final Business business) {
        return new CreateBusinessResult(
                business.id().value(),
                business.name().value(),
                business.slug().value(),
                business.timezone().value().getId());
    }
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/business/in/CreateBusinessUseCase.java`:

```java
package io.resrv.platform.application.business.in;

public interface CreateBusinessUseCase {

    CreateBusinessResult create(CreateBusinessCommand command);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/business/out/BusinessCommandPort.java`:

```java
package io.resrv.platform.application.business.out;

import io.resrv.platform.domain.business.Business;

public interface BusinessCommandPort {

    void save(Business business);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/business/out/BusinessQueryPort.java`:

```java
package io.resrv.platform.application.business.out;

import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

public interface BusinessQueryPort {

    Optional<Business> findById(BusinessId businessId);

    Optional<Business> findBySlug(BusinessSlug slug);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/membership/out/BusinessMembershipCommandPort.java`:

```java
package io.resrv.platform.application.membership.out;

import io.resrv.platform.domain.membership.BusinessMembership;

public interface BusinessMembershipCommandPort {

    void save(BusinessMembership membership);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/membership/out/BusinessMembershipQueryPort.java`:

```java
package io.resrv.platform.application.membership.out;

import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

public interface BusinessMembershipQueryPort {

    Optional<BusinessMembership> findActiveByAccountIdAndBusinessId(
            AccountId accountId, BusinessId businessId);
}
```

Create `platform-application/src/main/java/io/resrv/platform/application/business/CreateBusinessService.java`:

```java
package io.resrv.platform.application.business;

import io.resrv.platform.application.business.in.CreateBusinessCommand;
import io.resrv.platform.application.business.in.CreateBusinessResult;
import io.resrv.platform.application.business.in.CreateBusinessUseCase;
import io.resrv.platform.application.business.out.BusinessCommandPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessSlugAlreadyExistsException;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.shared.kernel.Timezone;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateBusinessService implements CreateBusinessUseCase {

    private final BusinessCommandPort businessCommandPort;
    private final BusinessQueryPort businessQueryPort;
    private final BusinessMembershipCommandPort membershipCommandPort;
    private final Clock clock;

    public CreateBusinessService(
            final BusinessCommandPort businessCommandPort,
            final BusinessQueryPort businessQueryPort,
            final BusinessMembershipCommandPort membershipCommandPort,
            final Clock clock) {
        this.businessCommandPort = businessCommandPort;
        this.businessQueryPort = businessQueryPort;
        this.membershipCommandPort = membershipCommandPort;
        this.clock = clock;
    }

    @Override
    public CreateBusinessResult create(final CreateBusinessCommand command) {
        final var slug = new BusinessSlug(command.slug());
        if (businessQueryPort.findBySlug(slug).isPresent()) {
            throw new BusinessSlugAlreadyExistsException(slug);
        }
        final var business =
                Business.create(
                        new BusinessName(command.name()),
                        slug,
                        Timezone.of(command.timezone()),
                        clock.instant());
        businessCommandPort.save(business);
        membershipCommandPort.save(
                BusinessMembership.owner(command.ownerAccountId(), business.id(), clock.instant()));
        return CreateBusinessResult.from(business);
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
./gradlew :platform-domain:test :platform-application:test --tests "*CreateBusinessServiceTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add platform-domain platform-application
git commit -m "feat(business): add business membership model"
```

---

### Task 5: Platform Persistence and Flyway Schema

**Files:**
- Create: `platform-adapter-persistence/src/main/resources/db/migration/V9__create_platform_schema.sql`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/account/AccountJpaEntity.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/account/AccountJpaRepository.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/account/AccountPersistenceAdapter.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/business/BusinessJpaEntity.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/business/BusinessJpaRepository.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/business/BusinessPersistenceAdapter.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/membership/BusinessMembershipJpaEntity.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/membership/BusinessMembershipJpaRepository.java`
- Create: `platform-adapter-persistence/src/main/java/io/resrv/platform/adapter/out/persistence/membership/BusinessMembershipPersistenceAdapter.java`
- Test: `platform-adapter-persistence/src/test/java/io/resrv/platform/adapter/out/persistence/PlatformPersistenceAdapterTest.java`

- [ ] **Step 1: Write persistence integration test**

Create `platform-adapter-persistence/src/test/java/io/resrv/platform/adapter/out/persistence/PlatformPersistenceAdapterTest.java`:

```java
package io.resrv.platform.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.platform.adapter.out.persistence.account.AccountPersistenceAdapter;
import io.resrv.platform.adapter.out.persistence.business.BusinessPersistenceAdapter;
import io.resrv.platform.adapter.out.persistence.membership.BusinessMembershipPersistenceAdapter;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountName;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@EnableAutoConfiguration
@ComponentScan("io.resrv.platform.adapter.out.persistence")
final class PlatformPersistenceAdapterTest {

    @Autowired private AccountPersistenceAdapter accountAdapter;
    @Autowired private BusinessPersistenceAdapter businessAdapter;
    @Autowired private BusinessMembershipPersistenceAdapter membershipAdapter;

    @Test
    void savesAndLoadsAccountBusinessMembership() {
        final var now = Instant.parse("2026-05-25T00:00:00Z");
        final var account =
                Account.create(
                        new AccountEmail("user@example.com"),
                        new AccountName("User One"),
                        "hashed-password",
                        now);
        final var business =
                Business.create(
                        new BusinessName("Salon A"),
                        new BusinessSlug("salon-a"),
                        Timezone.of("Asia/Seoul"),
                        now);
        accountAdapter.save(account);
        businessAdapter.save(business);
        membershipAdapter.save(BusinessMembership.owner(account.id(), business.id(), now));

        assertEquals(account.id(), accountAdapter.findByEmail(account.email()).orElseThrow().id());
        assertEquals(business.id(), businessAdapter.findBySlug(business.slug()).orElseThrow().id());
        assertTrue(
                membershipAdapter
                        .findActiveByAccountIdAndBusinessId(account.id(), business.id())
                        .isPresent());
    }
}
```

- [ ] **Step 2: Add Flyway migration**

Create `platform-adapter-persistence/src/main/resources/db/migration/V9__create_platform_schema.sql`:

```sql
CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE platform.account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_platform_account_email UNIQUE (email),
    CONSTRAINT ck_platform_account_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT ck_platform_account_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_platform_account_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE platform.business (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(63) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_platform_business_slug UNIQUE (slug),
    CONSTRAINT ck_platform_business_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_platform_business_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE platform.business_membership (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    business_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_platform_business_membership_account_business UNIQUE (account_id, business_id),
    CONSTRAINT ck_platform_business_membership_role CHECK (role IN ('OWNER', 'STAFF'))
);

CREATE INDEX idx_platform_business_membership_account
    ON platform.business_membership(account_id, active);

CREATE INDEX idx_platform_business_membership_business
    ON platform.business_membership(business_id, active);
```

- [ ] **Step 3: Add JPA entities and adapters**

For enums in JPA entities, use `@Enumerated(EnumType.STRING)`. Use these table annotations:

```java
@Table(schema = "platform", name = "account")
@Table(schema = "platform", name = "business")
@Table(schema = "platform", name = "business_membership")
```

Create `AccountJpaEntity` with fields matching `platform.account`. Create repository methods:

```java
Optional<AccountJpaEntity> findByEmail(String email);
```

Create `BusinessJpaEntity` with fields matching `platform.business`. Create repository methods:

```java
Optional<BusinessJpaEntity> findBySlug(String slug);
```

Create `BusinessMembershipJpaEntity` with fields matching `platform.business_membership`. Create repository method:

```java
Optional<BusinessMembershipJpaEntity> findByAccountIdAndBusinessIdAndActiveTrue(
        UUID accountId, UUID businessId);
```

Adapter mapping rules:

```java
AccountEmail email = new AccountEmail(entity.getEmail());
AccountName name = new AccountName(entity.getName());
AccountStatus status = entity.getStatus();

BusinessName name = new BusinessName(entity.getName());
BusinessSlug slug = new BusinessSlug(entity.getSlug());
Timezone timezone = Timezone.of(entity.getTimezone());
BusinessStatus status = entity.getStatus();
```

- [ ] **Step 4: Run persistence test**

Run:

```bash
./gradlew :platform-adapter-persistence:test --tests "*PlatformPersistenceAdapterTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add platform-adapter-persistence
git commit -m "feat(platform): persist accounts and businesses"
```

---

### Task 6: Platform Auth and Web API

**Files:**
- Create: `platform-application/src/main/java/io/resrv/platform/application/auth/in/LoginCommand.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/auth/in/LoginResult.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/auth/in/LoginUseCase.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/auth/out/TokenGenerationPort.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/auth/AuthenticationFailedException.java`
- Create: `platform-application/src/main/java/io/resrv/platform/application/auth/LoginService.java`
- Create: `platform-adapter-web/src/main/java/io/resrv/platform/adapter/in/web/account/AccountWebAdapter.java`
- Create: `platform-adapter-web/src/main/java/io/resrv/platform/adapter/in/web/auth/LoginWebAdapter.java`
- Create: `platform-adapter-web/src/main/java/io/resrv/platform/adapter/in/web/business/BusinessWebAdapter.java`
- Create: `platform-adapter-web/src/main/java/io/resrv/platform/adapter/in/web/security/AuthenticatedAccount.java`
- Create: `platform-adapter-web/src/main/java/io/resrv/platform/adapter/in/web/error/PlatformExceptionHandler.java`
- Create: `platform-api/src/main/java/io/resrv/platform/api/PlatformApiApplication.java`
- Create: `platform-api/src/main/java/io/resrv/platform/api/security/PlatformSecurityConfig.java`
- Create: `platform-api/src/main/java/io/resrv/platform/api/security/JwtTokenAdapter.java`
- Create: `platform-api/src/main/java/io/resrv/platform/api/security/PasswordHashingAdapter.java`
- Test: `platform-api/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java`

- [ ] **Step 1: Write API integration test**

Create `platform-api/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java`:

```java
package io.resrv.platform.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16:///resrv",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "resrv.jwt.secret-key=01234567890123456789012345678901",
            "resrv.jwt.issuer=resrv-test",
            "resrv.jwt.audience=resrv-api",
            "resrv.jwt.expiration=3600"
        })
@AutoConfigureMockMvc
final class PlatformApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void accountCanRegisterLoginAndCreateBusiness() throws Exception {
        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "owner@example.com",
                                          "name": "Owner One",
                                          "password": "passw0rd!"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email").value("owner@example.com"));

        final var login =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "email": "owner@example.com",
                                                  "password": "passw0rd!"
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken", notNullValue()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final var token = login.replaceAll(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(
                        post("/api/businesses")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Salon A",
                                          "slug": "salon-a",
                                          "timezone": "Asia/Seoul"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Salon A"))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"));
    }
}
```

- [ ] **Step 2: Add account-based auth application**

Create `LoginCommand`, `LoginResult`, `LoginUseCase`, `TokenGenerationPort`, `AuthenticationFailedException`, and `LoginService`.

Use this token generation contract:

```java
public interface TokenGenerationPort {

    LoginResult generate(AccountId accountId);
}
```

Use this login rule:

```java
final var account =
        accountQueryPort
                .findByEmail(new AccountEmail(command.email()))
                .orElseThrow(AuthenticationFailedException::new);
if (!account.active()
        || !passwordHashingPort.matches(command.password(), account.hashedPassword())) {
    throw new AuthenticationFailedException();
}
return tokenGenerationPort.generate(account.id());
```

Do not include `businessId`, `tenantId`, or role in JWT claims.

- [ ] **Step 3: Add platform web adapters**

Create request/response records inside each web package. Use these routes:

```text
POST /api/accounts
POST /api/auth/login
POST /api/businesses
```

`POST /api/accounts` returns `201 Created` with:

```json
{
  "id": "uuid",
  "email": "owner@example.com",
  "name": "Owner One"
}
```

`POST /api/auth/login` returns:

```json
{
  "accessToken": "jwt",
  "expiresIn": 3600
}
```

`POST /api/businesses` reads `AuthenticatedAccount` from JWT subject and calls `CreateBusinessUseCase`.

- [ ] **Step 4: Add platform boot and security**

Create `platform-api/src/main/java/io/resrv/platform/api/PlatformApiApplication.java`:

```java
package io.resrv.platform.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.resrv.platform")
@EnableJpaRepositories(basePackages = "io.resrv.platform")
@EntityScan(basePackages = "io.resrv.platform")
public class PlatformApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
```

Security rules:

```java
requestMatchers(HttpMethod.POST, "/api/accounts", "/api/auth/login").permitAll();
requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
anyRequest().authenticated();
```

JWT claims:

```java
subject = accountId.toString();
claim("accountId", accountId.toString());
claim("jti", UUID.randomUUID().toString());
```

- [ ] **Step 5: Run platform API integration test**

Run:

```bash
./gradlew :platform-api:test --tests "*PlatformApiIntegrationTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add platform-application platform-adapter-web platform-api
git commit -m "feat(auth): issue account scoped tokens"
```

---

### Task 7: Timeslot Booking Settings

**Files:**
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/settings/BusinessBookingSettings.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/settings/SlotDuration.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/settings/HoldTtl.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/settings/CancellationWindow.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/settings/MaxAdvanceBookingDays.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/settings/in/UpsertBusinessBookingSettingsCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/settings/in/BusinessBookingSettingsResult.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/settings/in/UpsertBusinessBookingSettingsUseCase.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/settings/out/BusinessBookingSettingsCommandPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/settings/out/BusinessBookingSettingsQueryPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/business/out/BusinessLookupPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/settings/BusinessBookingSettingsService.java`
- Test: `timeslot-domain/src/test/java/io/resrv/timeslot/domain/settings/BusinessBookingSettingsTest.java`
- Test: `timeslot-application/src/test/java/io/resrv/timeslot/application/settings/BusinessBookingSettingsServiceTest.java`

- [ ] **Step 1: Write settings domain test**

Create `timeslot-domain/src/test/java/io/resrv/timeslot/domain/settings/BusinessBookingSettingsTest.java`:

```java
package io.resrv.timeslot.domain.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class BusinessBookingSettingsTest {

    @Test
    void createsSettingsWithConfiguredRanges() {
        final var settings =
                BusinessBookingSettings.create(
                        BusinessId.create(),
                        new SlotDuration(30),
                        new HoldTtl(10),
                        new CancellationWindow(60),
                        new MaxAdvanceBookingDays(30),
                        Instant.parse("2026-05-25T00:00:00Z"));

        assertEquals(30, settings.slotDuration().minutes());
        assertEquals(10, settings.holdTtl().minutes());
        assertEquals(60, settings.cancellationWindow().minutes());
        assertEquals(30, settings.maxAdvanceBookingDays().days());
    }

    @Test
    void rejectsInvalidSlotDuration() {
        assertThrows(IllegalArgumentException.class, () -> new SlotDuration(7));
    }

    @Test
    void rejectsInvalidHoldTtl() {
        assertThrows(IllegalArgumentException.class, () -> new HoldTtl(31));
    }
}
```

- [ ] **Step 2: Add settings value objects**

Create value object rules exactly:

```java
public record SlotDuration(int minutes) {
    public SlotDuration {
        if (minutes < 5 || minutes > 480 || minutes % 5 != 0) {
            throw new IllegalArgumentException("Slot duration must be 5-480 minutes in 5 minute increments");
        }
    }
}

public record HoldTtl(int minutes) {
    public HoldTtl {
        if (minutes < 1 || minutes > 30) {
            throw new IllegalArgumentException("Hold TTL must be 1-30 minutes");
        }
    }
}

public record CancellationWindow(int minutes) {
    public CancellationWindow {
        if (minutes < 0 || minutes > 10080) {
            throw new IllegalArgumentException("Cancellation window must be 0-10080 minutes");
        }
    }
}

public record MaxAdvanceBookingDays(int days) {
    public MaxAdvanceBookingDays {
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("Max advance booking days must be 1-365");
        }
    }
}
```

Create `BusinessBookingSettings` with `businessId`, four settings, `createdAt`, `updatedAt`, `create`, `reconstitute`, `update`.

- [ ] **Step 3: Add settings application service**

`BusinessLookupPort` contract:

```java
package io.resrv.timeslot.application.business.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Optional;

public interface BusinessLookupPort {

    Optional<BusinessView> findActiveById(BusinessId businessId);

    record BusinessView(BusinessId id, String name, String slug, Timezone timezone) {}
}
```

Service rule:

```java
if (businessLookupPort.findActiveById(command.businessId()).isEmpty()) {
    throw new BusinessNotAvailableException(command.businessId());
}
```

Upsert behavior:

```java
final var existing = queryPort.findByBusinessId(command.businessId());
final var settings =
        existing
                .map(value -> value.update(slotDuration, holdTtl, cancellationWindow, maxAdvanceBookingDays, now))
                .orElseGet(() -> BusinessBookingSettings.create(command.businessId(), slotDuration, holdTtl, cancellationWindow, maxAdvanceBookingDays, now));
commandPort.save(settings);
```

- [ ] **Step 4: Run tests**

Run:

```bash
./gradlew :timeslot-domain:test :timeslot-application:test --tests "*BusinessBookingSettings*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add timeslot-domain timeslot-application
git commit -m "feat(timeslot): add booking settings"
```

---

### Task 8: Timeslot Resource with Settings Overrides

**Files:**
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/resource/Resource.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/resource/ResourceName.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/resource/ResourceSlug.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/resource/ResourceStatus.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/resource/ResourceBookingOverrides.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/resource/in/CreateResourceCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/resource/in/ResourceResult.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/resource/in/CreateResourceUseCase.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/resource/out/ResourceCommandPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/resource/out/ResourceQueryPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/resource/ResourceService.java`
- Test: `timeslot-application/src/test/java/io/resrv/timeslot/application/resource/ResourceServiceTest.java`

- [ ] **Step 1: Write resource service test**

Create `timeslot-application/src/test/java/io/resrv/timeslot/application/resource/ResourceServiceTest.java`:

```java
package io.resrv.timeslot.application.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.application.resource.in.CreateResourceCommand;
import io.resrv.timeslot.application.resource.out.ResourceCommandPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ResourceServiceTest {

    private BusinessBookingSettingsQueryPort settingsQueryPort;
    private ResourceCommandPort commandPort;
    private ResourceQueryPort queryPort;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        settingsQueryPort = mock(BusinessBookingSettingsQueryPort.class);
        commandPort = mock(ResourceCommandPort.class);
        queryPort = mock(ResourceQueryPort.class);
        service =
                new ResourceService(
                        settingsQueryPort,
                        commandPort,
                        queryPort,
                        Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void requiresBusinessBookingSettingsBeforeResourceCreation() {
        final var businessId = BusinessId.create();
        when(settingsQueryPort.findByBusinessId(businessId)).thenReturn(Optional.empty());

        assertThrows(
                BookingSettingsRequiredException.class,
                () ->
                        service.create(
                                new CreateResourceCommand(
                                        businessId,
                                        "Room A",
                                        "room-a",
                                        "Room description",
                                        null,
                                        null,
                                        null)));
    }

    @Test
    void createsResourceWithOverrides() {
        final var businessId = BusinessId.create();
        when(settingsQueryPort.findByBusinessId(businessId))
                .thenReturn(
                        Optional.of(
                                BusinessBookingSettings.create(
                                        businessId,
                                        new SlotDuration(30),
                                        new HoldTtl(10),
                                        new CancellationWindow(60),
                                        new MaxAdvanceBookingDays(30),
                                        Instant.parse("2026-05-25T00:00:00Z"))));
        when(queryPort.findByBusinessIdAndSlug(businessId, new ResourceSlug("room-a")))
                .thenReturn(Optional.empty());

        final var result =
                service.create(
                        new CreateResourceCommand(
                                businessId, "Room A", "room-a", "Room description", 60, 5, 120));

        assertEquals("Room A", result.name());
        verify(commandPort).save(org.mockito.ArgumentMatchers.any(Resource.class));
    }
}
```

- [ ] **Step 2: Add resource domain**

`ResourceBookingOverrides` must contain nullable value objects:

```java
public record ResourceBookingOverrides(
        SlotDuration slotDuration, HoldTtl holdTtl, CancellationWindow cancellationWindow) {

    public static ResourceBookingOverrides none() {
        return new ResourceBookingOverrides(null, null, null);
    }
}
```

`Resource` fields:

```java
ResourceId id;
BusinessId businessId;
ResourceName name;
ResourceSlug slug;
String description;
ResourceStatus status;
ResourceBookingOverrides bookingOverrides;
Instant createdAt;
Instant updatedAt;
```

`deactivate(now)` changes status to `INACTIVE` and does not touch reservations.

- [ ] **Step 3: Add resource application service**

Service rules:

```java
if (settingsQueryPort.findByBusinessId(command.businessId()).isEmpty()) {
    throw new BookingSettingsRequiredException(command.businessId());
}
if (queryPort.findByBusinessIdAndSlug(command.businessId(), slug).isPresent()) {
    throw new ResourceSlugAlreadyExistsException(slug);
}
```

Override mapping:

```java
final var overrides =
        new ResourceBookingOverrides(
                command.slotDurationMinutes() == null
                        ? null
                        : new SlotDuration(command.slotDurationMinutes()),
                command.holdTtlMinutes() == null ? null : new HoldTtl(command.holdTtlMinutes()),
                command.cancellationWindowMinutes() == null
                        ? null
                        : new CancellationWindow(command.cancellationWindowMinutes()));
```

- [ ] **Step 4: Run tests**

Run:

```bash
./gradlew :timeslot-domain:test :timeslot-application:test --tests "*ResourceServiceTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add timeslot-domain timeslot-application
git commit -m "feat(timeslot): add reservable resources"
```

---

### Task 9: Resource Schedule with Multiple Windows

**Files:**
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/schedule/ScheduleWindow.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/schedule/WeeklyResourceSchedule.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/schedule/DateResourceScheduleOverride.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/schedule/in/ReplaceWeeklyScheduleCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/schedule/in/ReplaceDateScheduleOverrideCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/schedule/in/ScheduleResult.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/schedule/out/ResourceScheduleCommandPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/schedule/out/ResourceScheduleQueryPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/schedule/ResourceScheduleService.java`
- Test: `timeslot-domain/src/test/java/io/resrv/timeslot/domain/schedule/ScheduleWindowTest.java`
- Test: `timeslot-application/src/test/java/io/resrv/timeslot/application/schedule/ResourceScheduleServiceTest.java`

- [ ] **Step 1: Write schedule window test**

Create `timeslot-domain/src/test/java/io/resrv/timeslot/domain/schedule/ScheduleWindowTest.java`:

```java
package io.resrv.timeslot.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ScheduleWindowTest {

    @Test
    void acceptsSameDayWindow() {
        assertDoesNotThrow(() -> new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(18, 0)));
    }

    @Test
    void rejectsOvernightWindow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScheduleWindow(LocalTime.of(22, 0), LocalTime.of(2, 0)));
    }

    @Test
    void rejectsOverlappingWindows() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ScheduleWindow.validateNoOverlap(
                                List.of(
                                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                                        new ScheduleWindow(LocalTime.of(11, 0), LocalTime.of(13, 0)))));
    }
}
```

- [ ] **Step 2: Add schedule domain**

Create `ScheduleWindow`:

```java
package io.resrv.timeslot.domain.schedule;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ScheduleWindow(LocalTime startTime, LocalTime endTime) {

    public ScheduleWindow {
        Objects.requireNonNull(startTime, "Schedule window start time must not be null");
        Objects.requireNonNull(endTime, "Schedule window end time must not be null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Schedule window must start and end on the same date");
        }
    }

    public static void validateNoOverlap(final List<ScheduleWindow> windows) {
        final var sorted =
                windows.stream().sorted(Comparator.comparing(ScheduleWindow::startTime)).toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                throw new IllegalArgumentException("Schedule windows must not overlap");
            }
        }
    }
}
```

`WeeklyResourceSchedule` identity is `(businessId, resourceId, dayOfWeek)`.

`DateResourceScheduleOverride` identity is `(businessId, resourceId, date)`.

Date override with empty windows means closed day. Date override replaces weekly schedule for the date.

- [ ] **Step 3: Add schedule service**

Service methods:

```java
ScheduleResult replaceWeekly(ReplaceWeeklyScheduleCommand command);
ScheduleResult replaceDateOverride(ReplaceDateScheduleOverrideCommand command);
void deleteDateOverride(BusinessId businessId, ResourceId resourceId, LocalDate date);
```

Validation:

```java
ensureResourceBelongsToBusiness(command.businessId(), command.resourceId());
ScheduleWindow.validateNoOverlap(command.windows());
```

Do not check business-level schedule. v1 has resource schedule only.

- [ ] **Step 4: Run tests**

Run:

```bash
./gradlew :timeslot-domain:test :timeslot-application:test --tests "*Schedule*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add timeslot-domain timeslot-application
git commit -m "feat(timeslot): support resource schedules"
```

---

### Task 10: Timeslot Persistence Schema

**Files:**
- Create: `timeslot-adapter-persistence/src/main/resources/db/migration/V10__create_timeslot_schema.sql`
- Create: JPA entities/repositories/adapters under `timeslot-adapter-persistence/src/main/java/io/resrv/timeslot/adapter/out/persistence`
- Test: `timeslot-adapter-persistence/src/test/java/io/resrv/timeslot/adapter/out/persistence/TimeslotPersistenceAdapterTest.java`

- [ ] **Step 1: Add Flyway migration**

Create `timeslot-adapter-persistence/src/main/resources/db/migration/V10__create_timeslot_schema.sql`:

```sql
CREATE SCHEMA IF NOT EXISTS timeslot;

CREATE TABLE timeslot.business_booking_settings (
    business_id UUID PRIMARY KEY,
    slot_duration_minutes INT NOT NULL,
    hold_ttl_minutes INT NOT NULL,
    cancellation_window_minutes INT NOT NULL,
    max_advance_booking_days INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_timeslot_slot_duration CHECK (
        slot_duration_minutes >= 5
        AND slot_duration_minutes <= 480
        AND slot_duration_minutes % 5 = 0
    ),
    CONSTRAINT ck_timeslot_hold_ttl CHECK (hold_ttl_minutes >= 1 AND hold_ttl_minutes <= 30),
    CONSTRAINT ck_timeslot_cancel_window CHECK (
        cancellation_window_minutes >= 0
        AND cancellation_window_minutes <= 10080
    ),
    CONSTRAINT ck_timeslot_max_advance CHECK (
        max_advance_booking_days >= 1
        AND max_advance_booking_days <= 365
    )
);

CREATE TABLE timeslot.resource (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    slug VARCHAR(63) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    slot_duration_minutes INT,
    hold_ttl_minutes INT,
    cancellation_window_minutes INT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_timeslot_resource_business_slug UNIQUE (business_id, slug),
    CONSTRAINT ck_timeslot_resource_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_timeslot_resource_slot_override CHECK (
        slot_duration_minutes IS NULL
        OR (
            slot_duration_minutes >= 5
            AND slot_duration_minutes <= 480
            AND slot_duration_minutes % 5 = 0
        )
    ),
    CONSTRAINT ck_timeslot_resource_hold_override CHECK (
        hold_ttl_minutes IS NULL OR (hold_ttl_minutes >= 1 AND hold_ttl_minutes <= 30)
    ),
    CONSTRAINT ck_timeslot_resource_cancel_override CHECK (
        cancellation_window_minutes IS NULL
        OR (cancellation_window_minutes >= 0 AND cancellation_window_minutes <= 10080)
    )
);

CREATE INDEX idx_timeslot_resource_business_status
    ON timeslot.resource(business_id, status);

CREATE TABLE timeslot.resource_weekly_schedule (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_timeslot_resource_weekly_schedule UNIQUE (business_id, resource_id, day_of_week),
    CONSTRAINT ck_timeslot_weekly_day CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE TABLE timeslot.resource_weekly_schedule_window (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES timeslot.resource_weekly_schedule(id) ON DELETE CASCADE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT ck_timeslot_weekly_window_range CHECK (start_time < end_time),
    CONSTRAINT uq_timeslot_weekly_window_order UNIQUE (schedule_id, sort_order)
);

CREATE TABLE timeslot.resource_date_schedule_override (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_timeslot_resource_date_override UNIQUE (business_id, resource_id, date)
);

CREATE TABLE timeslot.resource_date_schedule_override_window (
    id UUID PRIMARY KEY,
    override_id UUID NOT NULL REFERENCES timeslot.resource_date_schedule_override(id) ON DELETE CASCADE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT ck_timeslot_date_window_range CHECK (start_time < end_time),
    CONSTRAINT uq_timeslot_date_window_order UNIQUE (override_id, sort_order)
);
```

- [ ] **Step 2: Add JPA adapters**

Use these table annotations:

```java
@Table(schema = "timeslot", name = "business_booking_settings")
@Table(schema = "timeslot", name = "resource")
@Table(schema = "timeslot", name = "resource_weekly_schedule")
@Table(schema = "timeslot", name = "resource_weekly_schedule_window")
@Table(schema = "timeslot", name = "resource_date_schedule_override")
@Table(schema = "timeslot", name = "resource_date_schedule_override_window")
```

Entity package layout:

```text
io.resrv.timeslot.adapter.out.persistence.settings
io.resrv.timeslot.adapter.out.persistence.resource
io.resrv.timeslot.adapter.out.persistence.schedule
```

Repository required methods:

```java
Optional<BusinessBookingSettingsJpaEntity> findByBusinessId(UUID businessId);
Optional<ResourceJpaEntity> findByBusinessIdAndId(UUID businessId, UUID id);
Optional<ResourceJpaEntity> findByBusinessIdAndSlug(UUID businessId, String slug);
List<ResourceJpaEntity> findByBusinessIdAndStatus(UUID businessId, ResourceStatus status);
Optional<WeeklyResourceScheduleJpaEntity> findByBusinessIdAndResourceIdAndDayOfWeek(
        UUID businessId, UUID resourceId, int dayOfWeek);
Optional<DateResourceScheduleOverrideJpaEntity> findByBusinessIdAndResourceIdAndDate(
        UUID businessId, UUID resourceId, LocalDate date);
```

- [ ] **Step 3: Write and run persistence test**

Create `TimeslotPersistenceAdapterTest` that saves settings, resource, weekly schedule with two windows, and date override with empty windows. Assert each is loaded with same business/resource IDs and window count.

Run:

```bash
./gradlew :timeslot-adapter-persistence:test --tests "*TimeslotPersistenceAdapterTest"
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add timeslot-adapter-persistence
git commit -m "feat(timeslot): persist booking configuration"
```

---

### Task 11: Virtual Slot Generation

**Files:**
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/slot/SlotId.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/slot/Slot.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/slot/in/ListSlotsQuery.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/slot/in/SlotResult.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/slot/in/ListSlotsUseCase.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/slot/VirtualSlotService.java`
- Test: `timeslot-application/src/test/java/io/resrv/timeslot/application/slot/VirtualSlotServiceTest.java`

- [ ] **Step 1: Write slot generation test**

Create `timeslot-application/src/test/java/io/resrv/timeslot/application/slot/VirtualSlotServiceTest.java`:

```java
package io.resrv.timeslot.application.slot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

final class VirtualSlotServiceTest {

    @Test
    void generatesSlotsFromMultipleWindowsInBusinessTimezone() {
        final var slots =
                VirtualSlotService.generateSlots(
                        BusinessId.create(),
                        ResourceId.create(),
                        Timezone.of("Asia/Seoul"),
                        LocalDate.parse("2026-05-25"),
                        new SlotDuration(30),
                        List.of(
                                new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                                new ScheduleWindow(LocalTime.of(14, 0), LocalTime.of(15, 0))));

        assertEquals(4, slots.size());
        assertEquals(Instant.parse("2026-05-25T00:00:00Z"), slots.getFirst().startAt());
        assertEquals("2026-05-25T09:00+09:00", slots.getFirst().startAtBusinessTime().toString());
    }
}
```

- [ ] **Step 2: Add `SlotId` and `Slot`**

`SlotId` must be opaque and deterministic:

```java
package io.resrv.timeslot.domain.slot;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record SlotId(String value) {

    public static SlotId of(
            final BusinessId businessId,
            final ResourceId resourceId,
            final Instant startAt,
            final Instant endAt) {
        final var raw =
                businessId.value()
                        + "|"
                        + resourceId.value()
                        + "|"
                        + startAt
                        + "|"
                        + endAt;
        return new SlotId(Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
    }

    public DecodedSlotId decode() {
        final var raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        final var parts = raw.split("\\|");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid slotId");
        }
        return new DecodedSlotId(
                java.util.UUID.fromString(parts[0]),
                java.util.UUID.fromString(parts[1]),
                Instant.parse(parts[2]),
                Instant.parse(parts[3]));
    }

    public record DecodedSlotId(
            java.util.UUID businessId, java.util.UUID resourceId, Instant startAt, Instant endAt) {}
}
```

`Slot` fields:

```java
SlotId id;
BusinessId businessId;
ResourceId resourceId;
Instant startAt;
Instant endAt;
OffsetDateTime startAtBusinessTime;
OffsetDateTime endAtBusinessTime;
```

- [ ] **Step 3: Add virtual slot service**

Generation rule:

```java
for each ScheduleWindow:
  localStart = date.atTime(window.startTime())
  localEnd = date.atTime(window.endTime())
  slotStart = localStart
  while slotStart.plusMinutes(slotDuration.minutes()) <= localEnd:
    convert slotStart and slotEnd through business timezone to Instant
```

Filter rules:

```java
if (date.isBefore(todayInBusinessTimezone)) return empty list;
if (date.isAfter(todayInBusinessTimezone.plusDays(maxAdvanceBookingDays))) return empty list;
exclude active blockers returned by ReservationQueryPort.findActiveBlockers(
        businessId, resourceId, windowStartAt, windowEndAt, now)
```

- [ ] **Step 4: Run slot tests**

Run:

```bash
./gradlew :timeslot-application:test --tests "*VirtualSlotServiceTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add timeslot-domain timeslot-application
git commit -m "feat(timeslot): generate virtual slots"
```

---

### Task 12: Reservation Facts Model

**Files:**
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/reservation/Reservation.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/reservation/ReservationState.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/reservation/ReservationCancellationActor.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/reservation/ReservationInvalidStateException.java`
- Create: `timeslot-domain/src/main/java/io/resrv/timeslot/domain/reservation/ReservationHoldExpiredException.java`
- Test: `timeslot-domain/src/test/java/io/resrv/timeslot/domain/reservation/ReservationTest.java`

- [ ] **Step 1: Write reservation state test**

Create `timeslot-domain/src/test/java/io/resrv/timeslot/domain/reservation/ReservationTest.java`:

```java
package io.resrv.timeslot.domain.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class ReservationTest {

    @Test
    void heldReservationExpiresByTimeWithoutStatusMutation() {
        final var reservation =
                Reservation.hold(
                        BusinessId.create(),
                        ResourceId.create(),
                        AccountId.create(),
                        Instant.parse("2026-05-25T01:00:00Z"),
                        Instant.parse("2026-05-25T01:30:00Z"),
                        Instant.parse("2026-05-25T00:10:00Z"),
                        Instant.parse("2026-05-25T00:00:00Z"));

        assertEquals(ReservationState.HELD, reservation.stateAt(Instant.parse("2026-05-25T00:09:59Z")));
        assertEquals(ReservationState.EXPIRED, reservation.stateAt(Instant.parse("2026-05-25T00:10:00Z")));
    }

    @Test
    void cannotConfirmExpiredHold() {
        final var reservation =
                Reservation.hold(
                        BusinessId.create(),
                        ResourceId.create(),
                        AccountId.create(),
                        Instant.parse("2026-05-25T01:00:00Z"),
                        Instant.parse("2026-05-25T01:30:00Z"),
                        Instant.parse("2026-05-25T00:10:00Z"),
                        Instant.parse("2026-05-25T00:00:00Z"));

        assertThrows(
                ReservationHoldExpiredException.class,
                () -> reservation.confirm(Instant.parse("2026-05-25T00:10:00Z")));
    }
}
```

- [ ] **Step 2: Add reservation facts domain**

Use these fields. Do not add `status`:

```java
ReservationId id;
BusinessId businessId;
ResourceId resourceId;
AccountId customerAccountId;
Instant startAt;
Instant endAt;
Instant holdExpiresAt;
Instant createdAt;
Instant updatedAt;
Instant confirmedAt;
Instant releasedAt;
Instant cancelledAt;
ReservationCancellationActor cancelledBy;
Instant checkedInAt;
Instant noShowAt;
```

`ReservationState`:

```java
public enum ReservationState {
    HELD,
    EXPIRED,
    RELEASED,
    CONFIRMED,
    CUSTOMER_CANCELLED,
    BUSINESS_CANCELLED,
    CHECKED_IN,
    NO_SHOW
}
```

State calculation order:

```java
if (releasedAt != null) return RELEASED;
if (checkedInAt != null) return CHECKED_IN;
if (noShowAt != null) return NO_SHOW;
if (cancelledAt != null && cancelledBy == CUSTOMER) return CUSTOMER_CANCELLED;
if (cancelledAt != null && cancelledBy == BUSINESS) return BUSINESS_CANCELLED;
if (confirmedAt != null) return CONFIRMED;
if (!now.isBefore(holdExpiresAt)) return EXPIRED;
return HELD;
```

Transition rules:

```java
confirm(now): allowed only HELD at now
release(now): allowed only HELD at now
cancelByCustomer(now, cutoff): allowed only CONFIRMED and now < cutoff
cancelByBusiness(now): allowed only HELD or CONFIRMED
checkIn(now): allowed only CONFIRMED and now >= startAt
markNoShow(now): allowed only CONFIRMED and now >= endAt
```

- [ ] **Step 3: Run reservation tests**

Run:

```bash
./gradlew :timeslot-domain:test --tests "*ReservationTest"
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add timeslot-domain
git commit -m "feat(reservation): derive state from facts"
```

---

### Task 13: Reservation Persistence, Advisory Lock, and Active Blocker Query

**Files:**
- Modify: `timeslot-adapter-persistence/src/main/resources/db/migration/V10__create_timeslot_schema.sql`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/out/ReservationCommandPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/out/ReservationQueryPort.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/lock/out/SlotLockPort.java`
- Create: `timeslot-adapter-persistence/src/main/java/io/resrv/timeslot/adapter/out/persistence/reservation/ReservationJpaEntity.java`
- Create: `timeslot-adapter-persistence/src/main/java/io/resrv/timeslot/adapter/out/persistence/reservation/ReservationJpaRepository.java`
- Create: `timeslot-adapter-persistence/src/main/java/io/resrv/timeslot/adapter/out/persistence/reservation/ReservationPersistenceAdapter.java`
- Create: `timeslot-adapter-persistence/src/main/java/io/resrv/timeslot/adapter/out/persistence/lock/PostgresSlotLockAdapter.java`
- Test: `timeslot-adapter-persistence/src/test/java/io/resrv/timeslot/adapter/out/persistence/reservation/ReservationPersistenceAdapterTest.java`

- [ ] **Step 1: Extend migration with reservation table**

Append to `V10__create_timeslot_schema.sql`:

```sql
CREATE TABLE timeslot.reservation (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    customer_account_id UUID NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    hold_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancelled_by VARCHAR(32),
    checked_in_at TIMESTAMPTZ,
    no_show_at TIMESTAMPTZ,
    CONSTRAINT ck_timeslot_reservation_time_range CHECK (start_at < end_at),
    CONSTRAINT ck_timeslot_reservation_cancel_actor CHECK (
        (cancelled_at IS NULL AND cancelled_by IS NULL)
        OR (cancelled_at IS NOT NULL AND cancelled_by IN ('CUSTOMER', 'BUSINESS'))
    ),
    CONSTRAINT ck_timeslot_reservation_release_terminal CHECK (
        released_at IS NULL
        OR (
            confirmed_at IS NULL
            AND cancelled_at IS NULL
            AND checked_in_at IS NULL
            AND no_show_at IS NULL
        )
    ),
    CONSTRAINT ck_timeslot_reservation_confirmed_terminal CHECK (
        (checked_in_at IS NULL OR confirmed_at IS NOT NULL)
        AND (no_show_at IS NULL OR confirmed_at IS NOT NULL)
        AND (cancelled_at IS NULL OR confirmed_at IS NOT NULL)
    ),
    CONSTRAINT ck_timeslot_reservation_single_terminal CHECK (
        (CASE WHEN cancelled_at IS NULL THEN 0 ELSE 1 END)
        + (CASE WHEN checked_in_at IS NULL THEN 0 ELSE 1 END)
        + (CASE WHEN no_show_at IS NULL THEN 0 ELSE 1 END)
        <= 1
    )
);

CREATE INDEX idx_timeslot_reservation_customer
    ON timeslot.reservation(business_id, customer_account_id, start_at);

CREATE INDEX idx_timeslot_reservation_resource_window
    ON timeslot.reservation(business_id, resource_id, start_at, end_at);

CREATE INDEX idx_timeslot_reservation_active_blocker
    ON timeslot.reservation(business_id, resource_id, start_at, end_at, hold_expires_at);
```

- [ ] **Step 2: Add active blocker query**

Repository query:

```java
@Query(
        """
        SELECT reservation FROM ReservationJpaEntity reservation
        WHERE reservation.businessId = :businessId
          AND reservation.resourceId = :resourceId
          AND reservation.startAt < :endAt
          AND reservation.endAt > :startAt
          AND reservation.releasedAt IS NULL
          AND reservation.cancelledAt IS NULL
          AND reservation.noShowAt IS NULL
          AND (
            reservation.confirmedAt IS NOT NULL
            OR reservation.holdExpiresAt > :now
          )
        """)
List<ReservationJpaEntity> findActiveBlockers(
        UUID businessId, UUID resourceId, Instant startAt, Instant endAt, Instant now);
```

Row lock query:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query(
        """
        SELECT reservation FROM ReservationJpaEntity reservation
        WHERE reservation.businessId = :businessId
          AND reservation.id = :reservationId
        """)
Optional<ReservationJpaEntity> findByBusinessIdAndIdForUpdate(UUID businessId, UUID reservationId);
```

- [ ] **Step 3: Add advisory lock adapter**

Create `PostgresSlotLockAdapter`:

```java
package io.resrv.timeslot.adapter.out.persistence.lock;

import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.lock.out.SlotLockPort;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSlotLockAdapter implements SlotLockPort {

    private final EntityManager entityManager;

    public PostgresSlotLockAdapter(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockSlot(final ResourceId resourceId, final Instant slotStartAt) {
        entityManager
                .createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
                .setParameter("lockKey", resourceId.value() + "|" + slotStartAt)
                .getSingleResult();
    }
}
```

Create port:

```java
package io.resrv.timeslot.application.lock.out;

import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;

public interface SlotLockPort {

    void lockSlot(ResourceId resourceId, Instant slotStartAt);
}
```

- [ ] **Step 4: Write persistence test**

Create test with two reservations on same resource:

1. Expired hold: `hold_expires_at = now - 1 minute`, no confirmed/released/cancelled/no-show.
2. Active hold: `hold_expires_at = now + 1 minute`.

Assert `findActiveBlockers` returns only active hold.

Run:

```bash
./gradlew :timeslot-adapter-persistence:test --tests "*ReservationPersistenceAdapterTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add timeslot-application timeslot-adapter-persistence
git commit -m "feat(reservation): persist fact based reservations"
```

---

### Task 14: Hold, Confirm, Release, Cancel, Check-in, No-show Use Cases

**Files:**
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/in/HoldReservationCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/in/ConfirmReservationCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/in/ReleaseReservationCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/in/CancelReservationCommand.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/in/ReservationResult.java`
- Create: `timeslot-application/src/main/java/io/resrv/timeslot/application/reservation/ReservationService.java`
- Test: `timeslot-application/src/test/java/io/resrv/timeslot/application/reservation/ReservationServiceTest.java`

- [ ] **Step 1: Write reservation service tests**

Create tests for:

```java
holdRejectsExpiredSlotIdDate();
holdRejectsSlotOutsideGeneratedSlots();
holdLocksSlotBeforeActiveBlockerQuery();
confirmRejectsExpiredHold();
releaseRejectsConfirmedReservation();
customerCancelUsesCancellationWindow();
businessCancelCanCancelHeldOrConfirmed();
checkInRequiresConfirmedAndStartReached();
noShowRequiresConfirmedAndEndReached();
```

Use Mockito `InOrder` to assert:

```java
inOrder.verify(slotLockPort).lockSlot(resourceId, slotStartAt);
inOrder.verify(reservationQueryPort).findActiveBlockers(businessId, resourceId, slotStartAt, slotEndAt, now);
```

- [ ] **Step 2: Implement hold flow**

Hold flow:

```java
now = clock.instant();
business =
        businessLookupPort
                .findActiveById(businessId)
                .orElseThrow(() -> new BusinessNotAvailableException(businessId));
resource =
        resourceQueryPort
                .findActiveByBusinessIdAndId(businessId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(businessId, resourceId));
settings = resolve business settings + resource overrides;
slot = decode slotId;
assert slot.businessId == command.businessId;
assert slot.resourceId == command.resourceId;
assert slot.startAt > now;
assert slot is present in generated slots for business local date;
slotLockPort.lockSlot(resourceId, slot.startAt);
if (!reservationQueryPort
        .findActiveBlockers(businessId, resourceId, slot.startAt, slot.endAt, now)
        .isEmpty()) {
    throw new SlotUnavailableException(resourceId, slot.startAt);
}
reservation =
        Reservation.hold(
                businessId,
                resourceId,
                accountId,
                slot.startAt,
                slot.endAt,
                now.plusSeconds(holdTtl.minutes() * 60L),
                now);
reservationCommandPort.save(reservation);
```

Do not call an expired hold cleanup method. Expiration is time-derived.

- [ ] **Step 3: Implement transition flows**

Use `reservationCommandPort.findByBusinessIdAndIdForUpdate` for confirm/release/cancel/check-in/no-show.

Ownership rules:

```java
confirm/release/customer cancel: command.accountId must equal reservation.customerAccountId
business cancel/check-in/no-show: caller must have membership role OWNER or STAFF
```

Membership check uses a timeslot auth port:

```java
boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);
```

In v1 this port reads `platform.business_membership` read-only from same DB.

- [ ] **Step 4: Run reservation service tests**

Run:

```bash
./gradlew :timeslot-application:test --tests "*ReservationServiceTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add timeslot-application
git commit -m "feat(reservation): add hold transition flows"
```

---

### Task 15: Timeslot Web API and Security

**Files:**
- Create: `timeslot-adapter-web/src/main/java/io/resrv/timeslot/adapter/in/web/settings/BusinessBookingSettingsWebAdapter.java`
- Create: `timeslot-adapter-web/src/main/java/io/resrv/timeslot/adapter/in/web/resource/ResourceWebAdapter.java`
- Create: `timeslot-adapter-web/src/main/java/io/resrv/timeslot/adapter/in/web/schedule/ResourceScheduleWebAdapter.java`
- Create: `timeslot-adapter-web/src/main/java/io/resrv/timeslot/adapter/in/web/slot/SlotWebAdapter.java`
- Create: `timeslot-adapter-web/src/main/java/io/resrv/timeslot/adapter/in/web/reservation/ReservationWebAdapter.java`
- Create: `timeslot-adapter-web/src/main/java/io/resrv/timeslot/adapter/in/web/security/AuthenticatedAccount.java`
- Create: `timeslot-adapter-web/src/main/java/io/resrv/timeslot/adapter/in/web/error/TimeslotExceptionHandler.java`
- Create: `timeslot-booking-api/src/main/java/io/resrv/timeslot/api/TimeslotBookingApiApplication.java`
- Create: `timeslot-booking-api/src/main/java/io/resrv/timeslot/api/security/TimeslotSecurityConfig.java`
- Test: `timeslot-booking-api/src/test/java/io/resrv/timeslot/api/TimeslotBookingApiIntegrationTest.java`

- [ ] **Step 1: Add timeslot boot app**

Create `TimeslotBookingApiApplication`:

```java
package io.resrv.timeslot.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.resrv.timeslot")
@EnableJpaRepositories(basePackages = "io.resrv.timeslot")
@EntityScan(basePackages = "io.resrv.timeslot")
public class TimeslotBookingApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(TimeslotBookingApiApplication.class, args);
    }
}
```

- [ ] **Step 2: Add routes**

Use these routes:

```text
PUT /api/businesses/{businessId}/booking-settings
POST /api/businesses/{businessId}/resources
GET /api/businesses/{businessId}/resources
PUT /api/businesses/{businessId}/resources/{resourceId}/weekly-schedules/{dayOfWeek}
PUT /api/businesses/{businessId}/resources/{resourceId}/date-schedule-overrides/{date}
GET /api/businesses/{businessId}/resources/{resourceId}/slots?date=YYYY-MM-DD
POST /api/businesses/{businessId}/reservations
POST /api/businesses/{businessId}/reservations/{reservationId}/confirm
POST /api/businesses/{businessId}/reservations/{reservationId}/release
POST /api/businesses/{businessId}/reservations/{reservationId}/cancel
POST /api/businesses/{businessId}/reservations/{reservationId}/check-in
POST /api/businesses/{businessId}/reservations/{reservationId}/no-show
```

Hold request:

```json
{
  "resourceId": "uuid",
  "slotId": "opaque-slot-id"
}
```

Slot response time format:

```json
{
  "slotId": "opaque-slot-id",
  "startAt": "2026-05-25T10:00:00+09:00",
  "endAt": "2026-05-25T10:30:00+09:00"
}
```

Reservation response includes derived state:

```json
{
  "id": "uuid",
  "businessId": "uuid",
  "resourceId": "uuid",
  "customerAccountId": "uuid",
  "startAt": "2026-05-25T10:00:00+09:00",
  "endAt": "2026-05-25T10:30:00+09:00",
  "state": "HELD",
  "holdExpiresAt": "2026-05-25T09:40:00+09:00"
}
```

- [ ] **Step 3: Add security**

Timeslot API is resource server only. It validates platform JWT with same issuer/audience/secret. It extracts `accountId` from subject or `accountId` claim. It does not expect `businessId` in JWT.

Security rules:

```java
requestMatchers(HttpMethod.GET, "/api/businesses/*/resources", "/api/businesses/*/resources/*/slots").permitAll();
requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
anyRequest().authenticated();
```

Mutation endpoints require authenticated account and application-level membership/customer ownership checks.

- [ ] **Step 4: Write integration test**

`TimeslotBookingApiIntegrationTest` setup inserts:

```sql
INSERT INTO platform.account (
    id, email, name, hashed_password, status, created_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'owner@example.com',
    'Owner One',
    '$2a$10$testhash',
    'ACTIVE',
    '2026-05-25T00:00:00Z'
);

INSERT INTO platform.business (
    id, name, slug, timezone, status, created_at
) VALUES (
    '00000000-0000-0000-0000-000000000010',
    'Salon A',
    'salon-a',
    'Asia/Seoul',
    'ACTIVE',
    '2026-05-25T00:00:00Z'
);

INSERT INTO platform.business_membership (
    id, account_id, business_id, role, active, created_at
) VALUES (
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000010',
    'OWNER',
    true,
    '2026-05-25T00:00:00Z'
);
```

Test flow:

1. Put booking settings.
2. Create resource.
3. Put Monday schedule with two windows.
4. List slots for Monday.
5. Hold first slot.
6. Confirm held reservation.

Run:

```bash
./gradlew :timeslot-booking-api:test --tests "*TimeslotBookingApiIntegrationTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add timeslot-adapter-web timeslot-booking-api
git commit -m "feat(api): expose timeslot booking API"
```

---

### Task 16: Remove Old Tenant-local API Surface

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Delete: `domain/`
- Delete: `application/`
- Delete: `adapter-web/`
- Delete: `adapter-persistence/`
- Delete: `bootstrap/`
- Modify: `docs/api.md`
- Modify: `docs/architecture.md`
- Modify: `docs/product.md`
- Modify: `docs/status.md`

- [ ] **Step 1: Verify new API tests pass before deleting old modules**

Run:

```bash
./gradlew :shared-kernel:check :platform-api:check :timeslot-booking-api:check
```

Expected: PASS.

- [ ] **Step 2: Remove old modules from settings**

Modify `settings.gradle.kts` include block to remove:

```text
domain
application
adapter-web
adapter-persistence
bootstrap
```

Modify `jacocoLineCoverageMinimums` in `build.gradle.kts` to remove the same module keys.

- [ ] **Step 3: Delete old modules**

Remove these directories:

```text
domain/
application/
adapter-web/
adapter-persistence/
bootstrap/
```

Do not delete `docs/`, `config/`, `gradle/`, `compose.yml`, `rewrite.yml`, `AGENTS.md`.

- [ ] **Step 4: Update docs**

Update docs with these statements:

```text
Customer is now a platform Account.
Business replaces Tenant in domain and API terminology.
BusinessMembership grants OWNER/STAFF access to a Business.
Timeslot booking stores resources, schedules, booking settings, slots, and reservations.
Slots are virtual and selected by opaque slotId.
Reservation state is derived from timestamp facts; HELD and EXPIRED are not persisted statuses.
Expired hold cleanup worker is not part of correctness.
```

- [ ] **Step 5: Run full check**

Run:

```bash
./gradlew spotlessApply
./gradlew check
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts docs shared-kernel platform-domain platform-application platform-adapter-persistence platform-adapter-web platform-api timeslot-domain timeslot-application timeslot-adapter-persistence timeslot-adapter-web timeslot-booking-api
git rm -r domain application adapter-web adapter-persistence bootstrap
git commit -m "refactor: replace tenant booking API"
```

---

## Final Verification

- [ ] Run unit tests:

```bash
./gradlew :shared-kernel:test :platform-domain:test :platform-application:test :timeslot-domain:test :timeslot-application:test
```

Expected: PASS.

- [ ] Run persistence tests:

```bash
./gradlew :platform-adapter-persistence:test :timeslot-adapter-persistence:test
```

Expected: PASS. Docker must be running because Testcontainers uses PostgreSQL.

- [ ] Run API tests:

```bash
./gradlew :platform-api:test :timeslot-booking-api:test
```

Expected: PASS.

- [ ] Run full quality gate:

```bash
./gradlew spotlessApply
./gradlew check
```

Expected: PASS with Checkstyle, ArchUnit, JaCoCo, and integration tests passing.

## Self-Review Notes

- Spec coverage: module split, Account+Membership, Business terminology, booking settings, resource-only schedule, virtual slots, slotId hold, reservation facts, advisory lock, no expired-hold worker, OffsetDateTime response, Flyway forward migration, and docs update all have tasks.
- Placeholder scan: unresolved placeholder markers were checked and removed.
- Type consistency: plan uses `BusinessId`, `AccountId`, `ResourceId`, `ReservationId`, `BusinessBookingSettings`, `ResourceBookingOverrides`, `SlotId`, and `ReservationState` consistently across tasks.
