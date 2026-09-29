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

`SearchSkillsTest` — one journey, 15 lines:

1. Open the Club home page.
2. Type `skills` into the search bar and press Enter.
3. Check at least one topic came back, and the first result's title mentions `skills`.
4. Click the first result.
5. Check the URL is a topic URL (`/t/...`) and the heading matches the result
   that was clicked.

---

## 3. Project layout

Nine files. Read them in this order and the whole framework makes sense.

```
pom.xml               dependencies, and the -D pass-through into the test JVM
testng.xml            the suite: registers the listener, lists the test classes
Jenkinsfile           the CI/CD pipeline
src/test/resources/
  log4j2.xml          console appender + target/logs/automation.log

src/test/java/com/mot/
  Config.java         all settings; browser + headless overridable with -D
  BaseTest.java       the browser lifecycle AND all evidence capture
  TestListener.java   TestNG events -> report entries
  ExtentReport.java   the one HTML report + the "current test" pointer
  Log.java            one call -> log file AND report
  SearchSkillsTest.java   the test
  pages/
    HomePage.java           the search bar
    SearchResultsPage.java  the results list
    TopicPage.java          the opened topic
```

### The four rules

1. **Tests hold no selectors, no waits, no browser code.** `SearchSkillsTest`
   is 15 lines of user journey.
2. **`BaseTest` owns the browser and the evidence.** Setup and teardown in one
   file, readable top to bottom.
3. **`TestListener` owns the reporting.** Registered once in `testng.xml`, which
   is why no test contains reporting code.
4. **`Log.info()` writes twice** — log file and report — so the report reads as a
   step-by-step story for free.

### Deliberately kept simple

This is a beginner-level framework on purpose. These are the shortcuts, so you
can name them before an interviewer does:

| Simplification | What it would take to do "properly" |
|---|---|
| Tests run one at a time | `parallel="methods"` needs the browser fields in `BaseTest` and the two fields in `ExtentReport` wrapped in `ThreadLocal`, because Playwright's Java objects are not thread-safe |
| Settings are constants in `Config.java` | A `config.properties` file per environment (dev/stage/prod) |
| Video and trace are always retain-on-failure | A config switch for `always` / `never` / `retain-on-failure` |
| No `BasePage` | A shared parent only pays off once several pages need the same action |
| Only chromium / firefox / webkit | Real Chrome and Edge need `setChannel("chrome")` / `setChannel("msedge")` |
| A test that calls `throw new SkipException` gets its own report node | Tracking the report node per `ITestResult` instead of one static field |

---

## 4. What gets captured, and when

Policy is fixed: **keep on failure, delete on success.** It lives in
`BaseTest.tearDown`, which is 3 `if (failed)` checks and nothing more.

| Artifact | How it is produced | Kept when |
|---|---|---|
| Screenshot | `page.screenshot()` full page, in `BaseTest.tearDown` | test failed |
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

1. **Manage Jenkins → Plugins → Available** → install **HTML Publisher**, tick
   *Restart Jenkins when installation is complete*.
2. **Manage Jenkins → Tools**
   - *JDK installations* → Add JDK → **uncheck "Install automatically"** →
     Name `JDK21`, JAVA_HOME `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
   - *Maven installations* → Add Maven → **uncheck "Install automatically"** →
     Name `Maven3`, MAVEN_HOME `/opt/homebrew/Cellar/maven/3.9.9/libexec`
   - The names must match `tools { }` in the `Jenkinsfile` exactly.
   - These are needed because Jenkins started by launchd does **not** inherit your
     shell `PATH`, so `mvn` is not on it.
3. **New Item → Pipeline** → *Pipeline script from SCM* → Git →
   `https://github.com/sanyaanand12qw/ministry_of_testing` → branch `*/main` →
   Script path `Jenkinsfile`.
4. Run once so the build parameters appear, then use **Build with Parameters**.

Build parameters: `BROWSER`, `HEADLESS`, `SUITE_FILE`.

**Triggering:** a locally bound Jenkins (`--httpListenAddress=127.0.0.1`) cannot
receive GitHub webhooks, because GitHub has no route to it. Use **Build Now**, or
*Configure → Build Triggers → Poll SCM* with `H/5 * * * *` to pick up pushes within
five minutes. A webhook needs Jenkins reachable from the internet (ngrok/Cloudflare
tunnel, or a hosted controller).

**`HEADLESS=false` will not work** on this agent. Jenkins runs as a launchd service
with no window server session, so a headed browser has no display to draw into.
Run headed locally with `mvn clean test -Dheadless=false` instead.

### Making the report actually render

Jenkins serves any archived HTML with a deliberately strict header:

```
Content-Security-Policy: sandbox allow-same-origin; default-src 'none'; img-src 'self'; style-src 'self';
```

`default-src 'none'` blocks all JavaScript, and `style-src 'self'` blocks any CSS
that is not served by Jenkins itself. An Extent report hitting that looks like an
unstyled bullet list. Two separate fixes are needed:

**1. Framework side (already done).** `ExtentReport.start()` calls
`spark.config().setOfflineMode(true)`, which writes Extent's own CSS and JS into
`target/extent-report/spark/` so they come from Jenkins, not a CDN. This also
means the report works on a CI agent with no internet.

**2. Jenkins side.** Allow self-hosted scripts, fonts and video. Add this to the
Jenkins JVM arguments so it survives a restart — in
`~/Library/LaunchAgents/sh.brew.jenkins-lts.plist`, inside `ProgramArguments`:

```xml
<string>-Dhudson.model.DirectoryBrowserSupport.CSP=sandbox allow-scripts allow-same-origin allow-popups; default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; media-src 'self'</string>
```

then `brew services restart jenkins-lts`. To apply it immediately without a
restart, run the same `System.setProperty(...)` from **Manage Jenkins → Script
Console** — but that is lost on restart, which is why the plist entry matters.

`media-src 'self'` is the part that lets the `.webm` video play in the browser.

**This is a real trade-off:** it lets archived HTML run JavaScript in the Jenkins
origin. Acceptable on a local single-user instance where you control what gets
archived; on a shared controller, read
<https://www.jenkins.io/doc/book/security/configuring-content-security-policy/>
first.

### Which URL to open

| URL | What it is |
|---|---|
| `/job/<job>/<n>/Extent_20Report/` | **the report tab** — use this |
| `/job/<job>/<n>/artifact/target/extent-report/index.html` | the raw archived file; works, but it is the download view |

---

## 6. The full workflow, start to finish

### Local run

1. `mvn clean test`.
2. Surefire starts a JVM and hands `testng.xml` to TestNG.
3. TestNG reads the suite, registers `TestListener`, calls `onStart` →
   `ExtentReport.start()` creates the report.
4. `BaseTest.setUp` (`@BeforeMethod`) → Playwright starts → browser launches →
   fresh context with **video recording ON** → **tracing ON** → page opens.
5. `onTestStart` → a section for this test is created in the report, and
   `ExtentReport.currentTest` starts pointing at it.
6. The test drives page objects. Each `Log.info` writes to the log file and the
   report at the same time.
7. Test method ends → TestNG sets the result → `onTestSuccess` / `onTestFailure`
   marks the report section (a failure attaches the full stack trace).
8. `BaseTest.tearDown` (`@AfterMethod`) reads `ITestResult`:
   - failed → screenshot now, while the page is still alive
   - tracing stops **with** a path on failure, **without** one on success (discarded)
   - context closes → the `.webm` appears → kept on failure, deleted on success
   - browser and Playwright close
9. `onFinish` → `ExtentReport.flush()` writes `index.html`.
10. Surefire fails the build if any test failed.

### CI run

1. You push to `main` on GitHub.
2. Jenkins triggers (webhook, or SCM polling).
3. **Checkout** — the repo is cloned into the agent workspace.
4. **Install Browsers** — `mvn exec:java` downloads the engine into
   `$JENKINS_HOME/playwright-browsers`; a no-op on later builds.
5. **Test** — `mvn clean test -Dbrowser=… -Dheadless=…`. The `-D` flags come from
   the build parameters, get forwarded by Surefire's `systemPropertyVariables`,
   and are read by `Config.java`.
6. **Publish Report** — the whole `target/extent-report` folder is published as the
   *Extent Report* tab.
7. **post always** — `surefire-reports/*.xml` feeds the Jenkins test trend graph, and
   the report folder plus `automation.log` are archived as downloadable artifacts.

### When a test fails

1. Playwright throws — a failed assertion, or a timeout because a locator never
   appeared.
2. `onTestFailure` → the report section turns red and the stack trace is attached.
3. `BaseTest.tearDown` → screenshot, trace zip and video are written and linked
   into the report.
4. In the pipeline, `catchError` marks the build **FAILURE** but does **not** abort it,
   so the *Publish Report* stage and `post { always }` still run. **A failure never
   loses its evidence.**
5. Jenkins shows the build red; the test trend graph records the regression.
6. You debug in this order:
   - **Extent Report tab** → which step failed, the screenshot, the stack trace
   - **`automation.log`** → the full step sequence in order
   - **video** → what the user would have seen
   - **trace** → `npx playwright show-trace <zip>` for DOM, network and console at the
     exact failing action

---

## 7. Deliberately not built yet

Retry analyser for flaky tests, cross-browser matrix builds, Docker agent, Allure,
data-driven/Excel layer, API-level tests, Slack/email notifications, Jenkins shared
library.
