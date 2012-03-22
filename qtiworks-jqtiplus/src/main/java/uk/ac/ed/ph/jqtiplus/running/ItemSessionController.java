/* Copyright (c) 2012, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
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
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.jqtiplus.running;

import uk.ac.ed.ph.jqtiplus.JqtiExtensionManager;
import uk.ac.ed.ph.jqtiplus.JqtiExtensionPackage;
import uk.ac.ed.ph.jqtiplus.LifecycleEventType;
import uk.ac.ed.ph.jqtiplus.exception.QtiEvaluationException;
import uk.ac.ed.ph.jqtiplus.exception.QtiParseException;
import uk.ac.ed.ph.jqtiplus.exception2.QtiLogicException;
import uk.ac.ed.ph.jqtiplus.exception2.ResponseBindingException;
import uk.ac.ed.ph.jqtiplus.exception2.RuntimeValidationException;
import uk.ac.ed.ph.jqtiplus.exception2.TemplateProcessingInterrupt;
import uk.ac.ed.ph.jqtiplus.internal.util.ConstraintUtilities;
import uk.ac.ed.ph.jqtiplus.node.AssessmentObject;
import uk.ac.ed.ph.jqtiplus.node.content.ItemBody;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.CorrectResponse;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.EndAttemptInteraction;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.Interaction;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.Shuffleable;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.choice.Choice;
import uk.ac.ed.ph.jqtiplus.node.item.response.declaration.ResponseDeclaration;
import uk.ac.ed.ph.jqtiplus.node.item.response.processing.ResponseProcessing;
import uk.ac.ed.ph.jqtiplus.node.item.template.declaration.TemplateDeclaration;
import uk.ac.ed.ph.jqtiplus.node.item.template.processing.TemplateProcessing;
import uk.ac.ed.ph.jqtiplus.node.item.template.processing.TemplateProcessingRule;
import uk.ac.ed.ph.jqtiplus.node.outcome.declaration.OutcomeDeclaration;
import uk.ac.ed.ph.jqtiplus.node.result.ItemResult;
import uk.ac.ed.ph.jqtiplus.node.result.OutcomeVariable;
import uk.ac.ed.ph.jqtiplus.node.result.ResponseVariable;
import uk.ac.ed.ph.jqtiplus.node.result.SessionStatus;
import uk.ac.ed.ph.jqtiplus.node.result.TemplateVariable;
import uk.ac.ed.ph.jqtiplus.node.shared.VariableDeclaration;
import uk.ac.ed.ph.jqtiplus.node.shared.VariableType;
import uk.ac.ed.ph.jqtiplus.node.shared.declaration.DefaultValue;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentTest;
import uk.ac.ed.ph.jqtiplus.node.test.TemplateDefault;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentObject;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentTest;
import uk.ac.ed.ph.jqtiplus.resolution.RootObjectLookup;
import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.types.ResponseData;
import uk.ac.ed.ph.jqtiplus.value.BooleanValue;
import uk.ac.ed.ph.jqtiplus.value.NullValue;
import uk.ac.ed.ph.jqtiplus.value.NumberValue;
import uk.ac.ed.ph.jqtiplus.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage: one-shot, not thread safe.
 * FIXME: Document this!
 * 
 * @author David McKain
 */
public final class ItemSessionController {

    private static final Logger logger = LoggerFactory.getLogger(ItemSessionController.class);

    /** TODO: Make this settable! */
    public static int MAX_TEMPLATE_PROCESSING_TRIES = 100;

    private final JqtiExtensionManager jqtiExtensionManager;
    private final ResolvedAssessmentItem resolvedAssessmentItem;
    private final AssessmentItem item;
    private final ItemSessionState itemState;
    
    public ItemSessionController(ResolvedAssessmentItem resolvedAssessmentItem, ItemSessionState assessmentItemState) {
        this(null, resolvedAssessmentItem, assessmentItemState);
    }

    public ItemSessionController(JqtiExtensionManager jqtiExtensionManager, ResolvedAssessmentItem resolvedAssessmentItem, ItemSessionState assessmentItemState) {
        ConstraintUtilities.ensureNotNull(resolvedAssessmentItem, "resolvedAssessmentItem");
        ConstraintUtilities.ensureNotNull(assessmentItemState, "assessmentItemState");
        this.jqtiExtensionManager = jqtiExtensionManager;
        this.resolvedAssessmentItem = resolvedAssessmentItem;
        this.item = resolvedAssessmentItem.getItemLookup().extractAssumingSuccessful();
        this.itemState = assessmentItemState;
    }
    
    public JqtiExtensionManager getJqtiExtensionManager() {
        return jqtiExtensionManager;
    }

    public ResolvedAssessmentItem getResolvedAssessmentItem() {
        return resolvedAssessmentItem;
    }

    public AssessmentItem getItem() {
        return item;
    }

    public ItemSessionState getItemState() {
        return itemState;
    }

    //-------------------------------------------------------------------
    // Initialization & template processing
    
    public void initialize() throws RuntimeValidationException {
        initialize(null);
    }

    /**
     * Initialise the item by setting the template defaults, resetting variables,
     * and performing templateProcessing.
     * An item should only be initialised if it is going to be rendered/presented
     * 
     * @param templateDefaults given templateDefaults values
     * @throws RuntimeValidationException if a runtime validation error is detected during template
     *             processing.
     */
    public void initialize(List<TemplateDefault> templateDefaults) throws RuntimeValidationException {
        /* (We only allow initialization once. This contrasts with the original JQTI.) */
        if (itemState.isInitialized()) {
            throw new IllegalStateException("Item state has already been initialized");
        }

        final ItemProcessingContext context = new ItemProcessingContextImpl();

        fireLifecycleEvent(LifecycleEventType.ITEM_INITIALISATION_STARTING);
        try {
            /* Set up built-in variables */
            itemState.setCompletionStatus(AssessmentItem.VALUE_ITEM_IS_NOT_ATTEMPTED);
            itemState.setNumAttempts(0);

            /* Perform template processing as many times as required. */
            int templateProcessingAttemptNumber = 0;
            boolean templateProcessingCompleted = false;
            while (!templateProcessingCompleted) {
                templateProcessingCompleted = doTemplateProcessing(context, templateDefaults, ++templateProcessingAttemptNumber);
            }

            /* Initialises outcomeDeclaration's values. */
            for (final OutcomeDeclaration outcomeDeclaration : item.getOutcomeDeclarations()) {
                initValue(outcomeDeclaration);
            }

            /* Initialises responseDeclaration's values. */
            for (final ResponseDeclaration responseDeclaration : item.getResponseDeclarations()) {
                initValue(responseDeclaration);
            }

            /* Set the completion status to unknown */
            itemState.setCompletionStatus(AssessmentItem.VALUE_ITEM_IS_UNKNOWN);

            /* Initialize all interactions in the itemBody */
            for (final Interaction interaction : item.getItemBody().getInteractions()) {
                interaction.initialize(this);
            }

            itemState.setInitialized(true);
        }
        finally {
            fireLifecycleEvent(LifecycleEventType.ITEM_INITIALISATION_FINISHED);
        }
    }

    private boolean doTemplateProcessing(ItemProcessingContext context, List<TemplateDefault> templateDefaults, int attemptNumber)
            throws RuntimeValidationException {
        logger.debug("Template Processing attempt #{} starting", attemptNumber);

        /* Initialise template defaults with any externally provided defaults */
        if (templateDefaults != null) {
            logger.trace("Setting template default values");
            for (final TemplateDefault templateDefault : templateDefaults) {
                final TemplateDeclaration declaration = item.getTemplateDeclaration(templateDefault.getTemplateIdentifier());
                if (declaration != null) {
                    final Value defaultValue = templateDefault.evaluate(context);
                    itemState.setOverriddenTemplateDefaultValue(declaration.getIdentifier(), defaultValue);
                }
            }
        }

        /* Initialise template values. */
        for (final TemplateDeclaration templateDeclaration : item.getTemplateDeclarations()) {
            initValue(templateDeclaration);
        }

        if (attemptNumber > MAX_TEMPLATE_PROCESSING_TRIES) {
            logger.debug("Exceeded maxmimum number of template processing retries - leaving variables at default values");
            return true;
        }

        /* Perform templateProcessing. */
        final TemplateProcessing templateProcessing = item.getTemplateProcessing();
        if (templateProcessing != null) {
            logger.trace("Evaluating template processing rules");
            try {
                for (final TemplateProcessingRule templateProcessingRule : templateProcessing.getTemplateProcessingRules()) {
                    templateProcessingRule.evaluate(context);
                }
            }
            catch (final TemplateProcessingInterrupt e) {
                switch (e.getInterruptType()) {
                    case EXIT_TEMPLATE:
                        /* Exit template processing */
                        logger.trace("Template processing interrupted by exitTemplate");
                        return true;

                    case TEMPLATE_CONSTRAINT_FAILURE:
                        /* Failed templateCondition, so try again. */
                        logger.trace("Template processing interrupted by failed templateConstraint");
                        return false;

                    default:
                        break;
                }
            }
        }
        return true;
    }

    //-------------------------------------------------------------------
    // Response processing

    /**
     * Binds response variables for this assessmentItem, returning a List of response
     * variable identifiers for whom the given data could not be successfully bound.
     * <p>
     * This will modify {@link #itemState}
     * 
     * @return a List of identifiers corresponding to response variables which could not be
     *         successfully bound from the provided data. (E.g. expected a float but got a String)
     * @param responseMap Map of responses to set, keyed on response variable identifier
     * 
     * @throws QtiParseException if one of the response identifiers is not a valid identifier
     * @throws IllegalArgumentException if responseMap is null, contains a null value, or if
     *   any key fails to map to an interaction
     */
    public List<Identifier> bindResponses(Map<String, ResponseData> responseMap) {
        ConstraintUtilities.ensureNotNull(responseMap, "responseMap");
        logger.debug("Binding responses {}", responseMap);
        
        /* First set all responses bound to <endAttemptInteractions> to false initially.
         * These may be overridden for responses to the presented interactions below.
         * 
         * (The spec seems to indicate that ALL responses bound to these interactions
         * should be set, which is why we have this special code here.)
         */
        final ItemBody itemBody = item.getItemBody();
        for (final EndAttemptInteraction endAttemptInteraction : itemBody.search(EndAttemptInteraction.class)) {
            itemState.setResponseValue(endAttemptInteraction, BooleanValue.FALSE);
        }

        /* Now bind response values for each incoming response. (Note that this may be a subset
         * of all responses, since adaptive items will only present certain interactions at certain
         * times.) */
        final List<Identifier> badResponses = new ArrayList<Identifier>();
        for (final Entry<String, ResponseData> responseEntry : responseMap.entrySet()) {
            Identifier responseIdentifier = new Identifier(responseEntry.getKey());
            ResponseData responseData = responseEntry.getValue();
            ConstraintUtilities.ensureNotNull(responseData, "responseMap entry for key " + responseIdentifier);
            try {
                final Interaction interaction = itemBody.getInteraction(responseIdentifier);
                if (interaction != null) {
                    interaction.bindResponse(this, responseData);
                }
                else {
                    throw new IllegalArgumentException("No interaction found for response identifier " + responseIdentifier);
                }
            }
            catch (final ResponseBindingException e) {
                badResponses.add(responseIdentifier);
            }
        }
        return badResponses;
    }
    
    /**
     * Validates the currently-bound responses for each of the interactions
     * 
     * @return a List of identifiers corresponding to invalid responses. The List will be
     *         empty if all responses were valid.
     */
    public List<Identifier> validateResponses() {
        logger.debug("Validating responses");
        final List<Identifier> invalidResponseIdentifiers = new ArrayList<Identifier>();
        for (final Interaction interaction : item.getItemBody().getInteractions()) {
            final Value responseValue = itemState.getResponseValue(interaction);
            if (!interaction.validateResponse(this, responseValue)) {
                invalidResponseIdentifiers.add(interaction.getResponseIdentifier());
            }
        }
        return invalidResponseIdentifiers;
    }
    
    /**
     * Runs response processing on the currently bound responses, changing {@link #itemState}
     * as appropriate.
     * 
     * @throws RuntimeValidationException
     */
    public void processResponses() throws RuntimeValidationException {
        final ItemProcessingContext processingContext = new ItemProcessingContextImpl();
        logger.debug("Response processing starting");
        fireLifecycleEvent(LifecycleEventType.ITEM_RESPONSE_PROCESSING_STARTING);
        try {
            /* We always count the attempt, unless the response was to an endAttemptInteraction
             * with countAttempt set to false.
             */
            boolean countAttempt = true;
            for (final Interaction interaction : item.getItemBody().getInteractions()) {
                if (interaction instanceof EndAttemptInteraction) {
                    final EndAttemptInteraction endAttemptInteraction = (EndAttemptInteraction) interaction;
                    final BooleanValue value = (BooleanValue) itemState.getResponseValue(interaction);
                    if (value != null && value.booleanValue() == true) {
                        countAttempt = !Boolean.FALSE.equals(endAttemptInteraction.getCountAttempt());
                        break;
                    }
                }
            }
            if (countAttempt) {
                final int oldAttempts = itemState.getNumAttempts();
                itemState.setNumAttempts(oldAttempts + 1);
            }

            if (!item.getAdaptive()) {
                for (final OutcomeDeclaration outcomeDeclaration : item.getOutcomeDeclarations()) {
                    initValue(outcomeDeclaration);
                }
            }

            ResponseProcessing responseProcessing = null;
            final RootObjectLookup<ResponseProcessing> resolvedResponseProcessingTemplateLookup = resolvedAssessmentItem.getResolvedResponseProcessingTemplateLookup();
            if (resolvedResponseProcessingTemplateLookup!=null) {
                responseProcessing = resolvedResponseProcessingTemplateLookup.extractAssumingSuccessful();
            }
            else {
                responseProcessing = item.getResponseProcessing();
            }
            
            if (responseProcessing != null) {
                responseProcessing.evaluate(processingContext);
            }
            else {
                logger.debug("No responseProcessing rules or responseProcessing template exists, so no processing will be performed");
            }
        }
        finally {
            logger.debug("Response processing finished");
            fireLifecycleEvent(LifecycleEventType.ITEM_RESPONSE_PROCESSING_FINISHED);
        }
    }


    //-------------------------------------------------------------------
    // Shuffle callbacks (from interactions)

    public <C extends Choice> void shuffleInteractionChoiceOrder(final Interaction interaction, final List<C> choiceList) {
        final List<List<C>> choiceLists = new ArrayList<List<C>>();
        choiceLists.add(choiceList);
        shuffleInteractionChoiceOrders(interaction, choiceLists);
    }

    public <C extends Choice> void shuffleInteractionChoiceOrders(final Interaction interaction, final List<List<C>> choiceLists) {
        if (interaction instanceof Shuffleable) {
            if (((Shuffleable) interaction).getShuffle().booleanValue()) {
                final List<Identifier> choiceIdentifiers = new ArrayList<Identifier>();
                for (final List<C> choiceList : choiceLists) {
                    final List<Identifier> shuffleableChoiceIdentifiers = new ArrayList<Identifier>();

                    /* Build up sortable identifiers */
                    for (int i = 0; i < choiceList.size(); i++) {
                        final C choice = choiceList.get(i);
                        if (!choice.getFixed()) {
                            shuffleableChoiceIdentifiers.add(choice.getIdentifier());
                        }
                    }

                    /* Perform shuffle */
                    Collections.shuffle(shuffleableChoiceIdentifiers);

                    /* Then merge fixed identifiers back in */
                    for (int i = 0, sortedIndex = 0; i < choiceList.size(); i++) {
                        final C choice = choiceList.get(i);
                        if (choice.getFixed()) {
                            choiceIdentifiers.add(choice.getIdentifier());
                        }
                        else {
                            choiceIdentifiers.add(shuffleableChoiceIdentifiers.get(sortedIndex++));
                        }
                    }
                }
                itemState.setShuffledInteractionChoiceOrder(interaction, choiceIdentifiers);
            }
            else {
                itemState.setShuffledInteractionChoiceOrder(interaction, null);
            }
        }
    }

    //-------------------------------------------------------------------

    public Value lookupVariable(VariableDeclaration variableDeclaration) {
        ConstraintUtilities.ensureNotNull(variableDeclaration);
        return lookupVariable(variableDeclaration.getIdentifier());
    }

    public Value lookupVariable(Identifier identifier) {
        ConstraintUtilities.ensureNotNull(identifier);
        return itemState.getValue(identifier);
    }

    public Value lookupVariable(Identifier identifier, VariableType... permittedTypes) {
        ConstraintUtilities.ensureNotNull(identifier);
        Value value = null;
        for (final VariableType type : permittedTypes) {
            switch (type) {
                case TEMPLATE:
                    value = itemState.getTemplateValue(identifier);
                    break;

                case RESPONSE:
                    value = itemState.getResponseValue(identifier);
                    break;

                case OUTCOME:
                    value = itemState.getOutcomeValue(identifier);
                    break;

                default:
                    throw new QtiLogicException("Unexpected switch case");
            }
        }
        return value;
    }

    private void initValue(VariableDeclaration declaration) {
        ConstraintUtilities.ensureNotNull(declaration);
        itemState.setValue(declaration, computeInitialValue(declaration));
    }

    private Value computeInitialValue(Identifier identifier) {
        return computeDefaultValue(identifier);
    }

    private Value computeInitialValue(VariableDeclaration declaration) {
        ConstraintUtilities.ensureNotNull(declaration);
        return computeInitialValue(declaration.getIdentifier());
    }

    /* DM: This copes with defaults and overridden values */
    public Value computeDefaultValue(Identifier identifier) {
        ConstraintUtilities.ensureNotNull(identifier);
        return computeDefaultValue(ensureVariableDeclaration(identifier));
    }

    public Value computeDefaultValue(VariableDeclaration declaration) {
        ConstraintUtilities.ensureNotNull(declaration);
        Value result = itemState.getOverriddenDefaultValue(declaration);
        if (result == null) {
            final DefaultValue defaultValue = declaration.getDefaultValue();
            if (defaultValue != null) {
                result = defaultValue.evaluate();
            }
            else {
                result = NullValue.INSTANCE;
            }
        }
        return result;
    }

    private VariableDeclaration ensureVariableDeclaration(Identifier identifier) {
        ConstraintUtilities.ensureNotNull(identifier);
        final VariableDeclaration result = item.getVariableDeclaration(identifier);
        if (result == null) {
            throw new QtiEvaluationException("Item variable with identifier " + identifier + " is not defined");
        }
        return result;
    }

    private ResponseDeclaration ensureResponseDeclaration(Identifier responseIdentifier) {
        ConstraintUtilities.ensureNotNull(responseIdentifier);
        final ResponseDeclaration result = item.getResponseDeclaration(responseIdentifier);
        if (result == null) {
            throw new QtiEvaluationException("Response variable with identifier " + responseIdentifier + " is not defined");
        }
        return result;
    }

    public Value computeCorrectResponse(Identifier identifier) {
        ConstraintUtilities.ensureNotNull(identifier);
        return computeCorrectResponse(ensureResponseDeclaration(identifier));
    }

    public Value computeCorrectResponse(ResponseDeclaration declaration) {
        ConstraintUtilities.ensureNotNull(declaration);
        Value result = itemState.getOverriddenCorrectResponseValue(declaration);
        if (result == null) {
            final CorrectResponse correctResponse = declaration.getCorrectResponse();
            if (correctResponse != null) {
                result = correctResponse.evaluate();
            }
            else {
                result = NullValue.INSTANCE;
            }
        }
        return result;
    }

    /**
     * Returns true if this declarations value matches its correctValue.
     * Returns null if there is no correct value
     * NOTE: This only tests for "the" "correct" response, not "a" correct response.
     * 
     * @return true if the associated correctResponse matches the value; false or null otherwise.
     */
    public Boolean isCorrectResponse(ResponseDeclaration responseDeclaration) {
        final Value correctResponseValue = computeCorrectResponse(responseDeclaration);
        if (correctResponseValue.isNull()) {
            return null;
        }
        return correctResponseValue.equals(itemState.getValue(responseDeclaration));
    }

    //-------------------------------------------------------------------

    private void fireLifecycleEvent(LifecycleEventType eventType) {
        if (jqtiExtensionManager!=null) {
            for (final JqtiExtensionPackage extensionPackage : jqtiExtensionManager.getExtensionPackages()) {
                extensionPackage.lifecycleEvent(this, eventType);
            }
        }
    }

    //-------------------------------------------------------------------
    // Computes standalone ItemResult for this item. This wasn't available in the original JQTI

    public ItemResult computeItemResult() {
        final ItemResult result = new ItemResult(null);
        result.setIdentifier(item.getIdentifier());
        result.setDateStamp(new Date());
        result.setSessionStatus(itemState.getNumAttempts() > 0 ? SessionStatus.FINAL : SessionStatus.INITIAL); // TODO: Not really sure what's best here, but probably not important!
        recordItemVariables(result);
        return result;
    }

    void recordItemVariables(ItemResult result) {
        result.getItemVariables().clear();
        for (final Entry<Identifier, Value> mapEntry : itemState.getOutcomeValues().entrySet()) {
            final OutcomeDeclaration declaration = item.getOutcomeDeclaration(mapEntry.getKey());
            final Value value = mapEntry.getValue();
            final OutcomeVariable variable = new OutcomeVariable(result, declaration, value);
            result.getItemVariables().add(variable);
        }
        for (final Entry<Identifier, Value> mapEntry : itemState.getResponseValues().entrySet()) {
            final ResponseDeclaration declaration = item.getResponseDeclaration(mapEntry.getKey());
            final Value value = mapEntry.getValue();
            List<Identifier> interactionChoiceOrder = null;
            final Interaction interaction = item.getItemBody().getInteraction(declaration.getIdentifier());
            if (interaction != null && interaction instanceof Shuffleable) {
                interactionChoiceOrder = itemState.getShuffledInteractionChoiceOrder(interaction);
            }
            final ResponseVariable variable = new ResponseVariable(result, declaration, value, interactionChoiceOrder);
            result.getItemVariables().add(variable);
        }
        for (final Entry<Identifier, Value> mapEntry : itemState.getTemplateValues().entrySet()) {
            final TemplateDeclaration declaration = item.getTemplateDeclaration(mapEntry.getKey());
            final Value value = mapEntry.getValue();
            final TemplateVariable variable = new TemplateVariable(result, declaration, value);
            result.getItemVariables().add(variable);
        }
    }

    //-------------------------------------------------------------------

    /**
     * FIXME-DM: THIS LOGIC IS PROBABLY WRONG!!! Judging whether a response is correct
     * is in general not simply a case of comparing with <correctResponse/>
     * Returns true if this item reference was correctly responded;
     * Correctly responded means ALL defined responseVars match their associated correctResponse.
     * Returns null if any of the responseDeclarations don't have correctResponses.
     * 
     * @return true if this item reference was correctly responded; null if not all
     *         responseDeclarations contain correctResponses; false otherwise
     * @see #isIncorrect
     */
    public Boolean isCorrect() {
        for (final ResponseDeclaration responseDeclaration : item.getResponseDeclarations()) {
            if (responseDeclaration.getCorrectResponse() == null) {
                return null;
            }
        }
        for (final ResponseDeclaration responseDeclaration : item.getResponseDeclarations()) {
            if (!Boolean.TRUE.equals(responseDeclaration.isCorrectResponse(this))) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    /**
     * FIXME-DM: THIS LOGIC IS PROBABLY WRONG!!! Judging whether a response is correct
     * is in general not simply a case of comparing with <correctResponse/>
     * Returns the number of correct responses
     * 
     * @return the number of correct responses
     * @see #countIncorrect
     */
    public int countCorrect() {
        int count = 0;
        for (final ResponseDeclaration responseDeclaration : item.getResponseDeclarations()) {
            if (Boolean.TRUE.equals(responseDeclaration.isCorrectResponse(this))) {
                count++;
            }
        }
        return count;
    }

    /**
     * FIXME-DM: THIS LOGIC IS PROBABLY WRONG!!! Judging whether a response is correct
     * is in general not simply a case of comparing with <correctResponse/>
     * Returns true if this item reference was incorrectly responded;
     * Incorrectly responded means ANY defined responseVars didn't match their
     * associated correctResponse.
     * Returns null if any of the responseDeclarations don't have correctResponses.
     * 
     * @return true if this item reference was incorrectly responded; null if not all
     *         responseDeclarations contain correctResponses; false otherwise
     * @see #isCorrect
     */
    public Boolean isIncorrect() {
        for (final ResponseDeclaration responseDeclaration : item.getResponseDeclarations()) {
            if (responseDeclaration.getCorrectResponse() == null) {
                return null;
            }
        }
        for (final ResponseDeclaration responseDeclaration : item.getResponseDeclarations()) {
            if (!Boolean.TRUE.equals(responseDeclaration.isCorrectResponse(this))) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * FIXME-DM: THIS LOGIC IS PROBABLY WRONG!!! Judging whether a response is correct
     * is in general not simply a case of comparing with <correctResponse/>
     * Returns the number of incorrect responses
     * 
     * @return the number of incorrect responses
     * @see #countIncorrect
     */
    public int countIncorrect() {
        int count = 0;
        for (final ResponseDeclaration responseDeclaration : item.getResponseDeclarations()) {
            if (!Boolean.TRUE.equals(responseDeclaration.isCorrectResponse(this))) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode()
                + "(resolvedAssessmentItem=" + resolvedAssessmentItem
                + ",itemState=" + itemState
                + ")";
    }

    //---------------------------------------------------

    /**
     * Callback implementation of {@link ItemProcessingContext}
     */
    protected class ItemProcessingContextImpl implements ItemProcessingContext {

        public ItemProcessingContextImpl() {
        }
        
        @Override
        public boolean isItem() {
            return true;
        }

        @Override
        public boolean isTest() {
            return false;
        }

        @Override
        public ResolvedAssessmentObject<?> getResolvedAssessmentObject() {
            return resolvedAssessmentItem;
        }

        @Override
        public ResolvedAssessmentItem getResolvedAssessmentItem() {
            return resolvedAssessmentItem;
        }

        @Override
        public ResolvedAssessmentTest getResolvedAssessmentTest() {
            return null;
        }

        @Override
        public AssessmentObject getSubject() {
            return item;
        }
        
        @Override
        public AssessmentTest getSubjectTest() {
            return null;
        }
        
        @Override
        public AssessmentItem getSubjectItem() {
            return item;
        }

        @Override
        public void setTemplateValue(TemplateDeclaration variableDeclaration, Value value) {
            ConstraintUtilities.ensureNotNull(variableDeclaration);
            ConstraintUtilities.ensureNotNull(value);
            itemState.setTemplateValue(variableDeclaration, value);
        }

        @Override
        public void setOutcomeValue(OutcomeDeclaration variableDeclaration, Value value) {
            ConstraintUtilities.ensureNotNull(variableDeclaration);
            ConstraintUtilities.ensureNotNull(value);
            itemState.setOutcomeValue(variableDeclaration, value);
        }

        @Override
        public void setOutcomeValueFromLookupTable(OutcomeDeclaration outcomeDeclaration, NumberValue value) {
            ConstraintUtilities.ensureNotNull(outcomeDeclaration);
            ConstraintUtilities.ensureNotNull(value);
            itemState.setOutcomeValueFromLookupTable(outcomeDeclaration, value);
        }

        @Override
        public void setVariableValue(VariableDeclaration variableDeclaration, Value value) {
            ConstraintUtilities.ensureNotNull(variableDeclaration);
            ConstraintUtilities.ensureNotNull(value);
            itemState.setValue(variableDeclaration, value);
        }

        @Override
        public void setOverriddenResponseDefaultValue(ResponseDeclaration responseDeclaration, Value value) {
            ConstraintUtilities.ensureNotNull(responseDeclaration);
            ConstraintUtilities.ensureNotNull(value);
            itemState.setOverriddenResponseDefaultValue(responseDeclaration, value);
        }

        @Override
        public void setOverriddenCorrectResponseValue(ResponseDeclaration responseDeclaration, Value value) {
            ConstraintUtilities.ensureNotNull(responseDeclaration);
            ConstraintUtilities.ensureNotNull(value);
            itemState.setOverriddenCorrectResponseValue(responseDeclaration, value);
        }

        @Override
        public void setOverriddenOutcomeDefaultValue(OutcomeDeclaration outcomeDeclaration, Value value) {
            ConstraintUtilities.ensureNotNull(outcomeDeclaration);
            ConstraintUtilities.ensureNotNull(value);
            itemState.setOverriddenOutcomeDefaultValue(outcomeDeclaration, value);
        }

        @Override
        public Value lookupVariable(VariableDeclaration variableDeclaration) {
            return ItemSessionController.this.lookupVariable(variableDeclaration);
        }

        @Override
        public Value lookupVariable(Identifier identifier) {
            return ItemSessionController.this.lookupVariable(identifier);
        }

        @Override
        public Value lookupVariable(Identifier identifier, VariableType... permittedTypes) {
            return ItemSessionController.this.lookupVariable(identifier, permittedTypes);
        }

        @Override
        public Value computeDefaultValue(Identifier identifier) {
            return ItemSessionController.this.computeDefaultValue(identifier);
        }

        public Value computeDefaultValue(VariableDeclaration declaration) {
            return ItemSessionController.this.computeDefaultValue(declaration);
        }

        @Override
        public Value computeCorrectReponse(Identifier responseIdentifier) {
            ConstraintUtilities.ensureNotNull(responseIdentifier);
            return ItemSessionController.this.computeCorrectResponse(ensureResponseDeclaration(responseIdentifier));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + hashCode()
                    + "(controller=" + ItemSessionController.this
                    + ")";
        }
    }
}
