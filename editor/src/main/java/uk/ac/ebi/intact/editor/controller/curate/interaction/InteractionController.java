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
package uk.ac.ebi.intact.editor.controller.curate.interaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.hibernate.Hibernate;
import org.primefaces.model.DualListModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.util.DebugUtil;
import uk.ac.ebi.intact.editor.controller.curate.ParameterizableObjectController;
import uk.ac.ebi.intact.editor.controller.curate.experiment.ExperimentController;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.controller.curate.util.EditorIntactCloner;
import uk.ac.ebi.intact.editor.controller.curate.util.IntactObjectComparator;
import uk.ac.ebi.intact.editor.controller.curate.util.InteractionIntactCloner;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.clone.IntactClonerException;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.IllegalLabelFormatException;
import uk.ac.ebi.intact.model.util.InteractionShortLabelGenerator;

import javax.faces.event.ActionEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class InteractionController extends ParameterizableObjectController {

    private static final Log log = LogFactory.getLog( InteractionController.class );

    private Interaction interaction;
    private String ac;

    private DualListModel<String> experimentLists;
    private List<SelectItem> experimentSelectItems;

    private LinkedList<ParticipantWrapper> participantWrappers;

    private Experiment experiment;
    private List<Experiment> experimentsToUpdate;

    @Autowired
    private PublicationController publicationController;

    @Autowired
    private ExperimentController experimentController;

    public InteractionController() {
        experimentsToUpdate = new ArrayList<Experiment>();
    }

    @Override
    public AnnotatedObject getAnnotatedObject() {
        return getInteraction();
    }

    @Override
    public void setAnnotatedObject(AnnotatedObject annotatedObject) {
        setInteraction((Interaction)annotatedObject);
    }

    public DualListModel<String> getExperimentLists() {
        return experimentLists;
    }

    public void setExperimentLists( DualListModel<String> experimentLists ) {
        this.experimentLists = experimentLists;
    }

    public void loadData( ComponentSystemEvent event ) {
        if ( ac != null ) {
            if ( interaction == null || !ac.equals( interaction.getAc() ) || !Hibernate.isInitialized(interaction.getExperiments())) {
                interaction = loadByAc(IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao(), ac);
            }
        } else {
            ac = interaction.getAc();
        }

        if (interaction == null) {
            addErrorMessage("No interaction with this AC", ac);
            return;
        }

        if( interaction.getExperiments().isEmpty() ) {
            addErrorMessage( "This interaction isn't attached to an experiment", "Plase add one or delete it" );
        } else {

            // check if the publication or experiment are null in their controllers (this happens when the interaction
            // page is loaded directly using a URL)
            if ( publicationController.getPublication() == null ) {
                Publication publication = interaction.getExperiments().iterator().next().getPublication();
                publicationController.setPublication( publication );
            }

            if ( experimentController.getExperiment() == null ) {
                experimentController.setExperiment( interaction.getExperiments().iterator().next() );
            }
        }

        refreshExperimentLists();

        if (interaction != null) {
            if (!Hibernate.isInitialized(interaction.getComponents())) {
                interaction = IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().getByAc( ac );
            }
            refreshParticipants();
        }
    }

    public void refreshExperimentLists() {
        this.experimentSelectItems = new ArrayList<SelectItem>();

        SelectItem selectItem = new SelectItem(null, "Select experiment");
        selectItem.setNoSelectionOption(true);

        experimentSelectItems.add(selectItem);

        if(  interaction.getExperiments().size() > 1 ) {
            addWarningMessage( "There are more than one experiment attached to this interaction",
                    DebugUtil.acList(interaction.getExperiments()).toString());
        }

        if (!interaction.getExperiments().isEmpty()) {
            experiment = interaction.getExperiments().iterator().next();
        }

        if (publicationController.getPublication() != null) {
            final Publication pub = publicationController.getPublication();
            for ( Experiment e : pub.getExperiments() ) {
                experimentSelectItems.add(new SelectItem(e, e.getShortLabel(), e.getFullName()));
            }
        }
    }

    @Override
    public boolean doSaveDetails() {
        if (interaction.getAc() == null) {
            updateShortLabel();
        }

        boolean saved = false;

        for (ParticipantWrapper pw : participantWrappers) {
            Component component = pw.getParticipant();

            if (pw.isDeleted() && component.getAc() != null) {
                interaction.removeComponent(component);

                Component compToDelete;

                if (!getDaoFactory().getEntityManager().contains(component)) {
                    compToDelete = getDaoFactory().getComponentDao().getByAc(component.getAc());
                } else {
                    compToDelete = component;
                }

                if (compToDelete != null) {
                    getDaoFactory().getComponentDao().delete(compToDelete);
                }
            }
            if (component.getAc() == null) {
                getCorePersister().saveOrUpdate(component);
            }

            saved = true;
        }

        for (Experiment experimentToUpdate : experimentsToUpdate) {
            getCorePersister().saveOrUpdate(experimentToUpdate);
        }

        if (experiment != null) {
            experiment = reload(experiment);

            interaction.addExperiment(experiment);
            getDaoFactory().getExperimentDao().update(experiment);

            experimentController.setExperiment(experiment);
        }

        refreshParticipants();

        return saved;
    }

    public void experimentChanged(ValueChangeEvent evt) {
        if (evt.getOldValue() != null) {
            Experiment oldExp = (Experiment) evt.getOldValue();

            oldExp = reload(oldExp);

            interaction.removeExperiment(oldExp);
        }

        Experiment newExp = (Experiment) evt.getNewValue();

        newExp = reload(newExp);

        interaction.addExperiment(newExp);

        experimentsToUpdate.add(newExp);
    }

    private Experiment reload(Experiment oldExp) {
        if (oldExp != null && oldExp.getAc() != null) {
            oldExp = getDaoFactory().getExperimentDao().getByAc(oldExp.getAc());
        }
        return oldExp;
    }

    public String newInteraction(Publication publication, Experiment exp) {
        Interaction interaction = new InteractionImpl();
        interaction.setOwner(getIntactContext().getInstitution());

        CvInteractorType type = getDaoFactory().getCvObjectDao(CvInteractorType.class).getByPsiMiRef(CvInteractorType.INTERACTION_MI_REF);
        interaction.setCvInteractorType(type);

        setInteraction(interaction);

        if (publication != null) {
            publicationController.setPublication(publication);
        }

        if (exp != null) {
            experimentController.setExperiment(exp);
            interaction.addExperiment(exp);
        }

        refreshExperimentLists();
        refreshParticipants();

        return navigateToObject(interaction);
    }

    @Override
    public void modifyClone(AnnotatedObject clone) {
        Interaction clonedInteraction = (Interaction) clone;
        updateShortLabel(clonedInteraction);
    }

    @Override
    protected IntactCloner newClonerInstance() {
        return new InteractionIntactCloner();
    }

    public void refreshParticipants() {
        final Collection<Component> components = interaction.getComponents();
        participantWrappers = new LinkedList<ParticipantWrapper>();

        for ( Component component : components ) {
            participantWrappers.add( new ParticipantWrapper( component, getUnsavedChangeManager() ) );
        }
    }

    public void addParticipant(Component component) {
        interaction.addComponent(component);
        participantWrappers.addFirst(new ParticipantWrapper( component, getUnsavedChangeManager() ));

        try {
            updateShortLabel();
        } catch (Exception e) {
            addErrorMessage("Problem updating shortLabel", e.getMessage());
        }

        getUnsavedChangeManager().markAsUnsaved(interaction);
    }

    public String getAc() {
        if ( ac == null && interaction != null ) {
            return interaction.getAc();
        }
        return ac;
    }

    public int countParticipantsByInteractionAc( String ac ) {
        return getDaoFactory().getInteractionDao().countInteractorsByInteractionAc( ac );
    }

    public Experiment getFirstExperiment( Interaction interaction ) {
        return interaction.getExperiments().iterator().next();
    }

    @Override
    public void doRevertChanges(ActionEvent evt) {
        super.doRevertChanges(evt);

        for (ParticipantWrapper wrapper : participantWrappers) {
            revertParticipant(wrapper);
        }
    }

    public void deleteParticipant(ParticipantWrapper participantWrapper) {
        participantWrapper.setDeleted(true);

        Component participant = participantWrapper.getParticipant();
        //interaction.removeComponent(participant);
        //refreshParticipants();
        setUnsavedChanges(true);

        StringBuilder participantInfo = new StringBuilder();

        if (participant.getInteractor() != null) {
            participantInfo.append(participant.getInteractor().getShortLabel());
            participantInfo.append(" ");
        }

        if (participant.getAc() != null) {
            participantInfo.append("(").append(participant.getAc()+")");
        }

        updateShortLabel();

        addInfoMessage("Participant marked to be removed.", participantInfo.toString());
    }

    public void updateShortLabel() {
        updateShortLabel(getInteraction());
    }

    private void updateShortLabel(Interaction interaction) {
        if (participantWrappers.isEmpty()) {
            return;
        }

        String shortLabel = InteractionShortLabelGenerator.createCandidateShortLabel(interaction);
        try {
            shortLabel = InteractionShortLabelGenerator.nextAvailableShortlabel(shortLabel);
        } catch (IllegalLabelFormatException e) {
            addWarningMessage("Illegal shortLabel", e.getMessage());
            handleException(e);
        }
        interaction.setShortLabel(shortLabel);
    }

    public void revertParticipant(ParticipantWrapper participantWrapper) {
        participantWrapper.setDeleted(false);

        Component participant = participantWrapper.getParticipant();
        setUnsavedChanges(false);

        StringBuilder participantInfo = new StringBuilder();

        if (participant.getInteractor() != null) {
            participantInfo.append(participant.getInteractor().getShortLabel());
            participantInfo.append(" ");
        }

        if (participant.getAc() != null) {
            participantInfo.append("(").append(participant.getAc()+")");
        }
    }

    @Override
    protected void postRevert() {
        refreshParticipants();
    }

    public void cloneParticipant(ParticipantWrapper participantWrapper) {
        Component participant = participantWrapper.getParticipant();

        IntactCloner cloner = new EditorIntactCloner();
        
        try {
            Component clone = cloner.clone(participant);
            addParticipant(clone);
        } catch (IntactClonerException e) {
            addErrorMessage("Problem cloning participant", e.getMessage());
            handleException(e);
        }
    }

    public String getImexId() {
        return findXrefPrimaryId(CvDatabase.IMEX_MI_REF, CvXrefQualifier.IMEX_PRIMARY_MI_REF);
    }

    public void setImexId(String imexId) {
        setXref( CvDatabase.IMEX_MI_REF, CvXrefQualifier.IMEX_PRIMARY_MI_REF, imexId );
    }

    public void setAc( String ac ) {
        this.ac = ac;
    }

    public List<SelectItem> getExperimentSelectItems() {
        return experimentSelectItems;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public void setInteraction( Interaction interaction ) {
        this.interaction = interaction;
        this.ac = interaction.getAc();
    }

    public Collection<ParticipantWrapper> getParticipants() {
        return participantWrappers;
    }

    //////////////////////////////////
    // Participant related methods

    public String getInteractorIdentity(Interactor interactor) {
        if (interactor == null) return null;
        
        final Collection<InteractorXref> identities =
                AnnotatedObjectUtils.searchXrefsByQualifier( interactor, CvXrefQualifier.IDENTITY_MI_REF );
        StringBuilder sb = new StringBuilder(64);
        for ( Iterator<InteractorXref> iterator = identities.iterator(); iterator.hasNext(); ) {
            InteractorXref xref = iterator.next();
            sb.append( xref.getPrimaryId() );
            if( iterator.hasNext() ) {
                sb.append( "|" );
            }
        }
        return sb.toString();
    }

     // Confidence
    ///////////////////////////////////////////////

    public void newConfidence() {
        Confidence confidence = new Confidence();
        interaction.addConfidence(confidence);
    }

    public List<Confidence> getConfidences() {
        if (interaction == null) return Collections.EMPTY_LIST;
        
        final List<Confidence> confidences = new ArrayList<Confidence>( interaction.getConfidences() );
        Collections.sort( confidences, new IntactObjectComparator() );
        return confidences;
    }
}