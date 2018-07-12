# ef2so - An Entity Facts-To-Schema.org Converter
This is an on-demand [Entity Facts](http://www.dnb.de/entityfacts) (EF) to [Schema.org](https://schema.org/) Converter. That means, that the transformation work will be done in the moment of access! URL schema is `http://www.example.org/yourpath/{GND-IDN}`.

*Online Demo:* https://ef2sop2000451198trial.hanatrial.ondemand.com/ef2so/118540238
## Entity types
- Supported: `organisation`, `person` and `place`
- *Unsupported*: `family` (due to a lack in Schema.org)

## Workflow
1. Request: http://www.example.org/yourpath/118540238
2. Get data from Entity Facts: http://hub.culturegraph.org/entityfacts/118540238
3. Transform data (JSON-LD to JSON-LD): Transformation ([Mapping](src/main/resources/ef2so_transformation.xml)) is made with [Metafacture](https://github.com/metafacture/metafacture-core)

# Compile & Deploy
1. Download Maven Project from this repository.
2. Install [Maven](https://maven.apache.org/) project management tool.
3. Run in the folder with `pom.xml` the following command: `mvn clean package`
4. Take `target\ef2so.war`and deploy it on your webserver.
5. Open Browser, if you're running a local [Tomcat](http://tomcat.apache.org/): http://localhost:8080/118540238

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

# Contribution
Thanks to [jentschk](https://github.com/jentschk) for providing the conceptual mapping (Entity Facts data model to Schema.org data model).