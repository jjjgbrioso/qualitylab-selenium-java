# QualityLab: Selenium Java BDD framework

Automated UI tests for the deployed QualityLab site. The scenarios are written in Gherkin (Cucumber) and run through TestNG. The framework uses Selenium WebDriver and the Page Object Model.

The framework is independent of the web application. It only drives the site through a browser, so it does not import or change any application code.

| Part | Tool and version |
| --- | --- |
| Language | Java 21 |
| Build | Maven 3.9 through the included Maven Wrapper (no Maven install needed) |
| Browser automation | Selenium WebDriver 4.52, WebDriverManager 6.4 |
| BDD | Cucumber 7.34 (cucumber-java, cucumber-testng, cucumber-picocontainer) |
| Runner and reports | TestNG 7.12, Cucumber HTML/JSON, Surefire |
| Logging | SLF4J with Logback |

## What is tested

28 scenarios across the three modules (9 e-commerce, 11 banking including a four-row Scenario Outline, 8 travel).

| Module | Scenarios |
| --- | --- |
| E-commerce (`ecommerce.feature`) | Valid sign-in, wrong password, product search, product details, add to cart, update cart quantity, remove item, checkout with order history, admin inventory update |
| Banking (`banking.feature`) | Sign-in, account balances, deposit, withdrawal, successful transfer, insufficient balance, four invalid transfer cases, transaction history |
| Travel & Cruise (`travel.feature`) | Sign-in, hotel search, filter hotels, view rooms, create booking, invalid booking dates, unavailable (sold-out) room, cancel booking |

Tags: `@ecommerce`, `@banking`, `@travel`, `@smoke`, `@negative`, `@admin`.

### Safe to run against the live site

The tests run against a shared, live database, so they are written to leave it as they found it:

- **Relative checks.** Balances and stock are read before an action and compared afterwards. The tests never expect a fixed value that other users could change.
- **Unique amounts.** Each banking scenario uses a random amount between ₱1.01 and ₱9.99. This avoids the site's duplicate-transaction rule and keeps money movements small.
- **Free dates.** Hotel bookings use random dates 400 to 700 days ahead, so rooms are available.
- **Cleanup.** Orders and bookings are cancelled, and stock changes are reversed, after each scenario. Cleanup runs even if the scenario fails.

Deposits, withdrawals and transfers cannot be undone in the app. They are small and they balance each other over time, but they do appear in the test customer's history.

## Requirements

- **Java 21 (JDK).** Check with `java -version`. On Windows, Eclipse Temurin 21 works: `winget install EclipseAdoptium.Temurin.21.JDK`.
- **Maven.** Not needed. `mvnw` / `mvnw.cmd` download Maven 3.9.16 on first use.
- **A browser.** Chrome (default), Edge, or Firefox. WebDriverManager downloads the matching driver automatically.
- **Internet access** to the site under test and, on first run, to Maven Central.

### IntelliJ IDEA

1. **File → Open** and choose the `selenium-java` folder. IntelliJ imports it as a Maven project.
2. Set the project SDK to Java 21 (**File → Project Structure → Project**).
3. Install the **Cucumber for Java** and **Gherkin** plugins (**Settings → Plugins**). They let you jump from a Gherkin step to its Java code and run a single scenario from the gutter.
4. To run everything, right-click `src/test/resources/testng.xml` → **Run**, or run `TestRunner`.
5. Set credentials in the run configuration under **Environment variables**, or use `secrets.properties` (see below).

## Project structure

```text
selenium-java/
├── pom.xml                              Dependencies, Surefire and TestNG suite
├── mvnw, mvnw.cmd, .mvn/                Maven Wrapper
└── src/test/
    ├── java/com/qualitylab/
    │   ├── config/Config.java           Settings: system property > env var > secrets file > env file
    │   ├── driver/DriverFactory.java    Chrome, Edge or Firefox; headless or headed
    │   ├── context/TestContext.java     Per-scenario driver, shared values and cleanup queue
    │   ├── hooks/Hooks.java             Start browser; on failure screenshot; cleanup; quit
    │   ├── pages/                       Page objects (BasePage holds the explicit waits)
    │   │   ├── common/                  Home, login, navigation bar
    │   │   ├── ecommerce/               Shop, product, cart, checkout, order, orders
    │   │   ├── banking/                 Dashboard, deposit/withdraw/transfer, account
    │   │   ├── travel/                  Hotels, hotel, room, booking
    │   │   └── admin/                   Product inventory
    │   ├── steps/                       Step definitions per module
    │   ├── reporting/                   Execution summary plugin
    │   ├── runners/TestRunner.java      Cucumber + TestNG runner
    │   └── utils/                       Money parsing, test data, screenshots
    └── resources/
        ├── features/                    ecommerce, banking, travel .feature files
        ├── config/                      staging.properties, local.properties, secrets example
        ├── testdata/                    JSON test data (checkout details, account ids)
        ├── testng.xml                   TestNG suite
        ├── cucumber.properties          Keeps reports local (no publishing)
        └── logback-test.xml             Console and file logging
```

### Design notes

- **Locators** use the app's `data-testid` attributes only, so styling changes do not break the tests.
- **Waits** are explicit (`WebDriverWait`). The implicit wait is zero. Clicks wait for the element to be clickable, which also covers the moment before the page is ready to respond.
- **Page objects** hold locators and actions. Step classes hold assertions. Cucumber's PicoContainer injects the same `TestContext` into every step class for one scenario.

## Configuration

| Setting | Meaning | Default |
| --- | --- | --- |
| `BASE_URL` | Site under test | From the environment file |
| `TEST_USERNAME` | Customer email used by most scenarios | **Required** |
| `TEST_PASSWORD` | Customer password | **Required** |
| `ADMIN_USERNAME` | Admin email for the inventory scenario | Optional: the scenario is skipped without it |
| `ADMIN_PASSWORD` | Admin password | Optional |
| `BROWSER` | `chrome`, `edge` or `firefox` | `chrome` |
| `HEADLESS` | `true` runs without a visible window | `false` |
| `TIMEOUT_SECONDS` | Explicit wait limit | `25` (staging), `20` (local) |

Each value is taken from the first place it is found:

1. A `-D` option on the command line, e.g. `-DBROWSER=edge` or the short form `-Dbrowser=edge`
2. An environment variable with the same name
3. `src/test/resources/config/secrets.properties` (git-ignored; copy it from `secrets.properties.example`)
4. `src/test/resources/config/<env>.properties`

The environment is chosen with `-Denv=`:

- `staging` (default): the deployed site, https://quality-lab-jb.vercel.app
- `local`: http://localhost:3100

**Credentials are never stored in the repository.** Use environment variables or the git-ignored `secrets.properties`. The test customer must be the seeded banking customer (Alice), because `testdata/banking.json` uses her account ids.

### Setting credentials

PowerShell (for the current window only):

```powershell
$env:TEST_USERNAME = Read-Host "Test email"
$env:TEST_PASSWORD = Read-Host "Test password" -MaskInput
```

Command Prompt:

```bat
set TEST_USERNAME=your-test-email
set /p TEST_PASSWORD=Test password: 
```

macOS / Linux / Git Bash:

```bash
export TEST_USERNAME="your-test-email"
read -rs -p "Test password: " TEST_PASSWORD && export TEST_PASSWORD
```

## Running the tests

Run these in the `selenium-java` folder. On Windows use `mvnw.cmd` (PowerShell or Command Prompt); on macOS, Linux or Git Bash use `./mvnw`.

| Goal | Command |
| --- | --- |
| Everything against the live site | `mvnw.cmd test` |
| Headless | `mvnw.cmd test -Dheadless=true` |
| Another browser | `mvnw.cmd test -Dbrowser=edge` |
| Against a local copy | `mvnw.cmd test -Denv=local` |
| One module | `mvnw.cmd test -Dcucumber.filter.tags="@banking"` |
| Smoke tests only | `mvnw.cmd test -Dcucumber.filter.tags="@smoke"` |
| Skip negative cases | `mvnw.cmd test -Dcucumber.filter.tags="not @negative"` |
| Compile only | `mvnw.cmd test-compile` |

In PowerShell, wrap options that contain dots in quotes, for example `mvnw.cmd test "-Dcucumber.filter.tags=@banking"`.

## Reports and evidence

After a run, look in `target/`:

| File | Contents |
| --- | --- |
| `execution-summary.txt` | Totals (passed, failed, skipped), duration, environment, browser, and each failure with its reason |
| `cucumber-reports/cucumber.html` | Cucumber report: every scenario and step, with failure screenshots embedded |
| `cucumber-reports/cucumber.json` | The same results as JSON for CI tools |
| `surefire-reports/index.html` | TestNG report |
| `surefire-reports/testng-results.xml` | TestNG results as XML |
| `screenshots/` | One PNG per failed scenario, named after the scenario and time |
| `logs/test-run.log` | Run log: each scenario, the steps' actions, and cleanup |

Open the HTML reports in any browser, for example `start target\cucumber-reports\cucumber.html` in Command Prompt or `Invoke-Item target\cucumber-reports\cucumber.html` in PowerShell.

The summary is also printed at the end of the console output.

## Troubleshooting

- **`TEST_USERNAME is not set`**: set the credentials as shown above.
- **A browser does not start**: make sure the chosen browser is installed. WebDriverManager needs internet access the first time to download the driver.
- **Timeouts on the first scenario**: the live site can take a few seconds to wake up. Raise `TIMEOUT_SECONDS`, for example `-DTIMEOUT_SECONDS=40`.
- **`Admin` scenario skipped**: set `ADMIN_USERNAME` and `ADMIN_PASSWORD`.
