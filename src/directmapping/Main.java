package directmapping;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;

import r2rml.engine.Configuration;
import r2rml.engine.R2RMLProcessor;

public class Main {

	private static Logger logger = Logger.getLogger(DirectMapping.class.getName());

	public static void main(String[] args) {
		try {			
			CommandLineOptions cli = new CommandLineOptions(args);
			
			if(cli.outputFile != null) {
				System.out.println("We will immediately execute the direct mapping.");
				
				if(cli.mappingFile == null || cli.mappingFile.equals("")) {
					cli.mappingFile = File.createTempFile("mapping", ".ttl").getAbsolutePath();
					System.out.println("No mapping file was given, so mapping will be stored in: " + cli.mappingFile);
				}
			}
			
			DirectMapping dm = new DirectMapping(
						cli.mappingFile, 
						cli.baseIRI, 
						cli.connectionURL, 
						cli.user, 
						cli.password);
			dm.execute();
			
			if(cli.outputFile != null) {
				Configuration configuration = new Configuration();
				configuration.setBaseIRI(cli.baseIRI);
				configuration.setConnectionURL(cli.connectionURL);
				configuration.setUser(cli.user);
				configuration.setPassword(cli.password);
				configuration.setMappingFile(cli.mappingFile);
				
				R2RMLProcessor engine = new R2RMLProcessor(configuration);
				engine.execute();
				
				File o = new File(cli.outputFile);
				if(o.exists()) 
					o.delete();
				o.createNewFile(); // Make sure that file exists for APIs.

				FileOutputStream out = new FileOutputStream(o);
				RDFDataMgr.write(out, engine.getDataset().getDefaultModel(), Lang.TURTLE);
				out.close();
			}
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(-1);
		}
	}
}
