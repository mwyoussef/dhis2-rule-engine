package org.hisp.dhis.rules

import org.apache.commons.jexl2.JexlException
import org.apache.commons.logging.LogFactory
import org.hisp.dhis.rules.RuleExpression.Companion.FUNCTION_PATTERN
import org.hisp.dhis.rules.functions.RuleFunction
import org.hisp.dhis.rules.models.*
import java.util.*
import java.util.concurrent.Callable
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

actual class RuleEngineExecution actual constructor(
        private val expressionEvaluator: RuleExpressionEvaluator,
        private val rules: List<Rule>,
        valueMap: Map<String, RuleVariableValue>,
        private val supplementaryData: Map<String, List<String>>): Callable<List<RuleEffect>> {

    private val valueMap: MutableMap<String, RuleVariableValue>

    init {
        this.valueMap = HashMap(valueMap)
    }

    actual override fun call(): List<RuleEffect> {
        val ruleEffects = ArrayList<RuleEffect>()

        val ruleList = ArrayList(rules)

        ruleList.sortWith(Comparator { rule1, rule2 ->
            when {
                rule1.priority != null && rule2.priority != null -> rule1.priority.compareTo(rule2.priority)
                rule1.priority != null -> -1
                rule2.priority != null -> 1
                else -> 0
            }
        })

        ruleList.forEach {rule ->
            try {
                log.debug("Evaluating program rule: ${rule.name}")
                if (process(rule.condition).toBoolean()) {
                    rule.actions.forEach {
                        val ruleEffect = create(it)
                        if (isAssignToCalculatedValue(it)) {
                            updateValueMapForCalculatedValue(it as RuleActionAssign,
                                    RuleVariableValue.create(ruleEffect.data, RuleValueType.TEXT))
                        } else {
                            ruleEffects.add(create(it))
                        }
                    }
                }
            }catch (jexlException: JexlException) {
                log.error("Parser exception in ${rule.name} : ${jexlException.message}")
            } catch (e: Exception) {
                log.error("Exception in  ${rule.name} : ${e.message}")
            }
        }

        return ruleEffects
    }

    private fun isAssignToCalculatedValue(ruleAction: RuleAction): Boolean {
        return ruleAction is RuleActionAssign && ruleAction.field?.isEmpty() ?: false
    }

    private fun updateValueMapForCalculatedValue(ruleActionAssign: RuleActionAssign, value: RuleVariableValue) {
        valueMap[RuleExpression.unwrapVariableName(ruleActionAssign.content!!)] = value
    }

    private fun create(ruleAction: RuleAction): RuleEffect {
        // Only certain types of actions might
        // contain code to execute.
        return when (ruleAction) {
            is RuleActionAssign -> {
                val data = process(ruleAction.data!!)
                val variableValue = RuleVariableValue.create(data, RuleValueType.TEXT, listOf(data), Date().toString())
                val field = ruleAction.field
                val matcher = REGEX.findAll(field!!)
                matcher.asIterable()
                        .map { result -> result.groupValues[0].trim() }
                        .forEach { value -> valueMap[value] = variableValue }


                RuleEffect.create(ruleAction, data)
            }
            is RuleActionSendMessage -> RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionScheduleMessage -> RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionCreateEvent -> RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionDisplayKeyValuePair ->  RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionDisplayText ->  RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionErrorOnCompletion ->  RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionShowError ->  RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionShowWarning ->  RuleEffect.create(ruleAction, process(ruleAction.data!!))
            is RuleActionWarningOnCompletion ->  RuleEffect.create(ruleAction, process(ruleAction.data!!))
            else -> RuleEffect.create(ruleAction)
        }

    }

    private fun process(expression: String): String {
        expression.replace("\n", "").trim()

        // we don't want to run empty expression
        if (expression.isNotEmpty()) {
            val expressionWithVariableValues = bindVariableValues(expression)
            val expressionWithFunctionValues = bindFunctionValues(expressionWithVariableValues)

            log.debug("Evaluating expression: $expressionWithFunctionValues")
            return expressionEvaluator.evaluate(expressionWithFunctionValues)
        }

        return ""
    }

    private fun bindVariableValues(expression: String): String {
        val ruleExpression = RuleExpression.from(expression)
        val ruleExpressionBinder = RuleExpressionBinder.from(ruleExpression)

        // substitute variable values
        ruleExpression.variable.forEach {
            val variableValue = valueMap[RuleExpression.unwrapVariableName(it)]
            variableValue?.let { variable ->
                ruleExpressionBinder.bindVariable(it, variable.value ?: variable.type.defaultValue())
            }
        }

        return ruleExpressionBinder.build()
    }

    private fun bindFunctionValues(expression: String): String {

        val ruleExpression = RuleExpression.from(expression)
        val ruleExpressionBinder = RuleExpressionBinder.from(ruleExpression)

        ruleExpression.functions.forEach { function ->
            val ruleFunctionCall = RuleFunctionCall.from(function)
            val arguments = ruleFunctionCall.arguments.map { arg -> process(arg) }

            ruleExpressionBinder.bindFunction(
                    ruleFunctionCall.functionCall,
                    RuleFunction.create(ruleFunctionCall.functionName)?.evaluate(
                            arguments, valueMap,supplementaryData) ?: ""
            )
        }


        var processedExpression = ruleExpressionBinder.build()

        // In case if there are functions which
        // are not processed completely.
        if (processedExpression.contains(D2_FUNCTION_PREFIX)) {
            val functionMatcher = FUNCTION_PATTERN.find(processedExpression)

            functionMatcher?.let {result ->
                if(result?.groupValues[1].isNotEmpty())
                // Another recursive call to process rest of
                // the d2 function calls.
                    processedExpression = bindFunctionValues(processedExpression)
            }
        }

        return processedExpression
    }

    companion object {
        private const val D2_FUNCTION_PREFIX = "d2:"

        private val log = LogFactory.getLog(RuleEngineExecution::class.java)

        private val REGEX = "[a-zA-Z0-9]+(?:[\\w -]*[a-zA-Z0-9]+)*".toRegex(RegexOption.IGNORE_CASE)
    }
}