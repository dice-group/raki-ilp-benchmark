package org.dice_group.raki.core.ilp;

import java.util.*;

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
     * Gets a random set of negative uris.
     *
     * The ratio [0-1.0] describes how many percent of the original negative uris should be returned.
     *
     *
     * ## Example:
     *
     * If the learning problem has 10 negative uris.
     * A ratio of 0.5 will return 5 randomly choosen negative uris.
     * A ratio of 0.7 will return 7 randomly choosen negative uris.
     * A ratio of 0.79 will return 7 randomly choosen negative uris.
     *
     *
     * @param ratio the ratio (from 0 to 1) describing how many negative uris should be received max.
     * @param rand The Random object to user for the retrieval
     * @return A Set of random retrieved negative uris from the original negative uris. The size will be negativeUris.size()*ratio
     */
    public Set<String> getSomeNegativeUris(double ratio, Random rand){
        return getSomeUris(negativeUris, ratio, rand);
    }

    /**
     * Gets a random set of positive uris.
     *
     * The ratio [0-1.0] describes how many percent of the original positive uris should be returned.
     *
     *
     * ## Example:
     *
     * If the learning problem has 10 positive uris.
     * A ratio of 0.5 will return 5 randomly choosen positive uris.
     * A ratio of 0.7 will return 7 randomly choosen positive uris.
     * A ratio of 0.79 will return 7 randomly choosen positive uris.
     *
     *
     * @param ratio the ratio (from 0 to 1) describing how many positive uris should be received max.
     * @param rand The Random object to user for the retrieval
     * @return A Set of random retrieved positive uris from the original positive uris. The size will be positiveUris.size()*ratio
     */
    public Set<String> getSomePositiveUris(double ratio, Random rand){
        return getSomeUris(positiveUris, ratio, rand);
    }

    /**
     * Gets a random set of uris.
     *
     * The ratio [0-1.0] describes how many percent of the original uris should be returned.
     *
     *
     * ## Example:
     *
     * If the provided uris colletion has 10 uris.
     * A ratio of 0.5 will return 5 randomly choosen uris.
     * A ratio of 0.7 will return 7 randomly choosen uris.
     * A ratio of 0.79 will return 7 randomly choosen uris.
     *
     *
     * @param uris The uris to choose from.
     * @param ratio the ratio (from 0 to 1) describing how many uris should be received max.
     * @param rand The Random object to user for the retrieval
     * @return A Set of random retrieved uris from the original uris. The size will be uris.size()*ratio
     */
    private Set<String> getSomeUris(Collection<String> uris, double ratio, Random rand){
        //Convert positive Uris to list, so we can shuffle it.
        List<String> shuffleList = new ArrayList<>(uris);

        // random shuffle, so we can just choose the first N uris
        Collections.shuffle(shuffleList, rand);

        // assert that the amount of uris we want to retrieve is positive or 0, and not greater than the actual amount of positive Uris.
        // Max( 0, Min ( |positiveUris|, floor( |positiveUris| * ratio )  ))
        double retrieve = Math.max(0, Math.min(shuffleList.size(),
                Math.floor(shuffleList.size()*ratio)
        ));
        return new HashSet<>(shuffleList.subList(0, (int) retrieve));
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
