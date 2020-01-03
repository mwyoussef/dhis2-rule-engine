package org.hisp.dhis.rules.functions;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.hisp.dhis.parser.expression.function.SimpleNoSqlFunction;
import org.hisp.dhis.rules.RuleVariableValue;

import java.util.Map;

/**
 * @Author Zubair Asghar.
 * <p>
 * Counts the number of values that is entered for the source field in the argument.
 * The source field parameter is the name of one of the defined source fields in the program
 */
public class RuleFunctionCount
    extends SimpleNoSqlFunction
{
        private String count( String ruleVariableName, Map<String, RuleVariableValue> valueMap )
        {
                RuleVariableValue variableValue = valueMap.get( ruleVariableName );

                if ( variableValue != null)
                {
                        return Integer.toString( variableValue.candidates().size() );
                }
                else
                {
                        return "0";
                }
        }

        @Override
        public Object evaluate( ExpressionParser.ExprContext ctx, CommonExpressionVisitor visitor )
        {
                return count( visitor.castStringVisit( ctx.expr( 0 ) ), visitor.getValueMap() );
        }
}
