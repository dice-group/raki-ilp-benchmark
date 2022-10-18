package org.dice_group.raki.core.concepts;

import org.apache.commons.compress.utils.Sets;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class ManchesterSyntaxParser {

    protected static Logger LOGGER = LoggerFactory.getLogger(ManchesterSyntaxParser.class);

    private final BidirectionalShortFormProvider provider;
    private final ManchesterOWLSyntaxClassExpressionParser parser;

    /**
     * Creates a ManchesterSyntaxParser which can convert [OWLClassExpression] to a manchester syntax string representation
     * or parse a String in manchester syntax to an [OWLClassExpression]
     *
     * @param mainOntology The main ontology to use as the short name provider
     * @param owlBaseOntology the additional OWL base ontology
     * */
    public ManchesterSyntaxParser(OWLOntology mainOntology, OWLOntology owlBaseOntology){
        LOGGER.info("Creating BidirectionalShortFormProviderAdapter...");
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(mainOntology, owlBaseOntology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(mainOntology));

        //create underlying parser
        LOGGER.info("Creating OWLEntityChecker...");
        OWLEntityChecker checker = new ShortFormEntityChecker(provider);
        LOGGER.info("Creating OWLDataFactory...");
        OWLDataFactory dataFactory = new OWLDataFactoryImpl();
        LOGGER.info("Creating ManchesterOWLSyntaxClassExpressionParser...");
        parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        LOGGER.info("Created ManchesterOWLSyntaxClassExpressionParser");
    }


    /**
     *  Creates the OWLClassExpression from the concept, using the provided ontology and the provided owl base ontology.
     *
     * @param concept The concept to convert to OWLClassExpression
     * @return The converted Concept/OWLClassExpression
     */
    public OWLClassExpression parse(String concept){
        return parser.parse(concept);
    }

    /**
     * Renders a [OWLClassExpression] to a string in manchester syntax
     * @param expr the expression to render
     * @return The string representation of the expression in Manchester Syntax
     */
    public String render(OWLClassExpression expr){
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        return renderer.render(expr);
    }
}
