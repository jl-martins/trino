/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.jdbc.expression;

import io.trino.matching.Capture;
import io.trino.matching.Captures;
import io.trino.matching.Pattern;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcExpression;
import io.trino.spi.connector.AggregateFunction;
import io.trino.spi.expression.Variable;
import io.trino.spi.type.DoubleType;

import java.util.Optional;

import static com.google.common.base.Verify.verify;
import static io.trino.matching.Capture.newCapture;
import static io.trino.plugin.jdbc.expression.AggregateFunctionPatterns.basicAggregation;
import static io.trino.plugin.jdbc.expression.AggregateFunctionPatterns.expressionType;
import static io.trino.plugin.jdbc.expression.AggregateFunctionPatterns.functionName;
import static io.trino.plugin.jdbc.expression.AggregateFunctionPatterns.singleInput;
import static io.trino.plugin.jdbc.expression.AggregateFunctionPatterns.variable;
import static java.lang.String.format;

public class ImplementVarianceSamp
        implements AggregateFunctionRule
{
    private static final Capture<Variable> INPUT = newCapture();

    @Override
    public Pattern<AggregateFunction> getPattern()
    {
        return basicAggregation()
                // TODO (https://github.com/trinodb/trino/issues/6189): remove variance, an alias, from the list & simplify the pattern
                .with(functionName().matching(name -> "variance".equals(name) || "var_samp".equals(name)))
                .with(singleInput().matching(
                        variable()
                                .with(expressionType().matching(DoubleType.class::isInstance))
                                .capturedAs(INPUT)));
    }

    @Override
    public Optional<JdbcExpression> rewrite(AggregateFunction aggregateFunction, Captures captures, RewriteContext context)
    {
        Variable input = captures.get(INPUT);
        JdbcColumnHandle columnHandle = (JdbcColumnHandle) context.getAssignment(input.getName());
        verify(aggregateFunction.getOutputType() == columnHandle.getColumnType());

        return Optional.of(new JdbcExpression(
                format("var_samp(%s)", context.getIdentifierQuote().apply(columnHandle.getColumnName())),
                columnHandle.getJdbcTypeHandle()));
    }
}
