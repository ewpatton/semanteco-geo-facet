package edu.rpi.tw.escience.semanteco;

import edu.rpi.tw.escience.semanteco.query.Query;

/**
 * An object implementing the QueryVisitor will be called when
 * a SemantEco client requests data, allowing the visitor to
 * specialize the Query based on parameters passed through
 * the RESTful interface. 
 * 
 * @author ewpatton
 *
 */
public interface QueryVisitor {
	
	/**
	 * Gets the name of this query visitor for
	 * provenance purposes.
	 * @return
	 */
	String getName();
	
	/**
	 * Allows the QueryVisitor to visit the Query and make
	 * any necessary modifications based on the parameters
	 * coming from the RESTful call.
	 * @param query Query object to manipulate
	 * @param params Parameters from the RESTful call
	 */
	void visit(Query query, Request request);
}
