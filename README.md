# ef2so - An Entity Facts-To-Schema-org Converter
This is an on-demand [Entity Facts](http://www.dnb.de/entityfacts) (EF) to [Schema.org](https://schema.org/) Converter. That means, that the transformation work will be done in the moment of access! URL schema is `http://www.example.org:8080/yourpath/{GND-IDN}`.

*Online Demo:* https://ef2sop2000451198trial.hanatrial.ondemand.com/118540238
## Entity types
- Supported: `organisation`, `person` and `place`
- *Unsupported*: `family` (due to a lack in Schema.org)

## Workflow
1. Request: http://www.example.org:8080/yourpath/118540238
2. Get data from Entity Facts: http://hub.culturegraph.org/entityfacts/118540238
3. Transform data (JSON-LD to JSON-LD): Transformation ([Mapping](src/main/resources/ef2so_transformation.xml)) is made with [Metafacture](https://github.com/metafacture/metafacture-core)

# Compile & Deploy
1. Download Maven Project from this repository.
2. Install [Maven](https://maven.apache.org/) project management tool.
3. Run in the folder with `pom.xml` the following command: `mvn clean package`
4. Take `target\ef2so.war`and deploy it on your webserver.

# Contribution
Thanks to [jentschk](https://github.com/jentschk) for providing the conceptual mapping (Entity Facts data model to Schema.org data model).