package org.semanticweb.vlog4j.graal;

/**
 * An exception to signify that a conversion from Graal data structures to
 * VLog4J data structures could not be made.
 * 
 * @author Adrian Bielefeldt
 *
 */
public class GraalConvertException extends RuntimeException {

	/**
	 * generated serial version UID
	 */
	private static final long serialVersionUID = -3228005099627492816L;

	public GraalConvertException(final String message) {
		super(message);
	}

	public GraalConvertException(final String message, final Throwable exception) {
		super(message, exception);
	}
}
