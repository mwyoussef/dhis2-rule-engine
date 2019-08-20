package org.hisp.dhis.rules.models

data class RuleActionHideSection(
        val programStageSection: String?
) : RuleAction() {

    companion object {

        fun create(section: String): RuleActionHideSection {
            return RuleActionHideSection(section)
        }
    }
}
