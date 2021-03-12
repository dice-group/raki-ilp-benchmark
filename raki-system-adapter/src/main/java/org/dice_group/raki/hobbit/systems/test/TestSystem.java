package org.dice_group.raki.hobbit.systems.test;

import com.google.common.collect.Sets;
import org.dice_group.raki.hobbit.systems.AbstractRakiSystemAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestSystem extends AbstractRakiSystemAdapter {

    private OWLOntology ontology;
    private BidirectionalShortFormProviderAdapter provider;


    @Override
    public String createConcept(String posNegExample) throws IOException, Exception {
        JSONObject posNegJson = new JSONObject(posNegExample);
        Set<OWLIndividual> posExamples = getExamples(posNegJson.getJSONArray("positives"));
        Set<OWLIndividual> negExamples = getExamples(posNegJson.getJSONArray("negatives"));
        Set<OWLClass> posAxioms = new HashSet<OWLClass>();
        Set<OWLClass> negAxioms = new HashSet<OWLClass>();
        for(OWLIndividual pos : posExamples) {
            ontology.getClassAssertionAxioms(pos).forEach(classAssertion ->{
                posAxioms.addAll(classAssertion.getClassesInSignature());
            });
        }
        for(OWLIndividual neg : negExamples) {
            ontology.getClassAssertionAxioms(neg).forEach(classAssertion ->{
                negAxioms.addAll(classAssertion.getClassesInSignature());
            });        }
        //posAxioms.removeAll(negAxioms);

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        List<OWLClassExpression> classes = new ArrayList<OWLClassExpression>();
        posAxioms.forEach(owlClass -> {classes.add(owlClass.getNNF());});
        List<OWLClassExpression> notClasses = new ArrayList<OWLClassExpression>();
        negAxioms.forEach(owlClass -> {notClasses.add(owlClass.getObjectComplementOf());});

        OWLClassExpression pos = new OWLObjectIntersectionOfImpl(classes);
        OWLClassExpression neg = new OWLObjectIntersectionOfImpl(notClasses);
        classes.addAll(notClasses);
        OWLClassExpression best = new OWLObjectIntersectionOfImpl(classes);
        return renderer.render(best).replace("\n", " ");
    }


    private Set<OWLIndividual> getExamples(JSONArray examples) {
        Set<OWLIndividual> ret = new HashSet<OWLIndividual>();
        examples.forEach(uri -> {
            ret.add(new OWLNamedIndividualImpl(IRI.create(uri.toString())));
        });
        return ret;
    }

    @Override
    public void loadOntology(File ontologyFile) throws IOException, Exception {
        ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));

    }

}
