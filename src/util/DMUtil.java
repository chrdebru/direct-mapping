package util;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.XSD;

public class DMUtil {

	public static Resource getXSDDataTypeFor(String type) {
		// https://www.w3.org/TR/2012/REC-r2rml-20120927/#natural-mapping
		// If character string type and/or interval -> literal and (resp) undefined
		// So we keep it at the default of a plain literal
		if("BINARY, BINARY VARYING, BINARY LARGE OBJECT".contains(type))
			return XSD.hexBinary;
		if ("NUMERIC, DECIMAL".contains(type))
			return XSD.decimal;
		if ("SMALLINT, INTEGER, BIGINT".contains(type) || type.contains("INT("))
			return XSD.integer;
		if ("FLOAT, REAL, DOUBLE PRECISION".contains(type))
			return XSD.xdouble;
		if ("BOOLEAN, BOOL".contains(type))
			return XSD.xboolean;
		if ("DATE".contains(type))
			return XSD.date;
		if ("TIME".contains(type))
			return XSD.time;
		if ("TIMESTAMP".contains(type))
			return XSD.dateTime;

		return null;
	}

}
