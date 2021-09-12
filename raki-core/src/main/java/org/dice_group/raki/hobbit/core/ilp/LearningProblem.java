package org.dice_group.raki.hobbit.core.ilp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A learning problem is defined as a set of positive uris and a set of negative uris.
 *
 * A concept which were used to derive these sets can be set additionally.
 */
public class LearningProblem {

    private final Set<String> positiveUris = new HashSet<>();
    private final Set<String> negativeUris = new HashSet<>();
    private String concept = null;

    public void setConcept(String concept){
        this.concept = concept;
    }

    public boolean hasConcept(){
        return concept!=null && !concept.isEmpty();
    }

    public String getConcept(){
        return this.concept;
    }

    public void addPositiveUri(String uri){
        positiveUris.add(uri);
    }

    public void addNegativeUri(String uri){
        negativeUris.add(uri);
    }

    public void addPositiveUris(Collection<String> uris){
        positiveUris.addAll(uris);
    }

    public void addNegativeUris(Collection<String> uris){
        negativeUris.addAll(uris);
    }

    public Set<String> getNegativeUris(){
        return negativeUris;
    }

    public Set<String> getPositiveUris(){
        return positiveUris;
    }


    /**
     * Creates a JSON representation of the learning problem with the following structure
     *
     * <pre>{@code
     * {
     *    "positives" : [
     *      "http://positive_uri/1",
     *      "http://positive_uri/2",
     *      ...
     *      "http://positive_uri/n"
     *    ],
     *    "negatives" : [
     *      "http://negative_uri/1",
     *      "http://negative_uri/2",
     *      ...
     *      "http://negative_uri/m"
     *    ]
     * }
     * }
     * </pre>
     *
     * if the learning problem contains an underlying concept, this concept can be added as well (in Manchester syntax)
     *
     * <pre>{@code
     * {
     *    "concept" : "CONCEPT IN MANCHESTER SYNTAX"
     *    "positives" : [
     *      "http://positive_uri/1",
     *      "http://positive_uri/2",
     *      ...
     *      "http://positive_uri/n"
     *    ],
     *    "negatives" : [
     *      "http://negative_uri/1",
     *      "http://negative_uri/2",
     *      ...
     *      "http://negative_uri/m"
     *    ]
     * }
     * }
     * </pre>
     *
     * @param addConcept adds the underlying concept to the json repr. if concept is set.
     * @return the json representation of this learning problem
     */
    public String asJsonString(boolean addConcept){
        StringBuilder builder = new StringBuilder("{\n");
        if(addConcept && concept !=null && !concept.isEmpty()){
            builder.append("\"concept\" : \"").
                    append(concept).
                    append("\",\n");
        }
        builder.append("\"positives\" : [\n");
        addUrisToBuilder(positiveUris, builder);
        builder.append("],\n");
        builder.append("\"negatives\" : [\n");
        addUrisToBuilder(negativeUris, builder);
        builder.append("]\n}");
        return builder.toString();
    }

    private void addUrisToBuilder(Set<String> uris, StringBuilder builder) {
        int count=1;
        for(String uri : uris){
            builder.append("\"").
                    append(uri).
                    append("\"");
            if(count < uris.size()){
                builder.append(",");
            }
            builder.append("\n");
            count++;
        }
    }

    @Override
    public String toString() {
        return "LearningProblem{" +
                "positiveUris=" + positiveUris +
                ", negativeUris=" + negativeUris +
                ", concept='" + concept + '\'' +
                '}';
    }
}
