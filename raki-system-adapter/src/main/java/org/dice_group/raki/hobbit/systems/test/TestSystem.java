package org.dice_group.raki.hobbit.systems.test;

import com.google.common.collect.Sets;
import org.dice_group.raki.hobbit.system.AbstractRakiSystemAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TestSystem extends AbstractRakiSystemAdapter {

    private OWLOntology ontology;
    private BidirectionalShortFormProviderAdapter provider;


    public static void main(String[] args) throws Exception {
        TestSystem system = new TestSystem();
        system.loadOntology(new File("./raki-datagenerator/data/family/ontology.owl"));
        String lp = "{\n" +
                "  \"positives\": [\n" +
                "    \"http://www.benchmark.org/family#F6F86\",\n" +
                "    \"http://www.benchmark.org/family#F6F87\",\n" +
                "    \"http://www.benchmark.org/family#F6M69\"\n" +
                "  ],\n" +
                "  \"negatives\": [\n" +
                "    \"http://www.benchmark.org/family#F6F86\",\n" +
                "    \"http://www.benchmark.org/family#F6M73\",\n" +
                "    \"http://www.benchmark.org/family#F7F103\"\n" +

                "  ]\n" +
                "}";
        String concept = system.createConcept(lp);
        System.out.println(concept);
    }

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

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        Set<OWLClassExpression> classes = new HashSet<OWLClassExpression>();
        if(posAxioms.size()>1){
            posAxioms.remove(new OWLDataFactoryImpl().getOWLThing());
        }
        posAxioms.forEach(owlClass -> {classes.add(owlClass.getNNF());});

        OWLClassExpression pos = new OWLObjectUnionOfImpl(classes);
        return renderer.render(pos).replace("\n", " ");
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
