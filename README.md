# Direct Mapping Implementation via R2RML

The following tool generates an [R2RML](https://www.w3.org/TR/r2rml/#foreign-key) mapping that generates RDF according to the rules of a [direct mapping](https://www.w3.org/TR/rdb-direct-mapping/). This tool allows you to either:

* Generate a mapping that you can subsequently use with an R2RML processor, or
* Generate and immediately execute the mapping with the [this](https://github.com/chrdebru/r2rml) R2RML processor.

## Building and using the code

First, ensure that you have installed the [following R2RML processor](https://github.com/chrdebru/r2rml) on your machine using `mvn clean install`. 

To build the project and copy its dependencies, execute

```bash
$ mvn package
$ mvn dependency:copy-dependencies
```

To generate a mapping, you can execute:

```bash
$ java -jar directmapping.jar \
	--baseIRI http://www.example.org/my-db/ \
	--connectionURL jdbc:mysql://localhost:3306/directmappingtest \
	--user dmtest \
	--password dmtest \
	--mappingFile ./mapping.ttl
```

To execute a direct mapping, you can execute:

```bash
$ java -jar directmapping.jar \
	--baseIRI http://www.example.org/my-db/ \
	--connectionURL jdbc:mysql://localhost:3306/directmappingtest \
	--user dmtest \
	--password dmtest \
	--outputFile ./output.ttl
```

When you execute the direct mapping without specifying a mapping file, the tool will create a mapping file in your machine's `temp` folder. You can specify both a mapping and and output file. The format of both files is TURTLE. Consult `$ java -jar directmapping.jar -h` for more information on the parameters. 


## License
This implementation of R2RML is written by [Christophe Debruyne](http://www.christophedebruyne.be/) and released under the [MIT license](http://opensource.org/licenses/MIT).