package edu.rpi.tw.escience.waterquality.taglib;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

import edu.rpi.tw.escience.waterquality.Module;
import edu.rpi.tw.escience.waterquality.Resource;
import edu.rpi.tw.escience.waterquality.SemantAquaUI;
import edu.rpi.tw.escience.waterquality.impl.ModuleManagerFactory;
import edu.rpi.tw.escience.waterquality.ui.SemantAquaUIFactory;
import edu.rpi.tw.escience.waterquality.util.JavaScriptGenerator;

/**
 * The ScriptTag class provides a means for SemantAqua JSP to provide JavaScript
 * from the various modules as well as the autogenerated JavaScript provided
 * for executing QueryMethod annotated queries on the server via a RESTful call.
 * 
 * @author ewpatton
 *
 */
public class ScriptTag extends TagSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7903606798820268678L;
	private Logger log = Logger.getLogger(ScriptTag.class);
	
	/**
	 * Default constructor used by the JSP processor to instantiate this class
	 */
	public ScriptTag() {
		
	}
	
	@Override
	public int doStartTag() throws JspException {
		log.debug("Generating <script> entries");
		final JspWriter out = pageContext.getOut();
		final ServletContext context = pageContext.getServletContext();
		final SemantAquaUI ui = SemantAquaUIFactory.getInstance().getUI();
		final List<Resource> scripts = ui.getScripts();
		try {
			log.debug("Writing module autogen scripts");
			List<Module> modules = ModuleManagerFactory.getInstance().getManager().listModules();
			for(Module i : modules) {
				out.write("<script src=\"");
				out.write(context.getContextPath());
				out.write("/js/modules/");
				out.write(JavaScriptGenerator.cleanName(i.getName()));
				out.write(".js\" type=\"text/javascript\"></script>\r\n");
			}
			log.debug("Scripts: "+scripts.size());
			for(Resource i : scripts) {
				out.write("<script src=\"");
				out.write(context.getContextPath());
				out.write("/");
				out.write(i.getPath());
				out.write("\" type=\"text/javascript\"></script>\r\n");
			}
		}
		catch(IOException e) {
			log.error("Unable to generate list of scripts", e);
		}
		return SKIP_BODY;
	}

}
