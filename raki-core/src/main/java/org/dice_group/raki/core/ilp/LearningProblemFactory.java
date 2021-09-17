package org.dice_group.raki.core.ilp;

import org.apache.jena.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Factory class to create and parse Learning Problems.
 *
 */
public class LearningProblemFactory {

    /**
     * Creates an empty {@link LearningProblem}
     *
     * @return An empty Learning Problem
     */
    public static LearningProblem create(){
        return new LearningProblem();
    }

    /**
     * Creates an {@link LearningProblem} based upon the provided positives uris and negative uris.
     *
     * @param positiveUris The positive uris of the LP
     * @param negativeUris The negative uris of the LP
     * @return The corresponding Learning Problem
     */
    public static LearningProblem create(Collection<String> positiveUris, Collection<String> negativeUris){
        return create(positiveUris, negativeUris, null);
    }

    /**
     * Creates an {@link LearningProblem} based upon the provided positives uris and negative uris, as well as an optional concept.
     *
     * @param positiveUris The positive uris of the LP
     * @param negativeUris The negative uris of the LP
     * @param concept The concept representation, can be  null
     * @return The corresponding Learning Problem
     */
    public static LearningProblem create(Collection<String> positiveUris, Collection<String> negativeUris, String concept){
        LearningProblem lp  = new LearningProblem();
        lp.addNegativeUris(negativeUris);
        lp.addPositiveUris(positiveUris);

        //we don't need that, but just in case we check for null
        if(concept!=null) {
            lp.setConcept(concept);
        }
        return lp;
    }

    /**
     * Parses the given string as a json object, corresponding to our {@link LearningProblem}.
     *
     * A learning problem needs to have at least a "positives" and a "negatives" key,
     * whereas both has to be Arrays of Strings.
     * and optionally a "concept" key where the value is a {@link String}
     *
     * @param jsonString the json string representation of a {@link LearningProblem}
     * @return the LearningProblem or null if the LP wasn't a LP
     * @throws JSONException if the string is not in JSON.
     */
    public static LearningProblem parse(String jsonString)  throws JSONException{
        if(jsonString == null){
            return null;
        }
        return parse(new JSONObject(jsonString));
    }

    /**
     *
     * Parses a {@link JSONObject} as a {@link LearningProblem}
     * A learning problem needs to have at least a "positives" and a "negatives" key,
     * whereas both has to be Arrays of Strings.
     * and optionally a "concept" key where the value is a {@link String}.
     *
     * @param json the json representation of a {@link LearningProblem}
     * @return the corresponding Learning Problem or null
     */
    public static LearningProblem parse(JSONObject json){
        if(json == null || !json.has("positives") || !json.has("negatives")){
            return null;
        }
        Set<String> positivesUris = getUriArray(json.get("positives"));
        Set<String> negativeUris = getUriArray(json.get("negatives"));
        String concept = null;
        if(json.has("concept")) {
            concept = json.getString("concept");
        }
        if(positivesUris == null || negativeUris == null){
            return null;
        }
        return create(positivesUris, negativeUris, concept);
    }

    private static Set<String> getUriArray(Object jsonUris) {
        if(jsonUris instanceof JSONArray){
            Set<String> ret  = new HashSet<>();
            ((JSONArray) jsonUris).forEach( uri ->
                    ret.add(uri.toString())
            );
            return ret;
        }
        return null;
    }

    /**
     * Reads a json string representation of a JSON array containing multiple {@link LearningProblem}s
     *
     * A learning problem needs to have at least a "positives" and a "negatives" key,
     * whereas both has to be Arrays of Strings.
     * and optionally a "concept" key where the value is a {@link String}.
     *
     * @param jsonString The json array string, containing multiple {@link LearningProblem}s
     * @return a set of Learning problems described in the jsonString, or null
     * @throws JSONException if the string is not in JSON format
     */
    public static Set<LearningProblem> readMany(String jsonString) throws JSONException {
        if(jsonString == null){
            return null;
        }
        return readMany(new JSONArray(jsonString));

    }

    /**
     * Reads a json array containing multiple {@link LearningProblem}s
     *
     * A learning problem needs to have at least a "positives" and a "negatives" key,
     * whereas both has to be Arrays of Strings.
     * and optionally a "concept" key where the value is a {@link String}.
     *
     * @param jsonArray the json array containing multiple {@link LearningProblem}s
     * @return  a set of Learning problems described in the jsonArray, or null
     * @throws NullPointerException If the json contains an object not representing a learning problem.
     */
    public static Set<LearningProblem> readMany(JSONArray jsonArray) throws  NullPointerException{
        if(jsonArray == null){
            return null;
        }
        Set<LearningProblem> ret = new HashSet<>();
        jsonArray.forEach(problem ->
                {
                    if(problem instanceof JSONObject) {
                        ret.add(parse((JSONObject) problem));
                    }
                }
        );
        return ret;
    }

    /**
     * Reads a json string representation from a file as a JSON array containing multiple {@link LearningProblem}s
     *
     * A learning problem needs to have at least a "positives" and a "negatives" key,
     * whereas both has to be Arrays of Strings.
     * and optionally a "concept" key where the value is a {@link String}.
     *
     * @param jsonFile The file containing a json array, containing multiple {@link LearningProblem}s
     * @return a set of Learning problems described in the jsonString, or null
     * @throws IOException if the file couldn't be read.
     */
    public static Set<LearningProblem> readMany(File jsonFile) throws IOException {
        return readMany(FileUtils.readWholeFileAsUTF8(jsonFile.getAbsolutePath()));
    }
}
