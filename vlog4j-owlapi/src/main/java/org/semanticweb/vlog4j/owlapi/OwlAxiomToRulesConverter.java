package org.semanticweb.vlog4j.owlapi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*-
 * #%L
 * VLog4j OWL API Support
 * %%
 * Copyright (C) 2018 VLog4j Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;
import org.semanticweb.vlog4j.core.model.api.Atom;
import org.semanticweb.vlog4j.core.model.api.Conjunction;
import org.semanticweb.vlog4j.core.model.api.Rule;
import org.semanticweb.vlog4j.core.model.api.Term;
import org.semanticweb.vlog4j.core.model.api.Variable;
import org.semanticweb.vlog4j.core.model.implementation.ConjunctionImpl;
import org.semanticweb.vlog4j.core.model.implementation.RuleImpl;
import org.semanticweb.vlog4j.core.model.implementation.VariableImpl;

/**
 * Class for converting OWL axioms to rules.
 * 
 * @author Markus Kroetzsch
 *
 */
public class OwlAxiomToRulesConverter extends OWLAxiomVisitorAdapter implements OWLAxiomVisitor {

	final Set<Rule> rules = new HashSet<>();
	final Set<Atom> facts = new HashSet<>();
	final Variable frontierVariable = new VariableImpl("X");
	int freshVariableCounter = 0;

	/**
	 * Returns a fresh variable, which can be used as auxiliary variable in the
	 * current axiom's translation.
	 * 
	 * @return a variable
	 */
	Variable getFreshVariable() {
		this.freshVariableCounter++;
		return new VariableImpl("Y" + this.freshVariableCounter);
	}

	void addRule(AbstractClassToRuleConverter converter) {
		if (converter.isTautology()) {
			return;
		}
		Conjunction headConjunction;
		if (converter.head.isFalseOrEmpty()) {
			headConjunction = new ConjunctionImpl(
					Arrays.asList(OwlToRulesConversionHelper.getBottom(converter.mainTerm)));
		} else {
			headConjunction = new ConjunctionImpl(converter.head.getConjuncts());
		}

		Conjunction bodyConjunction;
		if (converter.body.isTrueOrEmpty()) {
			bodyConjunction = new ConjunctionImpl(Arrays.asList(OwlToRulesConversionHelper.getTop(converter.mainTerm)));
			if (headConjunction.getVariables().isEmpty()) {
				for (Atom conjunct : headConjunction.getAtoms()) {
					this.facts.add(conjunct);
				}
				return;
			}
		} else {
			bodyConjunction = new ConjunctionImpl(converter.body.getConjuncts());
		}

		this.rules.add(new RuleImpl(headConjunction, bodyConjunction));
	}

	/**
	 * Resets the internal counter used for generating fresh variables.
	 */
	void startAxiomConversion() {
		this.freshVariableCounter = 0;
	}

	void addSubClassAxiom(OWLClassExpression subClass, OWLClassExpression superClass) {
		startAxiomConversion();

		ClassToRuleHeadConverter headConverter = new ClassToRuleHeadConverter(this.frontierVariable, this);
		superClass.accept(headConverter);
		ClassToRuleBodyConverter bodyConverter = new ClassToRuleBodyConverter(this.frontierVariable, headConverter.body,
				headConverter.head, this);
		bodyConverter.handleDisjunction(subClass, this.frontierVariable);
		addRule(bodyConverter);
	}

	@Override
	public void visit(OWLSubClassOfAxiom axiom) {
		addSubClassAxiom(axiom.getSubClass(), axiom.getSuperClass());
	}

	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		Term subject = OwlToRulesConversionHelper.getIndividualTerm(axiom.getSubject());
		Term object = OwlToRulesConversionHelper.getIndividualTerm(axiom.getObject());
		Atom atom = OwlToRulesConversionHelper.getObjectPropertyAtom(axiom.getProperty(), subject, object);
		Atom bot = OwlToRulesConversionHelper.getBottom(subject);
		this.rules.add(new RuleImpl(new ConjunctionImpl(Arrays.asList(bot)), new ConjunctionImpl(Arrays.asList(atom))));
	}

	@Override
	public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDisjointClassesAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDataPropertyDomainAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		startAxiomConversion();
		Variable secondVariable = getFreshVariable();

		Atom firstAtom = null;
		Atom previousAtom = null;
		Atom currentAtom = null;
		for (OWLObjectPropertyExpression owlObjectPropertyExpression : axiom.getProperties()) {
			currentAtom = OwlToRulesConversionHelper.getObjectPropertyAtom(owlObjectPropertyExpression,
					this.frontierVariable, secondVariable);
			if (previousAtom == null) {
				firstAtom = currentAtom;
			} else {
				this.rules.add(new RuleImpl(new ConjunctionImpl(Arrays.asList(currentAtom)),
						new ConjunctionImpl(Arrays.asList(previousAtom))));
			}
			previousAtom = currentAtom;
		}

		if (currentAtom != null) {
			this.rules.add(new RuleImpl(new ConjunctionImpl(Arrays.asList(firstAtom)),
					new ConjunctionImpl(Arrays.asList(currentAtom))));
		}
	}

	@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");

	}

	@Override
	public void visit(OWLDifferentIndividualsAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDisjointDataPropertiesAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");
	}

	@Override
	public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLObjectPropertyRangeAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		Term subject = OwlToRulesConversionHelper.getIndividualTerm(axiom.getSubject());
		Term object = OwlToRulesConversionHelper.getIndividualTerm(axiom.getObject());
		this.facts.add(OwlToRulesConversionHelper.getObjectPropertyAtom(axiom.getProperty(), subject, object));
	}

	@Override
	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSubObjectPropertyOfAxiom axiom) {
		startAxiomConversion();
		Variable secondVariable = getFreshVariable();
		Atom subRole = OwlToRulesConversionHelper.getObjectPropertyAtom(axiom.getSubProperty(), this.frontierVariable,
				secondVariable);
		Atom superRole = OwlToRulesConversionHelper.getObjectPropertyAtom(axiom.getSuperProperty(),
				this.frontierVariable, secondVariable);
		this.rules.add(new RuleImpl(new ConjunctionImpl(Arrays.asList(superRole)),
				new ConjunctionImpl(Arrays.asList(subRole))));
	}

	@Override
	public void visit(OWLDisjointUnionAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDataPropertyRangeAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");
	}

	@Override
	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");
	}

	@Override
	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");
	}

	@Override
	public void visit(OWLClassAssertionAxiom axiom) {
		startAxiomConversion();
		Term term = OwlToRulesConversionHelper.getIndividualTerm(axiom.getIndividual());
		ClassToRuleHeadConverter headConverter = new ClassToRuleHeadConverter(term, this);
		axiom.getClassExpression().accept(headConverter);
		addRule(headConverter);
	}

	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
		OWLClassExpression firstClass = null;
		OWLClassExpression previousClass = null;
		OWLClassExpression currentClass = null;
		for (OWLClassExpression owlClassExpression : axiom.getClassExpressions()) {
			currentClass = owlClassExpression;
			if (previousClass == null) {
				firstClass = currentClass;
			} else {
				addSubClassAxiom(previousClass, currentClass);
			}
			previousClass = currentClass;
		}

		if (currentClass != null) {
			addSubClassAxiom(currentClass, firstClass);
		}
	}

	@Override
	public void visit(OWLDataPropertyAssertionAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");
	}

	@Override
	public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSubDataPropertyOfAxiom axiom) {
		throw new OwlFeatureNotSupportedException("OWL datatypes currently not supported in rules.");
	}

	@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSameIndividualAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSubPropertyChainOfAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLInverseObjectPropertiesAxiom axiom) {
		startAxiomConversion();
		Variable secondVariable = getFreshVariable();
		Atom firstRole = OwlToRulesConversionHelper.getObjectPropertyAtom(axiom.getFirstProperty(),
				this.frontierVariable, secondVariable);
		Atom secondRole = OwlToRulesConversionHelper.getObjectPropertyAtom(axiom.getSecondProperty(), secondVariable,
				this.frontierVariable);
		Conjunction firstRoleConjunction = new ConjunctionImpl(Arrays.asList(firstRole));
		Conjunction secondRoleConjunction = new ConjunctionImpl(Arrays.asList(secondRole));
		this.rules.add(new RuleImpl(secondRoleConjunction, firstRoleConjunction));
		this.rules.add(new RuleImpl(firstRoleConjunction, secondRoleConjunction));
	}

	@Override
	public void visit(OWLHasKeyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SWRLRule rule) {
		// TODO Auto-generated method stub

	}

}
