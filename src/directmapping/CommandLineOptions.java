package directmapping;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "r2rml")
public class CommandLineOptions {

	@Option(names = {"-h", "--help" }, usageHelp = true, description = "Display a help message")
	boolean help = false;

	@Option(names = {"-c", "--connectionURL"}, description = "A JDBC connection URL to a database", required = true)
	protected String connectionURL = null;

	@Option(names= {"-u", "--user"}, description = "Username for the user connecting to the database")
	protected String user = null;

	@Option(names= {"-p", "--password"}, description = "Password for the user connecting to the database")
	protected String password = null;

	@Option(names= {"-m", "--mappingFile"}, description = "The R2RML mapping file as output")
	protected String mappingFile = null;
	
	@Option(names= {"-o", "--outputFile"}, description = "The output file as if you want to immediately generate RDF")
	protected String outputFile = null;
	
	@Option(names = {"-b", "--baseIRI"}, description = "Used in resolving relative IRIs produced by the R2RML mapping" )
	protected String baseIRI = null;
	
//	@Option(names = {"--CSVFiles"}, description = "A list of paths to CSV files that are separated by semicolons (cannot be used with connectionURL)" )
//	String CSVFiles = null;

	public CommandLineOptions(String[] args) {
		try {
			CommandLineOptions options = CommandLine.populateCommand(this, args);
			if (options.help) {
				new CommandLine(this).usage(System.out);
				System.exit(0);
			}
		} catch (CommandLine.ParameterException pe) {
			System.out.println(pe.getMessage());
			new CommandLine(this).usage(System.out);
			System.exit(64);
		}
	}
}