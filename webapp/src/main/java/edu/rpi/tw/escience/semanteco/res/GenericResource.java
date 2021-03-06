package edu.rpi.tw.escience.semanteco.res;

import java.io.IOException;
import java.io.InputStream;

import edu.rpi.tw.escience.semanteco.Module;
import edu.rpi.tw.escience.semanteco.util.SemantEcoConfiguration;

/**
 * GenericResource is used for representing resources where
 * the portal does not understand what the content type of
 * the resource is meant to provide. Certain file types,
 * e.g. jsp or css, will be used for populating index.jsp
 * with appropriate content and have specialized classes
 * to represent them.
 * 
 * @author ewpatton
 *
 */
public class GenericResource extends OwnedResource {

	private String path = null;
	
	/**
	 * Generates a generic resource reference for the given
	 * module at the specified path.
	 * @param owner
	 * @param path
	 */
	public GenericResource(Module owner, String path) {
		super(owner);
		this.path = path;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public InputStream open() throws IOException {
		return null;
	}

	@Override
	public String toString() {
		String basePath = SemantEcoConfiguration.get().getBasePath();
		if(basePath.endsWith("/")) {
			return basePath+this.path;
		} else {
			return basePath+"/"+this.path;
		}
	}
}
