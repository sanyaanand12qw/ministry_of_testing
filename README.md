# Ministry of Testing — UI Automation Framework

Playwright + Java 21 + TestNG + Extent Reports, with a Jenkins CI/CD pipeline that
captures **screenshots, video, traces and logs** on failure.

Application under test: <https://club.ministryoftesting.com/> (a Discourse forum).

---

## 1. Quick start

```bash
# one-off on a new machine: download the browser binaries
mvn exec:java

# run the suite (headless, chromium)
mvn clean test

# watch it run in a real browser
mvn clean test -Dheadless=false

# a different engine
mvn clean test -Dbrowser=firefox
```

Open the report: `target/extent-report/index.html`

---

## 2. What the test does

`SearchSkillsTest` — one journey, asserted end to end:

1. Open the Club home page.
2. Type `skills` into the search bar and press Enter.
3. Check at least one topic is returned and the first result's title mentions `skills`.
4. Click the first result.
5. Check the topic page opened, the URL is a topic URL (`/t/...`), the heading
   matches the result that was clicked, and the topic has posts.

Steps 1–5 use soft assertions at the end so one mismatch does not hide the others.

---

## 3. Project layout

```
pom.xml                  dependencies + Surefire wiring
testng.xml               the suite: parallel=methods, listener registered
Jenkinsfile              the CI/CD pipeline
src/test/resources/
  config.properties      base url, browser, timeouts, capture policy
  log4j2.xml             console + file appenders
src/test/java/com/mot/
  config/ConfigReader        -D overrides > config.properties > fallback
  core/DriverManager         ThreadLocal Playwright / Browser / Context / Page
  core/PlaywrightFactory     builds the browser stack; keeps or drops video + trace
  core/BaseTest              @BeforeMethod / @AfterMethod + evidence capture
  pages/BasePage             shared actions, each one logged
  pages/HomePage             the search bar
  pages/SearchResultsPage    the results list
  pages/TopicPage            the opened topic
  listeners/TestListener     ITestListener -> Extent nodes, status, MDC, flush
  report/ExtentManager       ExtentReports singleton + ThreadLocal<ExtentTest>
  utils/ArtifactUtil         artifact folder layout + relative paths
  utils/Log                  one call -> log file + Extent report
  tests/SearchSkillsTest     the test
```

### Design rules

- **Tests hold no locators and no waits.** They read as the user journey.
- **One `BrowserContext` per test method** → empty cookies/storage, no test bleed.
- **Everything in `ThreadLocal`** → `parallel="methods"` is safe, because Playwright's
  Java objects are not thread-safe.
- **`Log.info()` writes twice** — once to `automation.log`, once to the Extent node —
  so the report reads as a step-by-step narrative with no duplicated code.
- **Evidence is captured in `@AfterMethod`, not in the listener.** That is the last
  point at which the page is still open, so it works regardless of the order TestNG
  fires its callbacks in.

---

## 4. What gets captured, and when

Capture policy lives in `config.properties` (`always` | `retain-on-failure` | `never`).
Default is `retain-on-failure`.

| Artifact | How it is produced | Kept when |
|---|---|---|
| Screenshot | `page.screenshot()` full page, in `@AfterMethod` | test failed |
| Video (`.webm`) | context option `setRecordVideoDir`, flushed on `context.close()` | test failed |
| Trace (`.zip`) | `context.tracing().start(screenshots, snapshots, sources)` | test failed |
| Log file | Log4j2 file appender | always |
| `surefire-reports/*.xml` | TestNG/Surefire | always |

Recording always has to be switched **on before** the test runs — you cannot
retro-fit a video after a failure. So the framework records eagerly and throws the
artifact away when the test passes.

### Where they land

```
target/
├── extent-report/
│   ├── index.html          <- open this
│   ├── screenshots/<test>.png
│   ├── videos/<test>.webm
│   └── traces/<test>.zip
├── logs/automation.log
└── surefire-reports/*.xml
```

Artifacts live **inside** the report folder on purpose: Extent links them with short
relative paths (`screenshots/x.png`), so the folder can be zipped, copied or published
by Jenkins and every link still works.

Reading a trace:

```bash
npx playwright show-trace target/extent-report/traces/<test>.zip
```

You get a filmstrip, the DOM snapshot at every action, the network log, the console
log, and the Java source line behind each step.

---

## 5. Jenkins setup

On the Jenkins controller, one-time:

1. **Manage Jenkins → Tools**
   - JDK named `JDK21`
   - Maven named `Maven3`
2. **Manage Jenkins → Plugins** → install **HTML Publisher**.
3. **New Item → Pipeline** → *Pipeline script from SCM* → Git →
   `https://github.com/sanyaanand12qw/ministry_of_testing` → Script path `Jenkinsfile`.
4. Run once so the build parameters appear, then use **Build with Parameters**.

Build parameters: `BROWSER`, `HEADLESS`, `SUITE_FILE`.

If the video does not play inside the report tab, Jenkins' content security policy is
blocking it. Either download the artifact instead, or relax the CSP from
**Manage Jenkins → Script Console**:

```groovy
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "sandbox allow-scripts; default-src 'self' 'unsafe-inline' data:;")
```

---

## 6. The full workflow, start to finish

### Local run

1. `mvn clean test`.
2. Surefire starts a JVM and hands `testng.xml` to TestNG.
3. TestNG reads the suite, registers `TestListener`, calls `onStart` → `ExtentReports`
   is created and `target/extent-report/index.html` is reserved.
4. `@BeforeMethod` → `PlaywrightFactory.createPage()`:
   driver starts → browser launches → fresh context → **tracing starts** →
   **video recording starts** → page opens.
5. `onTestStart` → an Extent node is created for the test and the test name goes into
   the logging MDC, so every log line is attributable.
6. The test drives page objects. Each action logs once and appears in the report once.
7. Test method ends → TestNG sets the result → `onTestSuccess` / `onTestFailure` marks
   the Extent node (a failure attaches the full stack trace).
8. `@AfterMethod` reads `ITestResult`:
   - failed → screenshot now, while the page is alive
   - tracing stops **with** a path on failure, **without** one on success (discarded)
   - context closes → the `.webm` lands on disk → kept on failure, deleted on success
   - the scratch recording folder is removed
   - browser and driver close, ThreadLocals are cleared
9. `onFinish` → `extent.flush()` writes the HTML.
10. Surefire fails the build if any test failed.

### CI run

1. You push to `main` on GitHub.
2. Jenkins triggers (webhook, or SCM polling).
3. **Checkout** — the repo is cloned into the agent workspace.
4. **Install Browsers** — `mvn exec:java` downloads the engine into
   `$JENKINS_HOME/playwright-browsers`; a no-op on later builds.
5. **Test** — `mvn clean test -Dbrowser=… -Dheadless=…`. The `-D` flags come from the
   build parameters and win over `config.properties`.
6. **Publish Report** — the whole `target/extent-report` folder is published as the
   *Extent Report* tab.
7. **post always** — `surefire-reports/*.xml` feeds the Jenkins test trend graph, and
   the report folder plus `automation.log` are archived as downloadable artifacts.

### When a test fails

1. Playwright throws — a failed assertion, or a timeout because a locator never
   appeared.
2. `onTestFailure` → the Extent node turns red and the stack trace is attached.
3. `@AfterMethod` → screenshot, trace zip and video are written and linked into the
   report.
4. In the pipeline, `catchError` marks the build **FAILURE** but does **not** abort it,
   so the *Publish Report* stage and `post { always }` still run. **A failure never
   loses its evidence.**
5. Jenkins shows the build red; the test trend graph records the regression.
6. You debug in this order:
   - **Extent Report tab** → which step failed, the screenshot, the stack trace
   - **`automation.log`** → the full step sequence, with the test name on every line
   - **video** → what the user would have seen
   - **trace** → `npx playwright show-trace <zip>` for DOM, network and console at the
     exact failing action

---

## 7. Deliberately not built yet

Retry analyser for flaky tests, cross-browser matrix builds, Docker agent, Allure,
data-driven/Excel layer, API-level tests, Slack/email notifications, Jenkins shared
library.
