package edu.rpi.tw.escience.semanteco.annotator;
import static edu.rpi.tw.escience.semanteco.query.Query.VAR_NS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.openrdf.repository.Repository;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;


//import edu.rpi.tw.data.csv.CSV2RDFDemo;
import edu.rpi.tw.data.csv.impl.DefaultEnhancementParameters;
import edu.rpi.tw.data.rdf.utils.pipes.starts.Cat;
//import edu.rpi.tw.data.csv.CSVHeadersForAnnotator;
import edu.rpi.tw.escience.semanteco.Module;
import edu.rpi.tw.escience.semanteco.ModuleConfiguration;
import edu.rpi.tw.escience.semanteco.QueryMethod;
import edu.rpi.tw.escience.semanteco.QueryMethod.HTTP;
import edu.rpi.tw.escience.semanteco.Request;
import edu.rpi.tw.escience.semanteco.SemantEcoUI;
import edu.rpi.tw.escience.semanteco.query.Query;
import edu.rpi.tw.escience.semanteco.query.QueryResource;
import edu.rpi.tw.escience.semanteco.query.Variable;
import edu.rpi.tw.escience.semanteco.query.Query.Type;
import edu.rpi.tw.escience.semanteco.HierarchicalMethod;
import edu.rpi.tw.escience.semanteco.HierarchyEntry;
import edu.rpi.tw.escience.semanteco.HierarchyVerb;

/*
 * treat enhancements atomically
 * 
 * literal:
 * replace conversion:range range with mapping
 * if the mapping is an owl class then it should always be Resource.
 * you can check against a literal list on the server side.
 * 
 * 
 * 
 * for class:
 * range of csvHeader is used to assert range_name string.
 * 
 * if it is 
 * 
 * 
 * 
 CO.csv.e1.params.ttl

1. A column "siteID"
if a class ns1:Site is dragged into the column "siteID",
then  enhancement with ov:csvHeader "siteID" is updated to:
 "conversion:range   rdfs:Resource;",

"conversion:range   X;" where X is whatever datatype is dragged.

rangeName becomes as below. re-use csvHeader string for range_name.

 *we should automatically add namespaces.

conversion:enhance [
         conversion:class_name "Site";
         conversion:subclass_of wgs:SpatialThing;
      ];

      conversion:enhance [
         ov:csvCol          1;
         ov:csvHeader       "AQS Site ID";
         #conversion:label   "AQS Site ID";
         conversion:comment "";
         conversion:range   rdfs:Resource;
         conversion:range_name "Site";
      ];

2. When a property is dragged:
"conversion:equivalent_property wildlife:hasStateProvince;"
 */


public class AnnotatorModule implements Module {

	private ModuleConfiguration config = null;
	private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
	private static final String BINDINGS = "bindings";
	private static final String FAILURE = "{\"success\":false}";
	private static OntModel model = null;
	private PrintWriter csvFileWriter = null;
	private PrintWriter enhancementFileWriter = null;
	private FileOutputStream enhancementFileStream;
	private String dataSetName;
	private String sourceName;
	private String csvFile="/Users/apseyed/Documents/rpi/output.ttl";

	public void setDataSetName(String dataSetName){this.dataSetName = dataSetName;}
	public void setSourceName(String sourceName){this.sourceName = sourceName;}
	public String getDataSetName(){return this.dataSetName;}
	public String getSourceName(){return this.sourceName;}

	public void setModel(OntModel model){
		AnnotatorModule.model = model;
	}
	public OntModel getModel(){
		return AnnotatorModule.model;
	}

	public void initModel() {
		if(model == null) {
			//there is no reason for the annotator to be working off an inferred model at this point
			model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
			FileManager.get().readModel(model, config.getResource("owl-files/oboe-biology-sans-imports.owl").toString()) ;	
			//FileManager.get().readModel(model, config.getResource("owl-files/oboe-characteristics.owl").toString()) ;	
			//FileManager.get().readModel(model, config.getResource("owl-files/oboe-core.owl").toString()) ;		
			//FileManager.get().readModel(model, config.getResource("owl-files/oboe-sbclter.owl").toString()) ;
			/*
					FileManager.get().readModel(model, config.getResource("owl-files/oboe-temporal.owl").toString()) ;
					FileManager.get().readModel(model, config.getResource("owl-files/oboe-spatial.owl").toString()) ;
					FileManager.get().readModel(model, config.getResource("owl-files/oboe-chemistry.owl").toString()) ;
					FileManager.get().readModel(model, config.getResource("owl-files/oboe-taxa.owl").toString()) ;
					FileManager.get().readModel(model, config.getResource("owl-files/oboe-taxa.owl").toString()) ;
					FileManager.get().readModel(model, config.getResource("owl-files/oboe-standards.owl").toString()) ;
			 */
		}
	}
	
	public void mireot(String classUri, String ontologyUri) throws OWLOntologyCreationException{
		//hard code both uris for testing
		classUri = "http://www.co-ode.org/ontologies/pizza/pizza.owl#ArtichokeTopping";
		ontologyUri = "http://www.co-ode.org/ontologies/pizza/pizza.owl";
		//load the ontology from ontologyUri
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		IRI documentIRI = IRI.create(ontologyUri);
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
        OWLClass classForAnno = dataFactory.getOWLClass(IRI.create(classUri));
		//first collect all annotations for a class
        Set<OWLAnnotation> annotations = classForAnno.getAnnotations(ontology);
        System.out.println("annotations: " + annotations.toString());
		//manager.loadOntology(IRI.create("http://"));
		// "importedFrom" annotation property
		
		OWLAnnotationProperty importedFromProperty = dataFactory.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000412"));

		
		
		return;
	}

	/**
	 * Reads the csvFile from params and writes to csvFileWriter to be used for conversion to RDF by csv2rdf4lod
	 * This is currently functional.
	 * @param request
	 * @return
	 * @throws FileNotFoundException 
	 */
	@QueryMethod(method=HTTP.POST)
	public void readCsvFileForInitialConversion(final Request request) throws FileNotFoundException{
		System.out.println(request.getParam("csvFile"));
		String csvFileAsString = (String) request.getParam("csvFile");
		request.getLogger().debug("The file object is of type : " + request.getParam("csvFile").getClass());
		csvFileWriter = new PrintWriter(csvFile);
		csvFileWriter.println(csvFileAsString);
		csvFileWriter.close();
		request.getLogger().debug("CSV file written to : " + csvFileWriter);
		return;
	}


	@QueryMethod
	public String queryForEnhancing(final Request request) throws FileNotFoundException{

		System.out.println(request.getParam("annotationMappings"));
		request.getLogger().debug("annotations: " + request.getParam("annotationMappings"));

		//holder for reading file off a parameter.
		//File f = (File) request.getParam("file");
		//FileInputStream in = new FileInputStream(f);
		//FileManager.get().readModel(model, "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl");

		//1) run the initial conversion from here and get the enhancement file

		//String[] arguments = new String[] {"/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/source/uk-offshore-oil-wells-short.csv"," --header-line '1'"," --delimiter ,"};
		
		//maybe the client can pass into the request object the name of the file
		String[] arguments = new String[] {csvFile," --header-line '1'"," --delimiter ,"};
		
		//String csvFile = "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/source/uk-offshore-oil-wells-short.csv";
        //parameters file for output
		//the name of the params file can be a variation of the csv file name as typical.
		String paramsFile = "/Users/apseyed/Documents/rpi/sample-enhancement.ttl";     
        String eId = "1";
        
        //String paramParams = "-v surrogate=https://github.com/timrdf/csv2rdf4lod-automation/wiki/CSV2RDF4LOD_BASE_URI# -v sourceID=scraperwiki-com " +
        //"-v datasetID=uk-offshore-oil-wells -v cellDelimiter=, -v header= -v dataStart= -v onlyIfCol= -v repeatAboveIfEmptyCol= -v interpretAsNull= " +
        //"-v dataEnd= -v subjectDiscriminator= -v datasetVersion=2011-Jan-24 -v whoami=apseyed -v machine_uri= -v person_uri= -v nowXSD=2013-03-14T23:35:16-04:00";
        
        //String h2p = "/Users/apseyed/Documents/rpi/csv2rdf4lod-automation/bin/util/header2params2.awk";
        String surrogate = "https://github.com/timrdf/csv2rdf4lod-automation/wiki/CSV2RDF4LOD_BASE_URI#";
        String cellDelimiter = ",";
        //get these from the request object
        String username = "user";
        String sourceId = "sourceX";
        String datasetId = "datasetX";
        String machineUri = "machineX";
        String datasetVersion = "2011-Jan-24";
        String conversionID = "1";
        //provide that array as argument and -v options to a new method that uses Jena to generate what header2params2.awk
        //simulateheader2params
        
        /*
        //generate headers
		List<String> headerList = CSVHeadersForAnnotator.getHeaders(arguments);
		//using headers write the parameters file (this method is rewritten awk script)
        generateParmsFileFromHeaders(headerList, paramsFile, surrogate, sourceId, 
    			datasetId, datasetVersion, null, 
    			conversionID, cellDelimiter, null, null, null,
    			null, null, null, username, 
    			machineUri, username);

		//2) get the json object from bbq statement for input to the enhancement work

		//3)do the conversion calling
		//queryForPropertyToEnhance
		//queryForHeaderToEnhance
        //can test the augmenting of the enhancements file here
        
        convertToRdfWithEnhancementsFile(csvFile, paramsFile);     
		*/
		//next you can test augmentation of the params file
		//you need to go back and pass appropriate params for raw vs. enhancement level params file (might need to retrace the logic, describe it, and mimic it.)
		//4) should we send the rdf file back to the client?

		return null;
	}
	
	public static void convertToRdfWithEnhancementsFile(String inFilename, String enhancementParametersURL ) {
		// TODO Auto-generated method stub
		//String inFilename                 = null;
	      int    header                     = 1;
	      int    primaryKeyColumn           = 0;
	      int    uriKeyColumn               = 0;
	      String baseURI                    = null;
	      String datasetIDTag               = null;
	      String conversionTag              = null;
	      String classURI                   = null;
	      String subjectNS                  = null; boolean uuidSubject   = true;
	      String predicateNS                = null; boolean uuidPredicate = true;
	      String objectNS                   = null; boolean uuidObject    = true;
	      String outputFileName             = null;
	      String metaOutputFileName         = null;
	      String outputExtension            = "ttl";
	      Set<String> voidFileExtensions    = null;
	      //String resourceOrLiteralBitString = null; // TODO: Deprecate
	    //  String enhancementParametersURL   = null;
	      String provenanceParametersURL    = null;
	      String converterIdentifier        = null;
	      boolean examplesOnly              = false;
	      int     sampleLimit               = -1;
	      voidFileExtensions = new HashSet<String>(); 
	    //java -Xmx3060m edu.rpi.tw.data.csv.CSVtoRDF 
			//source/uk-offshore-oil-wells-short.csv -sample 10 -ep automatic/uk-offshore-oil-wells-short.csv.raw.params.ttl 
			//-VoIDDumpExtensions ttl.gz -w automatic/uk-offshore-oil-wells-short.csv.raw.sample.ttl 
			//-id csv2rdf4lod_96add9a1c2a9b862527cd8d6e795a606   
		//outputFileName = "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/automatic/uk-offshore-oil-wells-short.csv.raw.sample.ttl";
	      
	    /* shouldn't need these next two as they are passed in */  
		//inFilename = "/Users/apseyed/Desktop/source/p-scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/source/uk-offshore-oil-wells-short.csv";
		
		//outputFileName = "/Users/apseyed/Desktop/source/p-scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/automatic/uk-offshore-oil-wells-short.csv.ttl";
		outputFileName = "/Users/apseyed/Documents/rpi/output.ttl";
		
		//enhancementParametersURL = "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/automatic/uk-offshore-oil-wells-short.csv.raw.params.ttl";
		//enhancementParametersURL = "/Users/apseyed/Desktop/source/p-scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl";
		converterIdentifier = "csv2rdf4lod_96add9a1c2a9b862527cd8d6e795a606";
		
		
		voidFileExtensions.add(".ttl.gz");
		// Load the initial enhancement parameters.
	      Repository enhancementParamsRep = Cat.load(enhancementParametersURL);
	      if( Cat.ENCOUNTERED_PARSE_ERROR ) {
	         System.err.println("ERROR; invalid RDF syntax in " + enhancementParametersURL);
	         System.exit(3);
	      }
	      DefaultEnhancementParameters enhancementParams = new DefaultEnhancementParameters(enhancementParamsRep, baseURI);


	         System.out.println("calling demo");

	         CSV2RDFForAnnotator csv2rdfObject = new CSV2RDFForAnnotator(inFilename,classURI, subjectNS,  uuidSubject,  predicateNS, uuidPredicate, 
                objectNS, uuidObject, enhancementParams, converterIdentifier, enhancementParametersURL,
                voidFileExtensions, examplesOnly, sampleLimit);
		
		 Repository toRDF = csv2rdfObject.toRDF(outputFileName, metaOutputFileName);
	      System.err.println("========== edu.rpi.tw.data.csv.CSVtoRDF complete. ==========");
	      
	      
		/*
		 * public CSVtoRDF(String inFileName,
                   String classURI,                           // This is outdated, but could become the generalization.
                   String subjectNS,   boolean uuidSubject,   // This is outdated, but could become the generalization.
                   String predicateNS, boolean uuidPredicate, // This is outdated, but could become the generalization.
                   String objectNS,    boolean uuidObject,    // This is outdated, but could become the generalization.
                   //deprecated String resourceOrLiteralBitString, 
                   EnhancementParameters enhancementParams, 
                   String converterIdentifier, String enhancementParametersURL,
                   Set<String> voidFileExtensions,
                   boolean examplesOnly, int sampleLimit)
		 */

	}

	/**
	 * @throws FileNotFoundException 
	 * 
	 */
	public void generateParmsFileFromHeaders(List<String> headers, String paramsFile, String surrogate, String sourceId, 
			String datasetId, String datasetVersion, String subjectDiscriminator, 
			String conversionID, String cellDelimiter, String header, String dataStart, String dataEnd,
			String onlyIfCol, String repeatAboveIfEmptyCol, String interpretAsNull, String username, 
			String machine_uri, String person_uri) throws FileNotFoundException{
		//header2params2.sh
		//simulates header2params2.awk
		//at /Users/apseyed/Documents/rpi/csv2rdf4lod-automation/bin/util/header2params2.awk

		//open a file stream
		enhancementFileWriter = new PrintWriter(paramsFile);

		//how do i assert prefix statements in jena?
		String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
		String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
		String todo = "http://www.w3.org/2000/01/rdf-schema#";
		String skos = "http://www.w3.org/2004/02/skos/core#";
		String time = "http://www.w3.org/2006/time#";
		String was = "http://www.w3.org/2003/01/geo/wgs84_pos#";
		String geonames = "http://www.geonames.org/ontology#";
		String geonamesid = "http://sws.geonames.org/";
		//String rdfs = "http://www.w3.org/2000/01/rdf-schema#";	
		//can you wrire curis in jena?	
		if(person_uri != null){
			//printf("<%s> foaf:holdsAccount <%s#%s> .\n",person_uri,   machine_uri,whoami);
			//create an rdf statement 
			//skipping this for now as its not crucial for conversion
		}
		enhancementFileWriter.println("@prefix owl:           <http://www.w3.org/2002/07/owl#> .");
		enhancementFileWriter.println("@prefix vann:          <http://purl.org/vocab/vann/> .");
		enhancementFileWriter.println("@prefix skos:          <http://www.w3.org/2004/02/skos/core#> .");
		enhancementFileWriter.println("@prefix time:          <http://www.w3.org/2006/time#> .");
		enhancementFileWriter.println("@prefix wgs:           <http://www.w3.org/2003/01/geo/wgs84_pos#> .");
		enhancementFileWriter.println("@prefix geonames:      <http://www.geonames.org/ontology#> .");
		enhancementFileWriter.println("@prefix geonamesid:    <http://sws.geonames.org/> .");
		enhancementFileWriter.println("@prefix govtrackusgov: <http://www.rdfabout.com/rdf/usgov/geo/us/> .");
		enhancementFileWriter.println("@prefix dbpedia:       <http://dbpedia.org/resource/> .");
		enhancementFileWriter.println("@prefix dbpediaprop:   <http://dbpedia.org/property/> .");
		enhancementFileWriter.println("@prefix dbpediaowl:    <http://dbpedia.org/ontology/> .");
		enhancementFileWriter.println("@prefix con:           <http://www.w3.org/2000/10/swap/pim/contact#> .");
		enhancementFileWriter.println( "@prefix muo:           <http://purl.oclc.org/NET/muo/muo#> .");
		enhancementFileWriter.println( "@prefix vs:            <http://www.w3.org/2003/06/sw-vocab-status/ns#> .");
		enhancementFileWriter.println( "@prefix frbr:          <http://purl.org/vocab/frbr/core#> .");
		enhancementFileWriter.println( "@prefix bibo:          <http://purl.org/ontology/bibo/> .");
		enhancementFileWriter.println("@prefix doap:          <http://usefulinc.com/ns/doap#> .");
		enhancementFileWriter.println("@prefix qb:            <http://purl.org/linked-data/cube#> .");
		enhancementFileWriter.println("@prefix dgtwc:         <http://data-gov.tw.rpi.edu/2009/data-gov-twc.rdf#> .");
		enhancementFileWriter.println("@prefix conversion:    <http://purl.org/twc/vocab/conversion/> .");
		enhancementFileWriter.println("@prefix void:          <http://rdfs.org/ns/void#> .");
		enhancementFileWriter.println("@prefix xsd:          <http://www.w3.org/2001/XMLSchema#> .");
		enhancementFileWriter.println("@prefix dcterms:         <http://purl.org/dc/terms/> .");
		enhancementFileWriter.println("@prefix foaf:         <http://xmlns.com/foaf/0.1/> .");
		enhancementFileWriter.println("@prefix ov:         <http://open.vocab.org/terms/> .");
		enhancementFileWriter.println("@prefix todo:         <http://www.w3.org/2000/01/rdf-schema#> .");
		enhancementFileWriter.println("@prefix rdf:         <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		enhancementFileWriter.println("@prefix rdfs:         <http://www.w3.org/2000/01/rdf-schema#> .");


		enhancementFileWriter.println();
		enhancementFileWriter.println();

		surrogate = "https://github.com/timrdf/csv2rdf4lod-automation/wiki/CSV2RDF4LOD_BASE_URI#";
		String STEP =  "enhancement/1";
		if(conversionID != null){
			String dataset = "<" + surrogate + "/source/" + sourceId + "/dataset/" + datasetId + "/version/" + datasetVersion + "/conversion/" + STEP + ">"  ;
			enhancementFileWriter.println(dataset);
			enhancementFileWriter.println("  a conversion:LayerDataset, void:Dataset;");
			enhancementFileWriter.println();
			enhancementFileWriter.println("conversion:base_uri           " + "\"" + surrogate + "\"^^xsd:anyURI;");
			enhancementFileWriter.println("conversion:source_identifier \"" + sourceId + "\";");
			enhancementFileWriter.println("conversion:dataset_identifier \"" + datasetId + "\";");
			enhancementFileWriter.println("conversion:version_identifier \"" + datasetVersion + "\";");
			enhancementFileWriter.println("conversion:enhancement_identifier \"" + conversionID + "\";");
			enhancementFileWriter.println();
			enhancementFileWriter.println("conversion:conversion_process [");
			enhancementFileWriter.println("   a conversion:EnhancementConversionProcess;");
			enhancementFileWriter.println("   conversion:enhancement_identifier \"" + conversionID + "\";");
			enhancementFileWriter.println();
			enhancementFileWriter.println("dcterms:creator [ a foaf:OnlineAccount; foaf:accountName " + "\"" + username + "\" ];");
			//     dcterms:created "2013-03-15T01:07:39-04:00"^^xsd:dateTime;
			enhancementFileWriter.println("conversion:delimits_cell \",\";");
			//loop on columns
			int columnNumber = 1;
			for(String header1 : headers ){
				System.out.println("header: " + header1);
				enhancementFileWriter.println("     conversion:enhance [ ");
				enhancementFileWriter.println("       ov:csvCol     " + columnNumber + ";");
				enhancementFileWriter.println("       ov:csvHeader     \"" + header1 + "\" ;");
				enhancementFileWriter.println("       #conversion:label \"" + header1 + "\" ;");


				enhancementFileWriter.println("       conversion:comment     " + columnNumber + ";");
				enhancementFileWriter.println("       conversion:range     todo:Literal ; ");
				enhancementFileWriter.println("    ];");
				columnNumber++;
			}

		}
		enhancementFileWriter.println("];");
		enhancementFileWriter.println(".");
		enhancementFileWriter.close();
	}

	/**
	 * need to create new enhancement
	 * @param request
	 * @return
	 * @throws FileNotFoundException 
	 */
	@QueryMethod
	public String queryForPropertyToEnhance(final Request request) throws FileNotFoundException{
		Model model = ModelFactory.createDefaultModel();
		String conversionPrefix = "http://purl.org/twc/vocab/conversion/";
		String enhancementFile2 = "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params2.ttl";
		FileManager.get().readModel(model, "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl");
		FileOutputStream enhancementFileStream2 = new FileOutputStream(enhancementFile2);
		Literal literalHeader = model.createLiteral("Deviated_Well");
		Resource propertyhasSpatialLocation = model.createResource("hasSpatialLocation");


		Property propertyEquiv = model.createProperty(conversionPrefix + "equivalent_property");
		StmtIterator enhanceStatements1 =  model.listStatements((Resource) null, (Property) null , (Literal) literalHeader );
		Statement s = null;
		while (enhanceStatements1.hasNext()) {
			s = enhanceStatements1.next();
			System.out.println("statement is : " + s);	
		}
		Resource subjectOfHeader = s.getSubject();
		Statement equivStatement = ResourceFactory.createStatement(subjectOfHeader, propertyEquiv, propertyhasSpatialLocation);
		model.add(equivStatement);
		model.write(enhancementFileStream2, "N-TRIPLE");



		return null;	
	}

	@QueryMethod
	public String queryForHeaderToEnhance(final Request request) throws FileNotFoundException{
		Model model = ModelFactory.createDefaultModel();
		//Model newModel = ModelFactory.createDefaultModel();
		//String enhancementFile = "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl";
		String enhancementFile2 = "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params2.ttl";
		//FileOutputStream enhancementFileStream = new FileOutputStream(enhancementFile);
		FileOutputStream enhancementFileStream2 = new FileOutputStream(enhancementFile2);
		FileManager.get().readModel(model, "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl");
		String conversionPrefix = "http://purl.org/twc/vocab/conversion/";

		String ov = "http://open.vocab.org/terms/";
		//this should be retrieved
		//how? list statements . null,property,voiddataset
		Property propertyEnhance = model.createProperty(conversionPrefix + "enhance");
		Property propertyRange = model.createProperty(conversionPrefix + "range");
		Property propertyRangeName = model.createProperty(conversionPrefix + "range_name");
		Property propertyClassName = model.createProperty(conversionPrefix + "class_name");
		Property propertySubclassOf = model.createProperty(conversionPrefix + "subclass_of");
		Property propertyCsvHeader = model.createProperty(ov + "csvHeader");


		Literal literalHeader = model.createLiteral("Deviated_Well");
		Resource superClass = model.createResource("SpatialLocation");


		StmtIterator enhanceStatements1 =  model.listStatements((Resource) null, (Property) propertyCsvHeader , (Literal) literalHeader );
		Statement s = null;
		while (enhanceStatements1.hasNext()) {
			s = enhanceStatements1.next();
			System.out.println("statement is : " + s);	
		}

		Resource subjectOfHeader = s.getSubject();

		//this is an object of what triple and what is the subject? that subject becomes subject of new triple with anonymous node enhancement.
		StmtIterator getStatement =  model.listStatements((Resource) null, (Property) propertyEnhance , (Resource) subjectOfHeader );
		Statement s2 = null;
		while (getStatement.hasNext()) {
			s2 = getStatement.next();
			System.out.println("other statement is : " + s2);	
		}
		Resource conversionProcess = s2.getSubject();
		//triple with anonymous node.
		//Node anonNode = Node.createAnon();
		Resource newAnon = model.createResource();
		Statement conversionProcessEnhanceAnon = ResourceFactory.createStatement(conversionProcess, propertyEnhance, newAnon);
		System.out.println("anon node output is : " + conversionProcessEnhanceAnon);
		model.add(conversionProcessEnhanceAnon);
		//now just add triples from anon

		/*
		 * conversion:enhance [
         conversion:class_name "Site";
         conversion:subclass_of wgs:SpatialThing;
      ];
		 */

		Statement classNameStatement = ResourceFactory.createStatement(newAnon, propertyClassName, literalHeader);
		Statement subclassStatement = ResourceFactory.createStatement(newAnon, propertySubclassOf, superClass);
		model.add(classNameStatement);
		model.add(subclassStatement);



		//find the statement that mentions "Deviated_Well" in the ov:csvHeader

		//query for statement with header in the range.

		Statement st = null;
		StmtIterator enhanceStatements =  model.listStatements((Resource) subjectOfHeader, (Property) propertyRange, (Literal) null);
		while (enhanceStatements.hasNext()) {
			st = enhanceStatements.next();
			System.out.println("statement is : " + st);	
		}  

		Statement replacement = ResourceFactory.createStatement(st.getSubject(), st.getPredicate(), model.createResource("http://www.w3.org/2000/01/rdf-schema#" +
				"Resource"));

		Statement newst = ResourceFactory.createStatement(st.getSubject(), propertyRangeName, literalHeader);


		model.remove(st);
		//st.changeObject(model.createResource("Resource"));
		model.add(replacement);
		model.add(newst);


		model.write(enhancementFileStream2, "N-TRIPLE");
		return null;


		//return null;

	}
	//can you just change the statement in the non-iterated model?

	@QueryMethod
	public String writeEnhancementForRangeTester(final Request request) throws FileNotFoundException{

		String type = "xsd:double";
		//you need to know what the property is.

		//		model.createLiteral(arg0)
		//can i test if a resource is a datatype?
		//we cannot assign the datatype until the dataproperty is also assigned.

		String hasProperty = "oboe:hasMeasurement";

		String header = "Deviated_Well";
		String rangeClass = "hasWell";


		FileOutputStream newEnhancementFile = new FileOutputStream("/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl.new");

		//writeEnhancementForRangeTesterModel(request, header, rangeClass);		
		Model newModel = writeEnhancementForRange(header, rangeClass);

		//write model
		newModel.write(newEnhancementFile, "N-TRIPLE");

		//what patterns do we supply when the symbol refers to a code?
		//A: just that its an instance of code, or also that its "about" country Y?


		//"Derviated_Well":oboe:DeviatedWell.
		//test: replace "conversion:range todo:Literal;" with "conversion:range rdf:Resource"
		return request.toString();	
	}

	/*
public String writeEnhancementForRangeTesterModel(Request request, String header, String rangeClass) throws FileNotFoundException{


	Model model = ModelFactory.createDefaultModel();
	FileOutputStream newEnhancementFile = new FileOutputStream("/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl.new");
	FileManager.get().readModel(model, "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl");
	Model newModel = ModelFactory.createDefaultModel();

	//a method: model, type of args, returns a model.
	//should modify the model not create a new one. for testing create a new file to write the model to.
	newModel = writeEnhancementForRange(model, newModel, header, rangeClass);

	//write model
	newModel.write(newEnhancementFile, "N-TRIPLE");


		return null;	
	}
	 */

	public Model writeEnhancementForRange(String header, String rangeClass){
		Model model = ModelFactory.createDefaultModel();
		Model newModel = ModelFactory.createDefaultModel();

		FileManager.get().readModel(model, "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl");

		String conversionPrefix = "http://purl.org/twc/vocab/conversion/";
		//this should be retrieved
		//how? list statements . null,property,voiddataset
		String dataset = "<https://github.com/timrdf/csv2rdf4lod-automation/wiki/CSV2RDF4LOD_BASE_URI#/source/scraperwiki-com/dataset/uk-offshore-oil-wells/version/2011-Jan-24/conversion/enhancement/1>";
		Resource subjectDataSet = model.createResource(dataset);
		Property propertyConversionProcess = model.createProperty(conversionPrefix + "conversion_process");
		Property propertyEnhance = model.createProperty(conversionPrefix + "enhance");
		Property propertyRange = model.createProperty(conversionPrefix + "range");
		Property propertyRangeName = model.createProperty(conversionPrefix + "range");
		StmtIterator iter = model.listStatements();
		// print out the predicate, subject and object of each statement
		while (iter.hasNext()) {
			Statement stmt      = iter.nextStatement();  // get next statement
			Resource  subject   = stmt.getSubject();     // get the subject
			Property  predicate = stmt.getPredicate();   // get the predicate
			RDFNode   object    = stmt.getObject();      // get the object
			String predString = predicate.toString();
			//if statement with property 'propertyConversionProcess'
			if(predicate.toString().trim().equals(propertyConversionProcess.toString().trim())){
				System.out.println("\nmatched: propertyConversionProcess!\n");
				if (object instanceof Resource) {
					//find triples with 'propertyEnhance'
					NodeIterator enhancements = model.listObjectsOfProperty((Resource) object, propertyEnhance);
					//iterate thru all enhancement blocks
					while(enhancements.hasNext()){
						System.out.println("\nmatched: propertyEnhance!\n");
						RDFNode node = enhancements.nextNode();
						System.out.println("node: " + ((Object) node).toString());			
						StmtIterator enhanceStatements =  model.listStatements((Resource) node, (Property) null, (Resource) null);
						// NodeIterator enhancementParameters = model.listObjectsOfProperty((Resource) node, enhance);	    
						while(enhanceStatements.hasNext()){
							//match and update range
							System.out.println("\n\ngot inside enhance!\n");
							Statement enhanceStatement = enhanceStatements.nextStatement();
							subject   = enhanceStatement.getSubject();     // get the subject
							predicate = enhanceStatement.getPredicate();   // get the predicate
							object    = enhanceStatement.getObject();      // get the object
							if (object instanceof Resource) {
								System.out.print(object.toString());
							} else {
								System.out.print(" \"" + object.toString() + "\"");
							}
							//here we can check properties for rewriting
							//match for range.	    
							if(predicate.toString().trim().equals(propertyRange.toString().trim())){
								Statement s = ResourceFactory.createStatement(subject,propertyRange, model.createResource("rdf:Resource"));
								newModel.add(s);
								return null;
								//after the iterator is done make change
								//enhanceStatement.changeObject(model.createResource("rdf:Resource"));
								//change the object of the statement (S, P, X) to (S, P, o).
							}
						}			    
					}	    
					//Statement s = ResourceFactory.createStatement(subject,propertyRangeName , model.createResource("test"));
					//model.add(s); // add the statement (triple) to the model
					//here new code
				}		        
			}
		}	    
		return newModel;
	}



	@QueryMethod
	public String writeEnhancement(final Request request){
		return request.toString();

	}


	@QueryMethod
	public String applyEnhancement(final Request request) throws FileNotFoundException{
		//


		return null;
	}


	@QueryMethod
	public String writeToEnhancement(final Request request) throws FileNotFoundException{
		//read in the csv file.
		//read enhancement into rdf model
		//find the triples with appropriate properies and rewrite it
		//how did you handle rewrite in your snomed stuff?

		//if you are modifying an enhancement you just need to assert triples on the bnode for that enhancement
		//if you are creating a new enhancement after asserting triple, you need to assert a triple with the 
		//conversion process.
		//how do i construct and assert a new triple in jena?

		//// add the property
		// johnSmith.addProperty(VCARD.FN, fullName);

		//add statements to a model
		//model.add(Statement s)
		//remove(Statement s)
		//Removes a statement.

		//Statement s = ResourceFactory.createStatement(subject, predicate, object);
		//model.add(s); // add the statement (triple) to the model

		//Statement.changeObject(String o)
		//change the object of the statement (S, P, X) to (S, P, o).

		Model model = ModelFactory.createDefaultModel();
		Model newModel = ModelFactory.createDefaultModel();

		//Node anonNode = Node.createAnon();
		//anonNode.


		//Model model = null;
		//model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);


		//load certain ontologies
		//model.read("http://was.tw.rpi.edu/semanteco/air/air.owl", "TTL");
		FileOutputStream newEnhancementFile =new FileOutputStream("/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl.new");
		FileManager.get().readModel(model, "/Users/apseyed/Desktop/source/scraperwiki-com/uk-offshore-oil-wells/version/2011-Jan-24/manual/uk-offshore-oil-wells-short.csv.e1.params.ttl") ;
		String conversionPrefix = "http://purl.org/twc/vocab/conversion/";
		String dataset = "<https://github.com/timrdf/csv2rdf4lod-automation/wiki/CSV2RDF4LOD_BASE_URI#/source/scraperwiki-com/dataset/uk-offshore-oil-wells/version/2011-Jan-24/conversion/enhancement/1>";
		Resource subjectDataSet = model.createResource(dataset);
		Property propertyConversionProcess = model.createProperty(conversionPrefix + "conversion_process");
		Property propertyEnhance = model.createProperty(conversionPrefix + "enhance");
		Property propertyRange = model.createProperty(conversionPrefix + "range");
		Property propertyRangeName = model.createProperty(conversionPrefix + "range");


		StmtIterator iter = model.listStatements();
		// print out the predicate, subject and object of each statement
		while (iter.hasNext()) {
			Statement stmt      = iter.nextStatement();  // get next statement
			Resource  subject   = stmt.getSubject();     // get the subject
			Property  predicate = stmt.getPredicate();   // get the predicate
			RDFNode   object    = stmt.getObject();      // get the object


			System.out.print(subject.toString());
			System.out.print(" " + predicate.toString() + " ");
			if (object instanceof Resource) {
				System.out.print(object.toString());
			} else {
				// object is a literal
				System.out.print(" \"" + object.toString() + "\"");
			}

			System.out.println(" .");
			String predString = predicate.toString();
			System.out.println("comparing " + predString + " with :  " + propertyConversionProcess.toString());
			//if statement with property 'propertyConversionProcess'
			if(predicate.toString().trim().equals(propertyConversionProcess.toString().trim())){
				System.out.println("\nmatched: propertyConversionProcess!\n");
				System.out.println("object string is!!!: " + " \"" + object.toString() + "\"");

				if (object instanceof Resource) {
					//fine triples with 'propertyEnhance'
					NodeIterator enhancements = model.listObjectsOfProperty((Resource) object, propertyEnhance);
					while(enhancements.hasNext()){
						System.out.println("\nmatched: propertyEnhance!\n");
						RDFNode node = enhancements.nextNode();
						System.out.println("node: " + ((Object) node).toString());		

						StmtIterator enhanceStatements =  model.listStatements((Resource) node, (Property) null, (Resource) null);
						// NodeIterator enhancementParameters = model.listObjectsOfProperty((Resource) node, enhance);


						while(enhanceStatements.hasNext()){
							System.out.println("\n\ngot inside enhance!!!\n");
							Statement enhanceStatement = enhanceStatements.nextStatement();
							//System.out.println("object string is!!!: " + " \"" + object.toString() + "\"");

							subject   = enhanceStatement.getSubject();     // get the subject
							predicate = enhanceStatement.getPredicate();   // get the predicate
							object    = enhanceStatement.getObject();      // get the object
							System.out.print("enhancing statement is: ");
							System.out.print(subject.toString());
							System.out.print(" " + predicate.toString() + " ");
							if (object instanceof Resource) {
								System.out.print(object.toString());
							} else {
								// object is a literal
								System.out.print(" \"" + object.toString() + "\"");
							}
							//here we can check properties for rewriting
							//match for range.
						}

						Statement s = ResourceFactory.createStatement(subject,propertyRangeName , model.createResource("test"));
						newModel.add(s); // add the statement (triple) to the model
						//here new code
					}		        
				}
				//return null;
			}else{
				//a triple without conversion process
				//write stmt to a new model
			}	    
		}	
		/*
		System.out.println("************Only triples with predicate conversion property!!!");
		StmtIterator triplesWithConversionProcess = subjectDataSet.listProperties(propertyConversionProcess);
		//model.listObjectsOfProperty(edward, siblingOf);
		while(triplesWithConversionProcess.hasNext()){
			System.out.println("got inside triplesWithConversionProcess!!!");
			Statement stmt      = triplesWithConversionProcess.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
		    System.out.println("sub: " + subject + " pred: " + predicate + " object: " + object);			
		}
		 */	

		newModel.write(newEnhancementFile, "N-TRIPLE");

		return null;		
	}



	public String getShortName(String inName)
	{
		int pAt = inName.indexOf("#");
		return (inName.substring(pAt+1));
	}

	public String jsonWrapper(Hashtable<String, String> table, String parent) throws JSONException{
		JSONArray data = new JSONArray();
		JSONObject response = new JSONObject();
		response.put("success", true);
		response.put("data", data);
		String str;
		Set<String> set = table.keySet();
		Iterator<String> itr = set.iterator();
		while (itr.hasNext()) {
			str = itr.next();
			System.out.println(str + ": " + table.get(str));

			JSONObject mapping = new JSONObject();
			mapping.put("id", str);
			//should use "short name" if there is no label.

			if(table.get(str) == ""){
				table.put(str, getShortName(str));
			}

			mapping.put("label", table.get(str));
			mapping.put("parent", parent);
			data.put(mapping);
		}
		return response.toString();
	}

	//would it be better to have one model and reasoner for this module, instead of per query method? yes.
	//do that through a constructor?
	@QueryMethod
	public String queryForAnnotatorRootClasses(final Request request) throws JSONException{


		//construct an owlontology and pose sparql queries against it.
		OntModel model = null;
		model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);


		//load certain ontologies
		//model.read("http://was.tw.rpi.edu/semanteco/air/air.owl", "TTL");
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-sbclter.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-temporal.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-spatial.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-biology.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-chemistry.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-anatomy.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-characteristics.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-taxa.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-standards.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-core.owl") ;
		setModel(model);
		//model.
		//InputStream is = new BufferedInputStream(new FileInputStream("blah.turtle"));


		//apply sparql queries against it
		//final Query query = config.getQueryFactory().newQuery(Type.CONSTRUCT);
		//final GraphComponentCollection construct = query.getConstructComponent();
		final Query query = config.getQueryFactory().newQuery(Type.SELECT);
		final Variable id = query.getVariable(VAR_NS+ "child");
		final Variable label = query.getVariable(VAR_NS+ "label");
		final Variable parent = query.getVariable(VAR_NS+ "parent");		

		final QueryResource PollutedThing = query.getResource("http://escience.rpi.edu/ontology/semanteco/2/0/pollution.owl#PollutedThing");
		final QueryResource Measurement = query.getResource("http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Measurement");
		final QueryResource Thing = query.getResource("http://www.w3.org/2002/07/owl#Thing");
		//http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Entity
		final QueryResource Entity = query.getResource("http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Entity");

		final QueryResource subClassOf = query.getResource(RDFS_NS+"subClassOf");
		final Variable site = query.getVariable(VAR_NS+"site");
		final QueryResource hasLabel = query.getResource(RDFS_NS + "label");

		Set<Variable> vars = new LinkedHashSet<Variable>();
		vars.add(id);
		vars.add(parent);
		vars.add(label);

		query.setVariables(vars);
		//query.addPattern(site, subClassOf, PollutedThing);
		//query.addPattern(site, subClassOf, Measurement);
		query.addPattern(id, subClassOf, parent);
		query.addPattern(id, subClassOf, Entity);
		query.addPattern(id, hasLabel, label);

		//construct.addPattern(site, subClassOf, PollutedThing);

		//return executeLocalQuery(query, model);
		String responseStr = FAILURE;
		//String resultStr = config.getQueryExecutor(request).accept("application/json").executeLocalQuery(query, model);

		Set master = new HashSet();		//model.
		Set<OntClass> classes = new HashSet<OntClass>();		//model.
		Set<String> labels = new HashSet<String>();		//model.

		OntClass thing = model.getOntClass( OWL.Thing.getURI() );
		//OntClass entity = model.getOntClass( "http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Entity" );

		Hashtable<String, String> table = new Hashtable<String, String>();

		for (Iterator<OntClass> i = thing.listSubClasses(true); i.hasNext(); ) { //true here is for direct
			OntClass hierarchyRoot = i.next();

			//classes.add( hierarchyRoot);
			//labels.add( hierarchyRoot.getLabel(null));
			System.out.println("root: " + hierarchyRoot.toString());
			System.out.println("label: " + hierarchyRoot.getLabel(null));

			if(hierarchyRoot.getLabel(null) == "" || hierarchyRoot.getLabel(null) == null){
				table.put(hierarchyRoot.toString(), getShortName(hierarchyRoot.toString()));
			}
			else{
				table.put(hierarchyRoot.toString(), hierarchyRoot.getLabel(null));
			}

		}	



		/*
		 * 
		for (Iterator<OntClass> i = model.listHierarchyRootClasses(); i.hasNext(); ) {
		    OntClass hierarchyRoot = i.next();
		    classes.add( hierarchyRoot);
		    labels.add( hierarchyRoot.getLabel(null));
		}
		 */
		master.add(classes);
		master.add(labels);

		return jsonWrapper(table, OWL.Thing.getURI().toString());

		//return master.toString();
		//return config.getQueryExecutor(request).accept("application/json").executeLocalQuery(query, model);

		/*	if(resultStr == null) {
			return responseStr;
		}
		try {
			JSONObject results = new JSONObject(resultStr);
			JSONObject response = new JSONObject();
			JSONArray data = new JSONArray();
			response.put("success", true);
			response.put("data", data);
			String superclassId = null;
			results = results.getJSONObject("results");
			JSONArray bindings = results.getJSONArray(BINDINGS);
			for(int i=0;i<bindings.length();i++) {
				JSONObject binding = bindings.getJSONObject(i);
				String subclassId = binding.getJSONObject("child").getString("value");
				String subclassLabel = binding.getJSONObject("label").getString("value");

				try {
					superclassId = binding.getJSONObject("parent").getString("value");
				}
				catch(Exception e) { }
				//if(labelStr == null) {
				//	labelStr = sourceUri.substring(sourceUri.lastIndexOf('/')+1).replace('-', '.');
				//}
				JSONObject mapping = new JSONObject();
				mapping.put("id", subclassId);
				mapping.put("label", subclassLabel);
				mapping.put("parent", superclassId);
				data.put(mapping);
			}
			responseStr = response.toString();
		} catch (JSONException e) {
			//log.error("Unable to parse JSON results", e);
		}
		return responseStr;		
		 */
	}

	@QueryMethod
	public String queryForAnnotatorSubClasses(final Request request) throws JSONException{

		String classRequiresSubclassesString = (String) request.getParam("SubClass");	
		if(classRequiresSubclassesString == null){
			return null;
		}

		OntModel model = null;
		model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);


		//load certain ontologies
		//model.read("http://was.tw.rpi.edu/semanteco/air/air.owl", "TTL");
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-sbclter.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-temporal.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-spatial.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-biology.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-chemistry.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-anatomy.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-characteristics.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-taxa.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-standards.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-core.owl") ;
		//construct an owlontology and pose sparql queries against it.
		///	OntModel model = getModel();


		//apply sparql queries against it
		//final Query query = config.getQueryFactory().newQuery(Type.CONSTRUCT);
		//final GraphComponentCollection construct = query.getConstructComponent();
		final Query query = config.getQueryFactory().newQuery(Type.SELECT);
		final Variable id = query.getVariable(VAR_NS+ "child");
		final Variable label = query.getVariable(VAR_NS+ "label");
		final Variable parent = query.getVariable(VAR_NS+ "parent");		

		final QueryResource PollutedThing = query.getResource("http://escience.rpi.edu/ontology/semanteco/2/0/pollution.owl#PollutedThing");
		final QueryResource Measurement = query.getResource("http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Measurement");
		final QueryResource Thing = query.getResource("http://www.w3.org/2002/07/owl#Thing");
		//http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Entity
		final QueryResource Entity = query.getResource("http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Entity");

		final QueryResource subClassOf = query.getResource(RDFS_NS+"subClassOf");
		final Variable site = query.getVariable(VAR_NS+"site");
		final QueryResource hasLabel = query.getResource(RDFS_NS + "label");

		Set<Variable> vars = new LinkedHashSet<Variable>();
		vars.add(id);
		vars.add(parent);
		vars.add(label);

		query.setVariables(vars);
		//query.addPattern(site, subClassOf, PollutedThing);
		//query.addPattern(site, subClassOf, Measurement);
		query.addPattern(id, subClassOf, parent);
		query.addPattern(id, subClassOf, Entity);
		query.addPattern(id, hasLabel, label);

		//construct.addPattern(site, subClassOf, PollutedThing);

		//return executeLocalQuery(query, model);
		String responseStr = FAILURE;
		//String resultStr = config.getQueryExecutor(request).accept("application/json").executeLocalQuery(query, model);

		Set master = new HashSet();		//model.
		Set<OntClass> classes = new HashSet<OntClass>();		//model.
		Set<String> labels = new HashSet<String>();		//model.

		//OntClass thing = model.getOntClass( OWL.Thing.getURI() );
		//OntClass subclass = model.getOntClass( classRequiresSubclassesString );

		OntClass subclass = model.getOntClass( classRequiresSubclassesString );

		Hashtable<String, String> table = new Hashtable<String, String>();

		for (Iterator<OntClass> i = subclass.listSubClasses(true); i.hasNext(); ) { //true here is for direct
			OntClass hierarchyRoot = i.next();


			if(hierarchyRoot.getLabel(null) == "" || hierarchyRoot.getLabel(null) == null){
				table.put(hierarchyRoot.toString(), getShortName(hierarchyRoot.toString()));
			}
			else{
				table.put(hierarchyRoot.toString(), hierarchyRoot.getLabel(null));
			}
			//classes.add( hierarchyRoot);
			//labels.add( hierarchyRoot.getLabel(null));

		}	



		/*
		 * 
		for (Iterator<OntClass> i = model.listHierarchyRootClasses(); i.hasNext(); ) {
		    OntClass hierarchyRoot = i.next();
		    classes.add( hierarchyRoot);
		    labels.add( hierarchyRoot.getLabel(null));
		}
		 */
		master.add(classes);
		master.add(labels);

		return jsonWrapper(table, classRequiresSubclassesString);

		//return master.toString();
		//return config.getQueryExecutor(request).accept("application/json").executeLocalQuery(query, model);

		/*	if(resultStr == null) {
			return responseStr;
		}
		try {
			JSONObject results = new JSONObject(resultStr);
			JSONObject response = new JSONObject();
			JSONArray data = new JSONArray();
			response.put("success", true);
			response.put("data", data);
			String superclassId = null;
			results = results.getJSONObject("results");
			JSONArray bindings = results.getJSONArray(BINDINGS);
			for(int i=0;i<bindings.length();i++) {
				JSONObject binding = bindings.getJSONObject(i);
				String subclassId = binding.getJSONObject("child").getString("value");
				String subclassLabel = binding.getJSONObject("label").getString("value");

				try {
					superclassId = binding.getJSONObject("parent").getString("value");
				}
				catch(Exception e) { }
				//if(labelStr == null) {
				//	labelStr = sourceUri.substring(sourceUri.lastIndexOf('/')+1).replace('-', '.');
				//}
				JSONObject mapping = new JSONObject();
				mapping.put("id", subclassId);
				mapping.put("label", subclassLabel);
				mapping.put("parent", superclassId);
				data.put(mapping);
			}
			responseStr = response.toString();
		} catch (JSONException e) {
			//log.error("Unable to parse JSON results", e);
		}
		return responseStr;		
		 */
	}


	@QueryMethod
	public String queryForAnnotatorRootObjectProperties(Request request) throws JSONException{
		//construct an owlontology and pose sparql queries against it.
		OntModel model = null;
		model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);



		//load certain ontologies
		//model.read("http://was.tw.rpi.edu/semanteco/air/air.owl", "TTL");
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-sbclter.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-temporal.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-spatial.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-biology.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-chemistry.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-anatomy.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-characteristics.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-taxa.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-standards.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-core.owl") ;
		//InputStream is = new BufferedInputStream(new FileInputStream("blah.turtle"));


		//apply sparql queries against it
		//final Query query = config.getQueryFactory().newQuery(Type.CONSTRUCT);
		//final GraphComponentCollection construct = query.getConstructComponent();
		final Query query = config.getQueryFactory().newQuery(Type.SELECT);


		final QueryResource topObjectProperty = query.getResource("http://www.w3.org/2002/07/owl#topObjectProperty");
		final QueryResource subPropertyOf = query.getResource(RDFS_NS+"subPropertyOf");
		final Variable site = query.getVariable(VAR_NS+"site");
		Set<Variable> vars = new LinkedHashSet<Variable>();
		vars.add(site);
		query.setVariables(vars);
		//query.addPattern(site, subClassOf, PollutedThing);
		//query.addPattern(site, subClassOf, Measurement);
		query.addPattern(site, subPropertyOf, topObjectProperty);

		//construct.addPattern(site, subClassOf, PollutedThing);

		//return executeLocalQuery(query, model);

		Set master = new HashSet();		//model.
		Set<OntProperty> props = new HashSet<OntProperty>();		//model.
		Set<String> labels = new HashSet<String>();		//model.

		//OntClass thing = model.getOntClass( OWL.Thing.getURI() );
		//OntClass entity = model.getOntClass( "http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Entity" );
		OntProperty topObjProp = model.getOntProperty("http://www.w3.org/2002/07/owl#topObjectProperty");

		Hashtable<String, String> table = new Hashtable<String, String>();

		for (Iterator<? extends OntProperty> i = topObjProp.listSubProperties(true); i.hasNext(); ) { //true here is for direct
			OntProperty hierarchyRoot = i.next();

			// props.add( hierarchyRoot);
			//labels.add( hierarchyRoot.getLabel(null));
			if(hierarchyRoot.getLabel(null) == "" || hierarchyRoot.getLabel(null) == null){
				table.put(hierarchyRoot.toString(), getShortName(hierarchyRoot.toString()));
			}
			else{
				table.put(hierarchyRoot.toString(), hierarchyRoot.getLabel(null));
			}

		}	



		/*
		 * 
				for (Iterator<OntClass> i = model.listHierarchyRootClasses(); i.hasNext(); ) {
				    OntClass hierarchyRoot = i.next();
				    classes.add( hierarchyRoot);
				    labels.add( hierarchyRoot.getLabel(null));
				}
		 */
		master.add(props);
		master.add(labels);
		return jsonWrapper(table, topObjProp.toString());
		//return master.toString();



		//return config.getQueryExecutor(request).accept("application/json").executeLocalQuery(query, model);
	}


	@QueryMethod
	public String queryForAnnotatorSubObjectProperties(Request request) throws JSONException{

		String classRequiresSubpropertyString = (String) request.getParam("SubProperty");	
		if(classRequiresSubpropertyString == null){
			return null;
		}
		//construct an owlontology and pose sparql queries against it.
		OntModel model = null;
		model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);


		//load certain ontologies
		//model.read("http://was.tw.rpi.edu/semanteco/air/air.owl", "TTL");
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-sbclter.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-temporal.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-spatial.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-biology.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-chemistry.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-anatomy.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-characteristics.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-taxa.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-standards.owl") ;
		FileManager.get().readModel(model, "/Users/apseyed/Documents/rpi/semanteco-products/obo-e-ontologies/oboe-core.owl") ;
		//InputStream is = new BufferedInputStream(new FileInputStream("blah.turtle"));


		//apply sparql queries against it
		//final Query query = config.getQueryFactory().newQuery(Type.CONSTRUCT);
		//final GraphComponentCollection construct = query.getConstructComponent();
		final Query query = config.getQueryFactory().newQuery(Type.SELECT);


		final QueryResource topObjectProperty = query.getResource("http://www.w3.org/2002/07/owl#topObjectProperty");
		final QueryResource subPropertyOf = query.getResource(RDFS_NS+"subPropertyOf");
		final Variable site = query.getVariable(VAR_NS+"site");
		Set<Variable> vars = new LinkedHashSet<Variable>();
		vars.add(site);
		query.setVariables(vars);
		//query.addPattern(site, subClassOf, PollutedThing);
		//query.addPattern(site, subClassOf, Measurement);
		query.addPattern(site, subPropertyOf, topObjectProperty);

		//construct.addPattern(site, subClassOf, PollutedThing);

		//return executeLocalQuery(query, model);

		Set master = new HashSet();		//model.
		Set<OntProperty> props = new HashSet<OntProperty>();		//model.
		Set<String> labels = new HashSet<String>();		//model.

		//OntClass thing = model.getOntClass( OWL.Thing.getURI() );
		//OntClass entity = model.getOntClass( "http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Entity" );
		OntProperty topObjProp = model.getOntProperty("http://www.w3.org/2002/07/owl#topObjectProperty");
		OntProperty subProp = model.getOntProperty( classRequiresSubpropertyString );

		Hashtable<String, String> table = new Hashtable<String, String>();

		for (Iterator<? extends OntProperty> i = subProp.listSubProperties(true); i.hasNext(); ) { //true here is for direct
			OntProperty hierarchyRoot = i.next();

			// props.add( hierarchyRoot);
			//  labels.add( hierarchyRoot.getLabel(null));
			if(hierarchyRoot.getLabel(null) == "" || hierarchyRoot.getLabel(null) == null){
				table.put(hierarchyRoot.toString(), getShortName(hierarchyRoot.toString()));
			}
			else{
				table.put(hierarchyRoot.toString(), hierarchyRoot.getLabel(null));
			}

		}	



		/*
		 * 
				for (Iterator<OntClass> i = model.listHierarchyRootClasses(); i.hasNext(); ) {
				    OntClass hierarchyRoot = i.next();
				    classes.add( hierarchyRoot);
				    labels.add( hierarchyRoot.getLabel(null));
				}
		 */
		master.add(props);
		master.add(labels);
		return jsonWrapper(table, classRequiresSubpropertyString);

		//return master.toString();



		//return config.getQueryExecutor(request).accept("application/json").executeLocalQuery(query, model);
	}



	public String executeLocalQuery(Query query, Model model) {

		if(System.getProperty("edu.rpi.tw.escience.writemodel", "false").equals("true")) {
			try {
				FileOutputStream fos = new FileOutputStream(System.getProperty("java.io.tmpdir")+"/model.rdf");
				model.write(fos);
				fos.close();
			}
			catch(Exception e) {
				// do nothing
			}
		}

		Model resultModel = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		long start = System.currentTimeMillis();
		QueryExecution qe = QueryExecutionFactory.create(query.toString(), model);
		try {
			switch(query.getType()) {
			case SELECT:
				ResultSet results = qe.execSelect();
				ResultSetFormatter.outputAsJSON(baos, results);
				//log.debug("Local query took "+(System.currentTimeMillis()-start)+" ms");
				return baos.toString("UTF-8");
			case DESCRIBE:
				resultModel = qe.execDescribe();
				resultModel.write(baos);
				//log.debug("Local query took "+(System.currentTimeMillis()-start)+" ms");
				return baos.toString("UTF-8");
			case CONSTRUCT:
				resultModel = qe.execConstruct();
				resultModel.write(baos);
				//log.debug("Local query took "+(System.currentTimeMillis()-start)+" ms");
				return baos.toString("UTF-8");
			case ASK:
				if(qe.execAsk()) {
					//log.debug("Local query took "+(System.currentTimeMillis()-start)+" ms");
					return "{\"result\":true}";
				}
				else {
					//	log.debug("Local query took "+(System.currentTimeMillis()-start)+" ms");
					return "{\"result\":false}";
				}
			}
		}
		catch(Exception e) {
			//	log.warn("Unable to execute query due to exception", e);
		}
		return null;
	}


	@Override
	public void visit(final Model model, final Request request) {
		// TODO populate data model
	}

	@Override
	public void visit(final OntModel model, final Request request) {
		// TODO populate ontology model
	}

	@Override
	public void visit(final Query query, final Request request) {
		// TODO modify queries
	}

	@Override
	public void visit(final SemantEcoUI ui, final Request request) {
		// TODO add resources to display
	}

	@Override
	public String getName() {
		return "Annotator";
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public String getExtraVersion() {
		return null;
	}

	@Override
	public void setModuleConfiguration(final ModuleConfiguration config) {
		this.config = config;
	}

}