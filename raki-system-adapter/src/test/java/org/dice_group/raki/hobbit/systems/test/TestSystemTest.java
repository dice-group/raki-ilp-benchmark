package org.dice_group.raki.hobbit.systems.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestSystemTest {

    protected static Logger LOGGER = LoggerFactory.getLogger(TestSystemTest.class);

    @Parameterized.Parameters
    public static Collection<Object> data(){
        Collection<Object> data = new ArrayList<>();
        data.add(new String[]{"{\"positives\": [\"https://w3id.org/scholarlydata/role/reviewer\", \"https://w3id.org/scholarlydata/role/programme-committee-member\"], " +
                "\"negatives\": [\"https://w3id.org/scholarlydata/role/proceedings-chair\"]}", "PublishingRole  and (not (OrganisingRole))"});
        data.add(new String[]{"{\"positives\": [\"https://w3id.org/scholarlydata/role/proceedings-chair\", \"https://w3id.org/scholarlydata/role/programme-committee-member\"], " +
                "\"negatives\": [\"https://w3id.org/scholarlydata/role/proceedings-chair\"]}", "PublishingRole  and OrganisingRole  and (not (OrganisingRole))"});
        data.add(new String[]{"{\"positives\": [\"https://w3id.org/scholarlydata/role/proceedings-chair\"], " +
                "\"negatives\": [\"https://w3id.org/scholarlydata/role/proceedings-chair\"]}", "OrganisingRole  and (not (OrganisingRole))"});
        return data;
    }

    private String posNegExample;
    private String expectedConcept;

    public TestSystemTest(String posNegExample, String expectedConcept){
        this.posNegExample=posNegExample;
        this.expectedConcept=expectedConcept;
    }

    @Test
    public void test() throws Exception {
        TestSystem system = new TestSystem();
        LOGGER.info("test");
        system.loadOntology(new File("ontology.owl"));
        String concept = system.createConcept(posNegExample);
        assertEquals(expectedConcept, concept);
    }
}
