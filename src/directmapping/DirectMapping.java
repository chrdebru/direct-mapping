package directmapping;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import r2rml.engine.R2RML;
import r2rml.util.IRISafe;
import util.DMUtil;
import util.DirectMappingException;

public class DirectMapping {

	private Connection connection;
	private Model mapping;
	private Map<String, Resource> triplesMapMap;

	private String mappingFile;
	private String baseIRI;
	private String connectionURL;
	private String user;
	private String password;

	public DirectMapping(String mappingFile, String baseIRI, String connectionURL, String user, String password) {
		this.mappingFile = mappingFile;
		this.baseIRI = baseIRI;
		this.connectionURL = connectionURL;
		this.user = user;
		this.password = password;
	}

	public Model execute() throws DirectMappingException {

		mapping = ModelFactory.createDefaultModel();
		mapping.setNsPrefix("rr", R2RML.NS);
		if(baseIRI != null && !"".equals(baseIRI)) {
			mapping.setNsPrefix("", baseIRI);
		}

		triplesMapMap = new HashMap<String, Resource>();

		openDatabaseConnection();

		List<String> tableNames = getTableNames();
		
		for(String tableName : tableNames)
			processTable(tableName);
		
		for(String tableName : tableNames)
			processFKsofTable(tableName);
		
		closeDatabaseConnection();

		writeToFile();
		
		return mapping;
	}

	private void processFKsofTable(String tableName) throws DirectMappingException {
		try {
			DatabaseMetaData dm = connection.getMetaData();
			ResultSet rs = dm.getImportedKeys(null, null, tableName);

			/*
			 * PKTABLE_NAME		- table to
			 * PKCOLUMN_NAME	- column to
			 * FKTABLE_NAME		- table from
			 * FKCOLUMN_NAME	- column from
			 * FK_NAME
			 */
			// first get all FK_names and their tables
			Map<String,String> fks = new HashMap<String,String>();
			while(rs.next()) {
				// this foreign key refers that table
				fks.put(rs.getString("FK_NAME"), rs.getString("PKTABLE_NAME"));
			}

			// now find all columns of a fk and create the predicate object map :-)
			for(String fk : fks.keySet()) {
				List<String> from_cols = new ArrayList<String>();
				List<String> to_cols = new ArrayList<String>();

				rs = dm.getImportedKeys(null, null, tableName);
				while(rs.next()) {
					if(rs.getString("FK_NAME").equals(fk)) {
						from_cols.add(rs.getString("FKCOLUMN_NAME"));
						to_cols.add(rs.getString("PKCOLUMN_NAME"));
					}
				}

				createPredicateObjectMapForFK(tableName, fk, fks.get(fk), from_cols, to_cols);
			}

		} catch(Exception e) {
			throw new DirectMappingException("Error processing FKs of table: " + tableName, e);
		}

	}

	private void createPredicateObjectMapForFK(String tableName, String fk, String reftable, List<String> from_cols, List<String> to_cols) {
		Resource tm = triplesMapMap.get(tableName);

		Resource pom = mapping.createResource();
		mapping.add(tm, R2RML.predicateObjectMap, pom);

		String predicate = baseIRI + IRISafe.toIRISafe(tableName) + "#ref-";
		Iterator<String> iter = from_cols.iterator();
		while(iter.hasNext()) {
			String field = iter.next();
			predicate += IRISafe.toIRISafe(field);
			if(iter.hasNext()) predicate += ";";
		}
		mapping.add(pom, R2RML.predicate, mapping.createResource(predicate));

		Resource om = mapping.createResource();
		mapping.add(pom, R2RML.objectMap, om);
		Resource parent = triplesMapMap.get(reftable);
		mapping.add(om, R2RML.parentTriplesMap, parent);

		for(int i = 0; i < from_cols.size(); i++) {
			Resource jc = mapping.createResource();
			mapping.add(om, R2RML.joinCondition, jc);
			mapping.add(jc, R2RML.child, from_cols.get(i));
			mapping.add(jc, R2RML.parent, to_cols.get(i));
		}
	}

	private void processTable(String tableName) throws DirectMappingException {
		Resource tm = mapping.createResource(baseIRI + IRISafe.toIRISafe(tableName) + "-TriplesMap");
		triplesMapMap.put(tableName, tm);

		try {
			String query = "SHOW COLUMNS FROM " + tableName;
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(query);

			String field, type, key;
			List<String> fields_key = new ArrayList<String>();
			while (rs.next()) {
				// Order is "Field", "Type", "Null", and "Key", but cases vary
				field = rs.getString(1);
				type = rs.getString(2).toUpperCase();
				key = rs.getString(4);

				createPredicateObjectMap(tm, tableName, field, type);

				if("PRI".equals(key.toUpperCase())) fields_key.add(field);
			}

			createLogicalTableAndSubjectMap(tm, tableName, fields_key.isEmpty(), fields_key);

			st.close();
		} catch(Exception e) {
			throw new DirectMappingException("Error querying table: " + tableName, e);
		}
	}

	private void createPredicateObjectMap(Resource tm, String tableName, String field, String type) {
		Resource pom = mapping.createResource();
		mapping.add(tm, R2RML.predicateObjectMap, pom);

		String predicate = baseIRI + IRISafe.toIRISafe(tableName) + "#" + IRISafe.toIRISafe(field);
		mapping.add(pom, R2RML.predicate, mapping.createResource(predicate));

		Resource om = mapping.createResource();
		mapping.add(pom, R2RML.objectMap, om);
		mapping.add(om, R2RML.column, field);

		Resource datatype = DMUtil.getXSDDataTypeFor(type);
		if(datatype != null)
			mapping.add(om, R2RML.datatype, datatype);
	}

	private void createLogicalTableAndSubjectMap(Resource tm, String tableName, boolean hasNoPK, List<String> fieldsPK) {
		/* Creating the logical table. if the table has no primary key, 
		 * the row node is a fresh blank node that is unique to this row. 
		 * Given that no all relational databases support ROWID functions, 
		 * we use a UUID as an alternative.
		 */
		Resource lt = mapping.createResource();
		mapping.add(tm, R2RML.logicalTable, lt);
		if(hasNoPK) {
			mapping.add(lt, R2RML.sqlQuery, "SELECT *, UUID() AS R2RML_ID FROM " + tableName);
		} else {
			mapping.add(lt, R2RML.tableName, tableName); 
		}

		// Creating the subject map
		Resource sm = mapping.createResource();
		mapping.add(tm, R2RML.subjectMap, sm);
		mapping.add(sm, R2RML.clazz, mapping.createResource(baseIRI + IRISafe.toIRISafe(tableName)));

		if(hasNoPK) {
			mapping.add(sm, R2RML.column, "R2RML_ID");
			mapping.add(sm, R2RML.termType, R2RML.BLANKNODE);
		} else {
			String template = baseIRI + IRISafe.toIRISafe(tableName) + "/" ;
			Iterator<String> iter = fieldsPK.iterator();
			while(iter.hasNext()) {
				String field = iter.next();
				template += IRISafe.toIRISafe(field) + "={" + field + "}";
				if(iter.hasNext()) template += ";";
			}
			mapping.add(sm, R2RML.template, template);
		}
	}

	private List<String> getTableNames() throws DirectMappingException {
		List<String> tableNames = new ArrayList<String>();
		try {
			String query = "SHOW TABLES";
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				String tableName = rs.getString(1);
				tableNames.add(tableName);
			}
			st.close();
		} catch(Exception e) {
			throw new DirectMappingException("Error querying database.", e);
		}
		return tableNames;
	}

	private void openDatabaseConnection() throws DirectMappingException {
		try {
			Properties props = new Properties();
			// Connecting to a database
			if(user != null && !"".equals(user))
				props.setProperty("user", user);
			if(password != null && !"".equals(password))
				props.setProperty("password", password);			
			connection = DriverManager.getConnection(connectionURL, props);
		} catch (SQLException e) {
			throw new DirectMappingException("Error connecting to database.", e);
		}
	}

	private void closeDatabaseConnection() throws DirectMappingException {
		try {
			if(!connection.isClosed())
				connection.close();
		} catch (SQLException e) {
			throw new DirectMappingException("Error closing connection with database.", e);
		}
	}

	private void writeToFile() throws DirectMappingException {
		try{
			File file = new File(mappingFile);
			if(!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			Path path = Paths.get(file.getPath());
			BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
			mapping.write(bw, "TURTLE");
			bw.close();
		} catch (IOException e) {
			throw new DirectMappingException("Error writing to file.", e);
		}		
	}

}
