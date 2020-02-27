package org.hisp.dhis.rules.models;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith( JUnit4.class )
public class RuleActionSetMandatoryFieldTests
{

    @Test( expected = NullPointerException.class )
    public void createMustThrowOnNullArgument()
    {
        RuleActionSetMandatoryField.create( null );
    }

    @Test
    public void equalsAndHashcodeMustConformToContract()
    {
        EqualsVerifier.forClass( RuleActionSetMandatoryField.create( "" ).getClass() )
            .suppress( Warning.NULL_FIELDS )
            .verify();
    }
}
