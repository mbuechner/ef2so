![Java CI with Maven](https://github.com/mbuechner/ef2so/workflows/Java%20CI%20with%20Maven/badge.svg) ![Docker](https://github.com/mbuechner/ef2so/workflows/Docker/badge.svg)
# EF2SO - An Entity Facts-To-Schema.org Converter
This is an on-demand [Entity Facts](http://www.dnb.de/entityfacts) (EF) to [Schema.org](https://schema.org/) Converter. That means, that the transformation work will be done in the moment of access! URL schema is `http://www.example.org/yourpath/{GND-IDN}`.

*EF2SO at DDBlabs:* https://labs.ddb.de/app/ef2so/118540238
## Entity types
- Supported: `organisation`, `person` and `place`
- *Unsupported*: `family` (due to a lack in Schema.org)

## Workflow
1. Request: http://www.example.org/yourpath/118540238
2. Get data from Entity Facts: http://hub.culturegraph.org/entityfacts/118540238
3. Transform data (JSON-LD to JSON-LD): Transformation ([Mapping](src/main/resources/ef2so_transformation.xml)) is made with [Metafacture](https://github.com/metafacture/metafacture-core)

# Contribution
Thanks to  [Jana Hentschke](https://twitter.com/junicatalo) for providing the conceptual mapping (Entity Facts data model to Schema.org data model).

# Compile & Deploy
1. Clone this repository and install [Amazon Corretto 25 LTS](https://aws.amazon.com/corretto/) and [Maven 3](https://maven.apache.org/).
2. Run `mvn clean verify` in the directory containing `pom.xml`.
3. Deploy `target/ef2so.war` to [Tomcat 11](https://tomcat.apache.org/) running Java 25.
4. Open http://localhost:8080/ef2so/118540238. Deploy the file as `ROOT.war` to serve it at http://localhost:8080/118540238.

## Project reports

GitHub Actions builds and publishes the [Maven project reports](https://mbuechner.github.io/ef2so/) on pushes to `master`. Run `mvn verify site` to generate them locally and open `target/site/index.html`.

# Docker

```sh
docker run --rm -p 8080:8080 ghcr.io/mbuechner/ef2so:latest
```

EF2SO serves `http://localhost:8080/118540238`. For a prefix, pass `--env "URLPATTERN=/foo/bar/*"` to serve it at `http://localhost:8080/foo/bar/118540238`. The image runs Tomcat on port 8080 as UID 1001 in group 0, which supports OpenShift's assigned UIDs. Use `docker build --pull -t ef2so .` for a local build with the current Tomcat 11 and Corretto 25 base images.

## Logging

Application logs go to the console. Set `EF2SO_LOG_LEVEL=DEBUG` for stack traces on upstream failures.

Tomcat writes one JSON record per request to `$CATALINA_BASE/logs/ef2so_access.YYYY-MM-DD.jsonl`. It includes the Unix timestamp in milliseconds, client and connection-peer IPs, method, path, protocol, status, response bytes, duration in microseconds, User-Agent and response encoding. Query strings, cookies and authorization headers are omitted. Logs rotate daily. Set `EF2SO_ACCESS_LOG_MAX_DAYS` to override the 30-day retention, and mount `/opt/tomcat/logs` to keep the files across container replacements.

Use `requestAttributes.requestId` to match a request to any associated console logs. EF2SO generates this ID. Tomcat records an incoming `X-Request-ID` separately under `requestHeaders`. The `failureReason` field distinguishes errors such as `invalid_idn`, `upstream_http`, `upstream_timeout` and `upstream_tls`. Requests rejected by Jersey or Tomcat outside EF2SO's error handling can be counted by HTTP status.

List failed requests with their path, reason and correlation ID.

```sh
jq -c 'select((.statusCode | tonumber) >= 400) |
  {status: (.statusCode | tonumber), path} +
  (.requestAttributes | {requestId, failureReason})' "$CATALINA_BASE"/logs/ef2so_access.*.jsonl
```

Count failures by HTTP status and reason.

```sh
jq -r 'select((.statusCode | tonumber) >= 400) |
  [.statusCode, .requestAttributes.failureReason] | @tsv' "$CATALINA_BASE"/logs/ef2so_access.*.jsonl |
  sort | uniq -c | sort -nr
```

For traffic behind a reverse proxy, set `EF2SO_TRUSTED_PROXY_REGEX` to match only the immediate trusted proxy addresses and configure that proxy to replace untrusted `X-Forwarded-For` values. Without that setting, the client IP is the connection peer.

## Container releases

- **Release.** Publish a stable `v*` release from a commit containing the [Docker workflow](.github/workflows/docker-publish.yml). The publisher runs on `master` and builds `linux/amd64` from the tagged source and Dockerfile.
- **Daily at 08:17 Berlin time.** A changed Git SHA, changed base digest or missing version tag triggers a rebuild. Otherwise, the published digest gets a fresh scan.
- **Promote.** HTTP, image and optional SBOM checks gate Cosign signing and version tagging. Fixable High or Critical CVEs block promotion. The maintained release also gets `latest` and `tagged`.

The latest stable release is maintained by default. Set `CONTAINER_RELEASE_TAG` to pin that choice, or use **Actions → Docker → Run workflow** on `master` with a `release_tag` for a one-off run.

Grant Actions write access to the GHCR package. The probe defaults to `OPTIONS /` on port 8080. Configure it with `CONTAINER_HTTP_{METHOD,PATH,STATUS}` and pass container environment variables via `CONTAINER_ENV`.

# How to use it on your website?
See [Google's documentation](https://developers.google.com/search/docs/guides/intro-structured-data). It's better to dynamically inject the Schema.org-JSON-LD, than waiting for the service. Google does support that! It's basically:
```html
<script type="application/ld+json">
{
   "@id":"http://d-nb.info/gnd/118540238",
   "@context":"http://schema.org/",
   "@type":"Person",
   "name":"Johann Wolfgang von Goethe",
   "birthDate":"28. August 1749",
   "deathDate":"22. März 1832",
   ...
}
</script>
```

## Link-Element
As far as I know it is **not possible** to use the link element and refer to the Schema.org data.

````html
<link href="http://www.example.org/yourpath/118540238" rel="alternate" type="application/ld+json" />
````
# Performance
First, that depends on your webserver of course! A non-representative benchmark showed that requests to EF2SO are only 33% slower than requests to Entity Facts.

## Benchmark
### Setup
- Virtual Users: 10 (cloud servers located in the USA)
- Entity Facts and EF2SO servers located in Frankfurt, Germany
- Duration 30sec.
- Request rate: up to 150r/sec.

### Results
- EF2SO: avg. 765.58ms
- Entity Facts: avg. 573.25ms

| %   | HTTP Status Code | Service | URL                                                | LoadTime (ms) |
| --- | ---------------- | ------- | -------------------------------------------------- | ------------- |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/100025250 | 520           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/100000193 | 472           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/100001394 | 555           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/100000355 | 483           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/140585524 | 569           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/100054102 | 659           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/118577182 | 543           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/100014704 | 601           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/118540238 | 629           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/118505556 | 589           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/106220063 | 659           |
| 100 | 200              | EF      | https://hub.culturegraph.org/entityfacts/100001467 | 600           |
| 100 | 501              | EF2SO   | https://labs.ddb.de/app/ef2so/118505556            | 719           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/100000355            | 752           |
| 100 | 501              | EF2SO   | https://labs.ddb.de/app/ef2so/140585524            | 854           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/100001394            | 777           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/106220063            | 736           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/100000193            | 775           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/118540238            | 673           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/100054102            | 736           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/100025250            | 768           |
| 100 | 501              | EF2SO   | https://labs.ddb.de/app/ef2so/118577182            | 811           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/100001467            | 725           |
| 100 | 200              | EF2SO   | https://labs.ddb.de/app/ef2so/100014704            | 861           |
