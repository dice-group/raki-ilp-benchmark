package org.dice_group.raki.hobbit.core.concepts;

import org.apache.commons.compress.utils.Sets;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class ManchesterSyntaxParser {


    /**
     *  Creates the OWLClassExpression from the concept, using the provided ontology and the provided owl base ontology.
     *
     * @param concept The concept to convert to OWLClassExpression
     * @param ontology The ontology to use as the short name provider
     * @param owlBaseOntology the additional OWL base ontology
     * @return The converted Concept/OWLClassExpression
     */
    public static OWLClassExpression parse(String concept, OWLOntology ontology, OWLOntology owlBaseOntology){
        BidirectionalShortFormProvider provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology, owlBaseOntology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));
        OWLEntityChecker checker = new ShortFormEntityChecker(provider);

        OWLDataFactory dataFactory = new OWLDataFactoryImpl();

        ManchesterOWLSyntaxClassExpressionParser parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        return parser.parse(concept);
    }
}
