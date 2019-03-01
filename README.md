# EF2SO - An Entity Facts-To-Schema.org Converter
This is an on-demand [Entity Facts](http://www.dnb.de/entityfacts) (EF) to [Schema.org](https://schema.org/) Converter. That means, that the transformation work will be done in the moment of access! URL schema is `http://www.example.org/yourpath/{GND-IDN}`.

*Online Demo:* https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/118540238

*EF2SO at DDBlabs:* https://labs.ddb.de/app/ef2so
## Entity types
- Supported: `organisation`, `person` and `place`
- *Unsupported*: `family` (due to a lack in Schema.org)

## Workflow
1. Request: http://www.example.org/yourpath/118540238
2. Get data from Entity Facts: http://hub.culturegraph.org/entityfacts/118540238
3. Transform data (JSON-LD to JSON-LD): Transformation ([Mapping](src/main/resources/ef2so_transformation.xml)) is made with [Metafacture](https://github.com/metafacture/metafacture-core)

# Contribution
Thanks to [jentschk](https://github.com/jentschk) for providing the conceptual mapping (Entity Facts data model to Schema.org data model).

# Compile & Deploy
1. Download Maven Project from this repository.
2. Install [Maven](https://maven.apache.org/) project management tool.
3. Run in the folder with `pom.xml` the following command: `mvn clean package`
4. Take `target\ef2so.war`and deploy it on your webserver.
5. Open Browser, if you're running a local [Tomcat](http://tomcat.apache.org/): http://localhost:8080/118540238

# Docker
Yes, there's a docker container for EF2SO availible at DockerHub. See https://hub.docker.com/r/mbuechner/ef2so
```
docker pull mbuechner/ef2so
```
## Container build
1. Checkout GitHub repository: `git clone https://github.com/mbuechner/ef2so`
2. Go into folder: `cd ef2so`
3. Run `docker build -t ef2so .`
4. Start container with: `docker run -d -p 8080:8080 -P ef2so`
5. Open browser: http://localhost:8080/118540238
 
Note: The container does listen on port 8080.

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

| %   |HTTP Status Code| Service | URL                                                                  | LoadTime (ms) |
| ---:| --------------:| ------- | -------------------------------------------------------------------- | -------------:|
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/100025250                    |           520 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/100000193                    |           472 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/100001394                    |           555 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/100000355                    |           483 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/140585524                    |           569 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/100054102                    |           659 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/118577182                    |           543 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/100014704                    |           601 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/118540238                    |           629 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/118505556                    |           589 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/106220063                    |           659 |
| 100 | 200            | EF      | http://hub.culturegraph.org/entityfacts/100001467                    |           600 |
| 100 | 501            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/118505556 |           719 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/100000355 |           752 |
| 100 | 501            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/140585524 |           854 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/100001394 |           777 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/106220063 |           736 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/100000193 |           775 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/118540238 |           673 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/100054102 |           736 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/100025250 |           768 |
| 100 | 501            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/118577182 |           811 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/100001467 |           725 |
| 100 | 200            | EF2SO   | https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/100014704 |           861 |
