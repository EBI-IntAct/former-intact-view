/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
 *
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
package uk.ac.ebi.intact.editor.controller.curate.participant;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.SelectableDataModelWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.model.impl.DefaultStoichiometry;
import psidev.psi.mi.jami.utils.AliasUtils;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.controller.curate.AnnotatedObjectController;
import uk.ac.ebi.intact.editor.controller.curate.UnsavedJamiChange;
import uk.ac.ebi.intact.editor.controller.curate.cloner.ParticipantJamiCloner;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ComplexController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ImportJamiCandidate;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ModelledParticipantImportController;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ModelledParticipantWrapper;
import uk.ac.ebi.intact.editor.util.SelectableCollectionDataModel;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.dao.CvTermDao;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.utils.IntactUtils;
import uk.ac.ebi.intact.model.AnnotatedObject;
import uk.ac.ebi.intact.model.CvTopic;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Modelled Participant controller.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class ModelledParticipantController extends AnnotatedObjectController {

    private static final Log log = LogFactory.getLog( ModelledParticipantController.class );

    private IntactModelledParticipant participant;

    private String interactor;
    private List<ImportJamiCandidate> interactorCandidates;

    private DataModel<IntactModelledFeature> featuresDataModel;
    private IntactModelledFeature[] selectedFeatures;

    /**
     * The AC of the participant to be loaded.
     */
    private String ac;

    @Autowired
    private ComplexController interactionController;

    private List<SelectItem> aliasTypeSelectItems;
    private List<SelectItem> participantTopicSelectItems;
    private List<SelectItem> databaseSelectItems;
    private List<SelectItem> qualifierSelectItems;
    private List<SelectItem> biologicalRoleSelectItems;

    public ModelledParticipantController() {
    }

    @Transactional(value = "jamiTransactionManager")
    public void loadData() {
        aliasTypeSelectItems = new ArrayList<SelectItem>();
        aliasTypeSelectItems.add(new SelectItem( null, "select type", "select type", false, false, true ));
        participantTopicSelectItems = new ArrayList<SelectItem>();
        participantTopicSelectItems.add(new SelectItem(null, "select topic", "select topic", false, false, true));
        databaseSelectItems = new ArrayList<SelectItem>();
        databaseSelectItems.add(new SelectItem( null, "select database", "select database", false, false, true ));
        qualifierSelectItems = new ArrayList<SelectItem>();
        qualifierSelectItems.add(new SelectItem( null, "select qualifier", "select qualifier", false, false, true ));
        biologicalRoleSelectItems = new ArrayList<SelectItem>();
        biologicalRoleSelectItems.add(new SelectItem(null, "select biological role", "select biological role", false, false, true));

        IntactDao intactDao = ApplicationContextProvider.getBean("intactDao");
        CvTermDao cvDao = intactDao.getCvTermDao();

        IntactCvTerm aliasTypeParent = cvDao.getByMIIdentifier("MI:0300", IntactUtils.ALIAS_TYPE_OBJCLASS);
        loadChildren(aliasTypeParent, aliasTypeSelectItems);

        IntactCvTerm participantTopicParent = cvDao.getByMIIdentifier("MI:0666", IntactUtils.TOPIC_OBJCLASS);
        loadChildren(participantTopicParent, participantTopicSelectItems);

        IntactCvTerm databaseParent = cvDao.getByMIIdentifier("MI:0473", IntactUtils.DATABASE_OBJCLASS);
        loadChildren(databaseParent, databaseSelectItems);

        IntactCvTerm qualifierParent = cvDao.getByMIIdentifier("MI:0353", IntactUtils.QUALIFIER_OBJCLASS);
        loadChildren(qualifierParent, qualifierSelectItems);

        IntactCvTerm bioRoleParent = cvDao.getByMIIdentifier("MI:0500", IntactUtils.BIOLOGICAL_ROLE_OBJCLASS);
        loadChildren(bioRoleParent, biologicalRoleSelectItems);
    }

    private void loadChildren(IntactCvTerm parent, List<SelectItem> selectItems){
        for (OntologyTerm child : parent.getChildren()){
            IntactCvTerm cv = (IntactCvTerm)child;
            SelectItem item = createSelectItem(cv);
            if (item != null){
                selectItems.add(item);
            }
            if (!cv.getChildren().isEmpty()){
                loadChildren(cv, selectItems);
            }
        }
    }

    private SelectItem createSelectItem( IntactCvTerm cv ) {
        if (!AnnotationUtils.collectAllAnnotationsHavingTopic(cv.getAnnotations(), null, "hidden").isEmpty()){
            boolean obsolete = AnnotationUtils.collectAllAnnotationsHavingTopic(cv.getAnnotations(), CvTopic.OBSOLETE_MI_REF, CvTopic.OBSOLETE).isEmpty();
            return new SelectItem( cv, cv.getShortName()+((obsolete? " (obsolete)" : "")), cv.getFullName());
        }
        return null;
    }

    @Override
    public AnnotatedObject getAnnotatedObject() {
        return null;
    }

    @Override
    public IntactPrimaryObject getJamiObject() {
        return this.participant;
    }

    @Override
    public void setJamiObject(IntactPrimaryObject annotatedObject) {
        this.participant = (IntactModelledParticipant)annotatedObject;
    }

    @Override
    public void setAnnotatedObject(AnnotatedObject annotatedObject) {
        // do nothing
    }

    public List<SelectItem> getAliasTypeSelectItems() {
        return aliasTypeSelectItems;
    }

    public void setAliasTypeSelectItems(List<SelectItem> aliasTypeSelectItems) {
        this.aliasTypeSelectItems = aliasTypeSelectItems;
    }

    public List<SelectItem> getParticipantTopicSelectItems() {
        return participantTopicSelectItems;
    }

    public void setParticipantTopicSelectItems(List<SelectItem> participantTopicSelectItems) {
        this.participantTopicSelectItems = participantTopicSelectItems;
    }

    public List<SelectItem> getDatabaseSelectItems() {
        return databaseSelectItems;
    }

    public void setDatabaseSelectItems(List<SelectItem> databaseSelectItems) {
        this.databaseSelectItems = databaseSelectItems;
    }

    public List<SelectItem> getQualifierSelectItems() {
        return qualifierSelectItems;
    }

    public void setQualifierSelectItems(List<SelectItem> qualifierSelectItems) {
        this.qualifierSelectItems = qualifierSelectItems;
    }

    public List<SelectItem> getBiologicalRoleSelectItems() {
        return biologicalRoleSelectItems;
    }

    public void setBiologicalRoleSelectItems(List<SelectItem> biologicalRoleSelectItems) {
        this.biologicalRoleSelectItems = biologicalRoleSelectItems;
    }

    @Transactional(value = "jamiTransactionManager")
    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            loadData();
            if ( ac != null ) {
                if ( participant == null || !ac.equals( participant.getAc() ) ) {
                    IntactDao intactDao = ApplicationContextProvider.getBean("intactDao");
                    participant = loadJamiByAc(IntactModelledParticipant.class, ac);
                }
            } else {
                if ( participant != null ) ac = participant.getAc();
            }

            if (participant == null) {
                addErrorMessage("No participant with this AC", ac);
                return;
            }

            featuresDataModel = new SelectableDataModelWrapper(new SelectableCollectionDataModel<ModelledFeature>(participant.getFeatures()), participant.getFeatures());

            if (participant.getInteraction() != null){
                if (interactionController.getComplex() == null || !interactionController.getComplex().getAc().equalsIgnoreCase(((IntactComplex) participant.getInteraction()).getAc())){
                    if( interactionController.getComplex() == null ) {
                        interactionController.setComplex((IntactComplex) participant.getInteraction());
                    }
                }
            }

            if (participant.getInteractor() != null) {
                interactor = participant.getInteractor().getShortName();
            }

            refreshTabsAndFocusXref();
        }

        generalLoadChecks();
    }

    @Override
    public void doPreSave() {
        // create master proteins from the unsaved manager
        final List<UnsavedJamiChange> transcriptCreated = getChangesController().getAllUnsavedJamiProteinTranscripts();

        for (UnsavedJamiChange unsaved : transcriptCreated) {
            IntactPrimaryObject transcript = unsaved.getUnsavedObject();

            String currentAc = participant != null ? participant.getAc() : null;

            // the object to save is different from the current object. Checks that the scope of this object to save is the ac of the current object being saved
            // if the scope is null or different, the object should not be saved at this stage because we only save the current object and changes associated with it
            // if current ac is null, no unsaved event should be associated with it as this object has not been saved yet
            if (unsaved.getScope() != null && unsaved.getScope().equals(currentAc)){
                getPersistenceController().doSaveJamiMasterProteins(transcript);
                getChangesController().removeFromHiddenChanges(unsaved);
            }
            else if (unsaved.getScope() == null && currentAc == null){
                getPersistenceController().doSaveJamiMasterProteins(transcript);
                getChangesController().removeFromHiddenChanges(unsaved);
            }
        }

        // the participant is not persisted, we can add it to the list of components in the interaction
        if (participant.getAc() == null){
            interactionController.getComplex().getParticipants().add(participant);
        }
    }

    @Override
    public Collection<String> collectParentAcsOfCurrentAnnotatedObject(){
        Collection<String> parentAcs = new ArrayList<String>();

        if (participant.getInteraction() != null){
            addParentAcsTo(parentAcs, (IntactComplex)participant.getInteraction());
        }

        return parentAcs;
    }

    @Override
    protected void refreshUnsavedChangesBeforeRevert(){
        Collection<String> parentAcs = new ArrayList<String>();

        if (participant.getInteraction() != null){
            addParentAcsTo(parentAcs, (IntactComplex)participant.getInteraction());
        }

        getChangesController().revertModelledParticipant(participant, parentAcs);
    }


    /**
     * Add all the parent acs of this interaction
     * @param parentAcs
     * @param inter
     */
    protected void addParentAcsTo(Collection<String> parentAcs, IntactComplex inter) {
        if (inter.getAc() != null){
            parentAcs.add(inter.getAc());
        }
    }

    @Override
    public void doPostSave(){
        interactionController.refreshParticipants();
    }

    @Override
    protected IntactModelledParticipant cloneAnnotatedObject(IntactPrimaryObject ao) {
        // to be overrided
        return (IntactModelledParticipant) ParticipantJamiCloner.cloneParticipant((IntactModelledParticipant) ao);
    }

    public boolean isInteractorSet(IntactModelledParticipant p){
        return !p.getInteractor().getShortName().equals("unspecified");
    }

    public String newParticipant(IntactComplex interaction) {
        this.interactor = null;

        IntactDao intactDao = ApplicationContextProvider.getBean("intactDao");
        CvTermDao cvObjectService = intactDao.getCvTermDao();

        CvTerm defaultBiologicalRole = cvObjectService.getByMIIdentifier(Participant.UNSPECIFIED_ROLE_MI, IntactUtils.BIOLOGICAL_ROLE_OBJCLASS);

        IntactModelledParticipant participant = new IntactModelledParticipant(new IntactInteractor("unspecified"));
        participant.setBiologicalRole(defaultBiologicalRole);
        participant.setStoichiometry(new DefaultStoichiometry((int)getEditorConfig().getDefaultStoichiometry()));

        // by setting the interaction of a participant, we don't add the participant to the collection of participants for this interaction so if we revert, it will not affect anything.
        // when saving, it will be added to the list of participants for this interaction. we just have to refresh the list of participants
        participant.setInteraction(interaction);

        setParticipant(participant);

        changed();

        return navigateToObject(participant);
    }

    public void importInteractor(ActionEvent evt) {
        ModelledParticipantImportController participantImportController = (ModelledParticipantImportController) getSpringContext().getBean("modelledParticipantImportController");
        interactorCandidates = new ArrayList<ImportJamiCandidate>(participantImportController.importParticipant(interactor));

        if (interactorCandidates.size() == 1) {
            interactorCandidates.get(0).setSelected(true);
        }
    }

    public void addInteractorToParticipant(ActionEvent evt) {
        for (ImportJamiCandidate importCandidate : interactorCandidates) {
            if (importCandidate.isSelected()) {
                // chain or isoform, we may have to update it later
                if (importCandidate.isChain() || importCandidate.isIsoform()){
                    Collection<String> parentAcs = new ArrayList<String>();

                    if (participant.getInteraction() != null){
                        addParentAcsTo(parentAcs, (IntactComplex)participant.getInteraction());
                    }
                }
                participant.setInteractor(importCandidate.getInteractor());

                // if the participant is a new participant, we don't need to add a unsaved notice because one already exists for creating a new participant
                if (participant.getAc() != null){
                    setUnsavedChanges(true);
                }
            }
        }
    }

    public void markFeatureToDelete(ModelledFeature feature) {
        IntactModelledFeature intactFeature = (IntactModelledFeature)feature;
        // don't forget to unlink features first
        if (intactFeature.getAc() == null) {
            participant.removeFeature(feature);
        } else {
            getChangesController().markToDelete(intactFeature, participant);
        }
    }

    public void deleteSelectedFeatures(ActionEvent evt) {
        for (IntactModelledFeature feature : selectedFeatures) {
            markFeatureToDelete(feature);
        }

        addInfoMessage("Features to be deleted", selectedFeatures.length+" have been marked to be deleted");
    }

    public String getAc() {
        if ( ac == null && participant != null ) {
            return participant.getAc();
        }
        return ac;
    }

    public void setAc( String ac ) {
        this.ac = ac;
    }

    public int getMinStoichiometry(){
        return this.participant.getStoichiometry() != null ? this.participant.getStoichiometry().getMinValue() : 0;
    }

    public int getMaxStoichiometry(){
        return this.participant.getStoichiometry() != null ? this.participant.getStoichiometry().getMaxValue() : 0;
    }

    public void setMinStoichiometry(int stc){
        if (this.participant.getStoichiometry() == null){
            this.participant.setStoichiometry(new IntactStoichiometry(stc));
        }
        else {
            IntactStoichiometry stoichiometry = (IntactStoichiometry)participant.getStoichiometry();
            this.participant.setStoichiometry(new IntactStoichiometry(stc, stoichiometry.getMaxValue()));
        }
    }

    public void setMaxStoichiometry(int stc){
        if (this.participant.getStoichiometry() == null){
            this.participant.setStoichiometry(new IntactStoichiometry(stc));
        }
        else {
            IntactStoichiometry stoichiometry = (IntactStoichiometry)participant.getStoichiometry();
            this.participant.setStoichiometry(new IntactStoichiometry(stoichiometry.getMinValue(), stc));
        }
    }

    public IntactModelledParticipant getParticipant() {
        return participant;
    }

    public ModelledParticipantWrapper getModelledParticipantWrapper() {
        return new ModelledParticipantWrapper( participant, getChangesController(), interactionController );
    }

    @Override
    public void refreshTabsAndFocusXref(){
        refreshTabs();
    }

    @Override
    public void refreshTabs(){
        super.refreshTabsAndFocusXref();
    }

    public void setParticipant( IntactModelledParticipant participant ) {
        this.participant = participant;

        if (participant != null){
            this.ac = participant.getAc();
        }
    }

    public String getAuthorGivenName() {
        psidev.psi.mi.jami.model.Alias author = AliasUtils.collectFirstAliasWithType(this.participant.getAliases(), Alias.AUTHOR_ASSIGNED_NAME_MI, Alias.AUTHOR_ASSIGNED_NAME);
        return author != null ? author.getName() : null;
    }

    public void setAuthorGivenName( String name ) {
        if (name == null){
            psidev.psi.mi.jami.utils.AliasUtils.removeAllAliasesWithType(this.participant.getAliases(), psidev.psi.mi.jami.model.Alias.AUTHOR_ASSIGNED_NAME_MI,
                    psidev.psi.mi.jami.model.Alias.AUTHOR_ASSIGNED_NAME);
        }
        else{
            psidev.psi.mi.jami.model.Alias alias = psidev.psi.mi.jami.utils.AliasUtils.collectFirstAliasWithType(this.participant.getAliases(), psidev.psi.mi.jami.model.Alias.AUTHOR_ASSIGNED_NAME_MI,
                    psidev.psi.mi.jami.model.Alias.AUTHOR_ASSIGNED_NAME);
            if (alias != null && alias.getName().equals(name)){
                ((AbstractIntactAlias)alias).setName(name);
                getChangesController().markAsUnsaved(participant);
            }
            else {
                this.participant.getAliases().add(new ModelledFeatureAlias(IntactUtils.createMIAliasType(psidev.psi.mi.jami.model.Alias.AUTHOR_ASSIGNED_NAME,
                        psidev.psi.mi.jami.model.Alias.AUTHOR_ASSIGNED_NAME_MI), name));
                getChangesController().markAsUnsaved(participant);
            }
        }
    }

    public String participantPrimaryId(IntactModelledParticipant component) {
        if (component == null) return null;
        if (component.getInteractor() == null) return null;

        final Xref xrefs = component.getInteractor().getPreferredIdentifier();

        if (xrefs == null && component.getInteractor() instanceof IntactInteractor) {
            String ac = ((IntactInteractor)component.getInteractor()).getAc();
            return ac != null ? ac : component.getInteractor().getShortName();
        }
        else if (xrefs == null){
            return component.getInteractor().getShortName();
        }

        return xrefs.getId();
    }

    private String joinIds(Collection<Xref> xrefs) {
        Collection<String> ids = new ArrayList<String>(xrefs.size());

        for (Xref xref : xrefs) {
            ids.add(xref.getId());
        }

        return StringUtils.join(ids, ", ");
    }

    public String getInteractor() {
        return interactor;
    }

    public void setInteractor(String interactor) {
        this.interactor = interactor;
    }

    public List<ImportJamiCandidate> getInteractorCandidates() {
        return interactorCandidates;
    }

    public void setInteractorCandidates(List<ImportJamiCandidate> interactorCandidates) {
        this.interactorCandidates = interactorCandidates;
    }

    public IntactModelledFeature[] getSelectedFeatures() {
        return selectedFeatures;
    }

    public void setSelectedFeatures(IntactModelledFeature[] selectedFeatures) {
        this.selectedFeatures = selectedFeatures;
    }

    public DataModel<IntactModelledFeature> getFeaturesDataModel() {
        return featuresDataModel;
    }

    @Override
    public void modifyClone(AnnotatedObject clone) {
        refreshTabs();
    }

    @Override
    public void newXref(ActionEvent evt) {
        this.participant.getXrefs().add(new ModelledParticipantXref());
        setUnsavedChanges(true);
    }

    @Override
    public void newAnnotation(ActionEvent evt) {
        this.participant.getAnnotations().add(new ModelledParticipantAnnotation());
        setUnsavedChanges(true);
    }

    @Override
    public void newAlias(ActionEvent evt) {
        this.participant.getAliases().add(new ModelledParticipantAlias());
        setUnsavedChanges(true);
    }

    @Override
    public String getCautionMessage() {
        psidev.psi.mi.jami.model.Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.participant.getAnnotations(), psidev.psi.mi.jami.model.Annotation.CAUTION_MI,
                psidev.psi.mi.jami.model.Annotation.CAUTION);
        return caution != null ? caution.getValue() : null;
    }

    @Override
    public String getJamiCautionMessage(IntactPrimaryObject ao) {
        IntactModelledParticipant participant = (IntactModelledParticipant)ao;
        psidev.psi.mi.jami.model.Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(participant.getAnnotations(), psidev.psi.mi.jami.model.Annotation.CAUTION_MI,
                psidev.psi.mi.jami.model.Annotation.CAUTION);
        return caution != null ? caution.getValue() : null;
    }

    @Override
    public String getInternalRemarkMessage() {
        psidev.psi.mi.jami.model.Annotation caution = AnnotationUtils.collectFirstAnnotationWithTopic(this.participant.getAnnotations(), null,
                "remark-internal");
        return caution != null ? caution.getValue() : null;
    }

    @Override
    public List getAnnotations() {
        return new ArrayList(this.participant.getAnnotations());
    }

    @Override
    public List getAliases() {
        return new ArrayList(this.participant.getAliases());
    }
}