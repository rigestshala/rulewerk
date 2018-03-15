package org.semanticweb.vlog4j.core.model.impl;

import org.semanticweb.vlog4j.core.model.api.Blank;
import org.semanticweb.vlog4j.core.model.api.TermType;
import org.semanticweb.vlog4j.core.model.validation.IllegalEntityNameException;

/**
 * Class for blank terms. A blank is an entity used to represent anonymous domain elements introduced during the reasoning process to satisfy existential
 * restrictions.
 *
 * @author david.carral@tu-dresden.de
 */
public class BlankImpl extends AbstractTerm implements Blank {

	/**
	 * Parameterized constructor. This constructor creates a blank term with the name <b>{@code name}</b>.
	 *
	 * @param name
	 *            cannot be a blank String (null, " ", empty string...).
	 * @throws IllegalEntityNameException
	 *             if the given name <b>{@code name}</b> is a blank String.
	 */
	public BlankImpl(final String name) throws IllegalEntityNameException {
		super(name);
	}

	/**
	 * Deep copy constructor. This constructor creates a <b>{@code BlankImpl}</b> object with the same name as <b>{@code copyBlank}</b>. The new object does not
	 * contain any reference to original data.
	 *
	 * @param copyBlank
	 *            is the <b>{@code BlankImpl}</b> object that will be copied/cloned.
	 *
	 * @throws IllegalEntityNameException
	 *             if the given field <b>{@code name}</b> in <b>{@code copyBlank}</b> is a blank String (null, " ", empty string...).
	 */
	public BlankImpl(final Blank copyBlank) throws IllegalEntityNameException {
		super(new String(copyBlank.getName()));
	}

	@Override
	public TermType getType() {
		return TermType.BLANK;
	}
}
