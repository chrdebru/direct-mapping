package test;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;

import directmapping.DirectMapping;
import directmapping.Main;
import junit.framework.TestCase;

public class TestWithH2 extends TestCase {

	private static Logger logger = Logger.getLogger(TestWithH2.class.getName());
	private static String connectionURL = "jdbc:h2:mem:test";

	public TestWithH2(String testName) {
		super(testName);
	}

	@BeforeClass
	public static void init() throws Exception {
		// Log4J junit configuration.
		BasicConfigurator.configure();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		try {
			logger.info("Starting in-memory database for unit tests");
			Class.forName("org.h2.Driver");
			DriverManager.getConnection(connectionURL + ";create=true;").close();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Exception during database startup.");
		}

		try {
			Connection connection = DriverManager.getConnection(connectionURL);
			Statement statement = connection.createStatement();
			statement.execute("CREATE TABLE LIKES (PERSON_ID INT NOT NULL, ANGLE1 INT NOT NULL, ANGLE2 INT NOT NULL)");
			statement.execute("INSERT INTO LIKES (PERSON_ID, ANGLE1, ANGLE2) VALUES (1, 60, 60), (1, 70, 70), (2, 70, 70)");
			statement.execute("CREATE TABLE PERSON (ID INT NOT NULL, FNAME VARCHAR(25) NOT NULL, LNAME VARCHAR(25) NOT NULL)");
			statement.execute("INSERT INTO PERSON (ID, FNAME, LNAME) VALUES (1, 'JANE', 'DOE'), (2, 'JOHN', 'DOE')");
			statement.execute("CREATE TABLE TRIANGLE ( ANGLE1 INT NOT NULL, ANGLE2 INT NOT NULL, ANGLE3 INT NOT NULL)");
			statement.execute("INSERT INTO TRIANGLE (ANGLE1, ANGLE2, ANGLE3) VALUES (60, 60, 60), (70, 70, 20)");
			statement.execute("ALTER TABLE LIKES ADD PRIMARY KEY (PERSON_ID, ANGLE1, ANGLE2)");
			statement.execute("ALTER TABLE PERSON ADD PRIMARY KEY (ID)");
			statement.execute("ALTER TABLE TRIANGLE ADD PRIMARY KEY (ANGLE1,ANGLE2)");
			statement.execute("ALTER TABLE LIKES ADD CONSTRAINT LIKES_IBFK_2 FOREIGN KEY (ANGLE1, ANGLE2) REFERENCES TRIANGLE (ANGLE1, ANGLE2)");
			statement.execute("ALTER TABLE LIKES ADD CONSTRAINT LIKES_IBFK_1 FOREIGN KEY (PERSON_ID) REFERENCES PERSON (ID)");
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Failure setting up the database.");
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		logger.info("Stopping in-memory database.");
		try {
			DriverManager.getConnection(connectionURL).close();
		} catch (SQLNonTransientConnectionException ex) {
			if (ex.getErrorCode() != 45000) {
				throw ex;
			}
			// Shutdown success
		}
	}

	public void testGenerationOfMapping() throws Exception {
		String file = File.createTempFile("mapping", ".ttl").getAbsolutePath();
		DirectMapping dm = new DirectMapping(file, "http://www.example.org/my-db/", connectionURL, null, null);
		Model mapping = dm.execute();
		mapping.write(System.out, "TURTLE");
		// We cannot compare the result with the expected outcome as blank node identifiers are different
		// Just check the outcome with the mapping in the resources folder...
	}
	
	public void testGenerationOfRDF() throws Exception {
		String file = File.createTempFile("output", ".ttl").getAbsolutePath();	
		String[] args = {"-b", "http://www.example.org/my-db/", "-o", file, "-c", connectionURL };
		Main.main(args);
		
		Model rdf = ModelFactory.createDefaultModel();
		rdf.read(file);
		rdf.write(System.out, "TURTLE");
		
//		The following is to be used with a MySQL database included in the test resources		
//		file = File.createTempFile("output", ".ttl").getAbsolutePath();	
//		args = new String[] {"-b", "http://www.example.org/my-db/", "-o", file, "-c", "jdbc:mysql://localhost:3306/directmappingtest", "-p", "dmtest", "-u", "dmtest" };
//		Main.main(args);
//		rdf = ModelFactory.createDefaultModel();
//		rdf.read(file);
//		rdf.write(System.out, "TURTLE");
	}
}
